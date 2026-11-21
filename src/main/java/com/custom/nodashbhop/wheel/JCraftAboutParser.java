package com.custom.nodashbhop.wheel;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Automatically captures and parses JCraft's `/stand about` (and `/spec about`) output
 * so every move in the Ability Wheel displays its full official description + Shift variant,
 * and marks present slots accurately for dynamic wheel sizing.
 */
public class JCraftAboutParser {
    private static final Pattern STRIP_COLOR = Pattern.compile("§[0-9a-fk-orA-FK-OR]");

    private long captureEndTimeMs = 0L;
    private boolean silentCapture = false;
    private WheelSlot lastMatchedSlot = null;

    private final MoveSlotData[] targetSlots;

    public JCraftAboutParser(MoveSlotData[] targetSlots) {
        this.targetSlots = targetSlots;
    }

    public void requestSilentSync(boolean specMode) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;
        lastMatchedSlot = null;
        silentCapture = true;
        captureEndTimeMs = System.currentTimeMillis() + 2400L;
        mc.player.connection.sendCommand(specMode ? "spec about" : "stand about");
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onChatReceived(ClientChatReceivedEvent event) {
        long now = System.currentTimeMillis();
        boolean inWindow = now <= captureEndTimeMs;

        Component msg = event.getMessage();
        if (msg == null) return;
        String raw = STRIP_COLOR.matcher(msg.getString()).replaceAll("").trim();
        if (raw.isEmpty()) return;

        boolean parsedAny = parseBlock(raw);

        if (inWindow && silentCapture && (parsedAny || looksLikeAboutHeader(raw))) {
            event.setCanceled(true);
        }
    }

    private boolean looksLikeAboutHeader(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("moves:") || lower.contains("abilities:")
                || lower.contains("light") || lower.contains("heavy")
                || lower.contains("barrage") || lower.contains("special 1")
                || lower.contains("special 2") || lower.contains("special 3")
                || lower.contains("ultimate") || lower.contains("utility")
                || lower.contains("crouching") || lower.contains("passive")
                || lower.startsWith("===") || lower.startsWith("---");
    }

    private boolean parseBlock(String rawBlock) {
        String[] lines = rawBlock.split("\\r?\\n");
        boolean matchedAnything = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (parseSingleLine(trimmed)) {
                matchedAnything = true;
            }
        }
        return matchedAnything;
    }

    private boolean parseSingleLine(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        boolean isCrouchLine = lower.contains("crouch") || lower.contains("sneak") || lower.contains("shift");

        WheelSlot slot = detectSlotFromLine(lower);
        if (slot != null) {
            lastMatchedSlot = slot;
        } else if (isCrouchLine && lastMatchedSlot != null) {
            slot = lastMatchedSlot;
        } else {
            return false;
        }

        MoveSlotData data = targetSlots[slot.index];
        data.present = true;

        int crouchIdx = findInlineCrouchSplit(line);
        if (crouchIdx > 0) {
            String normalPart = line.substring(0, crouchIdx).trim();
            String crouchPart = line.substring(crouchIdx).trim();
            applyParsedEntry(data, normalPart, false);
            applyParsedEntry(data, crouchPart, true);
            return true;
        }

        applyParsedEntry(data, line, isCrouchLine);
        return true;
    }

    private void applyParsedEntry(MoveSlotData data, String rawEntry, boolean isCrouch) {
        String cleaned = rawEntry
                .replaceAll("^[•\\-*]+\\s*", "")
                .replaceAll("(?i)^(crouching\\s+|crouch\\s+|sneaking\\s+|shift\\s+)?(light|heavy|barrage|special\\s*[123]|sp\\s*[123]|ultimate|ult|utility|util)(\\s*\\([^)]*\\))?\\s*[:\\-–]\\s*", "")
                .replaceAll("(?i)^crouching\\s*[:\\-–]\\s*", "")
                .trim();

        if (cleaned.isEmpty()) return;

        String name = "";
        String desc = cleaned;
        Matcher m = Pattern.compile("^([^:–\\-]{2,36})\\s*[:–\\-]\\s*(.+)$").matcher(cleaned);
        if (m.find()) {
            name = m.group(1).trim();
            desc = m.group(2).trim();
        }

        if (isCrouch) {
            data.hasCrouchVariant = true;
            if (!name.isEmpty()) {
                data.crouchName = name;
            }
            data.crouchDescription = desc;
        } else {
            if (!name.isEmpty()) {
                data.normalName = name;
            }
            data.normalDescription = desc;
        }
    }

    private int findInlineCrouchSplit(String line) {
        Matcher m = Pattern.compile("(?i)(?:\\||\\.)\\s*(crouching|crouch|shift)\\s*[:\\-–]").matcher(line);
        if (m.find()) {
            return m.start(1);
        }
        return -1;
    }

    private WheelSlot detectSlotFromLine(String lower) {
        if (lower.contains("special 3") || lower.contains("special3") || lower.contains("sp3") || lower.contains("(m)")) {
            return WheelSlot.SPECIAL3;
        }
        if (lower.contains("special 2") || lower.contains("special2") || lower.contains("sp2") || lower.contains("(g)")) {
            return WheelSlot.SPECIAL2;
        }
        if (lower.contains("special 1") || lower.contains("special1") || lower.contains("sp1") || lower.contains("(v)")) {
            return WheelSlot.SPECIAL1;
        }
        if (lower.contains("ultimate") || lower.contains("(ult") || lower.contains("ult:") || lower.contains("ult -")) {
            return WheelSlot.ULTIMATE;
        }
        if (lower.contains("utility") || lower.contains("(util") || lower.contains("movement:")) {
            return WheelSlot.UTILITY;
        }
        if (lower.contains("barrage") && (lower.contains("(") || lower.contains(":") || lower.contains("-"))) {
            return WheelSlot.BARRAGE;
        }
        if (lower.contains("heavy") && (lower.contains("(") || lower.contains(":") || lower.contains("-"))) {
            return WheelSlot.HEAVY;
        }
        if ((lower.contains("light") || lower.contains("m1")) && (lower.contains("(") || lower.contains(":") || lower.contains("-"))) {
            return WheelSlot.LIGHT;
        }
        return null;
    }
}

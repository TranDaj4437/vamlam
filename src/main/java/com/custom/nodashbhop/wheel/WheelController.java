package com.custom.nodashbhop.wheel;

import com.custom.nodashbhop.CombinedNetwork;
import com.custom.nodashbhop.JCraftStandCheck;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Core controller for JCraft Ability Wheel v3.3 (built directly against JCraft 0.18.2 internals):
 * - Direct server-side execution via CombinedNetwork.sendWheelCast:
 *     * Fixes SPECIAL1, SPECIAL2, SPECIAL3 100% (works even if unbound in Controls!)
 *     * Spec priority: executes JSpec.initMove directly even while Stand is summoned!
 *     * Instant Stand cast: calls JCraft.summon(player) + stand.initMove in the EXACT same server tick!
 *     * Holdable C key: sends hold/release packets cleanly!
 * - Mandom exact mapping: 2 halves = Countdown (UTILITY, mandom/utility.png) + Rewind (ULTIMATE, mandom/ult.png),
 *   center icon = mandom/ult.png (Rewind), normal M1 left-click untouched!
 */
public class WheelController {
    public static final KeyMapping KEY_CAST = new KeyMapping(
            "key.jcraftwheel.cast",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.jcraftwheel"
    );

    public static final KeyMapping KEY_WHEEL = new KeyMapping(
            "key.jcraftwheel.wheel",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            "key.categories.jcraftwheel"
    );

    private static final long HOLD_THRESHOLD_MS = 175L;
    private static final ResourceLocation MANDOM_REWIND_ICON =
            new ResourceLocation("jcraft", "textures/gui/ability_icons/mandom/ult.png");
    private static final ResourceLocation MANDOM_COUNTDOWN_ICON =
            new ResourceLocation("jcraft", "textures/gui/ability_icons/mandom/utility.png");

    public final MoveSlotData[] slots = new MoveSlotData[8];
    public final JCraftCooldownTracker cooldowns = new JCraftCooldownTracker();
    public final JCraftIconResolver iconResolver = new JCraftIconResolver();
    public final JCraftAboutParser aboutParser;

    private WheelSlot selectedWheelSlot = WheelSlot.HEAVY;
    private boolean specMode = false;
    private boolean hadStandPreviously = false;

    private String lastActiveEntityKey = "";
    private String lastKnownStandFullId = "";
    private int lastStandEntityId = -1;
    private boolean hadSummonedEntity = false;
    private int refreshCooldownTicks = 0;

    // X tap vs hold tracking
    private boolean xWasDown = false;
    private long xPressStartMs = 0L;
    private boolean wheelOpenedThisHold = false;

    // C holdable cast tracking
    private boolean castWasDown = false;
    private WheelSlot heldCastSlot = null;
    private boolean heldCastSpecMode = false;

    public WheelController() {
        for (WheelSlot ws : WheelSlot.values()) {
            slots[ws.index] = new MoveSlotData(ws);
        }
        this.aboutParser = new JCraftAboutParser(slots);
    }

    public boolean isCurrentMandom() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || specMode) return false;
        return StandThemeRegistry.isMandom(JCraftStandCheck.getStandId(mc.player));
    }

    /**
     * Returns ONLY the active non-Light slots for the wheel:
     * - Mandom: strictly 2 halves — Countdown (UTILITY) & Rewind (ULTIMATE) with real mandom PNG icons!
     * - Spec: strictly excludes UTILITY and any slot without a real icon/move
     * - Stand: excludes LIGHT (M1) and any missing slots so remaining moves expand evenly across 360 deg
     */
    public List<MoveSlotData> getActiveSlots() {
        List<MoveSlotData> active = new ArrayList<>();

        if (isCurrentMandom()) {
            MoveSlotData countdown = slots[WheelSlot.UTILITY.index];
            MoveSlotData rewind = slots[WheelSlot.ULTIMATE.index];
            countdown.present = true;
            rewind.present = true;
            if (countdown.normalName == null || countdown.normalName.equals(WheelSlot.UTILITY.defaultName)) {
                countdown.normalName = "Countdown";
            }
            if (rewind.normalName == null || rewind.normalName.equals(WheelSlot.ULTIMATE.defaultName)) {
                rewind.normalName = "Rewind";
            }
            if (countdown.normalDescription == null || countdown.normalDescription.isEmpty()) {
                countdown.normalDescription = "Saves position data of all entities in a 64 block radius for 30 seconds. Must be active to use Rewind.";
            }
            if (rewind.normalDescription == null || rewind.normalDescription.isEmpty()) {
                rewind.normalDescription = "Returns all entities to their saved positions. Requires Countdown to be active.";
            }
            if (countdown.normalIcon == null) countdown.normalIcon = MANDOM_COUNTDOWN_ICON;
            if (rewind.normalIcon == null) rewind.normalIcon = MANDOM_REWIND_ICON;

            active.add(countdown);
            active.add(rewind);
            return active;
        }

        if (specMode) {
            for (int i = 1; i < 7; i++) { // Exclude LIGHT (0) and UTILITY (7)
                MoveSlotData d = slots[i];
                if (d.present && d.normalIcon != null) {
                    active.add(d);
                }
            }
            if (active.isEmpty()) {
                for (int i = 1; i < 7; i++) {
                    if (slots[i].present) active.add(slots[i]);
                }
            }
            if (active.isEmpty()) {
                active.add(slots[WheelSlot.HEAVY.index]);
                active.add(slots[WheelSlot.SPECIAL1.index]);
                active.add(slots[WheelSlot.SPECIAL2.index]);
            }
            return active;
        }

        Minecraft mc = Minecraft.getInstance();
        String standId = mc.player != null ? JCraftStandCheck.getStandId(mc.player).toLowerCase(java.util.Locale.ROOT) : "";
        for (int i = 1; i < 8; i++) {
            if (isSlotRemovedForStand(standId, slots[i].slot)) continue;
            if (slots[i].present) {
                active.add(slots[i]);
            }
        }
        if (active.isEmpty()) {
            for (int i = 1; i < 8; i++) {
                if (isSlotRemovedForStand(standId, slots[i].slot)) continue;
                active.add(slots[i]);
            }
        }
        return active;
    }

    private boolean isSlotRemovedForStand(String standId, WheelSlot slot) {
        if (standId.equals("the_sun") && slot == WheelSlot.BARRAGE) {
            return true;
        }
        if (standId.equals("purple_haze") && slot == WheelSlot.UTILITY) {
            return true;
        }
        return false;
    }

    /**
     * Center hub icon:
     * - Mandom: Rewind icon (`jcraft:textures/gui/ability_icons/mandom/ult.png`)
     * - All other Stands/Specs: Light Punch (M1) icon
     */
    private static final ResourceLocation SPEC_ICON_VAMPIRE = new ResourceLocation("jcraft", "textures/item/stone_mask.png");
    private static final ResourceLocation SPEC_ICON_RANGER = new ResourceLocation("jcraft", "textures/item/holster.png");
    private static final ResourceLocation SPEC_ICON_BRAWLER = new ResourceLocation("jcraft", "textures/item/boxing_gloves.png");
    private static final ResourceLocation SPEC_ICON_ANUBIS = new ResourceLocation("jcraft", "textures/item/anubis.png");

    public MoveSlotData getCenterHubSlotData() {
        Minecraft mc = Minecraft.getInstance();
        if (specMode && mc.player != null) {
            ResourceLocation specHubIcon = resolveSpecCenterIcon(JCraftStandCheck.getSpecId(mc.player));
            if (specHubIcon != null) {
                MoveSlotData custom = new MoveSlotData(WheelSlot.LIGHT);
                custom.present = true;
                custom.normalIcon = specHubIcon;
                custom.crouchIcon = specHubIcon;
                custom.normalName = JCraftStandCheck.getSpecId(mc.player);
                return custom;
            }
        }
        if (isCurrentMandom()) {
            MoveSlotData rewind = slots[WheelSlot.ULTIMATE.index];
            if (rewind.normalIcon == null) rewind.normalIcon = MANDOM_REWIND_ICON;
            return rewind;
        }
        return slots[WheelSlot.LIGHT.index];
    }

    /**
     * Returns the actual JCraft ItemStack to render in the center hub for Vampire, Ranger, Brawler, and Anubis
     * (renders Anubis's real 3D sword item model cleanly via GuiGraphics.renderItem!).
     * Returns ItemStack.EMPTY for Hamon and all Stands.
     */
    public ItemStack getCenterHubItemStack() {
        Minecraft mc = Minecraft.getInstance();
        if (!specMode || mc.player == null) return ItemStack.EMPTY;
        String key = JCraftStandCheck.getSpecId(mc.player).toLowerCase(java.util.Locale.ROOT);
        ResourceLocation itemId = null;
        if (key.contains("vampire")) {
            itemId = new ResourceLocation("jcraft", "stone_mask");
        } else if (key.contains("ranger")) {
            itemId = new ResourceLocation("jcraft", "holster");
        } else if (key.contains("brawler")) {
            itemId = new ResourceLocation("jcraft", "boxing_gloves");
        } else if (key.contains("anubis")) {
            itemId = new ResourceLocation("jcraft", "anubis");
        }
        if (itemId != null) {
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        }
        return ItemStack.EMPTY;
    }

    private ResourceLocation resolveSpecCenterIcon(String specId) {
        if (specId == null) return null;
        String key = specId.toLowerCase(java.util.Locale.ROOT);
        if (key.contains("vampire")) return SPEC_ICON_VAMPIRE;
        if (key.contains("ranger")) return SPEC_ICON_RANGER;
        if (key.contains("brawler")) return SPEC_ICON_BRAWLER;
        if (key.contains("anubis")) return SPEC_ICON_ANUBIS;
        return null;
    }

    public WheelSlot getSelectedWheelSlot() {
        List<MoveSlotData> active = getActiveSlots();
        for (MoveSlotData d : active) {
            if (d.slot == selectedWheelSlot) {
                return selectedWheelSlot;
            }
        }
        selectedWheelSlot = active.get(0).slot;
        return selectedWheelSlot;
    }

    public void setSelectedWheelSlot(WheelSlot slot) {
        if (slot != null && slot != WheelSlot.LIGHT) {
            this.selectedWheelSlot = slot;
        }
    }

    public boolean isSpecMode() {
        return specMode;
    }

    public boolean canSwitchMode(Player player) {
        if (player == null) return false;
        return JCraftStandCheck.hasStand(player) && JCraftStandCheck.hasSpec(player);
    }

    public void toggleMode() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !canSwitchMode(mc.player)) return;
        specMode = !specMode;
        mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.45F, specMode ? 1.25F : 0.95F);
        forceRefresh(mc, true);
    }

    public static boolean isShiftHeldRaw() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return false;
        long win = mc.getWindow().getWindow();
        return InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public static boolean isWheelKeyHeldRaw() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return false;
        InputConstants.Key key = KEY_WHEEL.getKey();
        if (key.getType() == InputConstants.Type.KEYSYM && key.getValue() > 0) {
            return InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue());
        }
        return KEY_WHEEL.isDown();
    }

    public static boolean isCastKeyHeldRaw() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return false;
        InputConstants.Key key = KEY_CAST.getKey();
        if (key.getType() == InputConstants.Type.KEYSYM && key.getValue() > 0) {
            return InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue());
        }
        return KEY_CAST.isDown();
    }

    public String getCastKeyShortLabel() {
        if (KEY_CAST.isUnbound()) return "C";
        String s = KEY_CAST.getTranslatedKeyMessage().getString().trim().toUpperCase(Locale.ROOT);
        return s.length() > 3 ? s.substring(0, 3) : s;
    }

    public String getBoundKeyLabel(WheelSlot slot, boolean isSpec) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return "";
        String targetKeyName = switch (slot) {
            case LIGHT -> "";
            case HEAVY -> "key.jcraft.heavy";
            case BARRAGE -> "key.jcraft.barrage";
            case SPECIAL1 -> "key.jcraft.special1";
            case SPECIAL2 -> "key.jcraft.special2";
            case SPECIAL3 -> "key.jcraft.special3";
            case ULTIMATE -> "key.jcraft.ultimate";
            case UTILITY -> "key.jcraft.utility";
        };
        if (targetKeyName.isEmpty()) return "";
        for (KeyMapping km : mc.options.keyMappings) {
            if (km.getName().equalsIgnoreCase(targetKeyName)) {
                if (km.isUnbound()) return "";
                String label = km.getTranslatedKeyMessage().getString().trim();
                if (label.isEmpty() || label.equalsIgnoreCase("key.keyboard.unknown") || label.equalsIgnoreCase("none")) {
                    return "";
                }
                return label.toUpperCase(Locale.ROOT);
            }
        }
        return "";
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            cooldowns.resetAll();
            lastActiveEntityKey = "";
            lastKnownStandFullId = "";
            lastStandEntityId = -1;
            hadSummonedEntity = false;
            hadStandPreviously = false;
            heldCastSlot = null;
            return;
        }

        // Keep player crouched while holding Shift + Wheel open
        boolean wheelScreenOpen = mc.screen instanceof WheelRadialScreen;
        if (wheelScreenOpen && mc.options != null) {
            mc.options.keyShift.setDown(isShiftHeldRaw());
        }

        boolean hasStand = JCraftStandCheck.hasStand(mc.player);
        boolean hasSpec = JCraftStandCheck.hasSpec(mc.player);
        String currentStandFullId = hasStand ? JCraftStandCheck.getStandFullId(mc.player) : "none";
        boolean standChanged = hasStand && !currentStandFullId.equals(lastKnownStandFullId);

        // Default to STAND whenever player acquires a Stand OR swaps to a new Stand via Stand Arrow / evolution
        if ((hasStand && !hadStandPreviously) || standChanged) {
            specMode = false;
        } else if (!specMode && !hasStand && hasSpec) {
            specMode = true;
        } else if (specMode && !hasSpec && hasStand) {
            specMode = false;
        }
        hadStandPreviously = hasStand;
        lastKnownStandFullId = currentStandFullId;

        String activeId = specMode
                ? "SPEC:" + JCraftStandCheck.getSpecFullId(mc.player)
                : "STAND:" + currentStandFullId;

        Object standEnt = JCraftStandCheck.getStandEntity(mc.player);
        int curStandEntId = (standEnt instanceof net.minecraft.world.entity.Entity e) ? e.getId() : -1;
        boolean summonedNow = specMode
                ? (JCraftStandCheck.getSpecInstance(mc.player) != null)
                : (standEnt != null);

        if (!activeId.equals(lastActiveEntityKey) || standChanged) {
            lastActiveEntityKey = activeId;
            lastStandEntityId = curStandEntId;
            hadSummonedEntity = summonedNow;
            if (hasStand || hasSpec) {
                forceRefresh(mc, true);
            }
        } else if (!specMode && curStandEntId != -1 && curStandEntId != lastStandEntityId) {
            lastStandEntityId = curStandEntId;
            hadSummonedEntity = true;
            iconResolver.populateSlots(mc.player, false, slots, true);
        } else if (summonedNow && !hadSummonedEntity) {
            hadSummonedEntity = true;
            iconResolver.populateSlots(mc.player, specMode, slots, false);
        } else {
            hadSummonedEntity = summonedNow;
            if (refreshCooldownTicks > 0) refreshCooldownTicks--;
        }

        cooldowns.tick(mc.player, specMode);

        if (mc.screen != null && !wheelScreenOpen) {
            xWasDown = false;
            castWasDown = false;
            if (heldCastSlot != null) {
                CombinedNetwork.sendWheelHoldRelease(heldCastSlot.index, heldCastSpecMode);
                heldCastSlot = null;
            }
            return;
        }

        if (!hasStand && !hasSpec) {
            return;
        }

        // 1. Handle X key (Tap = quick cycle, Hold = open Wheel while keeping Shift crouch!)
        boolean xDown = isWheelKeyHeldRaw();
        long now = System.currentTimeMillis();

        if (xDown && !xWasDown) {
            xPressStartMs = now;
            wheelOpenedThisHold = false;
        } else if (xDown && xWasDown) {
            if (!wheelOpenedThisHold && (now - xPressStartMs) >= HOLD_THRESHOLD_MS) {
                wheelOpenedThisHold = true;
                if (!(mc.screen instanceof WheelRadialScreen)) {
                    if (refreshCooldownTicks == 0) {
                        iconResolver.populateSlots(mc.player, specMode, slots, false);
                        refreshCooldownTicks = 40;
                    }
                    boolean shiftBeforeOpen = isShiftHeldRaw();
                    mc.setScreen(new WheelRadialScreen(this));
                    if (shiftBeforeOpen && mc.options != null) {
                        mc.options.keyShift.setDown(true);
                    }
                }
            }
        } else if (!xDown && xWasDown) {
            if (!wheelOpenedThisHold && (now - xPressStartMs) < HOLD_THRESHOLD_MS) {
                cycleNextActiveSlot(mc);
            }
            wheelOpenedThisHold = false;
        }
        xWasDown = xDown;

        // 2. Handle C key (Direct server execution + HOLD support!)
        boolean castDown = isCastKeyHeldRaw();
        if (castDown && !castWasDown && !wheelScreenOpen) {
            WheelSlot target = getSelectedWheelSlot();
            boolean shift = isShiftHeldRaw();
            castSlot(target, shift, true);
        } else if (!castDown && castWasDown) {
            if (heldCastSlot != null) {
                CombinedNetwork.sendWheelHoldRelease(heldCastSlot.index, heldCastSpecMode);
                heldCastSlot = null;
            }
        }
        castWasDown = castDown;
    }

    private void cycleNextActiveSlot(Minecraft mc) {
        List<MoveSlotData> active = getActiveSlots();
        if (active.isEmpty()) return;
        WheelSlot cur = getSelectedWheelSlot();
        int curIdx = 0;
        for (int i = 0; i < active.size(); i++) {
            if (active.get(i).slot == cur) {
                curIdx = i;
                break;
            }
        }
        selectedWheelSlot = active.get((curIdx + 1) % active.size()).slot;
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, 1.35F);
        }
    }

    public void forceRefresh(Minecraft mc, boolean requestAbout) {
        if (mc.player == null) return;
        for (WheelSlot ws : WheelSlot.values()) {
            slots[ws.index].normalDescription = "";
            slots[ws.index].crouchDescription = "";
        }
        iconResolver.populateSlots(mc.player, specMode, slots, true);
        if (requestAbout) {
            aboutParser.requestSilentSync(specMode);
        }
    }

    public void castSlot(WheelSlot slot, boolean shiftRequested) {
        castSlot(slot, shiftRequested, false);
    }

    public void castSlot(WheelSlot slot, boolean shiftRequested, boolean holdWhileC) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || slot == null || slot == WheelSlot.LIGHT) return;

        this.selectedWheelSlot = slot;
        MoveSlotData data = slots[slot.index];
        boolean useShift = shiftRequested && data.hasCrouchVariant;

        // Send directly to server for 100% reliable SPECIAL1/2/3, instant Stand summon+cast, and Spec priority!
        CombinedNetwork.sendWheelCast(slot.index, specMode, useShift, holdWhileC);

        if (holdWhileC) {
            heldCastSlot = slot;
            heldCastSpecMode = specMode;
        }
    }

    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("jwheel")
                        .then(Commands.literal("reload")
                                .executes(ctx -> {
                                    Minecraft mc = Minecraft.getInstance();
                                    iconResolver.invalidateCache();
                                    forceRefresh(mc, true);
                                    if (mc.player != null) {
                                        mc.player.displayClientMessage(
                                                Component.literal("§b[JCraft Wheel] §aĐã đồng bộ lại icon & mô tả từ /stand about!"),
                                                false
                                        );
                                    }
                                    return 1;
                                })
                        )
        );
    }
}

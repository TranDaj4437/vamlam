package com.custom.nodashbhop.wheel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Full 100% Vietnamese translator for all 322 JCraft 0.18.2 + Silver Chariot Requiem 1.5.3
 * move descriptions (loaded from bundled assets/jcraftnodashbhop/lang/vi_moves.json),
 * with an expanded phrase-level fallback for any third-party addons.
 */
public final class JCraftViTranslator {
    private static final Map<String, String> EXACT_MAP = new HashMap<>();
    private static final Map<String, String> PHRASES = new LinkedHashMap<>();

    static {
        loadExactTranslations();

        // Fallback phrase dictionary for any future/custom third-party addon moves
        PHRASES.put("time grinds to a halt!", "Thời gian ngừng trôi!");
        PHRASES.put("stops time itself for", "ngừng hẳn thời gian trong");
        PHRASES.put("stops time for", "ngừng thời gian trong");
        PHRASES.put("fast reliable combo starter/extender", "đòn mở/nối combo nhanh, ổn định");
        PHRASES.put("combo starter/extender", "mở hoặc nối combo");
        PHRASES.put("combo starter", "mở combo");
        PHRASES.put("combo extender", "nối combo");
        PHRASES.put("combo finisher", "kết liễu combo");
        PHRASES.put("high stun", "choáng lâu");
        PHRASES.put("medium stun", "choáng vừa");
        PHRASES.put("low stun", "choáng nhẹ");
        PHRASES.put("unblockable", "không thể đỡ");
        PHRASES.put("uninterruptable", "không thể bị ngắt");
        PHRASES.put("uninterruptible", "không thể bị ngắt");
        PHRASES.put("super armor", "siêu giáp");
        PHRASES.put("hyper armor", "siêu giáp");
        PHRASES.put("block breaker", "phá đỡ");
        PHRASES.put("breaks block", "phá đỡ");
        PHRASES.put("knocks down", "đánh ngã");
        PHRASES.put("knockdown", "đánh ngã");
        PHRASES.put("launches upwards", "hất tung lên trên");
        PHRASES.put("launches", "hất tung");
        PHRASES.put("launcher", "đòn hất tung");
        PHRASES.put("counter", "phản đòn");
        PHRASES.put("windup", "trễ ra đòn");
        PHRASES.put("duration", "duy trì");
        PHRASES.put("cooldown", "hồi chiêu");
        PHRASES.put("on hit", "khi trúng");
        PHRASES.put("on whiff", "khi hụt");
        PHRASES.put("teleports", "dịch chuyển");
        PHRASES.put("teleport", "dịch chuyển");
        PHRASES.put("heals", "hồi máu");
        PHRASES.put("heal", "hồi máu");
        PHRASES.put("damage", "sát thương");
        PHRASES.put("range", "tầm xa");
        PHRASES.put("seconds", "giây");
        PHRASES.put("second", "giây");
        PHRASES.put("stun", "choáng");
        PHRASES.put("knockback", "đẩy lùi");
        PHRASES.put("bleeding", "chảy máu");
        PHRASES.put("poison", "trúng độc");
        PHRASES.put("blindness", "mù");
        PHRASES.put("slowness", "làm chậm");
        PHRASES.put("invisibility", "tàng hình");
        PHRASES.put("invisible", "tàng hình");
        PHRASES.put("projectile", "đạn bắn");
        PHRASES.put("explodes", "phát nổ");
        PHRASES.put("explosion", "vụ nổ");
        PHRASES.put("grab", "tóm giữ");
        PHRASES.put("barrage", "liên hoàn đấm");
        PHRASES.put("clone", "bản sao");
        PHRASES.put("detonate", "kích nổ");
        PHRASES.put("bomb", "bom");
        PHRASES.put("opponent", "đối thủ");
        PHRASES.put("target", "mục tiêu");
        PHRASES.put("enemies", "kẻ địch");
        PHRASES.put("enemy", "kẻ địch");
        PHRASES.put("user", "người dùng");
    }

    private JCraftViTranslator() {}

    private static void loadExactTranslations() {
        try (InputStream is = JCraftViTranslator.class.getResourceAsStream("/assets/jcraftnodashbhop/lang/vi_moves.json")) {
            if (is == null) return;
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Type mapType = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> loaded = new Gson().fromJson(reader, mapType);
                if (loaded != null) {
                    for (Map.Entry<String, String> e : loaded.entrySet()) {
                        if (e.getKey() != null && e.getValue() != null) {
                            EXACT_MAP.put(normalizeKey(e.getKey()), e.getValue());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static String normalizeKey(String raw) {
        return raw.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    public static String translate(String englishText) {
        if (englishText == null || englishText.trim().isEmpty()) {
            return "";
        }

        // 1. Exact full-sentence match from the 322-entry JCraft + SCR Vietnamese dictionary
        String norm = normalizeKey(englishText);
        String exact = EXACT_MAP.get(norm);
        if (exact != null) {
            return exact;
        }
        // Also try stripping trailing period or leading formatting codes for near-exact matches
        if (norm.endsWith(".")) {
            exact = EXACT_MAP.get(norm.substring(0, norm.length() - 1));
            if (exact != null) return exact;
        } else {
            exact = EXACT_MAP.get(norm + ".");
            if (exact != null) return exact;
        }

        // 2. Fallback phrase translator for any custom/third-party addon moves
        String out = englishText.trim();
        for (Map.Entry<String, String> e : PHRASES.entrySet()) {
            Pattern p = Pattern.compile("(?i)" + Pattern.quote(e.getKey()));
            Matcher m = p.matcher(out);
            if (m.find()) {
                out = m.replaceAll(Matcher.quoteReplacement(e.getValue()));
            }
        }
        out = out.replaceAll("\\s{2,}", " ").replaceAll("\\s+([,.!])", "$1").trim();
        if (!out.isEmpty()) {
            out = Character.toUpperCase(out.charAt(0)) + out.substring(1);
        }
        return out;
    }
}

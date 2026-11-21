package com.custom.nodashbhop.wheel;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Provides signature thematic colors for every JCraft Stand/Spec
 * and identifies evolved Stands that receive animated glitch (§k) error characters on both ends.
 */
public final class StandThemeRegistry {
    private static final Map<String, Integer> COLOR_MAP = new HashMap<>();

    static {
        put("star_platinum", 0xFFB366FF);
        put("star_platinum_the_world", 0xFFC084FC);
        put("sptw", 0xFFC084FC);

        put("shadow_the_world", 0xFFA78BFA);
        put("stw", 0xFFA78BFA);
        put("the_world", 0xFFFFD700);
        put("tw", 0xFFFFD700);
        put("the_world_over_heaven", 0xFFFFF3B0);
        put("twoh", 0xFFFFF3B0);
        put("the_world_high_voltage", 0xFFFFB703);
        put("twhv", 0xFFFFB703);

        put("whitesnake", 0xFFD8D0F0);
        put("white_snake", 0xFFD8D0F0);
        put("c_moon", 0xFF34D399);
        put("cmoon", 0xFF34D399);
        put("made_in_heaven", 0xFF7DD3FC);
        put("mih", 0xFF7DD3FC);

        put("gold_experience", 0xFFFACC15);
        put("gold_experience_requiem", 0xFFFFA500);
        put("ger", 0xFFFFA500);

        put("killer_queen", 0xFFF472B6);
        put("killer_queen_bites_the_dust", 0xFFE879F9);
        put("kqbtd", 0xFFE879F9);
        put("killer_queen_alternate_universe", 0xFFFB7185);
        put("kqau", 0xFFFB7185);

        put("king_crimson", 0xFFEF4444);
        put("silver_chariot", 0xFFE2E8F0);
        put("silver_chariot_requiem", 0xFFA78BFA);
        put("magicians_red", 0xFFF97316);
        put("hierophant_green", 0xFF22C55E);
        put("the_hand", 0xFF3B82F6);
        put("the_fool", 0xFFEAB308);
        put("cream", 0xFF9333EA);
        put("d4c", 0xFF38BDF8);
        put("dirty_deeds_done_dirt_cheap", 0xFF38BDF8);
        put("purple_haze", 0xFFA855F7);
        put("purple_haze_distortion", 0xFFD946EF);
        put("metallica", 0xFF94A3B8);
        put("horus", 0xFF67E8F9);
        put("mandom", 0xFFFB7185);
        put("aerosmith", 0xFFF87171);
        put("crazy_diamond", 0xFF60A5FA);

        // Specs
        put("vampire", 0xFFDC2626);
        put("hamon", 0xFFFBBF24);
        put("brawler", 0xFFFB923C);
        put("anubis", 0xFF2DD4BF);
        put("ranger", 0xFFD97706);
        put("spin", 0xFF4ADE80);
    }

    private StandThemeRegistry() {}

    private static void put(String id, int argb) {
        COLOR_MAP.put(id.toLowerCase(Locale.ROOT), argb);
    }

    public static int getThemeColor(String id, boolean isSpec) {
        if (id == null || id.isEmpty()) {
            return isSpec ? 0xFF6EE7B7 : 0xFFF8FAFC;
        }
        String key = normalize(id);
        if (COLOR_MAP.containsKey(key)) {
            return COLOR_MAP.get(key);
        }
        String compact = key.replace("_", "");
        for (Map.Entry<String, Integer> e : COLOR_MAP.entrySet()) {
            if (e.getKey().replace("_", "").equals(compact)) {
                return e.getValue();
            }
        }
        return isSpec ? 0xFF6EE7B7 : 0xFFF8FAFC;
    }

    public static boolean isEvolvedStand(String standId) {
        if (standId == null || standId.isEmpty()) return false;
        String key = normalize(standId);
        return key.contains("over_heaven") || key.contains("overheaven") || key.equals("twoh")
                || key.contains("made_in_heaven") || key.contains("madeinheaven") || key.equals("mih")
                || key.contains("requiem") || key.equals("ger") || key.equals("scr")
                || key.contains("bites_the_dust") || key.contains("bitesthedust") || key.equals("kqbtd") || key.equals("btd")
                || key.contains("star_platinum_the_world") || key.equals("sptw")
                || key.contains("distortion") || key.equals("phd")
                || key.equals("c_moon") || key.equals("cmoon");
    }

    public static boolean isMandom(String standId) {
        if (standId == null || standId.isEmpty()) return false;
        return normalize(standId).contains("mandom");
    }

    private static String normalize(String raw) {
        String key = raw.toLowerCase(Locale.ROOT).trim();
        if (key.contains(":")) key = key.substring(key.lastIndexOf(':') + 1);
        return key.replace("_entity", "").replace("entity", "").replaceAll("_+$", "");
    }
}

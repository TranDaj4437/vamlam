package com.custom.nodashbhop.wheel;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Exact JCraft 0.18.2 (+ Silver Chariot Requiem & custom Purple Haze) evolution requirements.
 * Evolved forms, non-evolving Stands, and Specs return null (no RequireEvo line shown).
 */
public final class StandEvoRegistry {
    private static final Map<String, String> EVO_MAP = new HashMap<>();

    static {
        // 1. Shadow The World -> The World
        put("shadow_the_world", "Diary Page");
        put("shadowtheworld", "Diary Page");
        put("stw", "Diary Page");

        // 2. The World -> The World: Over Heaven
        put("the_world", "DIO's Diary");
        put("theworld", "DIO's Diary");
        put("tw", "DIO's Diary");

        // 3. Whitesnake -> C-Moon
        put("whitesnake", "Green Baby");
        put("white_snake", "Green Baby");
        put("ws", "Green Baby");

        // 4. C-Moon -> Made in Heaven
        put("c_moon", "DIO's Diary");
        put("cmoon", "DIO's Diary");

        // 5. Star Platinum -> Star Platinum: The World
        put("star_platinum", "Living Arrow");
        put("starplatinum", "Living Arrow");
        put("sp", "Living Arrow");

        // 6. Killer Queen -> Killer Queen: Bites the Dust
        put("killer_queen", "Living Arrow");
        put("killerqueen", "Living Arrow");
        put("kq", "Living Arrow");

        // 7. Gold Experience -> Gold Experience Requiem (GER)
        put("gold_experience", "Requiem Arrow");
        put("goldexperience", "Requiem Arrow");
        put("ge", "Requiem Arrow");

        // 8. Purple Haze -> Purple Haze: Distortion (custom: 1 Stand User kill + 1 Flower)
        put("purple_haze", "Flower (1 Stand User Kill)");
        put("purplehaze", "Flower (1 Stand User Kill)");
        put("ph", "Flower (1 Stand User Kill)");

        // 9. Silver Chariot -> Silver Chariot Requiem (SCR) using JCraft's Requiem Arrow
        put("silver_chariot", "Requiem Arrow");
        put("silverchariot", "Requiem Arrow");
        put("sc", "Requiem Arrow");
    }

    private StandEvoRegistry() {}

    private static void put(String id, String item) {
        EVO_MAP.put(id.toLowerCase(Locale.ROOT), item);
    }

    /**
     * Returns the exact required evolution item, or null if the Stand is already evolved or does not evolve.
     */
    public static String getRequiredEvoItem(String standId) {
        if (standId == null || standId.isEmpty()) return null;
        String key = standId.toLowerCase(Locale.ROOT).trim();
        if (key.contains(":")) {
            key = key.substring(key.lastIndexOf(':') + 1);
        }
        key = key.replace("_entity", "").replace("entity", "").replaceAll("_+$", "");

        if (key.contains("over_heaven") || key.contains("overheaven") || key.equals("twoh")
                || key.contains("made_in_heaven") || key.contains("madeinheaven") || key.equals("mih")
                || key.contains("requiem") || key.equals("ger") || key.equals("scr")
                || key.contains("bites_the_dust") || key.contains("bitesthedust") || key.equals("kqbtd") || key.equals("btd")
                || key.contains("alternate_universe") || key.equals("kqau")
                || key.contains("high_voltage") || key.equals("twhv")
                || key.contains("star_platinum_the_world") || key.equals("sptw")
                || key.contains("distortion") || key.equals("phd")) {
            return null;
        }

        if (EVO_MAP.containsKey(key)) {
            return EVO_MAP.get(key);
        }
        String compact = key.replace("_", "").replace("-", "").replace(" ", "");
        if (EVO_MAP.containsKey(compact)) {
            return EVO_MAP.get(compact);
        }
        return null;
    }
}

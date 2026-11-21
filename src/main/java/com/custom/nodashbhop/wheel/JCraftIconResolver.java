package com.custom.nodashbhop.wheel;

import com.custom.nodashbhop.JCraftStandCheck;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Exact JCraft 0.18.2 Ability Icon & MoveMap resolver.
 * Checks JCraft 0.18.2's real path `textures/gui/ability_icons/<stand_or_spec>/<slot>.png` FIRST,
 * then falls back to ResourceManager scanning for custom addons (Crazy Diamond, KQAU, etc.).
 */
public final class JCraftIconResolver {
    private static final String[] MOVE_CLASS_ENUMS = {
            "net.arna.jcraft.api.attack.enums.MoveClass"
    };

    // Exact JCraft 0.18.2 filenames inside textures/gui/ability_icons/<entity>/
    private static final String[] EXACT_SLOT_FILES = {
            "light", "heavy", "barrage", "special1", "special2", "special3", "ult", "utility"
    };

    private static final Map<String, List<String>> STAND_ALIASES = new HashMap<>();

    static {
        addAliases("star_platinum", "star_platinum", "starplatinum", "sp");
        addAliases("star_platinum_the_world", "star_platinum_the_world", "sptw", "sp_tw");
        addAliases("the_world", "the_world", "theworld", "tw");
        addAliases("the_world_over_heaven", "the_world_over_heaven", "twoh", "tw_oh");
        addAliases("shadow_the_world", "shadow_the_world", "stw", "shadow_tw");
        addAliases("the_world_high_voltage", "the_world_high_voltage", "twhv", "tw_hv");
        addAliases("king_crimson", "king_crimson", "kingcrimson", "kc");
        addAliases("killer_queen", "killer_queen", "killerqueen", "kq");
        addAliases("killer_queen_bites_the_dust", "killer_queen_bites_the_dust", "kqbtd", "kq_btd", "btd");
        addAliases("killer_queen_alternate_universe", "killer_queen_alternate_universe", "kqau", "kq_au");
        addAliases("gold_experience", "gold_experience", "goldexperience", "ge");
        addAliases("gold_experience_requiem", "gold_experience_requiem", "ger");
        addAliases("whitesnake", "whitesnake", "white_snake", "ws");
        addAliases("c_moon", "c_moon", "cmoon");
        addAliases("made_in_heaven", "made_in_heaven", "madeinheaven", "mih");
        addAliases("silver_chariot", "silver_chariot", "silverchariot", "sc");
        addAliases("silver_chariot_requiem", "silver_chariot_requiem", "scr");
        addAliases("magicians_red", "magicians_red", "magiciansred", "mr");
        addAliases("hierophant_green", "hierophant_green", "hierophantgreen", "hg");
        addAliases("the_hand", "the_hand", "thehand", "th");
        addAliases("the_fool", "the_fool", "thefool", "fool");
        addAliases("cream", "cream");
        addAliases("d4c", "d4c", "dirty_deeds_done_dirt_cheap");
        addAliases("purple_haze", "purple_haze", "purplehaze", "ph");
        addAliases("purple_haze_distortion", "purple_haze_distortion", "phd", "ph_distortion");
        addAliases("metallica", "metallica");
        addAliases("horus", "horus");
        addAliases("mandom", "mandom");
        addAliases("aerosmith", "aerosmith", "aero");
        addAliases("crazy_diamond", "crazy_diamond", "crazydiamond", "cd");
        addAliases("anubis", "anubis");
        addAliases("brawler", "brawler");
        addAliases("vampire", "vampire", "vamp");
        addAliases("hamon", "hamon");
        addAliases("spin", "spin");
    }

    private static void addAliases(String canonical, String... aliases) {
        List<String> list = Arrays.asList(aliases);
        for (String a : aliases) {
            STAND_ALIASES.put(a.toLowerCase(Locale.ROOT), list);
        }
        STAND_ALIASES.put(canonical.toLowerCase(Locale.ROOT), list);
    }

    private final List<ResourceLocation> indexedTextures = new ArrayList<>();
    private boolean indexed = false;

    public void invalidateCache() {
        indexed = false;
        indexedTextures.clear();
    }

    private void ensureTexturesIndexed() {
        if (indexed) return;
        indexedTextures.clear();
        try {
            ResourceManager rm = Minecraft.getInstance().getResourceManager();
            Map<ResourceLocation, Resource> found = rm.listResources("textures/gui/ability_icons", rl -> rl.getPath().endsWith(".png"));
            indexedTextures.addAll(found.keySet());
        } catch (Exception ignored) {
        }
        indexed = true;
    }

    public void populateSlots(Player player, boolean specMode, MoveSlotData[] slots, boolean resetPresence) {
        ensureTexturesIndexed();
        ResourceManager rm = Minecraft.getInstance().getResourceManager();

        String fullId = specMode ? JCraftStandCheck.getSpecFullId(player) : JCraftStandCheck.getStandFullId(player);
        String shortId = specMode ? JCraftStandCheck.getSpecId(player) : JCraftStandCheck.getStandId(player);
        String namespace = fullId.contains(":") ? fullId.substring(0, fullId.indexOf(':')) : "jcraft";
        String entitySlug = shortId.toLowerCase(Locale.ROOT).replace("_entity", "").replace("entity", "").replaceAll("_+$", "");

        Object attackerObj = specMode
                ? JCraftStandCheck.getSpecInstance(player)
                : JCraftStandCheck.getStandEntity(player);
        Object moveMap = attackerObj != null
                ? JCraftStandCheck.invokeNoArg(attackerObj, "getMoveMap", "getMoves", "moves")
                : null;

        List<String> standTokens = buildStandTokens(entitySlug);

        for (WheelSlot ws : WheelSlot.values()) {
            MoveSlotData data = slots[ws.index];
            if (resetPresence) {
                data.present = false;
                data.normalName = ws.defaultName;
                data.crouchName = "";
                data.hasCrouchVariant = false;
                data.normalIcon = null;
                data.crouchIcon = null;
            }

            // 1. Live reflection from JCraft MoveMap / AbstractMove
            if (moveMap != null) {
                Object normalMove = findMoveInMap(moveMap, ws, false);
                Object crouchMove = findMoveInMap(moveMap, ws, true);

                if (normalMove != null) {
                    data.present = true;
                    String nName = extractMoveName(normalMove);
                    if (!nName.isEmpty()) data.normalName = nName;
                    String nDesc = extractMoveDescription(normalMove);
                    if (!nDesc.isEmpty() && data.normalDescription.isEmpty()) {
                        data.normalDescription = nDesc;
                    }
                }

                if (crouchMove != null && crouchMove != normalMove) {
                    String cName = extractMoveName(crouchMove);
                    if (!cName.isEmpty() && !cName.equalsIgnoreCase(data.normalName)) {
                        data.present = true;
                        data.hasCrouchVariant = true;
                        data.crouchName = cName;
                        String cDesc = extractMoveDescription(crouchMove);
                        if (!cDesc.isEmpty() && data.crouchDescription.isEmpty()) {
                            data.crouchDescription = cDesc;
                        }
                    }
                }
            }

            // 2. Direct exact lookup in JCraft 0.18.2's real folder: textures/gui/ability_icons/<entity>/<slot>.png
            String fileBase = EXACT_SLOT_FILES[ws.index];
            ResourceLocation exactNormal = findExactAbilityIcon(rm, namespace, standTokens, fileBase);
            if (exactNormal != null) {
                data.normalIcon = exactNormal;
                // Only infer presence from PNG when Stand/Spec is unsummoned (moveMap == null).
                // When moveMap != null, MoveMap is the sole authority so ghost icons never create empty slices.
                if (moveMap == null) {
                    data.present = true;
                }
            }

            if (data.hasCrouchVariant) {
                ResourceLocation exactCrouch = findExactAbilityIcon(rm, namespace, standTokens, fileBase + "_crouch");
                if (exactCrouch == null) {
                    exactCrouch = findExactAbilityIcon(rm, namespace, standTokens, "crouch_" + fileBase);
                }
                data.crouchIcon = (exactCrouch != null) ? exactCrouch : data.normalIcon;
            } else {
                ResourceLocation explicitCrouch = findExactAbilityIcon(rm, namespace, standTokens, fileBase + "_crouch");
                if (explicitCrouch != null) {
                    data.hasCrouchVariant = true;
                    data.crouchIcon = explicitCrouch;
                } else {
                    data.crouchIcon = data.normalIcon;
                }
            }
        }
    }

    private ResourceLocation findExactAbilityIcon(ResourceManager rm, String preferredNs,
                                                  List<String> standTokens, String fileBase) {
        String[] namespaces = preferredNs.equals("jcraft")
                ? new String[]{"jcraft"}
                : new String[]{preferredNs, "jcraft"};

        for (String ns : namespaces) {
            for (String token : standTokens) {
                ResourceLocation candidate = new ResourceLocation(ns, "textures/gui/ability_icons/" + token + "/" + fileBase + ".png");
                if (rm.getResource(candidate).isPresent()) {
                    return candidate;
                }
            }
        }

        // Fallback: scan indexed ability_icons textures for addon folders
        for (ResourceLocation rl : indexedTextures) {
            String path = rl.getPath().toLowerCase(Locale.ROOT);
            if (!path.endsWith("/" + fileBase + ".png")) continue;
            for (String token : standTokens) {
                if (token.length() >= 2 && path.contains("/" + token + "/")) {
                    return rl;
                }
            }
        }
        return null;
    }

    private List<String> buildStandTokens(String slug) {
        List<String> out = new ArrayList<>();
        if (slug == null || slug.isEmpty() || slug.equals("none")) return out;
        String clean = slug.toLowerCase(Locale.ROOT);
        out.add(clean);
        out.add(clean.replace("_", ""));
        List<String> known = STAND_ALIASES.get(clean);
        if (known != null) {
            for (String a : known) {
                if (!out.contains(a)) out.add(a);
            }
        }
        return out;
    }

    private Object findMoveInMap(Object moveMap, WheelSlot slot, boolean crouching) {
        if (moveMap == null) return null;
        Class<?> moveClassEnum = findMoveClassEnum();
        if (moveClassEnum == null || !moveClassEnum.isEnum()) return null;

        Object enumVal = matchEnumConstant(moveClassEnum, slot.moveClassTokens);
        if (enumVal == null) return null;

        try {
            for (Method m : moveMap.getClass().getMethods()) {
                String mn = m.getName();
                Class<?>[] params = m.getParameterTypes();
                if ((mn.equals("getEntries") || mn.equals("get"))
                        && params.length == 1
                        && params[0].isAssignableFrom(moveClassEnum)) {
                    Object res = m.invoke(moveMap, enumVal);
                    if (res instanceof Collection<?> col && !col.isEmpty()) {
                        for (Object entry : col) {
                            if (crouching) {
                                Object crouchVar = JCraftStandCheck.invokeNoArg(entry, "getCrouchingVariant", "crouchingVariant");
                                if (crouchVar != null) return unwrapMoveEntry(crouchVar);
                            } else {
                                return unwrapMoveEntry(entry);
                            }
                        }
                    } else if (!crouching && res != null) {
                        return unwrapMoveEntry(res);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Object unwrapMoveEntry(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Optional<?> opt) obj = opt.orElse(null);
        if (obj == null) return null;
        Object inner = JCraftStandCheck.invokeNoArg(obj, "getMove", "move");
        return inner != null ? inner : obj;
    }

    private String extractMoveName(Object move) {
        if (move == null) return "";
        Object val = JCraftStandCheck.invokeNoArg(move, "getName", "name");
        if (val instanceof Component comp) return comp.getString().trim();
        if (val instanceof String s) return s.trim();
        return "";
    }

    private String extractMoveDescription(Object move) {
        if (move == null) return "";
        Object val = JCraftStandCheck.invokeNoArg(move, "getDescription", "description");
        if (val instanceof Component comp) return comp.getString().trim();
        if (val instanceof String s) return s.trim();
        return "";
    }

    private Class<?> findMoveClassEnum() {
        for (String clsName : MOVE_CLASS_ENUMS) {
            try {
                return Class.forName(clsName);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Object matchEnumConstant(Class<?> enumCls, String[] tokens) {
        Object[] constants = enumCls.getEnumConstants();
        if (constants == null) return null;
        for (String tok : tokens) {
            for (Object c : constants) {
                if (c.toString().equalsIgnoreCase(tok)) return c;
            }
        }
        return null;
    }
}

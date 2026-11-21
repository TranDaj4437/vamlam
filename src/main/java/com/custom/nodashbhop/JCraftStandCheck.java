package com.custom.nodashbhop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;

public final class JCraftStandCheck {
    private static final String[] COMPONENT_UTILS_CLASSES = {
            "net.arna.jcraft.platform.JComponentPlatformUtils",
            "net.arna.jcraft.common.component.JComponentPlatformUtils",
            "net.arna.jcraft.api.component.JComponentPlatformUtils"
    };

    private static final String[] COOLDOWN_TYPE_CLASSES = {
            "net.arna.jcraft.common.util.CooldownType",
            "net.arna.jcraft.api.CooldownType"
    };

    private JCraftStandCheck() {}

    public static Object getStandComponent(LivingEntity player) {
        if (player == null) return null;
        return unwrap(getComponent(player, "getStandComponent", "getStandData"));
    }

    public static Object getSpecComponent(LivingEntity player) {
        if (player == null) return null;
        return unwrap(getComponent(player, "getSpecComponent", "getSpecData"));
    }

    public static Object getCooldownComponent(LivingEntity player) {
        if (player == null) return null;
        return unwrap(getComponent(player, "getCooldowns", "getCooldownComponent", "getCooldownData"));
    }

    public static Object getStandEntity(LivingEntity player) {
        Object comp = getStandComponent(player);
        if (comp == null) return null;
        return invokeNoArg(comp, "getStand", "getStandEntity");
    }

    public static Object getSpecInstance(LivingEntity player) {
        Object comp = getSpecComponent(player);
        if (comp == null) return null;
        return invokeNoArg(comp, "getSpec", "getSpecInstance");
    }

    public static String getStandFullId(LivingEntity player) {
        if (player == null) return "none";
        Object standComp = getStandComponent(player);
        if (standComp != null) {
            Object type = invokeNoArg(standComp, "getType", "getStandType");
            String full = extractFullId(type);
            if (!full.equals("unknown")) return full;
            Object standEntity = invokeNoArg(standComp, "getStand");
            if (standEntity != null) {
                return "jcraft:" + standEntity.getClass().getSimpleName()
                        .replace("Entity", "").toLowerCase(Locale.ROOT);
            }
            return "none";
        }
        String nbt = readCapTypeFromNbt(player, "stand", "Type", "StandType");
        return nbt.equals("NONE") ? "none" : "jcraft:" + nbt.toLowerCase(Locale.ROOT);
    }

    public static String getSpecFullId(LivingEntity player) {
        if (player == null) return "none";
        Object specComp = getSpecComponent(player);
        if (specComp != null) {
            Object type = invokeNoArg(specComp, "getType", "getSpecType");
            String full = extractFullId(type);
            if (!full.equals("unknown")) return full;
            Object specInstance = invokeNoArg(specComp, "getSpec");
            if (specInstance != null) {
                String cls = specInstance.getClass().getSimpleName().toLowerCase(Locale.ROOT);
                return cls.contains("none") ? "none" : "jcraft:" + cls.replace("spec", "");
            }
            return "none";
        }
        String nbt = readCapTypeFromNbt(player, "spec", "Type", "SpecType");
        return nbt.equals("NONE") ? "none" : "jcraft:" + nbt.toLowerCase(Locale.ROOT);
    }

    public static String getStandId(LivingEntity player) {
        if (player == null) return "NONE";
        Object standComp = getStandComponent(player);
        if (standComp != null) {
            Object type = invokeNoArg(standComp, "getType", "getStandType");
            String id = extractTypeId(type);
            if (!id.equals("UNKNOWN")) return id;
            Object standEntity = invokeNoArg(standComp, "getStand");
            if (standEntity != null) return standEntity.getClass().getSimpleName().toUpperCase(Locale.ROOT);
            return "NONE";
        }
        return readCapTypeFromNbt(player, "stand", "Type", "StandType");
    }

    public static String getSpecId(LivingEntity player) {
        if (player == null) return "NONE";
        Object specComp = getSpecComponent(player);
        if (specComp != null) {
            Object type = invokeNoArg(specComp, "getType", "getSpecType");
            String id = extractTypeId(type);
            if (!id.equals("UNKNOWN")) return id;
            Object specInstance = invokeNoArg(specComp, "getSpec");
            if (specInstance != null) {
                String cls = specInstance.getClass().getSimpleName().toUpperCase(Locale.ROOT);
                return cls.contains("NONE") ? "NONE" : cls;
            }
            return "NONE";
        }
        return readCapTypeFromNbt(player, "spec", "Type", "SpecType");
    }

    /**
     * Returns ONLY the clean Stand name (e.g., "The World", "Star Platinum") without any "Stand:" or "Name:" prefix.
     */
    public static String getStandDisplayName(LivingEntity player) {
        if (player == null || !hasStand(player)) return "";
        Object standComp = getStandComponent(player);
        if (standComp != null) {
            Object type = invokeNoArg(standComp, "getType", "getStandType");
            String fromData = extractCleanDisplayName(type);
            if (!fromData.isEmpty()) return fromData;

            Object standEntity = invokeNoArg(standComp, "getStand");
            if (standEntity != null) {
                Object nameObj = invokeNoArg(standEntity, "getDisplayName", "getName");
                if (nameObj instanceof Component comp) {
                    String s = comp.getString().trim();
                    if (!s.isEmpty() && !s.contains("entity.")) return s;
                }
            }
        }
        return prettifyId(getStandId(player));
    }

    public static String getSpecDisplayName(LivingEntity player) {
        if (player == null || !hasSpec(player)) return "";
        Object specComp = getSpecComponent(player);
        if (specComp != null) {
            Object type = invokeNoArg(specComp, "getType", "getSpecType");
            String fromData = extractCleanDisplayName(type);
            if (!fromData.isEmpty()) return fromData;
        }
        return prettifyId(getSpecId(player));
    }

    private static String extractCleanDisplayName(Object type) {
        if (type == null) return "";
        Object data = invokeNoArg(type, "getData");
        if (data != null) {
            Object info = invokeNoArg(data, "getInfo", "info");
            if (info != null) {
                Object name = invokeNoArg(info, "getName", "name");
                if (name instanceof Component comp) {
                    String s = comp.getString().trim();
                    if (!s.isEmpty()) return s;
                }
            }
            Object name = invokeNoArg(data, "getName", "name");
            if (name instanceof Component comp) {
                String s = comp.getString().trim();
                if (!s.isEmpty()) return s;
            }
        }
        for (String getter : new String[]{"getName", "getDisplayName", "getTranslationKey"}) {
            Object val = invokeNoArg(type, getter);
            if (val instanceof Component comp) {
                String s = comp.getString().trim();
                if (!s.isEmpty() && !s.contains("@")) return s;
            } else if (val instanceof String key && key.contains(".")) {
                String translated = Component.translatable(key).getString().trim();
                if (!translated.equals(key) && !translated.isEmpty()) return translated;
            }
        }
        return "";
    }

    public static String prettifyId(String rawId) {
        if (rawId == null || rawId.isEmpty() || rawId.equalsIgnoreCase("NONE")) return "";
        String clean = rawId;
        if (clean.contains(":")) clean = clean.substring(clean.lastIndexOf(':') + 1);
        clean = clean.replace("ENTITY", "").replace("_ENTITY", "").trim();
        String[] parts = clean.toLowerCase(Locale.ROOT).split("[_\\s]+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            if (p.equals("d4c") || p.equals("ger") || p.equals("twoh") || p.equals("sptw") || p.equals("kqbtd") || p.equals("kqau") || p.equals("mih")) {
                sb.append(p.toUpperCase(Locale.ROOT));
            } else {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
        }
        return sb.toString();
    }

    public static boolean hasStand(LivingEntity player) {
        return isValidId(getStandId(player));
    }

    public static boolean hasSpec(LivingEntity player) {
        return isValidId(getSpecId(player));
    }

    public static boolean hasStandOrSpec(LivingEntity player) {
        return hasStand(player) || hasSpec(player);
    }

    public static Class<?> findCooldownTypeEnum() {
        for (String name : COOLDOWN_TYPE_CLASSES) {
            try {
                return Class.forName(name);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static boolean lockUniversalCooldowns(LivingEntity player, int ticks) {
        Object cdComp = getCooldownComponent(player);
        if (cdComp == null) return false;
        Class<?> enumCls = findCooldownTypeEnum();
        if (enumCls == null || !enumCls.isEnum()) return false;

        boolean applied = false;
        Object[] constants = enumCls.getEnumConstants();
        if (constants == null) return false;

        for (Object c : constants) {
            String name = c.toString().toUpperCase(Locale.ROOT);
            if (name.equals("DASH") || name.contains("DASH")
                    || name.contains("COMBO_BREAKER") || name.contains("COOLDOWN_CANCEL")) {
                if (invokeSetCooldown(cdComp, enumCls, c, ticks)) {
                    applied = true;
                }
            }
        }
        return applied;
    }

    private static boolean invokeSetCooldown(Object cdComp, Class<?> enumCls, Object enumVal, int ticks) {
        for (String mName : new String[]{"setCooldown", "set", "startCooldown"}) {
            try {
                Method m = cdComp.getClass().getMethod(mName, enumCls, int.class);
                m.invoke(cdComp, enumVal, ticks);
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private static String extractFullId(Object type) {
        if (type == null) return "none";
        Object isNone = invokeNoArg(type, "isNone", "isEmpty");
        if (Boolean.TRUE.equals(isNone)) return "none";
        for (String getter : new String[]{"getId", "id", "getInternalName", "name"}) {
            Object val = invokeNoArg(type, getter);
            if (val != null) {
                String s = val.toString().trim().toLowerCase(Locale.ROOT);
                if (!s.isEmpty() && !s.contains("@")) {
                    return s.contains(":") ? s : "jcraft:" + s;
                }
            }
        }
        String raw = type.toString().trim().toLowerCase(Locale.ROOT);
        if (!raw.isEmpty() && !raw.contains("@")) {
            return raw.contains(":") ? raw : "jcraft:" + raw;
        }
        return "unknown";
    }

    private static String extractTypeId(Object type) {
        if (type == null) return "NONE";
        Object isNone = invokeNoArg(type, "isNone", "isEmpty");
        if (Boolean.TRUE.equals(isNone)) return "NONE";
        for (String getter : new String[]{"getId", "id", "name", "getName", "getInternalName"}) {
            Object val = invokeNoArg(type, getter);
            if (val != null) {
                String s = val.toString().trim();
                if (!s.isEmpty() && !s.contains("@")) return normalizeId(s);
            }
        }
        String raw = type.toString().trim();
        if (!raw.isEmpty() && !raw.contains("@")) return normalizeId(raw);
        return "UNKNOWN";
    }

    private static String normalizeId(String raw) {
        String s = raw.trim();
        if (s.contains(":")) s = s.substring(s.lastIndexOf(':') + 1);
        return s.toUpperCase(Locale.ROOT);
    }

    private static boolean isValidId(String id) {
        if (id == null || id.isEmpty()) return false;
        String u = id.toUpperCase(Locale.ROOT);
        return !u.equals("NONE") && !u.equals("NULL") && !u.equals("EMPTY")
                && !u.equals("UNKNOWN") && !u.endsWith(":NONE") && !u.contains("NONE");
    }

    private static String readCapTypeFromNbt(LivingEntity player, String capKeyword, String... fieldNames) {
        try {
            CompoundTag nbt = player.serializeNBT();
            if (nbt.contains("ForgeCaps")) {
                CompoundTag caps = nbt.getCompound("ForgeCaps");
                for (String key : caps.getAllKeys()) {
                    String lower = key.toLowerCase(Locale.ROOT);
                    if (lower.contains("jcraft") && lower.contains(capKeyword)) {
                        CompoundTag tag = caps.getCompound(key);
                        for (String field : fieldNames) {
                            if (tag.contains(field)) {
                                String val = tag.getString(field);
                                if (!val.isEmpty()) return normalizeId(val);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "NONE";
    }

    private static Object unwrap(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Optional<?> opt) return opt.orElse(null);
        try {
            Method resolve = obj.getClass().getMethod("resolve");
            Object res = resolve.invoke(obj);
            if (res instanceof Optional<?> opt) return opt.orElse(null);
        } catch (Exception ignored) {
        }
        return obj;
    }

    private static Object getComponent(LivingEntity entity, String... methodNames) {
        for (String clsName : COMPONENT_UTILS_CLASSES) {
            try {
                Class<?> cls = Class.forName(clsName);
                for (String mName : methodNames) {
                    for (Method m : cls.getMethods()) {
                        if (m.getName().equals(mName) && m.getParameterCount() == 1) {
                            if (m.getParameterTypes()[0].isAssignableFrom(entity.getClass())) {
                                return m.invoke(null, entity);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static Object invokeNoArg(Object target, String... methodNames) {
        if (target == null) return null;
        for (String mName : methodNames) {
            try {
                Method m = target.getClass().getMethod(mName);
                return m.invoke(target);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}

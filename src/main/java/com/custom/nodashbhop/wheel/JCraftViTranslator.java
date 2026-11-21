package com.custom.nodashbhop.wheel;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Natural Vietnamese translator for JCraft 0.18.2 /stand about and /spec about descriptions.
 */
public final class JCraftViTranslator {
    private static final Map<String, String> PHRASES = new LinkedHashMap<>();

    static {
        // Complete / long JCraft descriptions & common clauses
        PHRASES.put("time grinds to a halt!", "Thời gian ngừng trôi!");
        PHRASES.put("stops time itself for", "ngừng hẳn thời gian trong");
        PHRASES.put("stops time for", "ngừng thời gian trong");
        PHRASES.put("moves through stopped time to appear somewhere else instantly", "dịch chuyển tức thời xuyên qua thời gian ngừng sang vị trí khác");
        PHRASES.put("detaches from the user and lunges forward", "tách khỏi người dùng và lao tới phía trước");
        PHRASES.put("and the user and lunge forward", "cùng người dùng lao mạnh về phía trước");
        PHRASES.put("and the user lunge forward", "cùng người dùng lao mạnh về phía trước");
        PHRASES.put("fast reliable combo starter/extender with high stun", "đòn mở/nối combo nhanh, ổn định và gây choáng lâu");
        PHRASES.put("fast and reliable combo starter/extender with high stun potential", "đòn mở/nối combo nhanh, ổn định, khả năng gây choáng cao");
        PHRASES.put("fast reliable combo starter/extender, medium stun", "đòn mở/nối combo nhanh, ổn định, gây choáng vừa");
        PHRASES.put("slow, uninterruptable combo starter/extender", "ra đòn chậm nhưng không thể bị ngắt, dùng mở/nối combo");
        PHRASES.put("slow, uninterruptable launcher", "đòn hất tung chậm nhưng không thể bị ngắt");
        PHRASES.put("quick combo starter", "đòn mở combo nhanh");
        PHRASES.put("slower but has higher stun", "chậm hơn nhưng gây choáng lâu hơn");
        PHRASES.put("unblockable combo finisher", "đòn kết liễu combo không thể đỡ");
        PHRASES.put("teleports and knocks down on hit", "dịch chuyển áp sát và đánh ngã khi trúng");
        PHRASES.put("creates an automatic, heat-seeking sub-stand that explodes on contact", "triệu hồi Stand phụ tự động đuổi theo mục tiêu và phát nổ khi chạm");
        PHRASES.put("reflects 25% damage back to owner", "phản 25% sát thương ngược lại chủ nhân");
        PHRASES.put("grabs opponent by the face, then detonates them, launching them upwards", "tóm mặt đối thủ rồi kích nổ, hất văng lên trời");
        PHRASES.put("overrides current bomb with an aimable coin", "thay bom hiện tại bằng đồng xu có thể ngắm ném");
        PHRASES.put("crouch to plant on the ground below you, stealthily", "ngồi để đặt bom lén xuống mặt đất dưới chân");
        PHRASES.put("slight aoe damage, 3d movement tool", "gây sát thương diện rộng nhẹ, dùng cơ động 3 chiều");
        PHRASES.put("three attacks that can be chained together; final hit causes knockdown", "chuỗi 3 đòn liên tiếp, đòn cuối gây đánh ngã");
        PHRASES.put("launches 2 sets of 3 capsules in a hexagonal pattern", "bắn 2 đợt (mỗi đợt 3 viên nang) theo hình lục giác");
        PHRASES.put("launches 3 capsules simultaneously", "bắn 3 viên nang cùng lúc");
        PHRASES.put("fires a single, rapid capsule at the target", "bắn nhanh 1 viên nang vào mục tiêu");
        PHRASES.put("launches vertically and infects targets on hit for", "hất tung thẳng đứng và gây nhiễm độc khi trúng trong");
        PHRASES.put("allows charging of a speedometer for", "tích thanh tốc độ trong");
        PHRASES.put("charged by landing hits", "tích đầy bằng cách đánh trúng");
        PHRASES.put("all surrounding enemies become standless for", "toàn bộ kẻ địch xung quanh bị mất Stand trong");
        PHRASES.put("combo starter/extender", "mở hoặc nối combo");
        PHRASES.put("can combo start", "có thể mở combo");
        PHRASES.put("combo starter", "mở combo");
        PHRASES.put("combo extender", "nối combo");
        PHRASES.put("combo finisher", "kết liễu combo");
        PHRASES.put("fast poke, low stun", "đòn rỉa nhanh, gây choáng nhẹ");
        PHRASES.put("high stun", "choáng lâu");
        PHRASES.put("medium stun", "choáng vừa");
        PHRASES.put("low stun", "choáng nhẹ");
        PHRASES.put("stun on whiff", "bị choáng nếu đánh hụt");
        PHRASES.put("if the user whiffs this attack", "nếu đánh hụt đòn này");
        PHRASES.put("on hit the user gets", "khi đánh trúng, người dùng nhận");
        PHRASES.put("and the victim gets", "và mục tiêu bị dính");
        PHRASES.put("unblockable", "không thể đỡ");
        PHRASES.put("uninterruptable", "không thể bị ngắt");
        PHRASES.put("uninterruptible", "không thể bị ngắt");
        PHRASES.put("super armor", "siêu giáp");
        PHRASES.put("hyper armor", "siêu giáp");
        PHRASES.put("block breaker", "phá đỡ");
        PHRASES.put("breaks block", "phá đỡ");
        PHRASES.put("guard break", "phá phòng thủ");
        PHRASES.put("knocks down on hit", "đánh ngã khi trúng");
        PHRASES.put("causes knockdown", "gây đánh ngã");
        PHRASES.put("knocks down", "đánh ngã");
        PHRASES.put("knockdown", "đánh ngã");
        PHRASES.put("launches opponents into the air", "hất tung đối thủ lên không");
        PHRASES.put("launches upwards", "hất văng lên trên");
        PHRASES.put("launches", "hất tung");
        PHRASES.put("counter", "phản đòn");
        PHRASES.put("windup", "trễ ra đòn");
        PHRASES.put("duration", "duy trì");
        PHRASES.put("cooldown", "hồi chiêu");
        PHRASES.put("pilot mode", "chế độ điều khiển từ xa");
        PHRASES.put("time erase", "xóa thời gian");
        PHRASES.put("time acceleration", "gia tốc thời gian");
        PHRASES.put("dimension hop", "nhảy chiều không gian");
        PHRASES.put("deals damage over time", "gây sát thương theo thời gian");
        PHRASES.put("removes status effects", "xóa toàn bộ hiệu ứng");
        PHRASES.put("inflicts blindness and slowness", "gây mù và làm chậm");
        PHRASES.put("mining fatigue", "đào chậm");
        PHRASES.put("haste", "tăng tốc đánh");
        PHRASES.put("speed", "tốc độ");
        PHRASES.put("on hit", "khi trúng");
        PHRASES.put("on whiff", "khi hụt");
        PHRASES.put("simultaneously", "cùng lúc");
        PHRASES.put("fires a", "bắn ra");
        PHRASES.put("throws a", "ném ra");
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
        PHRASES.put("ground slam", "đập mạnh xuống đất");
        PHRASES.put("grab", "tóm giữ");
        PHRASES.put("barrage", "liên hoàn đấm");
        PHRASES.put("clone", "bản sao");
        PHRASES.put("knives", "phi dao");
        PHRASES.put("knife", "dao");
        PHRASES.put("detonate", "kích nổ");
        PHRASES.put("bomb", "bom");
        PHRASES.put("stand user", "người dùng Stand");
        PHRASES.put("opponent", "đối thủ");
        PHRASES.put("target", "mục tiêu");
        PHRASES.put("enemy", "kẻ địch");
        PHRASES.put("enemies", "kẻ địch");
        PHRASES.put("user", "người dùng");
        PHRASES.put("self-explanatory", "kích hoạt hiệu ứng như tên chiêu");
    }

    private JCraftViTranslator() {}

    public static String translate(String englishText) {
        if (englishText == null || englishText.trim().isEmpty()) {
            return "";
        }
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

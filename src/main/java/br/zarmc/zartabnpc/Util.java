package br.zarmc.zartabnpc;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Util {
    private static final LegacyComponentSerializer SER =
            LegacyComponentSerializer.builder().character('&').hexColors().build();

    private static final Pattern SECTION_HEX = Pattern.compile(
            "\u00a7x\u00a7([0-9a-fA-F])\u00a7([0-9a-fA-F])\u00a7([0-9a-fA-F])\u00a7([0-9a-fA-F])\u00a7([0-9a-fA-F])\u00a7([0-9a-fA-F])");

    private Util() {
    }

    /** Converte texto com &a, &l e &#RRGGBB em Component. */
    public static Component color(String s) {
        if (s == null || s.isEmpty()) {
            return Component.empty();
        }
        Matcher m = SECTION_HEX.matcher(s);
        s = m.replaceAll("&#$1$2$3$4$5$6");
        s = s.replace('\u00a7', '&');
        return SER.deserialize(s);
    }

    public static boolean hasPapi() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    /** Aplica placeholders proprios e do PlaceholderAPI. */
    public static String apply(Player p, String s) {
        if (s == null) {
            return "";
        }
        s = s.replace("%player%", p.getName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max%", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("%ping%", String.valueOf(p.getPing()))
                .replace("%world%", p.getWorld().getName());
        if (hasPapi()) {
            s = PlaceholderAPI.setPlaceholders(p, s);
        }
        return s;
    }

    /** Aceita base64 (Value do minecraft-heads), URL de textura ou nick. */
    public static String normalizeTexture(String in) {
        if (in.startsWith("http://") || in.startsWith("https://")) {
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + in + "\"}}}";
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        }
        return in;
    }
}

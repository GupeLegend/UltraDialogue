package mc.gupe.ultradialogue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/** Colores y placeholders. PlaceholderAPI es opcional: se busca por reflexion. */
public final class Text {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder().character('&').hexColors().build();
    private static Method papi;
    private static boolean papiSearched;

    private Text() {}

    private static boolean mini() {
        return "minimessage".equalsIgnoreCase(UltraDialogue.get().config().getString("format", "legacy"));
    }

    public static Component color(String s) {
        if (s == null) return Component.empty();
        Component c = mini() ? MiniMessage.miniMessage().deserialize(s) : LEGACY.deserialize(s);
        return c.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static Component color(Player p, String s) {
        return color(ph(p, s));
    }

    public static String ph(Player p, String s) {
        if (s == null) return "";
        if (p != null) s = s.replace("%player%", p.getName()).replace("{player}", p.getName());
        if (p == null || s.indexOf('%') < 0) return s;
        if (!papiSearched) {
            papiSearched = true;
            try {
                papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI")
                        .getMethod("setPlaceholders", org.bukkit.OfflinePlayer.class, String.class);
            } catch (Throwable ignored) { papi = null; }
        }
        if (papi == null) return s;
        try { return (String) papi.invoke(null, p, s); } catch (Throwable t) { return s; }
    }

    /** Mensaje del idioma con prefijo, ya coloreado. */
    public static Component msg(String key, String... pairs) {
        return color(UltraDialogue.get().lang().msg(key, pairs));
    }
}

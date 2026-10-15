package mc.gupe.ultradialogue.gui;

import mc.gupe.ultradialogue.UltraDialogue;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPG buttons for layout mode, drawn with the ultradialogue:boton font from the plugin's own pack.
 *
 * A button is: background (cap + stretches + cap), a negative space back to the start, the border
 * on top (tinted with the border colour), another step back and the centred label. Each pack
 * "letter" advances its width + 1, so every piece is followed by a -1 space.
 *
 * The slim pack bundled since 1.1.3 only ships the "liso" background and the "doble" border: those
 * are the only two that actually have glyphs. {@link #resolveFill} and {@link #resolveBorder} fall
 * back to them (with a one-line, once-per-name console notice) for any other name a dialogue asks
 * for, so an old file written against the full pack never breaks or shows a blank button.
 *
 * The index of each entry in {@link #FILLS} and {@link #BORDERS} is the character offset the pack
 * generator used: new entries only ever go at the end, the existing ones never move.
 */
public final class Buttons {

    public static final Key FONT = Key.key("ultradialogue", "boton");

    public static final List<String> FILLS = List.of("madera_oscura", "madera_clara", "pergamino", "piedra", "hierro",
            "obsidiana", "nether", "mar", "sombra", "tinta", "liso");
    public static final List<String> BORDERS = List.of("simple", "grueso", "doble", "remaches", "tallado",
            "contorno", "punteado", "esquinas", "marco", "linea");
    private static final String DEFAULT_FILL = "liso";
    private static final String DEFAULT_BORDER = "doble";
    private static final Set<String> AVAILABLE_FILLS = Set.of(DEFAULT_FILL);
    private static final Set<String> AVAILABLE_BORDERS = Set.of(DEFAULT_BORDER);
    private static final Set<String> warnedOnce = ConcurrentHashMap.newKeySet();

    public static final int CAP = 4;

    private static final int FILL_BASE = 0xE000, BORDER_BASE = 0xE400, SPACE_POS = 0xE800, SPACE_NEG = 0xE810;
    // Frame mode: a copy of "liso" above and below the label, in the extended private-use plane.
    private static final int FRAME_TOP = 0xF2000, FRAME_BOT = 0xF2400;
    private static final char NEG1 = (char) SPACE_NEG;

    private Buttons() {}

    /** Whether this player gets the pack-drawn buttons (otherwise they show as "[ text ]"). */
    public static boolean enabled(Player p) {
        if (p == null || Screen.oldClient(p)) return false;
        String mode = UltraDialogue.get().config().getString("screen.layout.buttons", "auto").toLowerCase(Locale.ROOT);
        if (mode.equals("text")) return false;
        if (mode.equals("pack")) return true;
        mc.gupe.ultradialogue.pack.PackManager pack = UltraDialogue.get().pack();
        return pack != null && pack.loaded(p);
    }

    /** The background actually drawn: anything the slim pack doesn't ship falls back to "liso". */
    public static String resolveFill(String s) {
        String f = s == null ? DEFAULT_FILL : s.toLowerCase(Locale.ROOT);
        if (AVAILABLE_FILLS.contains(f)) return f;
        warnFallback("style", f, DEFAULT_FILL);
        return DEFAULT_FILL;
    }

    /** The border actually drawn: anything the slim pack doesn't ship falls back to "doble". */
    public static String resolveBorder(String s) {
        String b = s == null ? DEFAULT_BORDER : s.toLowerCase(Locale.ROOT);
        if (AVAILABLE_BORDERS.contains(b) || isNone(b)) return b;
        warnFallback("border", b, DEFAULT_BORDER);
        return DEFAULT_BORDER;
    }

    private static void warnFallback(String kind, String name, String fallback) {
        if (!name.equals(fallback) && warnedOnce.add(kind + ":" + name))
            UltraDialogue.get().getLogger().info("Button " + kind + " '" + name + "' is not in the slim resource "
                    + "pack; using '" + fallback + "' instead.");
    }

    private static boolean isNone(String s) { return s.equals("ninguno") || s.equals("none"); }

    public static boolean validFill(String s) { return s == null || FILLS.contains(s.toLowerCase(Locale.ROOT)); }

    public static boolean validBorder(String s) {
        if (s == null) return true;
        String b = s.toLowerCase(Locale.ROOT);
        return BORDERS.contains(b) || isNone(b);
    }

    /** The least a button can measure so its label doesn't touch the border. */
    public static int minWidth(int labelWidth) { return labelWidth + 34; }

    /**
     * screen.layout.look: "frame" = the border texture only on the frame, "liso" filling the inside;
     * "filled" = the background fills the whole button.
     */
    public static boolean frameMode() {
        return "frame".equalsIgnoreCase(UltraDialogue.get().config().getString("screen.layout.look", "filled"));
    }

    /**
     * @param fill        background name
     * @param border      border name, or "ninguno"
     * @param borderColor border colour
     * @param fillColor   tints the background (null = its own colour)
     */
    public static Component box(String fill, String border, TextColor borderColor, TextColor fillColor,
                         int width, Component label, int labelWidth) {
        width = Math.max(width, 2 * CAP);
        if (frameMode()) return frame(borderColor, fillColor, width, label, labelWidth);
        String f = resolveFill(fill);
        String b = resolveBorder(border);

        List<Component> parts = new ArrayList<>();
        int fi = FILLS.indexOf(f);
        TextColor fc = fillColor != null ? fillColor : NamedTextColor.WHITE;
        parts.add(glyphs(strip(FILL_BASE + fi * 16, width) + space(-width), fc));
        if (!isNone(b)) {
            int bi = BORDERS.indexOf(b);
            parts.add(glyphs(strip(BORDER_BASE + bi * 16, width) + space(-width), borderColor));
        }
        int before = Math.max(0, (width - labelWidth) / 2);
        parts.add(glyphs(space(before), NamedTextColor.WHITE));
        parts.add(label);
        parts.add(glyphs(space(width - before - labelWidth), NamedTextColor.WHITE));
        return Component.textOfChildren(parts.toArray(new Component[0]));
    }

    /**
     * Frame mode: "liso" drawn full height (two copies, top and bottom), the label centred on top
     * tinted by innerColor, and the border colour as a thin accent line (the slim pack only ships
     * "doble" for this, regardless of the configured border).
     */
    private static Component frame(TextColor lineColor, TextColor innerColor, int width, Component label, int labelWidth) {
        width = Math.max(width, 2 * CAP + 2);
        int fi = FILLS.indexOf(DEFAULT_FILL);
        List<Component> parts = new ArrayList<>();
        parts.add(glyphs(strip(FRAME_TOP + fi * 16, width) + space(-width), NamedTextColor.WHITE));
        parts.add(glyphs(strip(FRAME_BOT + fi * 16, width) + space(-width), NamedTextColor.WHITE));
        int liso = FILL_BASE + fi * 16;
        parts.add(glyphs(space(CAP) + strip(liso, width - 2 * CAP) + space(-(width - CAP)),
                innerColor != null ? innerColor : NamedTextColor.WHITE));
        int bi = BORDERS.indexOf(DEFAULT_BORDER);
        parts.add(glyphs(strip(BORDER_BASE + bi * 16, width) + space(-width), lineColor));
        int before = Math.max(0, (width - labelWidth) / 2);
        parts.add(glyphs(space(before), NamedTextColor.WHITE));
        parts.add(label);
        parts.add(glyphs(space(width - before - labelWidth), NamedTextColor.WHITE));
        return Component.textOfChildren(parts.toArray(new Component[0]));
    }

    /** Fixed-width gap in pixels (negative = step back). */
    public static Component gap(int px) { return glyphs(space(px), NamedTextColor.WHITE); }

    private static Component glyphs(String s, TextColor color) {
        return Component.text(s).font(FONT).color(color).shadowColor(ShadowColor.none());
    }

    /** Left cap + 128..1 stretches + right cap, each followed by its own -1. */
    private static String strip(int base, int width) {
        StringBuilder sb = new StringBuilder();
        sb.appendCodePoint(base).append(NEG1);
        int rest = width - 2 * CAP;
        for (int i = 7; i >= 0; i--) {
            int w = 1 << i;
            while (rest >= w) {
                sb.appendCodePoint(base + 2 + i).append(NEG1);
                rest -= w;
            }
        }
        sb.appendCodePoint(base + 1).append(NEG1);
        return sb.toString();
    }

    private static String space(int px) {
        StringBuilder sb = new StringBuilder();
        int base = px < 0 ? SPACE_NEG : SPACE_POS;
        int left = Math.abs(px);
        for (int i = 10; i >= 0; i--) {
            int v = 1 << i;
            while (left >= v) { sb.append((char) (base + i)); left -= v; }
        }
        return sb.toString();
    }
}

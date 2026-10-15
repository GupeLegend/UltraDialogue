package mc.gupe.ultradialogue.util;

import java.io.InputStream;

/**
 * Width of every Minecraft font character, in pixels, so the "layout" text buttons line up without
 * a resource pack.
 *
 * glyph-widths.bin: 65536 bytes, one per character, in HALF pixels (unifont advances by halves).
 * Generated from the 26.1.2 client font (bitmaps + space.json + unifont). If Mojang changes the
 * font this has to be regenerated, or rows will drift by a pixel or two.
 */
public final class Glyphs {

    private static final byte[] HALF = new byte[65536];

    static {
        try (InputStream in = Glyphs.class.getResourceAsStream("/glyph-widths.bin")) {
            if (in != null) in.readNBytes(HALF, 0, HALF.length);
        } catch (Exception ignored) {}
    }

    private Glyphs() {}

    /** Width in half-pixels. Anything not in the table is 6px (latin) or 9px (symbols). */
    private static int half(char c) {
        int h = HALF[c] & 0xFF;
        if (h != 0) return h;
        return c < 0x2000 ? 12 : 18;
    }

    public static int width(char c) { return (half(c) + 1) / 2; }

    /**
     * Width in pixels of a string with legacy ('&', section) codes or MiniMessage tags. Bold adds
     * 1px per letter, same as the game.
     */
    public static int width(String s) {
        if (s == null) return 0;
        int h = 0;
        boolean bold = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < s.length()) {
                char k = Character.toLowerCase(s.charAt(i + 1));
                if (k == '#' && i + 7 < s.length()) { bold = false; i += 7; continue; }
                if (k == 'l') { bold = true; i++; continue; }
                if ("0123456789abcdefr".indexOf(k) >= 0) { bold = false; i++; continue; }
                if ("kmnox".indexOf(k) >= 0) { i++; continue; }
            }
            if (c == '<') {
                int end = s.indexOf('>', i);
                if (end > i) {
                    String tag = s.substring(i + 1, end).toLowerCase();
                    if (tag.equals("bold") || tag.equals("b")) bold = true;
                    else if (tag.equals("/bold") || tag.equals("/b") || tag.equals("reset")) bold = false;
                    i = end;
                    continue;
                }
            }
            h += half(c) + (bold && c != ' ' ? 2 : 0);
        }
        return (h + 1) / 2;
    }
}

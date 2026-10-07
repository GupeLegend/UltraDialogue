package mc.gupe.ultradialogue;

import java.io.InputStream;

/**
 * Ancho de cada letra de la fuente de Minecraft, para alinear los botones de texto del modo
 * "layout" sin resource pack.
 *
 * glyph-widths.bin: 65536 bytes, uno por caracter, en MEDIOS pixeles (unifont avanza de a medio).
 * Se genera desde la fuente del cliente 26.1.2 (bitmaps + space.json + unifont). Si Mojang cambia
 * la fuente, hay que regenerarlo o las filas se corren unos pixeles.
 */
final class Glyphs {

    private static final byte[] HALF = new byte[65536];

    static {
        try (InputStream in = Glyphs.class.getResourceAsStream("/glyph-widths.bin")) {
            if (in != null) in.readNBytes(HALF, 0, HALF.length);
        } catch (Exception ignored) {}
    }

    private Glyphs() {}

    /** Ancho en medios pixeles. Lo que no esta en la tabla mide 6 px (latino) o 9 (simbolos). */
    private static int half(char c) {
        int h = HALF[c] & 0xFF;
        if (h != 0) return h;
        return c < 0x2000 ? 12 : 18;
    }

    static int width(char c) { return (half(c) + 1) / 2; }

    /**
     * Ancho en pixeles de un texto con codigos & (legacy) o etiquetas <..> (MiniMessage). La negrita
     * suma 1 por letra, como en el juego.
     */
    static int width(String s) {
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

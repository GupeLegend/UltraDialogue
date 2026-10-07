package mc.gupe.ultradialogue;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Botones RPG del modo layout, dibujados con la fuente ultradialogue:boton del pack del plugin.
 *
 * Cada boton es: fondo (tapa + tramos + tapa), espacio negativo hasta el principio, borde encima
 * (teñido con el color del texto), otro retroceso y la etiqueta centrada. Las "letras" del pack
 * avanzan su ancho + 1, por eso cada pieza va seguida de un espacio de -1.
 *
 * ⚠ El orden de FILLS y BORDERS es el de generar_pack.py: el indice es el caracter. Lo nuevo va
 * siempre al final de la lista.
 */
final class Buttons {

    static final Key FONT = Key.key("ultradialogue", "boton");
    /** Fondos dibujados por el plugin (con tapas redondeadas). */
    static final List<String> FILLS = List.of("madera_oscura", "madera_clara", "pergamino", "piedra", "hierro",
            "obsidiana", "nether", "mar", "sombra", "tinta", "liso");
    /**
     * Fondos que apuntan a texturas de bloques del propio Minecraft del jugador (el pack no las
     * lleva). Cada textura de 16 px viene cortada en tramos de 16, 8, 4, 2 y 1.
     */
    static final List<String> VANILLA = List.of(
            "aldea_llanura", "aldea_desierto", "aldea_sabana", "aldea_taiga", "aldea_nieve", "aldea_pantano",
            "aldea_jungla", "aldea_cerezo", "aldea_bambu", "aldea_abedul", "aldea_roble_oscuro",
            "aldea_roble_palido", "troncos", "playa", "arenisca", "arena_roja", "badlands", "terracota_roja",
            "nieve", "hielo", "hielo_azul", "pantano", "pantano_ladrillo", "musgo", "champinon",
            "hongo_rojo", "oceano", "oceano_ladrillo", "oceano_oscuro", "cueva", "cueva_musgo",
            "piedra_lisa", "ladrillo_piedra", "profundidades", "pizarra", "toba", "calcita",
            "oscuridad_profunda", "amatista", "nether_yermo", "bosque_carmesi", "nylium_carmesi",
            "bosque_distorsionado", "nylium_distorsionado", "valle_almas", "tierra_almas", "deltas_basalto",
            "basalto_pulido", "bastion", "piedra_negra", "piedra_negra_dorada", "fortaleza",
            "ladrillo_nether_rojo", "magma", "verruga", "obsidiana_llorona", "end", "end_ladrillo", "purpur",
            "obsidiana_real", "biblioteca", "granja", "barril", "bloque_oro", "bloque_hierro",
            "bloque_diamante", "bloque_esmeralda", "bloque_lapislazuli", "bloque_cobre", "bloque_netherita",
            "cuarzo", "lana", "hormigon");
    static final List<String> BORDERS = List.of("simple", "grueso", "doble", "remaches", "tallado",
            "contorno", "punteado", "esquinas", "marco", "linea");
    static final int CAP = 4;
    /**
     * Botones ESCENA: 24 px de alto con su PROPIO marco (no se les dibuja borde aparte), tapas de 28 px
     * con siluetas del lugar y fondo en degradado. Orden = escenas.ORDEN (el indice es el caracter).
     * Mandan sobre los estilos viejos del mismo nombre (aldea_llanura, nether_yermo, biblioteca...).
     */
    static final List<String> SCENES = List.of(
            "aldea_llanura", "aldea_desierto", "aldea_sabana", "aldea_taiga", "aldea_nieve", "llanura",
            "llanura_girasoles", "llanura_nevada", "picos_hielo", "desierto", "pantano", "manglar", "bosque",
            "bosque_flores", "bosque_abedul", "bosque_abedul_antiguo", "bosque_oscuro", "jardin_palido",
            "taiga", "taiga_nevada", "taiga_pinos_antigua", "taiga_abetos_antigua", "sabana",
            "meseta_sabana", "sabana_ventosa", "colinas_ventosas", "colinas_grava", "bosque_ventoso",
            "jungla", "jungla_escasa", "jungla_bambu", "badlands", "badlands_erosionadas",
            "badlands_boscosas", "pradera", "arboleda_cerezos", "arboleda", "laderas_nevadas",
            "picos_helados", "picos_escarpados", "picos_rocosos", "rio", "rio_helado", "playa",
            "playa_nevada", "costa_rocosa", "oceano_calido", "oceano_templado", "oceano_templado_profundo",
            "oceano", "oceano_profundo", "oceano_frio", "oceano_frio_profundo", "oceano_helado",
            "oceano_helado_profundo", "campos_champinon", "cuevas_goteo", "cuevas_frondosas",
            "oscuridad_profunda", "nether_yermo", "bosque_carmesi", "bosque_distorsionado", "valle_almas",
            "deltas_basalto", "islas_end", "ciudad_end", "biblioteca", "granja", "bloque_esmeralda",
            "principal");
    static final int SCAP = 28;

    private static final int FILL_BASE = 0xE000, BORDER_BASE = 0xE400, SPACE_POS = 0xE800, SPACE_NEG = 0xE810,
            VANILLA_BASE = 0xEA00;
    // Modo marco: copias de cada fondo arriba (filas 0-15) y abajo (8-23), en el plano privado extendido.
    private static final int FRAME_TOP_V = 0xF0000, FRAME_BOT_V = 0xF1000, FRAME_TOP_P = 0xF2000, FRAME_BOT_P = 0xF2400;
    // Version ORIGINAL de cada fondo vanilla: motivo propio dibujado a mano (NO la textura real ni
    // un recorte de ella), con la paleta de ese bloque. Es el fondo por defecto (screen.layout.textures:
    // minimal); "vanilla" sigue existiendo como modo de compatibilidad opcional con la textura real.
    private static final int MINIMAL_BASE = 0xF3000;
    private static final int SCENE_BASE = 0xF6000;
    // Modo marco de esa misma version ORIGINAL (mismo esquema de 16 por estilo que FRAME_TOP_P/BOT_P).
    private static final int FRAME_TOP_M = 0xF4000, FRAME_BOT_M = 0xF4800;

    /** screen.layout.textures: "minimal" = motivo propio dibujado a mano por el plugin (por defecto, el diseño
     * original de esta version), "vanilla" = la textura real del bloque (modo de compatibilidad opcional). */
    static boolean minimalTextures() {
        return !"vanilla".equalsIgnoreCase(UltraDialogue.get().config().getString("screen.layout.textures", "minimal"));
    }
    private static final char NEG1 = (char) SPACE_NEG;

    private Buttons() {}

    /** Si a este jugador se le dibujan los botones con el pack (si no, van como "[ texto ]"). */
    static boolean enabled(Player p) {
        if (p == null || Screen.oldClient(p)) return false;
        String mode = UltraDialogue.get().config().getString("screen.layout.buttons", "auto").toLowerCase(Locale.ROOT);
        if (mode.equals("text")) return false;
        if (mode.equals("pack")) return true;
        PackManager pack = UltraDialogue.get().pack();
        return pack != null && pack.loaded(p);
    }

    /**
     * Estilos retirados (la categoria "Propios" y los materiales salvo biblioteca, granja y esmeralda):
     * siguen aceptandose y se dibujan con el estilo parecido, asi ningun dialogo viejo se rompe.
     */
    static final java.util.Map<String, String> ALIAS = java.util.Map.ofEntries(
            java.util.Map.entry("madera_oscura", "aldea_roble_oscuro"), java.util.Map.entry("madera_clara", "aldea_llanura"),
            java.util.Map.entry("pergamino", "aldea_desierto"), java.util.Map.entry("piedra", "cueva"),
            java.util.Map.entry("hierro", "piedra_lisa"), java.util.Map.entry("obsidiana", "obsidiana_real"),
            java.util.Map.entry("nether", "fortaleza"), java.util.Map.entry("mar", "oceano"),
            java.util.Map.entry("sombra", "oscuridad_profunda"), java.util.Map.entry("tinta", "piedra_lisa"),
            // "principal" (y "simple"): el de los NPC principales, liso, sin diseño.
            java.util.Map.entry("simple", "principal"),
            java.util.Map.entry("barril", "aldea_taiga"),
            java.util.Map.entry("bloque_oro", "granja"), java.util.Map.entry("bloque_hierro", "piedra_lisa"),
            java.util.Map.entry("bloque_diamante", "hielo_azul"), java.util.Map.entry("bloque_lapislazuli", "hielo_azul"),
            java.util.Map.entry("bloque_cobre", "arena_roja"), java.util.Map.entry("bloque_netherita", "piedra_negra"),
            java.util.Map.entry("cuarzo", "calcita"), java.util.Map.entry("lana", "nieve"),
            java.util.Map.entry("hormigon", "calcita"));

    /** El estilo que de verdad se dibuja (los retirados pasan a su reemplazo). */
    static String resolve(String s) {
        if (s == null) return null;
        String f = s.toLowerCase(Locale.ROOT);
        return ALIAS.getOrDefault(f, f);
    }

    static boolean validFill(String s) {
        if (s == null) return true;
        String f = s.toLowerCase(Locale.ROOT);
        return SCENES.contains(f) || FILLS.contains(f) || VANILLA.contains(f) || ALIAS.containsKey(f);
    }

    static boolean validBorder(String s) {
        if (s == null) return true;
        String b = s.toLowerCase(Locale.ROOT);
        return BORDERS.contains(b) || b.equals("ninguno") || b.equals("none");
    }

    /** Lo minimo que mide un boton para que la etiqueta no toque los bordes. */
    static int minWidth(int labelWidth) { return labelWidth + 34; }

    /**
     * screen.layout.look: "frame" = la textura del estilo solo en el MARCO y adentro el fondo liso
     * (minimalista); "filled" = la textura llena todo el boton.
     */
    static boolean frameMode() {
        return "frame".equalsIgnoreCase(UltraDialogue.get().config().getString("screen.layout.look", "filled"));
    }

    /**
     * @param fill      nombre del fondo
     * @param border    nombre del borde, o "ninguno"
     * @param borderColor color del borde
     * @param fillColor tiñe el fondo (null = sus colores). Los propios solo se tiñen si son "tinta".
     */
    static Component box(String fill, String border, TextColor borderColor, TextColor fillColor,
                         int width, Component label, int labelWidth) {
        width = Math.max(width, 2 * CAP);
        int si = SCENES.indexOf(resolve(fill == null ? "" : fill));
        if (si >= 0) return scene(si, width, label, labelWidth);
        if (frameMode()) return frame(fill, borderColor, fillColor, width, label, labelWidth);
        String f = fill == null ? "" : resolve(fill);
        String b = border == null ? "" : border.toLowerCase(Locale.ROOT);
        int bi = BORDERS.indexOf(b);

        List<Component> parts = new ArrayList<>();
        int vi = VANILLA.indexOf(f);
        if (vi >= 0) {
            TextColor fc = fillColor != null ? fillColor : NamedTextColor.WHITE;
            parts.add(glyphs((minimalTextures() ? strip(MINIMAL_BASE + vi * 16, width) : tiled(VANILLA_BASE + vi * 32, width))
                    + space(-width), fc));
        } else {
            int fi = Math.max(0, FILLS.indexOf(f));
            boolean tintable = FILLS.get(fi).equals("tinta") || FILLS.get(fi).equals("liso");
            TextColor fc = tintable && fillColor != null ? fillColor : NamedTextColor.WHITE;
            parts.add(glyphs(strip(FILL_BASE + fi * 16, width) + space(-width), fc));
        }
        if (bi >= 0)
            parts.add(glyphs(strip(BORDER_BASE + bi * 16, width) + space(-width), borderColor));
        int before = Math.max(0, (width - labelWidth) / 2);
        parts.add(glyphs(space(before), NamedTextColor.WHITE));
        parts.add(label);
        parts.add(glyphs(space(width - before - labelWidth), NamedTextColor.WHITE));
        return Component.textOfChildren(parts.toArray(new Component[0]));
    }

    /**
     * Modo marco: textura del estilo en todo el alto (dos copias, arriba y abajo), encima el fondo
     * liso metido 4 px por cada lado, encima la linea de color y la etiqueta. Asi solo se ve la
     * textura en el marco. color-fondo tiñe el interior liso.
     */
    private static Component frame(String fill, TextColor lineColor, TextColor innerColor,
                                   int width, Component label, int labelWidth) {
        width = Math.max(width, 2 * CAP + 2);
        String f = fill == null ? "" : resolve(fill);
        List<Component> parts = new ArrayList<>();
        int vi = VANILLA.indexOf(f);
        if (vi >= 0) {
            if (minimalTextures()) {
                parts.add(glyphs(strip(FRAME_TOP_M + vi * 16, width) + space(-width), NamedTextColor.WHITE));
                parts.add(glyphs(strip(FRAME_BOT_M + vi * 16, width) + space(-width), NamedTextColor.WHITE));
            } else {
                parts.add(glyphs(tiled(FRAME_TOP_V + vi * 32, width) + space(-width), NamedTextColor.WHITE));
                parts.add(glyphs(tiled(FRAME_BOT_V + vi * 32, width) + space(-width), NamedTextColor.WHITE));
            }
        } else {
            int fi = Math.max(0, FILLS.indexOf(f));
            parts.add(glyphs(strip(FRAME_TOP_P + fi * 16, width) + space(-width), NamedTextColor.WHITE));
            parts.add(glyphs(strip(FRAME_BOT_P + fi * 16, width) + space(-width), NamedTextColor.WHITE));
        }
        int liso = FILL_BASE + FILLS.indexOf("liso") * 16;
        parts.add(glyphs(space(CAP) + strip(liso, width - 2 * CAP) + space(-(width - CAP)),
                innerColor != null ? innerColor : NamedTextColor.WHITE));
        parts.add(glyphs(strip(BORDER_BASE + BORDERS.indexOf("linea") * 16, width) + space(-width), lineColor));
        int before = Math.max(0, (width - labelWidth) / 2);
        parts.add(glyphs(space(before), NamedTextColor.WHITE));
        parts.add(label);
        parts.add(glyphs(space(width - before - labelWidth), NamedTextColor.WHITE));
        return Component.textOfChildren(parts.toArray(new Component[0]));
    }

    /** Boton escena: tapa (28) + tramos + tapa (28), y la etiqueta centrada encima. */
    private static Component scene(int si, int width, Component label, int labelWidth) {
        width = Math.max(width, 2 * SCAP);
        int base = SCENE_BASE + si * 16;
        StringBuilder sb = new StringBuilder();
        sb.appendCodePoint(base).append(NEG1);
        int rest = width - 2 * SCAP;
        for (int i = 7; i >= 0; i--) {
            int w = 1 << i;
            while (rest >= w) {
                sb.appendCodePoint(base + 2 + i).append(NEG1);
                rest -= w;
            }
        }
        sb.appendCodePoint(base + 1).append(NEG1);
        List<Component> parts = new ArrayList<>();
        parts.add(glyphs(sb + space(-width), NamedTextColor.WHITE));
        int before = Math.max(0, (width - labelWidth) / 2);
        parts.add(glyphs(space(before), NamedTextColor.WHITE));
        parts.add(label);
        parts.add(glyphs(space(width - before - labelWidth), NamedTextColor.WHITE));
        return Component.textOfChildren(parts.toArray(new Component[0]));
    }

    /** Espacio de ancho exacto en pixeles (negativo = hacia atras). */
    static Component gap(int px) { return glyphs(space(px), NamedTextColor.WHITE); }

    private static Component glyphs(String s, TextColor color) {
        return Component.text(s).font(FONT).color(color).shadowColor(ShadowColor.none());
    }

    /** Tapa izquierda + tramos de 128..1 + tapa derecha, cada uno con su -1. */
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

    /**
     * Fondo vanilla: en cada columna se usa el tramo MAS ANCHO que empieza justo ahi dentro de la
     * textura (columna % 16), asi el dibujo sigue sin cortes. Tramos: 16 en +0, 8 en +1, 4 en +3,
     * 2 en +7, 1 en +15 (mismo orden que generar_pack.py).
     */
    private static String tiled(int base, int width) {
        StringBuilder sb = new StringBuilder();
        int x = 0;
        int[][] slices = {{16, 0}, {8, 1}, {4, 3}, {2, 7}, {1, 15}};
        while (x < width) {
            int o = x % 16, rest = width - x;
            for (int[] s : slices) {
                if (o % s[0] == 0 && s[0] <= rest) {
                    sb.appendCodePoint(base + s[1] + o / s[0]).append(NEG1);
                    x += s[0];
                    break;
                }
            }
        }
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

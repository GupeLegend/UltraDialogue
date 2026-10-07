package mc.gupe.ultradialogue;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Los iconos de los botones y la cara 2D del retrato.
 *
 * Un icono se dibuja DENTRO del texto con un componente "object" de tipo sprite (1.21.9+): el
 * cliente pinta la textura a la altura de una letra. Las texturas vienen en el pack del plugin,
 * en el atlas de bloques (textures/block/icon/), que es el que anima.
 *
 * ⚠ Sin el pack, un sprite que no existe se ve como el cuadro violeta y negro de "textura faltante".
 * Por eso cada icono tiene un simbolo de texto de respaldo, que se usa con quien no cargo el pack
 * (lo rechazo, fallo la descarga, o entra con un cliente viejo por ViaVersion).
 */
public final class Icons {

    /** {@code listed}: sale en /ud icons. {@code sprite}: tiene dibujo en el pack (si no, siempre simbolo). */
    public record Preset(String name, String alias, String glyph, String color, boolean listed, boolean sprite) {}

    private static final Map<String, Preset> PRESETS = new LinkedHashMap<>();
    private static final Map<String, String> ALIASES = new HashMap<>();

    // El juego de simbolos de la 1.1.2: solo texto, se ven igual con o sin pack.
    private static void symbol(String name, String alias, String glyph, String color) {
        put(new Preset(name, alias, glyph, color, true, false));
    }

    private static void put(Preset p) {
        PRESETS.put(p.name(), p);
        ALIASES.put(p.name(), p.name());
        ALIASES.put(p.alias(), p.name());
    }

    static {
        // El juego de simbolos (1.1.2). Los 20 iconos de la 1.1.1 se quitaron.
        symbol("mision", "quest", "!", "#F2B21E");          // hay una mision
        symbol("principal", "main", "•", "#4CD964");       // identificador: mision primaria
        symbol("secundaria", "side", "•", "#4F8BFF");      // identificador: mision secundaria
        symbol("acertijo", "riddle", "•", "#B36BFF");      // identificador: acertijo
        symbol("aceptar", "accept", "✓", "#4CD964");
        symbol("volver", "back", "«", "#C6E86B");
        symbol("salir", "exit", "×", "#E04B4B");
        symbol("premio", "reward", "±", "#FFD23F");        // premios de la mision
        symbol("cuento", "story", "♪", "#7FD6FF");         // un relato del NPC para una mision
        symbol("probabilidad", "chance", "%", "#FF9F43");  // algo puede pasar (o tocar) con cierta probabilidad
    }

    private Icons() {}

    /** Los que se muestran en /ud icons (el juego de simbolos actual). */
    public static Collection<Preset> presets() {
        return PRESETS.values().stream().filter(Preset::listed).toList();
    }

    /** "mision secundaria", "mision+secundaria", "[mision, secundaria]" -> cada nombre. */
    static List<String> names(String icon) {
        List<String> r = new ArrayList<>();
        if (icon == null) return r;
        for (String s : icon.replace("[", " ").replace("]", " ").split("[\\s,+]+")) if (!s.isBlank()) r.add(s);
        return r;
    }

    /** Los nombres de un icono que no existen (para avisar al cargar). */
    static List<String> unknown(String icon) {
        List<String> r = new ArrayList<>();
        for (String s : names(icon)) if (resolve(s) == null) r.add(s);
        return r;
    }

    /** Nombre oficial del icono (acepta el alias en ingles y tildes). null si no existe. */
    public static String resolve(String name) {
        if (name == null) return null;
        String n = java.text.Normalizer.normalize(name.trim().toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return ALIASES.get(n);
    }

    /** El icono (o los iconos, separados por espacio) listo para poner en un texto. */
    public static Component of(Player p, String name) {
        List<Component> parts = new ArrayList<>();
        for (String s : names(name)) {
            String n = resolve(s);
            if (n == null) continue;
            Preset pr = PRESETS.get(n);
            if (!parts.isEmpty()) parts.add(Component.space());
            if (pr.sprite() && sprites(p)) parts.add(Component.object(ObjectContents.sprite(Key.key("ultradialogue", "block/icon/" + n))));
            else parts.add(Component.text(pr.glyph(), TextColor.fromHexString(pr.color())));
        }
        return parts.isEmpty() ? Component.empty() : Component.textOfChildren(parts.toArray(new Component[0]));
    }

    /** Ancho en pixeles del icono tal como lo va a ver este jugador (sprite o simbolo). */
    static int width(Player p, String name) {
        int w = 0, count = 0;
        for (String s : names(name)) {
            String n = resolve(s);
            if (n == null) continue;
            Preset pr = PRESETS.get(n);
            w += (pr.sprite() && sprites(p)) ? 9 : Glyphs.width(pr.glyph());
            if (count++ > 0) w += 4;
        }
        return w;
    }

    /**
     * Si a este jugador se le pueden mandar sprites.
     *   auto   = solo si cargo el pack del plugin (lo normal)
     *   sprite = siempre (para quien sirve el pack por su cuenta, fusionado con otros)
     *   text   = nunca: siempre los simbolos de texto
     * Los clientes viejos nunca: no conocen los componentes "object".
     */
    public static boolean sprites(Player p) {
        if (p == null || Screen.oldClient(p)) return false;
        String mode = UltraDialogue.get().config().getString("icons.mode", "auto").toLowerCase(Locale.ROOT);
        if (mode.equals("text")) return false;
        if (mode.equals("sprite")) return true;
        PackManager pack = UltraDialogue.get().pack();
        return pack != null && pack.loaded(p);
    }

    /**
     * La cara 2D de la skin del retrato, para ponerla junto al nombre del NPC. null si el retrato no
     * es una cabeza (item:, none) o si el cliente es viejo.
     * La textura la resuelve el CLIENTE: el servidor no le pide nada a Mojang.
     */
    public static Component face(Player viewer, String portrait, String npcTexture) {
        if (viewer == null || Screen.oldClient(viewer)) return null;
        String[] alts = (portrait == null ? "npc" : portrait).split("\\|");
        for (String a : alts) {
            String r = a.trim();
            String low = r.toLowerCase(Locale.ROOT);
            if (low.equals("npc")) {
                if (npcTexture == null || npcTexture.isEmpty()) continue;
                return head(PlayerHeadObjectContents.property("textures", npcTexture));
            }
            if (low.startsWith("player:") || low.startsWith("jugador:")) {
                String name = r.substring(r.indexOf(':') + 1).trim();
                return Component.object(ObjectContents.playerHead(name));
            }
            if (low.startsWith("texture:") || low.startsWith("textura:")) {
                return head(PlayerHeadObjectContents.property("textures", r.substring(r.indexOf(':') + 1).trim()));
            }
            return null;   // item: o none: no hay cara
        }
        return null;
    }

    private static Component head(PlayerHeadObjectContents.ProfileProperty prop) {
        return Component.object(ObjectContents.playerHead().profileProperty(prop).hat(true).build());
    }
}

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

    public record Preset(String name, String alias, String glyph, String color) {}

    private static final Map<String, Preset> PRESETS = new LinkedHashMap<>();
    private static final Map<String, String> ALIASES = new HashMap<>();

    private static void add(String name, String alias, String glyph, String color) {
        Preset p = new Preset(name, alias, glyph, color);
        PRESETS.put(name, p);
        ALIASES.put(name, name);
        ALIASES.put(alias, name);
    }

    static {
        add("mision", "quest", "!", "#F2B21E");
        add("aceptar", "accept", "✔", "#4CD964");
        add("pregunta", "question", "?", "#4FC3F7");
        add("charla", "talk", "☺", "#FFFFFF");
        add("tienda", "shop", "$", "#55FF55");
        add("secreto", "secret", "✦", "#B36BFF");
        add("eleccion", "choice", "✧", "#FF6EC7");
        add("regalo", "gift", "❖", "#F7A04A");
        add("volver", "back", "«", "#BDBDBD");
        add("salir", "exit", "✖", "#E04B4B");
        add("staff", "admin", "☼", "#E03A3A");
        add("gema", "gem", "◆", "#C98BFF");
        add("espada", "sword", "⚔", "#DDE3EE");
        add("corazon", "heart", "❤", "#E83B4E");
        add("calavera", "skull", "☠", "#EFE8D8");
        add("cofre", "chest", "▣", "#C08040");
        add("llave", "key", "⚷", "#F2C84B");
        add("mapa", "map", "▤", "#EBD9A6");
        add("estrella", "star", "★", "#FFD23F");
        add("libro", "book", "❏", "#A36BD1");
    }

    private Icons() {}

    public static Collection<Preset> presets() { return PRESETS.values(); }

    /** Nombre oficial del icono (acepta el alias en ingles y tildes). null si no existe. */
    public static String resolve(String name) {
        if (name == null) return null;
        String n = java.text.Normalizer.normalize(name.trim().toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return ALIASES.get(n);
    }

    /** El icono listo para poner en un texto. Vacio si el nombre no existe. */
    public static Component of(Player p, String name) {
        String n = resolve(name);
        if (n == null) return Component.empty();
        Preset pr = PRESETS.get(n);
        if (sprites(p)) return Component.object(ObjectContents.sprite(Key.key("ultradialogue", "block/icon/" + n)));
        return Component.text(pr.glyph(), TextColor.fromHexString(pr.color()));
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

package mc.gupe.ultradialogue.gui;

import mc.gupe.ultradialogue.UltraDialogue;
import mc.gupe.ultradialogue.pack.PackManager;
import mc.gupe.ultradialogue.util.Glyphs;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * The icons a button can show, and the 2D face for the portrait.
 *
 * An icon is drawn INSIDE the text with a sprite "object" component (1.21.9+): the client paints
 * the texture at letter height. {@link Preset#sprite()} is currently always false - every preset
 * since 1.1.2 is a plain text symbol, which looks the same with or without the resource pack - but
 * the mechanism is kept so a future sprite icon only needs a texture and {@code sprite = true}.
 */
public final class Icons {

    /** {@code listed}: shown by /ud icons. {@code sprite}: has artwork in the pack (if not, always the symbol). */
    public record Preset(String name, String alias, String glyph, String color, boolean listed, boolean sprite) {}

    private static final Map<String, Preset> PRESETS = new LinkedHashMap<>();
    private static final Map<String, String> ALIASES = new HashMap<>();

    // The current (1.1.2+) symbol set: plain text, looks the same with or without the pack.
    private static void symbol(String name, String alias, String glyph, String color) {
        put(new Preset(name, alias, glyph, color, true, false));
    }

    private static void put(Preset p) {
        PRESETS.put(p.name(), p);
        ALIASES.put(p.name(), p.name());
        ALIASES.put(p.alias(), p.name());
    }

    static {
        symbol("mision", "quest", "!", "#F2B21E");          // there is a quest
        symbol("principal", "main", "•", "#4CD964");   // marks: main quest
        symbol("secundaria", "side", "•", "#4F8BFF");  // marks: side quest
        symbol("acertijo", "riddle", "•", "#B36BFF");  // marks: riddle
        symbol("aceptar", "accept", "✓", "#4CD964");
        symbol("volver", "back", "«", "#C6E86B");
        symbol("salir", "exit", "×", "#E04B4B");
        symbol("premio", "reward", "±", "#FFD23F");    // quest rewards
        symbol("cuento", "story", "♪", "#7FD6FF");     // an NPC's story about a quest
        symbol("probabilidad", "chance", "%", "#FF9F43");   // something may happen (or drop) with some odds
    }

    private Icons() {}

    /** The ones shown by /ud icons (the current symbol set). */
    public static Collection<Preset> presets() {
        return PRESETS.values().stream().filter(Preset::listed).toList();
    }

    /** "mision secundaria", "mision+secundaria", "[mision, secundaria]" -> each name. */
    public static List<String> names(String icon) {
        List<String> r = new ArrayList<>();
        if (icon == null) return r;
        for (String s : icon.replace("[", " ").replace("]", " ").split("[\\s,+]+")) if (!s.isBlank()) r.add(s);
        return r;
    }

    /** The names in an icon spec that don't exist (to warn about when loading). */
    public static List<String> unknown(String icon) {
        List<String> r = new ArrayList<>();
        for (String s : names(icon)) if (resolve(s) == null) r.add(s);
        return r;
    }

    /** The icon's canonical name (accepts the English alias and accents). null if it doesn't exist. */
    public static String resolve(String name) {
        if (name == null) return null;
        String n = java.text.Normalizer.normalize(name.trim().toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return ALIASES.get(n);
    }

    /** The icon (or icons, space-separated), ready to put in a text. */
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

    /** Width in pixels of the icon exactly as this player will see it (sprite or symbol). */
    public static int width(Player p, String name) {
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
     * Whether this player can be sent sprite icons.
     *   auto   = only if they loaded the plugin's own pack (the normal case)
     *   sprite = always (for servers that bundle this pack with others themselves)
     *   text   = never: always the text symbols
     * Old clients never get sprites: they don't know the "object" component.
     */
    public static boolean sprites(Player p) {
        if (p == null || Screen.oldClient(p) || PackManager.bedrock(p)) return false;
        String mode = UltraDialogue.get().config().getString("icons.mode", "auto").toLowerCase(Locale.ROOT);
        if (mode.equals("text")) return false;
        if (mode.equals("sprite")) return true;
        PackManager pack = UltraDialogue.get().pack();
        return pack != null && pack.loaded(p);
    }

    /**
     * The 2D face from the portrait's skin, shown next to the NPC's name. null if the portrait
     * isn't a head (item:, none) or the client is too old.
     * The CLIENT resolves the texture: the server never asks Mojang for anything.
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
            return null;   // item: or none: no face
        }
        return null;
    }

    private static Component head(PlayerHeadObjectContents.ProfileProperty prop) {
        return Component.object(ObjectContents.playerHead().profileProperty(prop).hat(true).build());
    }
}

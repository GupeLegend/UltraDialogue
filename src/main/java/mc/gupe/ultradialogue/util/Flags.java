package mc.gupe.ultradialogue.util;

import mc.gupe.ultradialogue.UltraDialogue;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Per-player flags ("already talked to the chronicler", "accepted the quest"...). They live on
 * the player itself (PersistentDataContainer): there is no file that can be lost or corrupted.
 *
 * The key uses the plugin's name as its namespace. It used to be called "Dialogos", so old flags
 * live under "dialogos:marcas" and are read and moved to the new key the first time. Keep this
 * bridge while players who haven't logged in since the rename still exist.
 */
public final class Flags {

    private static NamespacedKey key;
    @SuppressWarnings("deprecation")
    private static final NamespacedKey LEGACY = new NamespacedKey("dialogos", "marcas");

    private Flags() {}

    private static NamespacedKey key() {
        if (key == null) key = new NamespacedKey(UltraDialogue.get(), "flags");
        return key;
    }

    public static Set<String> of(Player p) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        String s = pdc.get(key(), PersistentDataType.STRING);
        String old = pdc.get(LEGACY, PersistentDataType.STRING);
        Set<String> r = new LinkedHashSet<>();
        if (s != null && !s.isEmpty()) r.addAll(Arrays.asList(s.split(",")));
        if (old != null) {
            if (!old.isEmpty()) r.addAll(Arrays.asList(old.split(",")));
            pdc.remove(LEGACY);
            save(p, r);
        }
        return r;
    }

    public static boolean has(Player p, String f) {
        return of(p).contains(f.toLowerCase());
    }

    public static void set(Player p, String f, boolean on) {
        Set<String> s = of(p);
        if (on ? s.add(f.toLowerCase()) : s.remove(f.toLowerCase())) save(p, s);
    }

    public static void clear(Player p) {
        p.getPersistentDataContainer().remove(key());
        p.getPersistentDataContainer().remove(LEGACY);
    }

    private static void save(Player p, Set<String> s) {
        if (s.isEmpty()) p.getPersistentDataContainer().remove(key());
        else p.getPersistentDataContainer().set(key(), PersistentDataType.STRING, String.join(",", s));
    }
}

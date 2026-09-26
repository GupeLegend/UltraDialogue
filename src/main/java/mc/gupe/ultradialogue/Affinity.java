package mc.gupe.ultradialogue;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Afinidad de cada jugador con cada personaje: un numero por dialogo ("kadir" = kadir.yml).
 * Vive en el jugador (PersistentDataContainer), igual que las marcas: no hay archivo.
 *
 * El jugador NUNCA ve el numero ni se le avisa cuando cambia. Solo lo lee el staff con /ud affinity.
 *
 * Para que no se pueda farmear hablando sin parar, las SUBIDAS tienen dos frenos:
 *   - cooldown: un personaje solo da afinidad una vez cada X minutos (por jugador);
 *   - max-per-talk: dentro de esa charla, como mucho N puntos en total.
 * Las BAJADAS no tienen freno: una respuesta grosera siempre cuenta.
 */
public final class Affinity {

    private static NamespacedKey kValue, kLast;

    private Affinity() {}

    private static NamespacedKey value() {
        if (kValue == null) kValue = new NamespacedKey(UltraDialogue.get(), "affinity");
        return kValue;
    }

    private static NamespacedKey last() {
        if (kLast == null) kLast = new NamespacedKey(UltraDialogue.get(), "affinity_last");
        return kLast;
    }

    /** Limites de un personaje: los de su archivo (affinity:) o, si no los tiene, los de config.yml. */
    public record Limits(int min, int max, long cooldownMs, int maxPerTalk) {}

    public static Limits limits(String id) {
        FileConfiguration c = UltraDialogue.get().config();
        Dialogue d = UltraDialogue.get().registry().byId(id);
        int min = d != null && d.affinityMin != null ? d.affinityMin : c.getInt("affinity.min", 0);
        int max = d != null && d.affinityMax != null ? d.affinityMax : c.getInt("affinity.max", 100);
        double cd = d != null && d.affinityCooldown != null ? d.affinityCooldown : c.getDouble("affinity.cooldown-minutes", 30);
        int per = d != null && d.affinityPerTalk != null ? d.affinityPerTalk : c.getInt("affinity.max-per-talk", 3);
        return new Limits(min, Math.max(min, max), (long) (cd * 60_000), per);
    }

    public static int get(Player p, String id) {
        Integer v = read(p, value()).get(id.toLowerCase());
        return v == null ? limits(id).min() : v;
    }

    public static void set(Player p, String id, int v) {
        Limits l = limits(id);
        Map<String, Integer> m = read(p, value());
        m.put(id.toLowerCase(), Math.max(l.min(), Math.min(l.max(), v)));
        write(p, value(), m);
    }

    public static void reset(Player p, String id) {
        Map<String, Integer> m = read(p, value());
        m.remove(id.toLowerCase());
        write(p, value(), m);
        Map<String, Integer> t = read(p, last());
        t.remove(id.toLowerCase());
        write(p, last(), t);
    }

    public static Map<String, Integer> all(Player p) {
        return read(p, value());
    }

    /**
     * Aplica un cambio respetando los frenos. {@code session} guarda lo que ya se sumo en esta
     * charla; puede ser null (por ejemplo desde el comando del staff, que no tiene frenos).
     */
    public static void change(Player p, String id, int delta, Screen.Session session) {
        id = id.toLowerCase();
        if (delta > 0 && session != null) {
            Limits l = limits(id);
            Boolean ok = session.gainAllowed.get(id);
            if (ok == null) {
                // Primera subida de esta charla: se mira el cooldown y, si pasa, se marca la hora.
                long now = System.currentTimeMillis();
                Map<String, Integer> t = read(p, last());
                Integer lastMin = t.get(id);   // se guarda en minutos para que entre en un int
                ok = lastMin == null || now - lastMin * 60_000L >= l.cooldownMs();
                if (ok) {
                    t.put(id, (int) (now / 60_000L));
                    write(p, last(), t);
                }
                session.gainAllowed.put(id, ok);
            }
            if (!ok) return;
            int used = session.gained.getOrDefault(id, 0);
            delta = Math.min(delta, l.maxPerTalk() - used);
            if (delta <= 0) return;
            session.gained.put(id, used + delta);
        }
        set(p, id, get(p, id) + delta);
    }

    // "kadir=12;otro=5"
    private static Map<String, Integer> read(Player p, NamespacedKey k) {
        Map<String, Integer> m = new LinkedHashMap<>();
        String s = p.getPersistentDataContainer().get(k, PersistentDataType.STRING);
        if (s == null || s.isEmpty()) return m;
        for (String part : s.split(";")) {
            int i = part.indexOf('=');
            if (i <= 0) continue;
            try { m.put(part.substring(0, i), Integer.parseInt(part.substring(i + 1))); }
            catch (NumberFormatException ignored) {}
        }
        return m;
    }

    private static void write(Player p, NamespacedKey k, Map<String, Integer> m) {
        if (m.isEmpty()) { p.getPersistentDataContainer().remove(k); return; }
        StringBuilder sb = new StringBuilder();
        for (var e : m.entrySet()) {
            if (sb.length() > 0) sb.append(';');
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        p.getPersistentDataContainer().set(k, PersistentDataType.STRING, sb.toString());
    }
}

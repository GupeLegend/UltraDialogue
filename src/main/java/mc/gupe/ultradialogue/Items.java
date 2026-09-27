package mc.gupe.ultradialogue;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Objetos para las condiciones "has:"/"hand:" y las acciones "take:"/"give:".
 *
 * Formatos que se aceptan (con cantidad al final, opcional):
 *   IRON_INGOT  /  minecraft:iron_ingot       material vanilla (solo items "normales", ver abajo)
 *   ultraboss:AlmaFaraon                      item de UltraBoss (marca oculta sb_item, con sus nombres viejos)
 *   ultrarevive:self | boost | end            un totem de UltraRevive
 *   pdc:<namespace:clave>=<valor>             cualquier marca oculta de otro plugin
 *   item_model:ultraboss:alma_faraon          por componente item_model
 *   name:&6Llave del Sotano  (nombre:)        por nombre visible, ultimo recurso
 *
 * ⚠ Un material vanilla SOLO cuenta items sin marca oculta ni item_model propio. Si no, "has:
 * GOLD_NUGGET 5" contaria las almas de UltraBoss (que son pepitas de oro por dentro) y "take:"
 * se las llevaria.
 */
public final class Items {

    /** Un objeto pedido + cuanto. */
    public record Want(String spec, int amount) {}

    private Items() {}

    /** "IRON_INGOT 16" -> (IRON_INGOT, 16). El nombre puede tener espacios; la cantidad va al final. */
    public static Want parse(String value, int defaultAmount) {
        String v = value.trim();
        int sp = v.lastIndexOf(' ');
        if (sp > 0) {
            try {
                int n = Integer.parseInt(v.substring(sp + 1).trim());
                return new Want(v.substring(0, sp).trim(), Math.max(1, n));
            } catch (NumberFormatException ignored) { }
        }
        return new Want(v, defaultAmount);
    }

    /** Si el formato se entiende. Para avisar en consola al cargar el dialogo. */
    public static boolean valid(String spec) {
        String low = spec.toLowerCase(Locale.ROOT);
        if (low.startsWith("item_model:") || low.startsWith("name:") || low.startsWith("nombre:")) return spec.indexOf(':') < spec.length() - 1;
        if (low.startsWith("pdc:")) return spec.contains("=") && spec.indexOf(':', 4) > 0;
        if (low.startsWith("ultraboss:") || low.startsWith("ultrarevive:")) return spec.length() > low.indexOf(':') + 1;
        return material(spec) != null;
    }

    /** Si se puede ENTREGAR con "give:" (item_model, name y pdc no alcanzan para fabricar el item). */
    public static boolean givable(String spec) {
        String low = spec.toLowerCase(Locale.ROOT);
        return low.startsWith("ultraboss:") || low.startsWith("ultrarevive:") || material(spec) != null;
    }

    private static Material material(String spec) {
        Material m = Material.matchMaterial(spec);
        return m != null && m.isItem() && !m.isAir() ? m : null;
    }

    public static boolean matches(ItemStack it, String spec) {
        if (it == null || it.getType().isAir()) return false;
        String low = spec.toLowerCase(Locale.ROOT);
        ItemMeta m = it.getItemMeta();

        if (low.startsWith("ultraboss:")) {
            String id = spec.substring(10);
            return m != null && (tag(m, "ultraboss", "sb_item", id) || tag(m, "ultraspawnboss", "sb_item", id)
                    || tag(m, "ultrarevivir", "sb_item", id));
        }
        if (low.startsWith("ultrarevive:")) {
            String id = spec.substring(12);
            return m != null && (tag(m, "ultrarevive", "ur_item", id) || tag(m, "ultrarevivir", "ur_item", id));
        }
        if (low.startsWith("pdc:")) {
            int eq = spec.indexOf('=');
            NamespacedKey k = NamespacedKey.fromString(spec.substring(4, eq).toLowerCase(Locale.ROOT));
            return m != null && k != null && spec.substring(eq + 1).equals(
                    m.getPersistentDataContainer().get(k, PersistentDataType.STRING));
        }
        if (low.startsWith("item_model:")) {
            NamespacedKey k = NamespacedKey.fromString(spec.substring(11).toLowerCase(Locale.ROOT));
            return m != null && k != null && m.hasItemModel() && k.equals(m.getItemModel());
        }
        if (low.startsWith("name:") || low.startsWith("nombre:")) {
            String want = plain(Text.color(spec.substring(spec.indexOf(':') + 1)));
            return m != null && m.hasDisplayName() && want.equals(plain(m.displayName()));
        }
        Material mat = material(spec);
        return mat != null && it.getType() == mat && isPlain(m);
    }

    private static boolean tag(ItemMeta m, String ns, String key, String value) {
        @SuppressWarnings("deprecation") NamespacedKey k = new NamespacedKey(ns, key);
        String v = m.getPersistentDataContainer().get(k, PersistentDataType.STRING);
        return v != null && v.equalsIgnoreCase(value);
    }

    /** Sin marcas ocultas ni modelo propio: un item vanilla de verdad (renombrado con yunque vale). */
    private static boolean isPlain(ItemMeta m) {
        if (m == null) return true;
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        return pdc.isEmpty() && !m.hasItemModel();
    }

    private static String plain(net.kyori.adventure.text.Component c) {
        return c == null ? "" : org.bukkit.ChatColor.stripColor(LegacyComponentSerializer.legacySection().serialize(c)).trim();
    }

    /** Los slots donde se busca y se quita: la mano primero, despues el resto, despues la otra mano. */
    private static List<Integer> order(PlayerInventory inv) {
        List<Integer> slots = new ArrayList<>();
        slots.add(inv.getHeldItemSlot());
        for (int i = 0; i < 36; i++) if (i != inv.getHeldItemSlot()) slots.add(i);
        slots.add(40);   // mano secundaria
        return slots;
    }

    public static int count(Player p, String spec) {
        int n = 0;
        PlayerInventory inv = p.getInventory();
        for (int slot : order(inv)) {
            ItemStack it = inv.getItem(slot);
            if (matches(it, spec)) n += it.getAmount();
        }
        return n;
    }

    public static int countInHand(Player p, String spec) {
        ItemStack it = p.getInventory().getItemInMainHand();
        return matches(it, spec) ? it.getAmount() : 0;
    }

    /** Quita TODO o NADA: primero cuenta; si no alcanza, no toca el inventario. */
    public static boolean take(Player p, String spec, int amount) {
        if (count(p, spec) < amount) return false;
        PlayerInventory inv = p.getInventory();
        int left = amount;
        for (int slot : order(inv)) {
            if (left <= 0) break;
            ItemStack it = inv.getItem(slot);
            if (!matches(it, spec)) continue;
            int use = Math.min(left, it.getAmount());
            left -= use;
            if (use == it.getAmount()) inv.setItem(slot, null);
            else it.setAmount(it.getAmount() - use);
        }
        return true;
    }

    /** Entrega. Lo que no entra en el inventario cae a los pies del jugador. false si no se sabe fabricar. */
    public static boolean give(Player p, String spec, int amount) {
        String low = spec.toLowerCase(Locale.ROOT);
        // Los items de otros plugins los fabrica cada plugin con su propio comando: asi salen con
        // todas sus marcas, su nombre en el idioma del server y su textura.
        if (low.startsWith("ultraboss:")) {
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ub i give " + spec.substring(10) + " " + p.getName() + " " + amount);
        }
        if (low.startsWith("ultrarevive:")) {
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ur give " + spec.substring(12) + " " + p.getName() + " " + amount);
        }
        Material mat = material(spec);
        if (mat == null) return false;
        int left = amount;
        while (left > 0) {
            int n = Math.min(left, mat.getMaxStackSize());
            left -= n;
            for (ItemStack rest : p.getInventory().addItem(new ItemStack(mat, n)).values())
                p.getWorld().dropItemNaturally(p.getLocation(), rest);
        }
        return true;
    }
}

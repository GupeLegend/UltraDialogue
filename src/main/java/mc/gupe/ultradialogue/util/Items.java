package mc.gupe.ultradialogue.util;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
 * Items for the "has:"/"hand:" conditions and the "take:"/"give:" actions.
 *
 * Accepted formats (an optional amount at the end):
 *   IRON_INGOT  /  minecraft:iron_ingot       a vanilla material (only "plain" items, see below)
 *   ultraboss:EjemploTrofeo                   an UltraBoss item (its hidden sb_item tag, old names included)
 *   ultrarevive:self | boost | end            an UltraRevive totem
 *   pdc:<namespace:key>=<value>               any other plugin's hidden tag
 *   item_model:ultraboss:ejemplotrofeo        by its item_model component
 *   name:&6Cellar Key  (nombre:)              by its display name, last resort
 *
 * A vanilla material only counts items with no hidden tag and no item_model. Otherwise "has:
 * GOLD_NUGGET 5" would count UltraBoss souls (gold nuggets underneath) and "take:" would remove them.
 */
public final class Items {

    /** A requested item and how many. */
    public record Want(String spec, int amount) {}

    private Items() {}

    /** "IRON_INGOT 16" -> (IRON_INGOT, 16). The name may contain spaces; the amount is the last token. */
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

    /** Whether the format is understood, to warn in the console when a dialogue loads. */
    public static boolean valid(String spec) {
        String low = spec.toLowerCase(Locale.ROOT);
        if (low.startsWith("item_model:") || low.startsWith("name:") || low.startsWith("nombre:")) return spec.indexOf(':') < spec.length() - 1;
        if (low.startsWith("pdc:")) return spec.contains("=") && spec.indexOf(':', 4) > 0;
        if (low.startsWith("ultraboss:") || low.startsWith("ultrarevive:")) return spec.length() > low.indexOf(':') + 1;
        return material(spec) != null;
    }

    /** Whether it can be handed out with "give:" (item_model, name and pdc aren't enough to craft one). */
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

    /** No hidden tags and no own model: a real vanilla item (renamed on an anvil is fine). */
    private static boolean isPlain(ItemMeta m) {
        if (m == null) return true;
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        return pdc.isEmpty() && !m.hasItemModel();
    }

    private static String plain(net.kyori.adventure.text.Component c) {
        return c == null ? "" : org.bukkit.ChatColor.stripColor(LegacyComponentSerializer.legacySection().serialize(c)).trim();
    }

    /** The slots that are searched and emptied: the held hand first, then the rest, then off-hand. */
    private static List<Integer> order(PlayerInventory inv) {
        List<Integer> slots = new ArrayList<>();
        slots.add(inv.getHeldItemSlot());
        for (int i = 0; i < 36; i++) if (i != inv.getHeldItemSlot()) slots.add(i);
        slots.add(40);   // off-hand
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

    /** Takes ALL of it or NONE: counts first; if it's short, the inventory is left untouched. */
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

    /** Hands it out. What doesn't fit falls at the player's feet. false if it can't be crafted. */
    public static boolean give(Player p, String spec, int amount) {
        String low = spec.toLowerCase(Locale.ROOT);
        // Other plugins' items are built by that plugin's own command, so they keep all their tags,
        // their name in the server's language and their texture.
        if (low.startsWith("ultraboss:")) {
            Tasks.console("ub i give " + spec.substring(10) + " " + p.getName() + " " + amount);
            return true;
        }
        if (low.startsWith("ultrarevive:")) {
            Tasks.console("ur give " + spec.substring(12) + " " + p.getName() + " " + amount);
            return true;
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

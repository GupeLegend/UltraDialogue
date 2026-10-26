package mc.gupe.ultradialogue.listener;

import mc.gupe.ultradialogue.UltraDialogue;
import mc.gupe.ultradialogue.model.Dialogue;
import mc.gupe.ultradialogue.util.Text;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Map;
import java.util.UUID;

/**
 * Besides FancyNpcs, any entity (a villager, an armor stand...) can have a dialogue with a tag:
 *   /tag @e[type=villager,limit=1,sort=nearest] add dialogue:merchant
 * (the old "dialogo:" tag still works too).
 */
public final class Listeners implements Listener {

    private static final Map<UUID, Long> lastClick = new java.util.concurrent.ConcurrentHashMap<>();
    private final UltraDialogue pl;

    public Listeners(UltraDialogue pl) { this.pl = pl; }

    /** Common entry point for FancyNpcs and tagged entities. */
    public static void talk(UltraDialogue pl, Player p, Dialogue d, Location npc, String texture) {
        long now = System.currentTimeMillis();
        Long before = lastClick.put(p.getUniqueId(), now);
        if (before != null && now - before < pl.config().getLong("cooldown-ms", 700)) return;

        // No dependency on UltraRevive: downed players can't start a conversation.
        for (var v : p.getMetadata("ultrarevive_downed")) {
            if (v.asBoolean()) {
                p.sendMessage(Text.msg("downed"));
                return;
            }
        }
        pl.screen().open(p, d, null, npc, texture);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Entity en = e.getRightClicked();
        for (String tag : en.getScoreboardTags()) {
            String id = tag.startsWith("dialogue:") ? tag.substring(9) : tag.startsWith("dialogo:") ? tag.substring(8) : null;
            if (id == null) continue;
            Dialogue d = pl.registry().byId(id);
            if (d == null) return;
            e.setCancelled(true);
            talk(pl, e.getPlayer(), d, en.getLocation(), null);
            return;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pl.screen().forget(e.getPlayer());
        lastClick.remove(e.getPlayer().getUniqueId());
    }
}

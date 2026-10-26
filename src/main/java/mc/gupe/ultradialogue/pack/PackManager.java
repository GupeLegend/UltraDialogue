package mc.gupe.ultradialogue.pack;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who can see the drawn buttons. UltraDialogue doesn't send packs: {@link PackInstaller} puts the
 * buttons inside the plugin that builds the server's pack. So once the buttons are in that pack, a
 * player has them when the last pack they were sent loaded fine. Everyone else gets "[ text ]" and
 * text symbols (see {@link mc.gupe.ultradialogue.gui.Icons#sprites}) instead of missing textures.
 *
 * Some pack plugins (CraftEngine) send the pack while the player is still connecting, and Paper
 * reports nothing to other plugins at that stage. For those, a player counts as having it unless
 * they decline it or it fails later on.
 *
 * Bedrock players (Geyser + Floodgate) never get them: the buttons are drawn with Java font glyphs
 * that Bedrock can't show, even if Geyser reports the Java pack as loaded.
 */
public final class PackManager implements Listener {

    private final boolean inServerPack;
    private final boolean sentWhileConnecting;
    private final Set<UUID> loaded = ConcurrentHashMap.newKeySet();
    private final Set<UUID> refused = ConcurrentHashMap.newKeySet();

    /**
     * @param inServerPack        the buttons are inside the pack the server sends (installed or merged by hand)
     * @param sentWhileConnecting that pack is sent before the player is in the world, so no status arrives
     */
    public PackManager(boolean inServerPack, boolean sentWhileConnecting) {
        this.inServerPack = inServerPack;
        this.sentWhileConnecting = sentWhileConnecting;
    }

    public boolean inServerPack() { return inServerPack; }

    public boolean loaded(Player p) {
        if (!inServerPack || bedrock(p)) return false;
        UUID id = p.getUniqueId();
        return loaded.contains(id) || (sentWhileConnecting && !refused.contains(id));
    }

    private static Boolean hasFloodgate;

    /** Is this a Bedrock player? Asks Floodgate by name, so it's optional. */
    public static boolean bedrock(Player p) {
        if (Boolean.FALSE.equals(hasFloodgate) || p == null) return false;
        try {
            Object api = Class.forName("org.geysermc.floodgate.api.FloodgateApi").getMethod("getInstance").invoke(null);
            hasFloodgate = true;
            return (boolean) api.getClass().getMethod("isFloodgatePlayer", UUID.class).invoke(api, p.getUniqueId());
        } catch (Throwable t) {
            hasFloodgate = false;
            return false;
        }
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        switch (e.getStatus()) {
            case SUCCESSFULLY_LOADED -> { loaded.add(id); refused.remove(id); }
            case DECLINED, FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD, DISCARDED -> { loaded.remove(id); refused.add(id); }
            default -> { }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        loaded.remove(e.getPlayer().getUniqueId());
        refused.remove(e.getPlayer().getUniqueId());
    }
}

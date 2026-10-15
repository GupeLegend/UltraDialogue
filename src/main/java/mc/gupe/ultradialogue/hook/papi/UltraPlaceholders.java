package mc.gupe.ultradialogue.hook.papi;

import mc.gupe.ultradialogue.UltraDialogue;
import mc.gupe.ultradialogue.util.Affinity;
import mc.gupe.ultradialogue.util.Flags;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * %ultradialogue_affinity_<dialogue>%   that player's affinity with that character
 * %ultradialogue_flag_<flag>%           true / false
 *
 * Only registered if PlaceholderAPI is enabled (the only class that imports its classes).
 * Affinity and flags live on the connected player: offline returns 0 / false.
 */
public final class UltraPlaceholders extends PlaceholderExpansion {

    private final UltraDialogue pl;

    public UltraPlaceholders(UltraDialogue pl) { this.pl = pl; }

    @Override public String getIdentifier() { return "ultradialogue"; }
    @Override public String getAuthor() { return "Social Studio"; }
    @Override public String getVersion() { return pl.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }   // so /papi reload doesn't drop it

    @Override
    public String onRequest(OfflinePlayer off, String params) {
        String prm = params.toLowerCase();
        Player p = off == null ? null : off.getPlayer();
        if (prm.startsWith("affinity_")) {
            String id = prm.substring(9);
            return String.valueOf(p == null ? Affinity.limits(id).min() : Affinity.get(p, id));
        }
        if (prm.startsWith("flag_")) {
            return String.valueOf(p != null && Flags.has(p, prm.substring(5)));
        }
        return null;
    }
}

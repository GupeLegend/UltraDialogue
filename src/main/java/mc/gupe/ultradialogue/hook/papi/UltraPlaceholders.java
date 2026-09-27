package mc.gupe.ultradialogue.hook.papi;

import mc.gupe.ultradialogue.Affinity;
import mc.gupe.ultradialogue.Flags;
import mc.gupe.ultradialogue.UltraDialogue;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * %ultradialogue_affinity_<dialogo>%   la afinidad de ese jugador con ese personaje
 * %ultradialogue_flag_<marca>%         true / false
 *
 * Solo se registra si PlaceholderAPI esta prendido (es la unica clase que importa sus clases).
 * La afinidad y las marcas viven en el jugador conectado: desconectado devuelve 0 / false.
 */
public final class UltraPlaceholders extends PlaceholderExpansion {

    private final UltraDialogue pl;

    public UltraPlaceholders(UltraDialogue pl) { this.pl = pl; }

    @Override public String getIdentifier() { return "ultradialogue"; }
    @Override public String getAuthor() { return "Social Studio"; }
    @Override public String getVersion() { return pl.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }   // que /papi reload no la borre

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

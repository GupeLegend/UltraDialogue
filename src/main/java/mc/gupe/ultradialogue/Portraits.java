package mc.gupe.ultradialogue;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * La cabeza que sale a la izquierda del texto.
 *   npc               - la skin del NPC de FancyNpcs que se clickeo
 *   player:Name       - la skin de esa cuenta            (tambien jugador:)
 *   texture:<base64>  - una textura fija                 (tambien textura:)
 *   item:MATERIAL     - cualquier item
 *   none              - sin retrato                      (tambien ninguno)
 * Con "|" se da un respaldo: "npc | item:BOOK".
 *
 * REGLA: nunca se le pide nada a Mojang desde el hilo principal. "player:" se resuelve en
 * segundo plano al cargar y se guarda; hasta que llega, sale el respaldo o una cabeza generica.
 */
public final class Portraits {

    private final UltraDialogue pl;
    private final Map<String, PlayerProfile> players = new ConcurrentHashMap<>();

    public Portraits(UltraDialogue pl) { this.pl = pl; }

    private static String playerName(String alt) {
        String low = alt.toLowerCase(Locale.ROOT);
        if (low.startsWith("player:")) return alt.substring(7).trim();
        if (low.startsWith("jugador:")) return alt.substring(8).trim();
        return null;
    }

    public void preload(Collection<Dialogue> ds) {
        for (Dialogue d : ds) for (String alt : d.portrait.split("\\|")) {
            String name = playerName(alt.trim());
            if (name == null) continue;
            String k = name.toLowerCase(Locale.ROOT);
            if (players.containsKey(k)) continue;
            Bukkit.createProfile(name).update().whenComplete((profile, error) -> {
                if (error == null && profile != null && profile.hasTextures()) players.put(k, profile);
                else pl.getLogger().warning("Could not fetch the skin of '" + name + "' for a portrait.");
            });
        }
    }

    /** @return null si el retrato es "none". */
    public ItemStack of(String portrait, String npcTexture) {
        String[] alts = (portrait == null ? "npc" : portrait).split("\\|");
        for (int i = 0; i < alts.length - 1; i++) {
            String a = alts[i].trim();
            String low = a.toLowerCase(Locale.ROOT);
            String name = playerName(a);
            boolean missing = (low.equals("npc") && (npcTexture == null || npcTexture.isEmpty()))
                    || (name != null && !players.containsKey(name.toLowerCase(Locale.ROOT)));
            if (!missing) return one(a, npcTexture);
        }
        return one(alts[alts.length - 1].trim(), npcTexture);
    }

    private ItemStack one(String r, String npcTexture) {
        String low = r.toLowerCase(Locale.ROOT);
        if (low.equals("none") || low.equals("ninguno")) return null;
        if (low.startsWith("item:")) {
            Material m = Material.matchMaterial(r.substring(5).trim());
            return new ItemStack(m == null || !m.isItem() ? Material.PLAYER_HEAD : m);
        }
        String name = playerName(r);
        if (name != null) return head(players.get(name.toLowerCase(Locale.ROOT)));
        if (low.startsWith("texture:") || low.startsWith("textura:")) return head(texture(r.substring(8).trim()));
        return head(npcTexture == null || npcTexture.isEmpty() ? null : texture(npcTexture));
    }

    private static PlayerProfile texture(String b64) {
        UUID id = UUID.nameUUIDFromBytes(b64.getBytes(StandardCharsets.UTF_8));
        PlayerProfile p = Bukkit.createProfile(id, "ud" + Integer.toHexString(b64.hashCode()));
        p.setProperty(new ProfileProperty("textures", b64));
        return p;
    }

    private static ItemStack head(PlayerProfile profile) {
        ItemStack it = new ItemStack(Material.PLAYER_HEAD);
        if (profile != null && it.getItemMeta() instanceof SkullMeta sm) {
            sm.setPlayerProfile(profile);
            it.setItemMeta(sm);
        }
        return it;
    }
}

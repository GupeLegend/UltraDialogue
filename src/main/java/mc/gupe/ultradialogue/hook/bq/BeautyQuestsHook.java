package mc.gupe.ultradialogue.hook.bq;

import fr.skytasul.quests.api.QuestsAPI;
import fr.skytasul.quests.api.questers.Quester;
import fr.skytasul.quests.api.quests.Quest;
import fr.skytasul.quests.api.stages.StageType;
import mc.gupe.ultradialogue.UltraDialogue;
import mc.gupe.ultradialogue.hook.QuestBridge;
import mc.gupe.ultradialogue.model.DialogueSignalEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Optional;

/**
 * Bridge to BeautyQuests 2.1. ONLY instantiated (by name, from UltraDialogue) if BeautyQuests is
 * enabled: it is, together with StageUltraDialogue, the only class that imports its classes.
 *
 * The stage is registered in onEnable: BeautyQuests loads its quests one tick AFTER every plugin
 * has enabled. Registered any later, quests using this stage would fail to load.
 */
public final class BeautyQuestsHook implements QuestBridge, Listener {

    public BeautyQuestsHook(UltraDialogue pl) {
        ItemStack icon = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta m = icon.getItemMeta();
        m.setDisplayName("§bDiálogo (UltraDialogue)");
        icon.setItemMeta(m);
        QuestsAPI.getAPI().getStages().register(new StageType<>("ULTRADIALOGUE", StageUltraDialogue.class,
                "Diálogo (UltraDialogue)", StageUltraDialogue::deserialize, icon, StageUltraDialogue.Creator::new));
        pl.getServer().getPluginManager().registerEvents(this, pl);
    }

    @EventHandler
    public void onSignal(DialogueSignalEvent e) {
        for (StageUltraDialogue st : StageUltraDialogue.LOADED)
            if (st.key.equals(e.getKey())) st.signal(e.getPlayer());
    }

    @Override
    public boolean active(Player p, String key) {
        String k = key.toLowerCase();
        for (StageUltraDialogue st : StageUltraDialogue.LOADED)
            if (st.key.equals(k) && st.isActive(p)) return true;
        return false;
    }

    private static Quest quest(int id) {
        return QuestsAPI.getAPI().getQuestsManager().getQuest(id);
    }

    /** The "quester" that THIS quest uses for this player (solo or group, depending on the quest). */
    private static Optional<? extends Quester> quester(Quest q, Player p) {
        return q == null ? Optional.empty() : q.getQuesterStrategy().getPlayerQuester(p);
    }

    @Override
    public boolean completed(Player p, int id) {
        Quest q = quest(id);
        return quester(q, p).map(q::hasFinished).orElse(false);
    }

    @Override
    public boolean started(Player p, int id) {
        Quest q = quest(id);
        return quester(q, p).map(q::hasStarted).orElse(false);
    }

    @Override
    public boolean start(Player p, int id, boolean force) {
        Quest q = quest(id);
        if (q == null) return false;
        if (!force) {
            q.attemptStart(p);   // respects requirements, quest limits, etc.
            return true;
        }
        Optional<? extends Quester> qs = quester(q, p);
        if (qs.isEmpty()) return false;
        q.start(qs.get(), false);
        return true;
    }

    @Override
    public String name() { return "BeautyQuests"; }
}

package mc.gupe.ultradialogue.hook.bq;

import fr.skytasul.quests.api.editors.TextEditor;
import fr.skytasul.quests.api.questers.Quester;
import fr.skytasul.quests.api.stages.AbstractStage;
import fr.skytasul.quests.api.stages.StageController;
import fr.skytasul.quests.api.stages.StageDescriptionPlaceholdersContext;
import fr.skytasul.quests.api.stages.creation.StageCreation;
import fr.skytasul.quests.api.stages.creation.StageCreationContext;
import fr.skytasul.quests.api.stages.creation.StageGuiLine;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BeautyQuests stage "ULTRADIALOGUE": completes when a dialogue runs "quest: complete <key>"
 * (mision: completar) with the same key.
 *
 * In the quest file:
 *   stageType: ULTRADIALOGUE
 *   key: brann_iron
 *   npc: brann            (optional, only for the description)
 */
public class StageUltraDialogue extends AbstractStage {

    /** Every loaded stage of this type right now, to find the ones matching a key. */
    static final Set<StageUltraDialogue> LOADED = ConcurrentHashMap.newKeySet();

    final String key;
    final String npc;

    public StageUltraDialogue(StageController controller, String key, String npc) {
        super(controller);
        this.key = key == null ? "" : key.toLowerCase(Locale.ROOT);
        this.npc = npc;
        LOADED.add(this);
    }

    public static StageUltraDialogue deserialize(ConfigurationSection s, StageController controller) {
        return new StageUltraDialogue(controller, s.getString("key"), s.getString("npc"));
    }

    @Override
    protected void serialize(ConfigurationSection s) {
        s.set("key", key);
        if (npc != null && !npc.isEmpty()) s.set("npc", npc);
    }

    @Override
    public void unload() {
        super.unload();
        LOADED.remove(this);
    }

    @Override
    public String getDefaultDescription(StageDescriptionPlaceholdersContext context) {
        return npc == null || npc.isEmpty() ? "Talk to the NPC" : "Talk to " + npc;
    }

    /** The player is currently on this stage. */
    boolean isActive(Player p) {
        return hasApplicableQuester(p);
    }

    /** The dialogue's signal arrived: advance every quest of this player that's on this stage. */
    void signal(Player p) {
        if (!hasApplicableQuester(p)) return;
        // Copy: finishing the stage changes the list while it's being iterated.
        for (Quester q : new ArrayList<>(controller.getApplicableQuesters(p))) finishStage(q);
    }

    // ---------------------------------------------------------------- in-game editor

    public static class Creator extends StageCreation<StageUltraDialogue> {

        private static final int SLOT_KEY = 6;
        private static final int SLOT_NPC = 7;

        private String key;
        private String npc;

        public Creator(StageCreationContext<StageUltraDialogue> context) {
            super(context);
        }

        @Override
        public void setupLine(StageGuiLine line) {
            super.setupLine(line);
            line.setItem(SLOT_KEY, item(Material.NAME_TAG, "§eSignal key",
                    "§7The same one the dialogue uses in", "§f  quest: complete <key>"), e ->
                    new TextEditor<String>(e.getPlayer(), e::reopen, v -> {
                        key = v.toLowerCase(Locale.ROOT);
                        line.refreshItemLoreOptionValue(SLOT_KEY, key);
                        e.reopen();
                    }).start());
            line.setItem(SLOT_NPC, item(Material.PLAYER_HEAD, "§eNPC name §7(optional)",
                    "§7Only for the description:", "§f  \"Talk to <npc>\""), e ->
                    new TextEditor<String>(e.getPlayer(), e::reopen, v -> {
                        npc = v;
                        line.refreshItemLoreOptionValue(SLOT_NPC, npc);
                        e.reopen();
                    }).start());
        }

        @Override
        public void start(Player p) {
            super.start(p);
            // A new stage with no key wouldn't work: it's asked for as soon as it's created.
            new TextEditor<String>(p, context::remove, v -> {
                key = v.toLowerCase(Locale.ROOT);
                getLine().refreshItemLoreOptionValue(SLOT_KEY, key);
                context.reopenGui();
            }).start();
        }

        @Override
        public void edit(StageUltraDialogue stage) {
            super.edit(stage);
            key = stage.key;
            npc = stage.npc;
            getLine().refreshItemLoreOptionValue(SLOT_KEY, key);
            if (npc != null) getLine().refreshItemLoreOptionValue(SLOT_NPC, npc);
        }

        @Override
        protected StageUltraDialogue finishStage(StageController controller) {
            return new StageUltraDialogue(controller, key, npc);
        }

        private static ItemStack item(Material m, String name, String... lore) {
            ItemStack it = new ItemStack(m);
            ItemMeta meta = it.getItemMeta();
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            it.setItemMeta(meta);
            return it;
        }
    }
}

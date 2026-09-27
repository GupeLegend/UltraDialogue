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
 * Etapa de BeautyQuests "ULTRADIALOGUE": se completa cuando un dialogo corre
 * "quest: complete <clave>" (mision: completar) con la misma clave.
 *
 * En el archivo de la mision:
 *   stageType: ULTRADIALOGUE
 *   key: brann_hierro
 *   npc: brann            (opcional, solo para la descripcion)
 */
public class StageUltraDialogue extends AbstractStage {

    /** Todas las etapas de este tipo cargadas ahora, para encontrar las de una clave. */
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
        return npc == null || npc.isEmpty() ? "Habla con el NPC" : "Habla con " + npc;
    }

    /** El jugador esta ahora en esta etapa. */
    boolean isActive(Player p) {
        return hasApplicableQuester(p);
    }

    /** Llego la señal del dialogo: avanza todas las misiones del jugador que esten en esta etapa. */
    void signal(Player p) {
        if (!hasApplicableQuester(p)) return;
        // Copia: terminar la etapa cambia la lista mientras se recorre.
        for (Quester q : new ArrayList<>(controller.getApplicableQuesters(p))) finishStage(q);
    }

    // ---------------------------------------------------------------- editor dentro del juego

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
            line.setItem(SLOT_KEY, item(Material.NAME_TAG, "§eClave de la señal",
                    "§7La misma que usa el diálogo en", "§f  quest: complete <clave>"), e ->
                    new TextEditor<String>(e.getPlayer(), e::reopen, v -> {
                        key = v.toLowerCase(Locale.ROOT);
                        line.refreshItemLoreOptionValue(SLOT_KEY, key);
                        e.reopen();
                    }).start());
            line.setItem(SLOT_NPC, item(Material.PLAYER_HEAD, "§eNombre del NPC §7(opcional)",
                    "§7Solo para la descripción:", "§f  \"Habla con <npc>\""), e ->
                    new TextEditor<String>(e.getPlayer(), e::reopen, v -> {
                        npc = v;
                        line.refreshItemLoreOptionValue(SLOT_NPC, npc);
                        e.reopen();
                    }).start());
        }

        @Override
        public void start(Player p) {
            super.start(p);
            // Una etapa nueva sin clave no serviria: se pide apenas se crea.
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

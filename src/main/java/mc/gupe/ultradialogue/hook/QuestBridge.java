package mc.gupe.ultradialogue.hook;

import org.bukkit.entity.Player;

/**
 * What UltraDialogue needs from a quest plugin. The only implementation is {@code hook.bq.BeautyQuestsHook},
 * loaded by name ONLY if BeautyQuests is enabled: this way no main class ever touches a BeautyQuests
 * class directly (without the plugin, the JVM would throw NoClassDefFoundError just loading it).
 */
public interface QuestBridge {

    /** The player is currently on an ULTRADIALOGUE stage with this key. */
    boolean active(Player p, String key);

    /** Finished the quest with this id. */
    boolean completed(Player p, int questId);

    /** Has the quest with this id started and not finished. */
    boolean started(Player p, int questId);

    /** Starts the quest; {@code force} = ignore its requirements. false if it doesn't exist. */
    boolean start(Player p, int questId, boolean force);

    /** The plugin's name, for console messages. */
    String name();
}

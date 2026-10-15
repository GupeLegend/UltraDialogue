package mc.gupe.ultradialogue.model;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired by the "quest: complete <key>" action (mision: completar). Listened to by BeautyQuests'
 * ULTRADIALOGUE stage, but it is a plain Bukkit event: any plugin can listen for it and use the key
 * for whatever it needs, with no dependency on UltraDialogue at all.
 */
public final class DialogueSignalEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String key;

    public DialogueSignalEvent(Player player, String key) {
        this.player = player;
        this.key = key;
    }

    public Player getPlayer() { return player; }

    /** The signal's key, lower-cased. */
    public String getKey() { return key; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}

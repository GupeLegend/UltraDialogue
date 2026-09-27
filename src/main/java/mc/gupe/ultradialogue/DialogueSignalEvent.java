package mc.gupe.ultradialogue;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Lo dispara la accion "quest: complete <clave>" (mision: completar). La escucha la etapa
 * ULTRADIALOGUE de BeautyQuests, pero es un evento Bukkit comun: cualquier plugin puede escucharlo
 * y usar la clave para lo que quiera, sin depender de UltraDialogue para nada mas.
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

    /** La clave de la señal, en minusculas. */
    public String getKey() { return key; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}

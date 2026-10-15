package mc.gupe.ultradialogue.listener;

import mc.gupe.ultradialogue.UltraDialogue;
import mc.gupe.ultradialogue.model.Dialogue;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;

/**
 * Hooks FancyNpcs' click event (NpcInteractEvent) BY REFLECTION: the plugin compiles and starts
 * without FancyNpcs, and if it's missing there's simply no NPC to link a dialogue to.
 *
 * If the NPC has a dialogue, the event is CANCELLED: FancyNpcs doesn't run its own actions and the
 * conversation opens instead. npcs.yml is never touched: removing the NPC from its dialogue just
 * goes back to how it was before.
 *
 * Signatures verified against FancyNpcs 2.12.1: getNpc(), getPlayer(), getInteractionType(),
 * Npc.getData() -> NpcData.getName() / getLocation() / getSkinData().getTextureValue().
 */
public final class NpcHook {

    private NpcHook() {}

    @SuppressWarnings("unchecked")
    public static void register(UltraDialogue pl) {
        Class<? extends Event> type;
        try {
            type = (Class<? extends Event>) Class.forName("de.oliver.fancynpcs.api.events.NpcInteractEvent");
        } catch (ClassNotFoundException e) {
            pl.getLogger().info("FancyNpcs not found: dialogues open by command or entity tag only.");
            return;
        }
        Bukkit.getPluginManager().registerEvent(type, new Listener() {}, EventPriority.LOW, (l, e) -> {
            if (type.isInstance(e)) handle(pl, e);
        }, pl, true);
        pl.getLogger().info("Hooked into FancyNpcs.");
    }

    private static void handle(UltraDialogue pl, Event e) {
        try {
            Object data = call(call(e, "getNpc"), "getData");
            Dialogue d = pl.registry().byNpc((String) call(data, "getName"));
            if (d == null) return;

            Object click = call(e, "getInteractionType");
            if (click != null && click.toString().equals("LEFT_CLICK") && !pl.config().getBoolean("left-click", false)) return;

            ((Cancellable) e).setCancelled(true);
            Player p = (Player) call(e, "getPlayer");
            Location loc = (Location) call(data, "getLocation");
            Object skin = call(data, "getSkinData");
            String texture = skin == null ? null : (String) call(skin, "getTextureValue");
            Listeners.talk(pl, p, d, loc, texture);
        } catch (Throwable t) {
            pl.getLogger().warning("Could not read the FancyNpcs click: " + t);
        }
    }

    private static Object call(Object o, String method) throws ReflectiveOperationException {
        if (o == null) return null;
        Method m = o.getClass().getMethod(method);
        m.setAccessible(true);
        return m.invoke(o);
    }
}

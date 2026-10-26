package mc.gupe.ultradialogue.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Where each task runs. Paper's entity and global schedulers behave like the old main-thread
 * scheduler on Paper and Purpur, and pick the right region thread on Folia: one jar for all three.
 */
public final class Tasks {

    private static Plugin plugin;

    private Tasks() {}

    public static void init(Plugin owner) { plugin = owner; }

    /** On the player's (or entity's) thread: right now if we're already on it, otherwise next tick. */
    public static void of(Entity e, Runnable r) {
        if (Bukkit.isOwnedByCurrentRegion(e)) r.run();
        else e.getScheduler().run(plugin, t -> r.run(), null);
    }

    /** On the entity's thread, always next tick (for things that must wait for the current event). */
    public static void later(Entity e, Runnable r) {
        e.getScheduler().run(plugin, t -> r.run(), null);
    }

    /** Console commands and server-wide work: Folia only allows them on the global thread. */
    public static void global(Runnable r) {
        if (Bukkit.isGlobalTickThread()) r.run();
        else Bukkit.getGlobalRegionScheduler().execute(plugin, r);
    }

    public static void console(String command) {
        global(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }
}

package mc.gupe.ultradialogue.command;

import mc.gupe.ultradialogue.util.Tasks;
import mc.gupe.ultradialogue.UltraDialogue;
import mc.gupe.ultradialogue.model.Dialogue;
import mc.gupe.ultradialogue.util.Flags;
import mc.gupe.ultradialogue.util.Affinity;
import mc.gupe.ultradialogue.util.Text;
import net.kyori.adventure.text.Component;
import mc.gupe.ultradialogue.gui.Icons;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * /ud [editor]                                     opens the button editor (in-game only)
 * /ud open <id> [player] [node]                     also works from console, or as another plugin's action
 * /ud reload
 * /ud list
 * /ud flags <player> [clear [flag]]
 * /ud affinity <player> [id] [set|add|reset] [n]    (the player never sees this number)
 * /ud icons                                         the icons available, with their name
 * The old Spanish subcommands (abrir, recargar, lista, marcas, borrar) still work.
 */
public final class Command implements TabExecutor {

    private final UltraDialogue pl;

    public Command(UltraDialogue pl) { this.pl = pl; }

    @Override
    public boolean onCommand(CommandSender s, org.bukkit.command.Command c, String l, String[] a) {
        if (a.length == 0) {
            if (s instanceof Player p) pl.editor().open(p);
            else help(s, l);
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "editor" -> {
                if (s instanceof Player p) pl.editor().open(p);
                else s.sendMessage(Text.msg("editor-players-only"));
            }
            case "open", "abrir" -> {
                if (a.length < 2) { help(s, l); return true; }
                Dialogue d = pl.registry().byId(a[1]);
                if (d == null) { s.sendMessage(Text.msg("not-found", "{id}", a[1])); return true; }
                Player p = a.length > 2 ? Bukkit.getPlayerExact(a[2]) : (s instanceof Player pp ? pp : null);
                if (p == null) { s.sendMessage(Text.msg("player-not-found")); return true; }
                String node = a.length > 3 ? a[3] : null;
                if (node != null && !d.nodes.containsKey(node)) {
                    s.sendMessage(Text.msg("node-not-found", "{id}", d.id, "{node}", node));
                    return true;
                }
                // Opened by hand there is no NPC: no distance check, no skin ("npc" = its fallback).
                // On the player's thread: on Folia they may be in another region than the console.
                Tasks.of(p, () -> pl.screen().open(p, d, node, null, null));
            }
            case "reload", "recargar" -> {
                int n = pl.reload();
                s.sendMessage(Text.msg("reloaded", "{n}", String.valueOf(n), "{npcs}", String.valueOf(pl.registry().linked())));
                if (pl.registry().errors() > 0)
                    s.sendMessage(Text.msg("errors", "{n}", String.valueOf(pl.registry().errors())));
            }
            case "list", "lista" -> {
                s.sendMessage(Text.msg("list-header"));
                for (Dialogue d : pl.registry().all()) {
                    s.sendMessage(Text.color(pl.lang().raw("list-line", "{id}", d.id, "{nodes}", String.valueOf(d.nodes.size()),
                            "{npcs}", d.npcs.isEmpty() ? "-" : String.join(", ", d.npcs))));
                }
            }
            case "flags", "marcas" -> {
                if (a.length < 2) { help(s, l); return true; }
                Player p = Bukkit.getPlayerExact(a[1]);
                if (p == null) { s.sendMessage(Text.msg("player-offline")); return true; }
                boolean clear = a.length > 2 && (a[2].equalsIgnoreCase("clear") || a[2].equalsIgnoreCase("borrar"));
                String one = clear && a.length > 3 ? a[3] : null;
                Tasks.of(p, () -> {
                    if (clear) {
                        if (one != null) Flags.set(p, one, false); else Flags.clear(p);
                        s.sendMessage(Text.msg("done"));
                    }
                    Set<String> f = Flags.of(p);
                    s.sendMessage(Text.msg("flags", "{player}", p.getName(),
                            "{flags}", f.isEmpty() ? pl.lang().raw("none") : String.join(", ", f)));
                });
            }
            case "affinity", "afinidad" -> {
                if (a.length < 2) { help(s, l); return true; }
                Player p = Bukkit.getPlayerExact(a[1]);
                if (p == null) { s.sendMessage(Text.msg("player-offline")); return true; }
                if (a.length == 2) {
                    Tasks.of(p, () -> {
                        var all = Affinity.all(p);
                        s.sendMessage(Text.msg("affinity-list", "{player}", p.getName(),
                                "{list}", all.isEmpty() ? pl.lang().raw("none") : all.toString().replaceAll("[{}]", "")));
                    });
                    return true;
                }
                String id = a[2].toLowerCase();
                String op = a.length >= 4 ? a[3].toLowerCase() : "show";
                int n;
                try { n = op.equals("set") || op.equals("add") ? Integer.parseInt(a.length > 4 ? a[4] : "x") : 0; }
                catch (NumberFormatException e) { help(s, l); return true; }
                if (!op.equals("show") && !op.equals("set") && !op.equals("add") && !op.equals("reset")) { help(s, l); return true; }
                Tasks.of(p, () -> {
                    switch (op) {
                        case "set" -> Affinity.set(p, id, n);
                        case "add" -> Affinity.change(p, id, n, null);   // no brakes: this is staff
                        case "reset" -> Affinity.reset(p, id);
                        default -> { }
                    }
                    s.sendMessage(Text.msg("affinity-show", "{player}", p.getName(), "{id}", id,
                            "{value}", String.valueOf(Affinity.get(p, id))));
                });
            }
            case "icons", "iconos" -> {
                Player viewer = s instanceof Player pp ? pp : null;
                s.sendMessage(Text.msg("icons-header"));
                for (Icons.Preset ic : Icons.presets()) {
                    s.sendMessage(Component.textOfChildren(Component.text("  "), Icons.of(viewer, ic.name()),
                            Text.color(pl.lang().raw("icons-line", "{name}", ic.name(), "{alias}", ic.alias()))));
                }
                s.sendMessage(Text.color(pl.lang().raw("icons-footer")));
            }
            default -> help(s, l);
        }
        return true;
    }

    private void help(CommandSender s, String l) {
        for (String k : new String[]{"help-editor", "help-open", "help-reload", "help-list", "help-flags", "help-affinity", "help-icons"})
            s.sendMessage(Text.color(pl.lang().raw(k, "{cmd}", l)));
    }

    @Override
    public List<String> onTabComplete(CommandSender s, org.bukkit.command.Command c, String l, String[] a) {
        List<String> r = new ArrayList<>();
        String sub = a[0].toLowerCase();
        boolean open = sub.equals("open") || sub.equals("abrir");
        boolean flags = sub.equals("flags") || sub.equals("marcas");
        boolean aff = sub.equals("affinity") || sub.equals("afinidad");
        if (aff && a.length == 2) for (Player p : Bukkit.getOnlinePlayers()) r.add(p.getName());
        if (aff && a.length == 3) for (Dialogue d : pl.registry().all()) r.add(d.id);
        if (aff && a.length == 4) r.addAll(List.of("set", "add", "reset"));
        if (a.length == 1) r.addAll(List.of("editor", "open", "reload", "list", "flags", "affinity", "icons"));
        else if (a.length == 2 && open) for (Dialogue d : pl.registry().all()) r.add(d.id);
        else if ((a.length == 3 && open) || (a.length == 2 && flags))
            for (Player p : Bukkit.getOnlinePlayers()) r.add(p.getName());
        else if (a.length == 4 && open) {
            Dialogue d = pl.registry().byId(a[1]);
            if (d != null) r.addAll(d.nodes.keySet());
        } else if (a.length == 3 && flags) r.add("clear");
        String last = a[a.length - 1].toLowerCase();
        r.removeIf(x -> !x.toLowerCase().startsWith(last));
        return r;
    }
}

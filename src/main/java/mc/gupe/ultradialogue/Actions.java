package mc.gupe.ultradialogue;

import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Cada accion es una linea "tipo: valor". En todas se puede usar %player% y placeholders.
 * Los nombres en español del plugin viejo (ir, comando, mensaje...) siguen valiendo.
 *
 *   goto: node               pasa a otro nodo ("end" = terminar)
 *   dialogue: id [node]      pasa a otro dialogo
 *   menu: name               abre un menu (ver menu-command en config.yml)
 *   command: spawn           lo ejecuta el jugador
 *   console: say hi %player% lo ejecuta la consola
 *   message / broadcast / actionbar: &aText
 *   title: Title|Subtitle
 *   sound: entity.villager.yes 1 1   (tambien ENTITY_VILLAGER_YES)
 *   flag: name   unflag: name
 *   close
 */
public final class Actions {

    public static final Set<String> TYPES = Set.of("goto", "dialogue", "menu", "command", "console", "message",
            "broadcast", "actionbar", "title", "sound", "flag", "unflag", "close");

    private static final Map<String, String> ALIASES = new HashMap<>();
    static {
        for (String t : TYPES) ALIASES.put(t, t);
        ALIASES.put("ir", "goto");
        ALIASES.put("dialogo", "dialogue");
        ALIASES.put("dialog", "dialogue");
        ALIASES.put("comando", "command");
        ALIASES.put("consola", "console");
        ALIASES.put("mensaje", "message");
        ALIASES.put("anuncio", "broadcast");
        ALIASES.put("titulo", "title");
        ALIASES.put("sonido", "sound");
        ALIASES.put("marcar", "flag");
        ALIASES.put("desmarcar", "unflag");
        ALIASES.put("cerrar", "close");
    }

    private final UltraDialogue pl;

    public Actions(UltraDialogue pl) { this.pl = pl; }

    /** @return {tipo normalizado al ingles, valor}. Un tipo desconocido se devuelve tal cual. */
    public static String[] split(String a) {
        int i = a.indexOf(':');
        String type = (i < 0 ? a : a.substring(0, i)).trim().toLowerCase(Locale.ROOT);
        String value = i < 0 ? "" : a.substring(i + 1).trim();
        return new String[]{ALIASES.getOrDefault(type, type), value};
    }

    /** Si alguna accion deja al jugador en otra pantalla de dialogo. */
    public static boolean changesScreen(List<String> acts) {
        for (String a : acts) {
            String[] t = split(a);
            if (t[0].equals("dialogue")) return true;
            if (t[0].equals("goto") && !Dialogue.isEnd(t[1])) return true;
        }
        return false;
    }

    public void run(Player p, List<String> acts, Screen.Session s) {
        for (String a : acts) {
            String[] t = split(a);
            String v = Text.ph(p, t[1]);
            try {
                switch (t[0]) {
                    case "goto" -> { pl.screen().show(p, s, t[1]); return; }
                    case "dialogue" -> {
                        String[] x = t[1].split("\\s+");
                        Dialogue d = pl.registry().byId(x[0]);
                        if (d == null) {
                            p.sendMessage(Text.msg("not-found", "{id}", x[0]));
                            pl.screen().close(p);
                        } else {
                            pl.screen().switchTo(p, s, d, x.length > 1 ? x[1] : null);
                        }
                        return;
                    }
                    case "menu" -> console(pl.config().getString("menu-command", "dm open {menu} {player}")
                            .replace("{menu}", v).replace("{player}", p.getName()));
                    case "command" -> p.performCommand(noSlash(v));
                    case "console" -> console(noSlash(v));
                    case "message" -> p.sendMessage(Text.color(v));
                    case "broadcast" -> Bukkit.getServer().sendMessage(Text.color(v));
                    case "actionbar" -> p.sendActionBar(Text.color(v));
                    case "title" -> {
                        String[] x = v.split("\\|", 2);
                        p.showTitle(Title.title(Text.color(x[0]), Text.color(x.length > 1 ? x[1] : ""),
                                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(600))));
                    }
                    case "sound" -> sound(p, v);
                    case "flag" -> Flags.set(p, v, true);
                    case "unflag" -> Flags.set(p, v, false);
                    case "close" -> pl.screen().close(p);
                    default -> pl.getLogger().warning("Unknown action: '" + a + "'");
                }
            } catch (Exception e) {
                pl.getLogger().warning("Action failed '" + a + "': " + e);
            }
        }
    }

    public void sound(Player p, String s) {
        if (s == null || s.isBlank()) return;
        String[] x = s.trim().split("\\s+");
        float vol = x.length > 1 ? Float.parseFloat(x[1]) : 1f;
        float pitch = x.length > 2 ? Float.parseFloat(x[2]) : 1f;
        String name = x[0];
        if (name.contains(".") || name.contains(":")) {
            p.playSound(p.getLocation(), name.toLowerCase(Locale.ROOT), vol, pitch);
            return;
        }
        try {
            Sound snd = (Sound) Sound.class.getField(name.toUpperCase(Locale.ROOT)).get(null);
            p.playSound(p.getLocation(), snd, vol, pitch);
        } catch (ReflectiveOperationException e) {
            pl.getLogger().warning("Unknown sound: '" + name + "'");
        }
    }

    private static String noSlash(String c) { return c.startsWith("/") ? c.substring(1) : c; }

    private static void console(String c) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
    }
}

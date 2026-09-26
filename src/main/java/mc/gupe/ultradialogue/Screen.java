package mc.gupe.ultradialogue;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Dibuja cada turno de la charla y recibe la respuesta.
 *
 * Cada vez que se muestra algo se genera un token nuevo. Los botones llevan el token con el que
 * nacieron, asi que un boton viejo (de una pantalla anterior, de un chat de hace rato, o de
 * antes de un reload) no hace nada: solo cuenta lo que el jugador tiene delante.
 */
public final class Screen {

    /** Estado de una charla en curso. {@code npc} y {@code texture} se conservan de nodo en nodo. */
    public static final class Session {
        public Dialogue dialogue;
        public String node;
        public long token;
        public final Location npc;
        public final String texture;
        /** Afinidad en esta charla: si el cooldown dejo subir (por personaje) y cuanto se sumo. */
        public final Map<String, Boolean> gainAllowed = new HashMap<>();
        public final Map<String, Integer> gained = new HashMap<>();

        Session(Dialogue d, Location npc, String texture) {
            this.dialogue = d;
            this.npc = npc;
            this.texture = texture;
        }
    }

    private static final int MAX_JUMPS = 16;
    private static final int DIALOG_PROTOCOL = 771; // 1.21.6

    private final UltraDialogue pl;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final AtomicLong tokens = new AtomicLong();
    private int jumps;

    public Screen(UltraDialogue pl) { this.pl = pl; }

    private FileConfiguration cfg() { return pl.config(); }

    /** Empieza una charla desde el nodo de inicio (o el indicado). */
    public void open(Player p, Dialogue d, String node, Location npc, String texture) {
        Session s = new Session(d, npc, texture);
        sessions.put(p.getUniqueId(), s);
        jumps = 0;
        show(p, s, node != null ? node : d.firstNode(p));
    }

    /** Cambia a otro dialogo sin perder el NPC con el que se esta hablando. */
    public void switchTo(Player p, Session s, Dialogue d, String node) {
        s.dialogue = d;
        show(p, s, node != null ? node : d.firstNode(p));
    }

    public void show(Player p, Session s, String node) {
        if (node == null || Dialogue.isEnd(node)) { close(p); return; }
        Dialogue.Node n = s.dialogue.nodes.get(node);
        if (n == null) {
            p.sendMessage(Text.msg("node-not-found", "{id}", s.dialogue.id, "{node}", node));
            close(p);
            return;
        }
        // Un "goto" dentro de on-show que apunte a si mismo seria un bucle infinito.
        if (++jumps > MAX_JUMPS) {
            pl.getLogger().warning("[" + s.dialogue.id + "] too many jumps in a row at '" + node + "', stopping.");
            close(p);
            return;
        }
        s.node = node;
        s.token = tokens.incrementAndGet();
        long token = s.token;

        if (!n.onShow().isEmpty()) {
            pl.actions().run(p, n.onShow(), s);
            if (sessions.get(p.getUniqueId()) != s || s.token != token) return; // on-show ya cambio de pantalla
        }
        jumps = 0;

        pl.actions().sound(p, s.dialogue.sound != null ? s.dialogue.sound : cfg().getString("talk-sound", ""));

        List<Option> options = options(p, s, n);
        String name = n.speaker() != null ? n.speaker() : s.dialogue.name;
        String text = pickText(p, s, n);
        if (usesChat(p)) chat(p, text, name, options, token);
        else dialog(p, s, text, name, options, token);
    }

    private record Option(Component text, Component tooltip, Dialogue.Answer answer) {}

    /** La ultima variante que vio cada jugador en cada nodo: la siguiente vez sale otra. */
    private final Map<UUID, Map<String, Integer>> lastVariant = new HashMap<>();

    private String pickText(Player p, Session s, Dialogue.Node n) {
        List<Integer> ok = new ArrayList<>();
        for (int i = 0; i < n.variants().size(); i++)
            if (Conditions.test(p, n.variants().get(i).conditions(), s.dialogue.id)) ok.add(i);
        if (ok.isEmpty()) return n.text();
        String key = s.dialogue.id + "/" + n.id();
        Map<String, Integer> seen = lastVariant.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        Integer before = seen.get(key);
        if (ok.size() > 1 && before != null) ok.remove(before);
        int pick = ok.get(RND.nextInt(ok.size()));
        seen.put(key, pick);
        return n.variants().get(pick).text();
    }

    private static final Random RND = new Random();

    private List<Option> options(Player p, Session s, Dialogue.Node n) {
        List<Dialogue.Answer> fixed = new ArrayList<>(), rotating = new ArrayList<>();
        for (Dialogue.Answer a : n.answers()) {
            if (!Conditions.test(p, a.conditions(), s.dialogue.id)) continue;
            (a.always() || n.show() <= 0 ? fixed : rotating).add(a);
        }
        // show: N -> de las que no son "always", solo N al azar. Se respeta el orden del archivo.
        if (n.show() > 0 && rotating.size() > n.show()) {
            List<Dialogue.Answer> copy = new ArrayList<>(rotating);
            Collections.shuffle(copy, RND);
            Set<Dialogue.Answer> keep = new HashSet<>(copy.subList(0, n.show()));
            rotating.removeIf(a -> !keep.contains(a));
        }
        List<Option> r = new ArrayList<>();
        for (Dialogue.Answer a : n.answers()) {
            if (!fixed.contains(a) && !rotating.contains(a)) continue;
            r.add(new Option(Text.color(p, a.text()), a.tooltip() == null ? null : Text.color(p, a.tooltip()), a));
        }
        // Sin respuestas: "Continuar" si hay next, y si no un "Adios" para poder salir.
        if (r.isEmpty()) {
            boolean more = n.next() != null;
            String t = pl.lang().raw(more ? "button-continue" : "button-goodbye");
            r.add(new Option(Text.color(p, t), null,
                    new Dialogue.Answer(t, null, List.of(), more ? n.next() : "end", List.of(), false)));
        }
        return r;
    }

    private void dialog(Player p, Session s, String textRaw, String name, List<Option> options, long token) {
        int textWidth = clamp(cfg().getInt("screen.text-width", 260));
        int buttonWidth = clamp(cfg().getInt("screen.button-width", 220));
        int columns = s.dialogue.columns > 0 ? s.dialogue.columns : cfg().getInt("screen.columns", 1);

        Component text = Text.color(p, textRaw);
        ItemStack head = pl.portraits().of(s.dialogue.portrait, s.texture);
        List<DialogBody> body = new ArrayList<>();
        if (head != null) {
            body.add(DialogBody.item(head)
                    .description(DialogBody.plainMessage(text, textWidth))
                    .showDecorations(false)
                    .showTooltip(false)
                    .build());
        } else {
            body.add(DialogBody.plainMessage(text, textWidth));
        }

        ClickCallback.Options once = ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(15)).build();
        List<ActionButton> buttons = new ArrayList<>();
        for (Option o : options) {
            ActionButton.Builder b = ActionButton.builder(o.text()).width(buttonWidth)
                    .action(DialogAction.customClick((view, who) -> click(who, token, o.answer()), once));
            if (o.tooltip() != null) b.tooltip(o.tooltip());
            buttons.add(b.build());
        }

        var type = DialogType.multiAction(buttons).columns(Math.max(1, columns));
        if (cfg().getBoolean("screen.exit-button", false)) {
            Dialogue.Answer leave = new Dialogue.Answer(null, null, List.of(), "end", List.of(), true);
            type.exitAction(ActionButton.builder(Text.color(pl.lang().raw("button-goodbye")))
                    .width(buttonWidth)
                    .action(DialogAction.customClick((view, who) -> click(who, token, leave), once))
                    .build());
        }

        DialogBase base = DialogBase.builder(Text.color(p, name))
                .canCloseWithEscape(cfg().getBoolean("screen.close-with-esc", true))
                .pause(false)
                // NONE: al clickear, la pantalla se queda como esta hasta que el servidor la
                // reemplaza (siguiente nodo, sin parpadeo) o la cierra. NO usar WAIT_FOR_RESPONSE:
                // al cerrar, el cliente vuelve a la pantalla anterior a la espera, que es este
                // mismo dialogo, y "Adios" no salia a la primera.
                .afterAction(DialogBase.DialogAfterAction.NONE)
                .body(body)
                .build();

        var built = type.build();
        p.showDialog(Dialog.create(f -> f.empty().base(base).type(built)));
    }

    private void chat(Player p, String textRaw, String name, List<Option> options, long token) {
        p.sendMessage(Component.empty());
        p.sendMessage(Text.color(p, pl.lang().raw("chat-header", "{name}", name)));
        for (String line : textRaw.split("\n")) p.sendMessage(Text.color(p, " " + line));
        String format = pl.lang().raw("chat-option");
        ClickCallback.Options once = ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(15)).build();
        for (Option o : options) {
            String[] parts = format.split("\\{text}", 2);
            Component line = Text.color(parts[0]).append(o.text());
            if (parts.length > 1) line = line.append(Text.color(parts[1]));
            line = line.clickEvent(ClickEvent.callback(who -> click(who, token, o.answer()), once));
            if (o.tooltip() != null) line = line.hoverEvent(HoverEvent.showText(o.tooltip()));
            p.sendMessage(line);
        }
    }

    // Los callbacks pueden llegar fuera del hilo principal: todo lo que toque el mundo va por aca.
    private void click(Audience who, long token, Dialogue.Answer a) {
        if (!(who instanceof Player p)) return;
        if (Bukkit.isPrimaryThread()) answer(p, token, a);
        else Bukkit.getScheduler().runTask(pl, () -> answer(p, token, a));
    }

    private void answer(Player p, long token, Dialogue.Answer a) {
        Session s = sessions.get(p.getUniqueId());
        if (s == null || s.token != token || !p.isOnline()) {
            if (s == null && !usesChat(p)) p.closeDialog();
            return;
        }

        double max = cfg().getDouble("max-distance", 8.0);
        if (s.npc != null && max > 0 && (!s.npc.getWorld().equals(p.getWorld()) || s.npc.distance(p.getLocation()) > max)) {
            close(p);
            p.sendMessage(Text.msg("too-far"));
            return;
        }

        boolean continues = (a.goTo() != null && !Dialogue.isEnd(a.goTo())) || Actions.changesScreen(a.actions());
        // Si la respuesta termina la charla (abre un menu, corre un comando...) se cierra ANTES:
        // un inventario abierto encima de un dialogo que despues se cierra se llevaria el menu.
        if (!continues) close(p);
        jumps = 0;
        pl.actions().run(p, a.actions(), s);
        if (a.goTo() != null && sessions.get(p.getUniqueId()) == s && s.token == token) show(p, s, a.goTo());
    }

    public void close(Player p) {
        sessions.remove(p.getUniqueId());
        if (!usesChat(p)) p.closeDialog();
    }

    public void forget(Player p) {
        sessions.remove(p.getUniqueId());
        lastVariant.remove(p.getUniqueId());
    }

    public void closeAll() {
        for (UUID id : new ArrayList<>(sessions.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.closeDialog();
        }
        sessions.clear();
    }

    public boolean usesChat(Player p) {
        return "chat".equalsIgnoreCase(cfg().getString("mode", "dialog")) || oldClient(p);
    }

    private static Boolean hasVia;

    // ViaVersion deja entrar clientes anteriores a 1.21.6, que no tienen pantallas de dialogo.
    private static boolean oldClient(Player p) {
        if (Boolean.FALSE.equals(hasVia)) return false;
        try {
            Object api = Class.forName("com.viaversion.viaversion.api.Via").getMethod("getAPI").invoke(null);
            int v = (int) Class.forName("com.viaversion.viaversion.api.ViaAPI")
                    .getMethod("getPlayerVersion", UUID.class).invoke(api, p.getUniqueId());
            hasVia = true;
            return v > 0 && v < DIALOG_PROTOCOL;
        } catch (Throwable t) {
            hasVia = false;
            return false;
        }
    }

    private static int clamp(int v) { return Math.max(1, Math.min(1024, v)); }
}

package mc.gupe.ultradialogue.gui;

import mc.gupe.ultradialogue.util.Tasks;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import mc.gupe.ultradialogue.UltraDialogue;
import mc.gupe.ultradialogue.model.Dialogue;
import mc.gupe.ultradialogue.util.Conditions;
import mc.gupe.ultradialogue.util.Glyphs;
import mc.gupe.ultradialogue.util.Text;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Draws every turn of a conversation and handles the player's answer.
 *
 * Every time something is shown, a new token is generated. Buttons carry the token they were
 * born with, so an old button (from a previous screen, from chat a while ago, or from before a
 * reload) does nothing: only what the player currently has in front of them counts.
 */
public final class Screen {

    /** State of an ongoing conversation. {@code npc} and {@code texture} carry over from node to node. */
    public static final class Session {
        public Dialogue dialogue;
        public String node;
        public long token;
        public final Location npc;
        public final String texture;
        /** Affinity gained in this conversation: whether the cooldown allowed it (per character) and how much. */
        public final Map<String, Boolean> gainAllowed = new HashMap<>();
        public final Map<String, Integer> gained = new HashMap<>();
        /** Screens visited, for the Back button. */
        final Deque<Step> history = new ArrayDeque<>();
        Step shown;
        boolean backing;

        Session(Dialogue d, Location npc, String texture) {
            this.dialogue = d;
            this.npc = npc;
            this.texture = texture;
        }
    }

    record Step(Dialogue dialogue, String node) {}

    private static final int MAX_JUMPS = 16;
    private static final int MAX_HISTORY = 32;
    private static final int DIALOG_PROTOCOL = 771; // 1.21.6

    private final UltraDialogue pl;
    private final Map<UUID, Session> sessions = new java.util.concurrent.ConcurrentHashMap<>();
    private final AtomicLong tokens = new AtomicLong();
    private int jumps;

    public Screen(UltraDialogue pl) { this.pl = pl; }

    private FileConfiguration cfg() { return pl.config(); }

    /** Starts a conversation from its start node (or the given one). */
    public void open(Player p, Dialogue d, String node, Location npc, String texture) {
        Session s = new Session(d, npc, texture);
        sessions.put(p.getUniqueId(), s);
        jumps = 0;
        show(p, s, node != null ? node : d.firstNode(p));
    }

    /** Switches to another dialogue without losing track of the NPC being talked to. */
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
        // A "goto" inside on-show pointing at itself would be an infinite loop.
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
            if (sessions.get(p.getUniqueId()) != s || s.token != token) return; // on-show already changed screens
        }
        jumps = 0;

        // The previous screen is only remembered here: a node whose on-show jumps elsewhere was
        // never actually seen, and going Back to it would just send the player right back.
        Step now = new Step(s.dialogue, node);
        if (s.shown != null && !s.shown.equals(now) && !s.backing) {
            s.history.push(s.shown);
            if (s.history.size() > MAX_HISTORY) s.history.removeLast();
        }
        s.backing = false;
        s.shown = now;

        pl.actions().sound(p, s.dialogue.sound != null ? s.dialogue.sound : cfg().getString("talk-sound", ""));

        List<Option> options = options(p, s, n);
        String name = n.title() != null ? n.title() : n.speaker() != null ? n.speaker() : s.dialogue.name;
        String text = pickText(p, s, n);
        if (usesChat(p)) chat(p, s, n, Icons.face(p, s.dialogue.portrait, s.texture), text, name, options, token);
        else dialog(p, s, n, text, name, options, token);
    }

    /** {@code locked}: greyed out, clicking it only shows a notice. */
    private record Option(Component text, Component tooltip, Dialogue.Answer answer, boolean locked, boolean nav) {

        Option(Component text, Component tooltip, Dialogue.Answer answer, boolean locked) {
            this(text, tooltip, answer, locked, false);
        }
    }

    /** The last variant each player saw on each node: the next time a different one is picked. */
    private final Map<UUID, Map<String, Integer>> lastVariant = new java.util.concurrent.ConcurrentHashMap<>();

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

    /** The button's text with its icon in front. With no text, the button is just the icon (size 1). */
    private String rawText(Dialogue.Answer a) {
        if (a.text() != null) return a.text();
        if (Dialogue.BACK.equals(a.goTo())) return pl.lang().raw("button-back");
        if (Dialogue.EXIT.equals(a.goTo())) return pl.lang().raw("button-exit");
        return null;
    }

    private Component label(Player p, Dialogue.Answer a) {
        String raw = rawText(a);
        Component text = raw == null || raw.isEmpty() ? null : Text.color(p, raw);
        if (a.icon() == null) return text == null ? Component.empty() : text;
        Component icon = Icons.of(p, a.icon());
        return text == null ? icon : Component.textOfChildren(icon, Component.space(), text);
    }

    /** The same label, but without its colours and in grey: it's clear it isn't available yet. */
    private Component lockedLabel(Player p, Dialogue.Answer a) {
        String plain = rawText(a) == null ? "" : LegacyComponentSerializer.legacyAmpersand()
                .serialize(Text.color(p, rawText(a))).replaceAll("&[0-9a-fk-orxA-FK-ORX]", "");
        Component text = plain.isEmpty() ? null : Text.color(cfg().getString("screen.locked-color", "&8") + plain);
        if (a.icon() == null) return text == null ? Component.empty() : text;
        Component icon = Icons.of(p, a.icon());
        return text == null ? icon : Component.textOfChildren(icon, Component.space(), text);
    }

    private List<Option> options(Player p, Session s, Dialogue.Node n) {
        List<Dialogue.Answer> fixed = new ArrayList<>(), rotating = new ArrayList<>();
        Set<Dialogue.Answer> locked = new HashSet<>();
        for (Dialogue.Answer a : n.answers()) {
            if (!Conditions.test(p, a.conditions(), s.dialogue.id)) {
                if (a.locked() == null) continue;
                locked.add(a); // locked ones don't rotate: they're there to show what's missing
                fixed.add(a);
                continue;
            }
            (a.always() || n.show() <= 0 ? fixed : rotating).add(a);
        }
        // show: N -> only N of the non-"always" ones, at random. The file's order is kept.
        if (n.show() > 0 && rotating.size() > n.show()) {
            List<Dialogue.Answer> copy = new ArrayList<>(rotating);
            Collections.shuffle(copy, RND);
            Set<Dialogue.Answer> keep = new HashSet<>(copy.subList(0, n.show()));
            rotating.removeIf(a -> !keep.contains(a));
        }
        List<Option> r = new ArrayList<>();
        for (Dialogue.Answer a : n.answers()) {
            if (!fixed.contains(a) && !rotating.contains(a)) continue;
            if (locked.contains(a)) {
                String why = a.locked().isEmpty() ? pl.lang().raw("locked-tooltip") : a.locked();
                r.add(new Option(lockedLabel(p, a), Text.color(p, why), a, true));
                continue;
            }
            r.add(new Option(label(p, a), a.tooltip() == null ? null : Text.color(p, a.tooltip()), a, false));
        }
        // No answers: "Continue" if there is a next, otherwise a "Goodbye" so the player can leave.
        if (r.isEmpty() && (n.nav().isEmpty() && !n.navExit() || n.next() != null)) {
            boolean more = n.next() != null;
            String t = pl.lang().raw(more ? "button-continue" : "button-goodbye");
            r.add(new Option(Text.color(p, t), null,
                    new Dialogue.Answer(t, null, List.of(), more ? n.next() : "end", List.of(), false), false));
        }

        // The nav row comes after the answers. "Back" always shows (on the first screen it closes
        // the conversation): if it appeared and disappeared, the grid shape would change.
        for (Dialogue.Answer a : n.nav()) {
            if (Dialogue.EXIT.equals(a.goTo()) && n.layout().isEmpty()) continue;
            boolean ok = Conditions.test(p, a.conditions(), s.dialogue.id);
            if (!ok && a.locked() == null) continue;
            Dialogue.Answer sized = a.width() > 0 ? a : new Dialogue.Answer(a.text(), a.tooltip(), a.conditions(),
                    a.goTo(), a.actions(), a.always(), a.icon(), navWidth(), a.locked());
            if (!ok) {
                String why = a.locked().isEmpty() ? pl.lang().raw("locked-tooltip") : a.locked();
                r.add(new Option(lockedLabel(p, sized), Text.color(p, why), sized, true, true));
            } else {
                r.add(new Option(label(p, sized), a.tooltip() == null ? null : Text.color(p, a.tooltip()), sized, false, true));
            }
        }

        // Minecraft only centres a lone leftover button at the end of the grid: the full-width one
        // is moved to the back so it ends up alone in its own row.
        if (n.layout().isEmpty() && columns(s, n) > 1) {
            List<Option> wide = new ArrayList<>();
            r.removeIf(o -> o.answer().wide() && wide.add(o));
            r.addAll(wide);
        }
        return r;
    }

    private int columns(Session s, Dialogue.Node n) {
        return n.columns() > 0 ? n.columns() : s.dialogue.columns > 0 ? s.dialogue.columns : cfg().getInt("screen.columns", 1);
    }

    private int navWidth() { return clamp(cfg().getInt("screen.nav-width", 64)); }

    private void dialog(Player p, Session s, Dialogue.Node n, String textRaw, String name, List<Option> options, long token) {
        int textWidth = clamp(cfg().getInt("screen.text-width", 260));
        int buttonWidth = clamp(cfg().getInt("screen.button-width", 220));
        int columns = columns(s, n);

        Component text = Text.color(p, textRaw);
        // A head portrait (npc / player: / texture:) becomes the 2D face next to the name. A 3D
        // head item in the dialogue body looked like a dark silhouette, without the skin.
        Component face = Icons.face(p, s.dialogue.portrait, s.texture);
        ItemStack head = face != null ? null : pl.portraits().of(s.dialogue.portrait, s.texture);
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
        // A locked one can be clicked any number of times: it only shows a notice, never changes the screen.
        ClickCallback.Options many = ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES)
                .lifetime(Duration.ofMinutes(15)).build();
        List<ActionButton> buttons = new ArrayList<>();
        for (Option o : options) {
            int w = o.answer().width() > 0 ? o.answer().width() : buttonWidth;
            ActionButton.Builder b = ActionButton.builder(o.text()).width(w)
                    .action(o.locked()
                            ? DialogAction.customClick((view, who) -> lockedClick(who, token, o), many)
                            : DialogAction.customClick((view, who) -> click(who, token, o.answer()), once));
            if (o.tooltip() != null) b.tooltip(o.tooltip());
            buttons.add(b.build());
        }

        Dialogue.Answer leave = new Dialogue.Answer(null, null, List.of(), "end", List.of(), true);
        ActionButton exit = ActionButton.builder(Text.color(pl.lang().raw(n.navExit() ? "button-exit" : "button-goodbye")))
                .width(n.navExit() ? navWidth() : buttonWidth)
                .action(DialogAction.customClick((view, who) -> click(who, token, leave), once))
                .build();
        DialogType built;
        // Bedrock players see the dialogue as a Geyser form, where text in the body can't be
        // clicked: for them the layout rows become ordinary buttons.
        boolean rowsInBody = !n.layout().isEmpty() && !mc.gupe.ultradialogue.pack.PackManager.bedrock(p);
        if (rowsInBody) {
            // Layout mode: answers are text rows in the body, and the nav: buttons (Back, Exit,
            // Reward... in the order they're written) are REAL buttons in a single row at the end:
            // with that many columns, Minecraft's own grid does keep them together.
            body.addAll(rows(p, n, options, token));
            List<ActionButton> nav = new ArrayList<>();
            for (int i = 0; i < options.size(); i++) if (options.get(i).nav()) nav.add(buttons.get(i));
            built = nav.isEmpty() ? DialogType.notice(exit)
                    : DialogType.multiAction(nav).columns(nav.size()).build();
        } else if (buttons.isEmpty()) {
            // Just text + "exit" (no answers, no next): Minecraft doesn't accept an empty button
            // list, so this uses the single-button screen type instead.
            built = DialogType.notice(exit);
        } else {
            var type = DialogType.multiAction(buttons).columns(Math.max(1, columns));
            // nav's "Exit" (or config.yml's) goes at the very bottom: the only fixed button there is.
            // (A layout node already has its nav: Exit among the buttons.)
            if (n.layout().isEmpty() && (n.navExit() || cfg().getBoolean("screen.exit-button", false))) type.exitAction(exit);
            built = type.build();
        }

        Component title = face == null ? Text.color(p, name) : Component.textOfChildren(face, Component.space(), Text.color(p, name));
        DialogBase base = DialogBase.builder(title)
                .canCloseWithEscape(cfg().getBoolean("screen.close-with-esc", true))
                .pause(false)
                // NONE: on click, the screen stays as-is until the server replaces it (next node, no
                // flicker) or closes it. NOT WAIT_FOR_RESPONSE: on close, the client would return to
                // the screen it was waiting on, which is this same dialogue, and "Goodbye" wouldn't
                // close on the first try.
                .afterAction(DialogBase.DialogAfterAction.NONE)
                .body(body)
                .build();

        p.showDialog(Dialog.create(f -> f.empty().base(base).type(built)));
    }

    /**
     * Layout mode. The client lays BUTTONS out in equal rows, but the body's text can be clicked
     * (each piece carries its own click) and every line is free. That's how 1-2-2-1 is drawn
     * without a resource pack: each row is one body message, centred, with its "buttons" padded
     * with spaces up to the width they're due.
     */
    private List<DialogBody> rows(Player p, Dialogue.Node n, List<Option> options, long token) {
        List<Option> main = new ArrayList<>();
        for (Option o : options) if (!o.nav()) main.add(o);
        List<List<Option>> rows = new ArrayList<>();
        int i = 0;
        for (int count : n.layout()) {
            if (i >= main.size()) break;
            rows.add(main.subList(i, Math.min(main.size(), i + count)));
            i += count;
        }
        while (i < main.size()) rows.add(List.of(main.get(i++)));

        int gap = Math.max(0, cfg().getInt("screen.layout.gap", 2)) * 4;
        String[] fmtOn = format("screen.layout.button-format"), fmtOff = format("screen.layout.locked-format");
        // With the plugin's pack: drawn RPG buttons. Without it: "[ text ]".
        boolean panels = Buttons.enabled(p);
        Dialogue.Look base = new Dialogue.Look(cfg().getString("screen.layout.style", "liso"),
                cfg().getString("screen.layout.border", "doble"), null, null);
        Session sess = sessions.get(p.getUniqueId());
        Dialogue.Look nodeLook = n.look().over(sess == null ? Dialogue.Look.NONE : sess.dialogue.look).over(base);

        // Shared width: the widest row wins, and the rest share that same width. That way the
        // borders of a row of 1, of 2 or of 3 buttons line up, like a grid.
        // screen.layout.row-width: "full" = every row the same width (grid), "fit" = each row only
        // as wide as its text, or a number = minimum grid width in pixels.
        String mode = cfg().getString("screen.layout.row-width", "full").trim().toLowerCase(Locale.ROOT);
        boolean fit = mode.equals("fit");
        int total = mode.matches("[0-9]+") ? Math.max(1, Math.min(1024, Integer.parseInt(mode))) : fit ? 0 : Dialogue.WIDE;
        Map<List<Option>, Integer> rowNeed = new HashMap<>();
        for (List<Option> row : rows) {
            int need = 0;
            for (Option o : row) if (o.answer().width() <= 0)
                need = Math.max(need, panels ? Buttons.minWidth(labelWidth(p, o)) : boxWidth(p, o, o.locked() ? fmtOff : fmtOn));
            rowNeed.put(row, need);
            total = Math.max(total, need * row.size() + gap * (row.size() - 1));
        }

        ClickCallback.Options once = ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(15)).build();
        ClickCallback.Options many = ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES)
                .lifetime(Duration.ofMinutes(15)).build();
        List<DialogBody> out = new ArrayList<>();
        for (List<Option> row : rows) {
            int target = fit ? rowNeed.get(row) : (total - gap * (row.size() - 1)) / row.size();
            List<Component> parts = new ArrayList<>();
            for (int j = 0; j < row.size(); j++) {
                Option o = row.get(j);
                if (j > 0) parts.add(panels ? Buttons.gap(gap) : Component.text(" ".repeat(gap / 4)));
                Component seg;
                if (panels) {
                    Dialogue.Look lk = o.answer().look().over(nodeLook);
                    // force: config's background and border win over each dialogue's own (colours
                    // still come from each button).
                    if (cfg().getBoolean("screen.layout.force", true))
                        lk = new Dialogue.Look(base.style(), base.border(), lk.borderColor(), lk.fillColor());
                    // force-border: config's border for everyone, but each dialogue keeps its own background.
                    else if (cfg().getBoolean("screen.layout.force-border", false))
                        lk = new Dialogue.Look(lk.style(), base.border(), lk.borderColor(), lk.fillColor());
                    if (cfg().getBoolean("screen.layout.force-colors", true))
                        lk = new Dialogue.Look(lk.style(), lk.border(), null, null);
                    // size/width on the answer = that button keeps its own width (the row stops
                    // lining up with the others: that's intentional).
                    int w = o.answer().width() > 0 ? Math.max(o.answer().width(), Buttons.minWidth(labelWidth(p, o))) : target;
                    seg = Buttons.box(lk.style(), lk.border(), borderColor(o, lk), color(lk.fillColor()),
                            w, o.text(), labelWidth(p, o));
                } else {
                    String[] fmt = o.locked() ? fmtOff : fmtOn;
                    int free = Math.max(0, target - boxWidth(p, o, fmt));
                    int left = free / 8, right = (free - left * 4) / 4;
                    seg = Component.textOfChildren(Text.color(p, fmt[0]), Component.text(" ".repeat(left)),
                            o.text(), Component.text(" ".repeat(right)), Text.color(p, fmt[1]));
                }
                // The click goes on the parent: children inherit it, so the whole "box", spaces included, responds.
                seg = seg.clickEvent(o.locked()
                        ? ClickEvent.callback(who -> lockedClick(who, token, o), many)
                        : ClickEvent.callback(who -> click(who, token, o.answer()), once));
                if (o.tooltip() != null) seg = seg.hoverEvent(HoverEvent.showText(o.tooltip()));
                parts.add(seg);
            }
            out.add(DialogBody.plainMessage(Component.textOfChildren(parts.toArray(new Component[0])), clamp(total + 16)));
        }
        return out;
    }

    /**
     * Border colour: the answer's own (border-color) if set; if locked, "locked"'s; otherwise the
     * LAST icon with a colour in screen.layout.border-colors ("mision secundaria" = blue);
     * otherwise "default" (white, plain questions).
     */
    private net.kyori.adventure.text.format.TextColor borderColor(Option o, Dialogue.Look lk) {
        if (lk.borderColor() != null && color(lk.borderColor()) != null) return color(lk.borderColor());
        // getConfigurationSection does NOT fall back to the jar's defaults when the section is
        // missing from the server's config.yml (Bukkit returns an empty section): keys are checked
        // one by one instead, which does respect defaults, then by their English aliases.
        String path = "screen.layout.border-colors";
        var sec = cfg().getConfigurationSection(path);
        java.util.function.Function<String, String> get = k -> {
            if (sec != null)
                for (String key : sec.getKeys(false)) {
                    String canon = Icons.resolve(key);
                    if (key.equalsIgnoreCase(k) || (canon != null && canon.equals(k))) return sec.getString(key);
                }
            return cfg().getString(path + "." + k);
        };
        if (o.locked()) return colorOr(get.apply("locked"), "#5A5A5A");
        List<String> names = Icons.names(o.answer().icon());
        for (int i = names.size() - 1; i >= 0; i--) {
            String canon = Icons.resolve(names.get(i));
            String c = canon == null ? null : get.apply(canon);
            if (c != null && color(c) != null) return color(c);
        }
        return colorOr(get.apply("default"), "#FFFFFF");
    }

    private static net.kyori.adventure.text.format.TextColor color(String hex) {
        return hex == null ? null : net.kyori.adventure.text.format.TextColor.fromHexString(hex.trim());
    }

    private static net.kyori.adventure.text.format.TextColor colorOr(String hex, String def) {
        var c = color(hex);
        return c != null ? c : color(def);
    }

    private String[] format(String key) {
        String[] f = cfg().getString(key, "&8[ {text}&8 ]").split("\\{text}", 2);
        return f.length > 1 ? f : new String[]{f[0], ""};
    }

    /** What a button takes up with no padding: the format's borders plus the label. */
    private int boxWidth(Player p, Option o, String[] fmt) {
        return Glyphs.width(fmt[0]) + Glyphs.width(fmt[1]) + labelWidth(p, o);
    }

    private int labelWidth(Player p, Option o) {
        Dialogue.Answer a = o.answer();
        String raw = rawText(a);
        int w = raw == null ? 0 : Glyphs.width(Text.ph(p, raw));
        if (a.icon() != null) w += Icons.width(p, a.icon()) + (w > 0 ? 4 : 0);
        return w;
    }

    private void chat(Player p, Session s, Dialogue.Node n, Component face, String textRaw, String name,
                      List<Option> options, long token) {
        p.sendMessage(Component.empty());
        Component header = Text.color(p, pl.lang().raw("chat-header", "{name}", name));
        p.sendMessage(face == null ? header : Component.textOfChildren(face, Component.space(), header));
        for (String line : textRaw.split("\n")) p.sendMessage(Text.color(p, " " + line));
        String format = pl.lang().raw("chat-option");
        ClickCallback.Options once = ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(15)).build();
        ClickCallback.Options many = ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES)
                .lifetime(Duration.ofMinutes(15)).build();
        List<Option> all = new ArrayList<>(options);
        if (n.navExit() && all.stream().noneMatch(o -> Dialogue.EXIT.equals(o.answer().goTo()))) all.add(new Option(Text.color(pl.lang().raw("button-exit")), null,
                new Dialogue.Answer(null, null, List.of(), "end", List.of(), true), false));
        for (Option o : all) {
            String[] parts = format.split("\\{text}", 2);
            Component line = Text.color(parts[0]).append(o.text());
            if (parts.length > 1) line = line.append(Text.color(parts[1]));
            line = line.clickEvent(o.locked()
                    ? ClickEvent.callback(who -> lockedClick(who, token, o), many)
                    : ClickEvent.callback(who -> click(who, token, o.answer()), once));
            if (o.tooltip() != null) line = line.hoverEvent(HoverEvent.showText(o.tooltip()));
            p.sendMessage(line);
        }
    }

    private void lockedClick(Audience who, long token, Option o) {
        if (!(who instanceof Player p)) return;
        Runnable r = () -> {
            Session s = sessions.get(p.getUniqueId());
            if (s == null || s.token != token || !p.isOnline()) return;
            p.sendActionBar(o.tooltip());
            pl.actions().sound(p, cfg().getString("screen.locked-sound", "block.note_block.bass 0.6 0.8"));
        };
        Tasks.of(p, r);
    }

    // Callbacks can arrive off the player's thread: anything touching the world goes through here.
    private void click(Audience who, long token, Dialogue.Answer a) {
        if (!(who instanceof Player p)) return;
        Tasks.of(p, () -> answer(p, token, a));
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

        // Anti-duplication: conditions were checked when the screen was DRAWN. If in between the
        // player dropped the items (or lost the permission, the flag...), nothing runs and the
        // node is redrawn with the buttons that actually apply now. All in the same tick.
        if (!Conditions.test(p, a.conditions(), s.dialogue.id)) {
            jumps = 0;
            show(p, s, s.node);
            return;
        }

        if (Dialogue.EXIT.equals(a.goTo())) { close(p); return; }
        if (Dialogue.BACK.equals(a.goTo())) {
            Step back = s.history.poll();
            jumps = 0;
            if (back == null) { close(p); return; }
            s.dialogue = back.dialogue();
            s.backing = true;
            show(p, s, back.node());
            return;
        }

        boolean continues = (a.goTo() != null && !Dialogue.isEnd(a.goTo())) || mc.gupe.ultradialogue.util.Actions.changesScreen(a.actions());
        // If the answer ends the conversation (opens a menu, runs a command...) close it FIRST: an
        // inventory opened on top of a dialogue that then closes would take the menu down with it.
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

    // ViaVersion lets clients older than 1.21.6 in, and they have no dialogue screens.
    public static boolean oldClient(Player p) {
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

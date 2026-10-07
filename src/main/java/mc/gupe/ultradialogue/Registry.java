package mc.gupe.ultradialogue;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * Lee dialogues/ (subcarpetas incluidas) y avisa en consola de cada referencia rota.
 * Cada clave se acepta en ingles y en español (text/texto, answers/respuestas...): los
 * archivos escritos para el plugin viejo "Dialogos" cargan sin tocarlos.
 */
public final class Registry {

    private final UltraDialogue pl;
    private final Map<String, Dialogue> byId = new LinkedHashMap<>();
    private final Map<String, Dialogue> byNpc = new HashMap<>();
    private int errors;

    public Registry(UltraDialogue pl) { this.pl = pl; }

    public int load() {
        byId.clear();
        byNpc.clear();
        errors = 0;
        List<File> files = new ArrayList<>();
        collect(pl.view().file("dialogues"), files);
        files.sort(Comparator.comparing(File::getName));
        for (File f : files) {
            try {
                read(f);
            } catch (Exception e) {
                errors++;
                pl.getLogger().warning(f.getName() + ": " + e.getMessage());
            }
        }
        validate();
        pl.portraits().preload(byId.values());
        pl.getLogger().info(byId.size() + " dialogues, " + byNpc.size() + " NPCs linked.");
        return byId.size();
    }

    private static void collect(File dir, List<File> out) {
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.isDirectory()) collect(f, out);
            else if (f.getName().endsWith(".yml")) out.add(f);
        }
    }

    private void read(File f) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.load(f);
        } catch (Exception e) {
            throw new IllegalStateException("invalid YAML - " + String.valueOf(e.getMessage()).split("\n")[0]);
        }
        String id = f.getName().substring(0, f.getName().length() - 4).toLowerCase(Locale.ROOT);
        if (byId.containsKey(id)) throw new IllegalStateException("duplicate id '" + id + "' (two files with that name)");

        List<Dialogue.Rule> start = new ArrayList<>();
        Object st = any(y, "start", "inicio");
        if (st instanceof List<?> l) {
            for (Object o : l) if (o instanceof Map<?, ?> m) start.add(new Dialogue.Rule(list(any(m, "if", "si")), str(any(m, "node", "nodo"))));
        } else if (st != null) {
            start.add(new Dialogue.Rule(List.of(), String.valueOf(st)));
        }

        List<String> npcs = new ArrayList<>();
        for (String n : list(y.get("npcs"))) npcs.add(n.toLowerCase(Locale.ROOT));

        Object cols = any(y, "columns", "columnas");
        Dialogue d = new Dialogue(id, strOr(any(y, "name", "nombre"), id), strOr(any(y, "portrait", "retrato"), "npc"),
                str(any(y, "sound", "sonido")), cols instanceof Number n ? n.intValue() : 0, npcs, start);

        d.look = look(id, "(dialogue)", y.getValues(false));

        if (any(y, "affinity", "afinidad") instanceof ConfigurationSection af) {
            if (af.contains("min")) d.affinityMin = af.getInt("min");
            if (af.contains("max")) d.affinityMax = af.getInt("max");
            if (any(af, "cooldown-minutes", "cooldown-minutos") instanceof Number cd) d.affinityCooldown = cd.doubleValue();
            if (any(af, "max-per-talk", "max-por-charla") instanceof Number per) d.affinityPerTalk = per.intValue();
        }

        Object ns = any(y, "nodes", "nodos");
        if (!(ns instanceof ConfigurationSection sec) || sec.getKeys(false).isEmpty())
            throw new IllegalStateException("has no 'nodes'");
        for (String k : sec.getKeys(false)) {
            ConfigurationSection n = sec.getConfigurationSection(k);
            if (n == null) continue;
            List<Dialogue.Answer> as = new ArrayList<>();
            Object ansList = any(n, "answers", "respuestas");
            if (ansList instanceof List<?> l) {
                for (Object o : l) if (o instanceof Map<?, ?> m) as.add(answer(id, k, m));
            }
            // nav: [back, exit] o respuestas completas (un "Premio" con sus acciones).
            List<Dialogue.Answer> nav = new ArrayList<>();
            boolean navExit = false;
            for (Object o : any(n, "nav", "navegacion", "navegación") instanceof List<?> l ? l : List.of()) {
                if (o instanceof Map<?, ?> m) { nav.add(answer(id, k, m)); continue; }
                switch (String.valueOf(o).trim().toLowerCase(Locale.ROOT)) {
                    case "back", "volver" -> nav.add(new Dialogue.Answer(null, null, List.of(), Dialogue.BACK,
                            List.of(), true, null, 0, null));
                    case "exit", "salir" -> {
                        // Se guarda en su lugar: en modo layout Salir va en la fila de nav, en ese orden.
                        navExit = true;
                        nav.add(new Dialogue.Answer(null, null, List.of(), Dialogue.EXIT, List.of(), true, null, 0, null));
                    }
                    default -> warn(id, k + ": unknown nav button '" + o + "' (back | exit | a full answer)");
                }
            }
            // Frases alternativas: cada una es una linea, una lista de lineas, o {text, if}.
            List<Dialogue.Variant> vs = new ArrayList<>();
            if (any(n, "variants", "variantes") instanceof List<?> l) {
                for (Object o : l) {
                    if (o instanceof Map<?, ?> m)
                        vs.add(new Dialogue.Variant(String.join("\n", list(any(m, "text", "texto"))), list(any(m, "if", "si"))));
                    else
                        vs.add(new Dialogue.Variant(String.join("\n", list(o)), List.of()));
                }
            }
            String text = String.join("\n", list(any(n, "text", "texto")));
            if (text.isEmpty() && vs.isEmpty()) warn(id, k + ": node has no 'text' nor 'variants'");
            d.nodes.put(k, new Dialogue.Node(k, str(any(n, "speaker", "name", "nombre")), text, vs,
                    list(any(n, "on-show", "al-mostrar")), as, str(any(n, "next", "siguiente")),
                    any(n, "show", "mostrar") instanceof Number num ? num.intValue() : 0,
                    any(n, "columns", "columnas") instanceof Number c ? c.intValue() : 0,
                    str(any(n, "title", "titulo", "título")), nav, navExit, layout(id, k, any(n, "layout", "filas")),
                    look(id, k, n.getValues(false))));
        }

        byId.put(id, d);
        for (String npc : npcs) {
            Dialogue before = byNpc.put(npc, d);
            if (before != null) warn(id, "NPC '" + npc + "' was already in '" + before.id + "', this one wins");
        }
    }

    private void validate() {
        for (Dialogue d : byId.values()) {
            for (Dialogue.Rule r : d.start) {
                node(d, r.node(), "start");
                conditions(d, "start", r.conditions());
            }
            for (Dialogue.Node n : d.nodes.values()) {
                if (n.next() != null) node(d, n.next(), n.id() + ".next");
                actions(d, n.id() + ".on-show", n.onShow());
                layout(d, n);
                List<Dialogue.Answer> all = new ArrayList<>(n.answers());
                for (Dialogue.Answer a : n.nav()) if (!Dialogue.BACK.equals(a.goTo()) && !Dialogue.EXIT.equals(a.goTo())) all.add(a);
                for (Dialogue.Answer a : all) {
                    if (a.text() == null && a.icon() == null) warn(d.id, n.id() + ": an answer has no 'text' nor 'icon'");
                    if (a.locked() != null && a.conditions().isEmpty())
                        warn(d.id, n.id() + ": 'locked' does nothing without an 'if'");
                    if (a.icon() != null)
                        for (String bad : Icons.unknown(a.icon()))
                            warnOnce(d.id, "unknown icon '" + bad + "' (see /ud icons); the button shows without it");
                    if (a.goTo() != null) node(d, a.goTo(), n.id() + ".goto");
                    conditions(d, n.id(), a.conditions());
                    actions(d, n.id(), a.actions());
                }
            }
        }
    }

    /**
     * Minecraft reparte los botones en filas iguales y solo deja un boton solo y centrado si sobra
     * al final. Si las cuentas no dan, el boton ancho termina compartiendo fila: se avisa al cargar.
     * Solo se puede contar cuando ningun boton depende de un if (o los que dependen tienen locked).
     */
    private void layout(Dialogue d, Dialogue.Node n) {
        if (!n.layout().isEmpty()) {
            int sum = n.layout().stream().mapToInt(Integer::intValue).sum();
            boolean stable = n.show() <= 0 && n.answers().stream().allMatch(a -> a.conditions().isEmpty() || a.locked() != null);
            if (stable && sum != n.answers().size())
                warn(d.id, n.id() + ": layout has room for " + sum + " buttons but there are " + n.answers().size()
                        + " answers (extra ones go one per row, nav buttons get their own last row)");
            return;
        }
        int cols = n.columns() > 0 ? n.columns() : d.columns > 0 ? d.columns : pl.config().getInt("screen.columns", 1);
        if (cols < 2) return;
        long wide = n.answers().stream().filter(Dialogue.Answer::wide).count();
        if (wide == 0) return;
        if (wide > 1) { warn(d.id, n.id() + ": only one full-width answer fits per screen with " + cols + " columns"); return; }
        if (n.show() > 0) return;
        for (Dialogue.Answer a : n.answers()) if (!a.conditions().isEmpty() && a.locked() == null) return;
        int rest = n.answers().size() - 1 + (int) n.nav().stream().filter(a -> !Dialogue.EXIT.equals(a.goTo())).count();
        if (rest % cols != 0)
            warn(d.id, n.id() + ": the full-width answer will share its row: the other " + rest
                    + " buttons must fill whole rows of " + cols);
    }

    private void actions(Dialogue d, String where, List<String> acts) {
        for (String a : acts) {
            String[] t = Actions.split(a);
            if (!Actions.TYPES.contains(t[0])) warn(d.id, where + ": unknown action '" + a + "'");
            else if (t[0].equals("goto")) node(d, t[1], where);
            else if (t[0].equals("take") || t[0].equals("give")) {
                Items.Want w = Items.parse(t[1], 1);
                if (!Items.valid(w.spec())) warn(d.id, where + ": unknown item '" + w.spec() + "' in '" + a + "'");
                else if (t[0].equals("give") && !Items.givable(w.spec()))
                    warn(d.id, where + ": '" + w.spec() + "' can be checked or taken, but not given");
            }
            else if (t[0].equals("quest")) {
                String[] x = t[1].trim().split("\\s+", 2);
                String op = x[0].toLowerCase(Locale.ROOT);
                boolean signal = op.equals("complete") || op.equals("completar");
                if (x.length < 2 || !(signal || java.util.Set.of("start", "empezar", "force", "forzar").contains(op)))
                    warn(d.id, where + ": bad quest action '" + a + "' (complete <key> | start <id> | force <id>)");
                else if (!signal && pl.quests() == null)
                    warnOnce(d.id, "uses 'quest: " + op + "' but BeautyQuests is not installed: it will do nothing");
            }
            else if (t[0].equals("affinity") && Actions.parseAffinity(t[1], d.id) == null)
                warn(d.id, where + ": bad affinity action '" + a + "' (use +2, -3, =10 or '<id> +2')");
            else if (t[0].equals("dialogue")) {
                String[] p = t[1].split("\\s+");
                Dialogue o = byId.get(p[0].toLowerCase(Locale.ROOT));
                if (o == null) warn(d.id, where + ": there is no dialogue '" + p[0] + "'");
                else if (p.length > 1) node(o, p[1], where);
            }
        }
    }

    private final java.util.Set<String> warnedOnce = new java.util.HashSet<>();

    private void warnOnce(String id, String s) {
        if (warnedOnce.add(id + s)) pl.getLogger().warning("[" + id + "] " + s);
    }

    /** Condiciones de objetos y misiones: que el objeto exista y que haya plugin de misiones. */
    private void conditions(Dialogue d, String where, List<String> lines) {
        for (String line : lines) for (String part : line.split("\\s*\\|\\|\\s*")) {
            String c = part.trim();
            if (c.startsWith("!") && !c.startsWith("!=")) c = c.substring(1).trim();
            String low = c.toLowerCase(Locale.ROOT);
            for (String pre : new String[]{"has:", "tiene:", "hand:", "mano:"})
                if (low.startsWith(pre)) {
                    String spec = Items.parse(c.substring(pre.length()), 1).spec();
                    if (!Items.valid(spec)) warn(d.id, where + ": unknown item '" + spec + "' in condition '" + part.trim() + "'");
                }
            for (String pre : new String[]{"quest:", "mision:", "misión:"})
                if (low.startsWith(pre)) {
                    String[] x = c.substring(pre.length()).trim().split("\\s+", 2);
                    if (x.length < 2 || !java.util.Set.of("active", "activa", "completed", "completada", "terminada",
                            "started", "en-curso", "empezada").contains(x[0].toLowerCase(Locale.ROOT)))
                        warn(d.id, where + ": bad quest condition '" + part.trim() + "' (active <key> | completed <id> | started <id>)");
                    else if (pl.quests() == null)
                        warnOnce(d.id, "uses quest conditions but BeautyQuests is not installed: they will be false");
                }
        }
    }

    private void node(Dialogue d, String n, String where) {
        if (n == null || Dialogue.isEnd(n) || d.nodes.containsKey(n)) return;
        warn(d.id, where + ": node '" + n + "' does not exist");
    }

    private void warn(String id, String s) {
        errors++;
        pl.getLogger().warning("[" + id + "] " + s);
    }

    /** layout: [1, 2, 2, 1] o '1-2-2-1'. */
    private List<Integer> layout(String id, String node, Object o) {
        if (o == null) return List.of();
        List<String> parts = o instanceof List<?> ? list(o) : List.of(String.valueOf(o).split("[\\s,\\-]+"));
        List<Integer> r = new ArrayList<>();
        for (String s : parts) {
            try {
                int v = Integer.parseInt(s.trim());
                if (v < 1 || v > 6) { warn(id, node + ": a layout row must have 1 to 6 buttons, got " + v); continue; }
                r.add(v);
            } catch (NumberFormatException e) {
                warn(id, node + ": bad layout '" + o + "' (use [1, 2, 2, 1])");
                return List.of();
            }
        }
        return r;
    }

    private Dialogue.Answer answer(String id, String node, Map<?, ?> m) {
        Object lock = any(m, "locked", "bloqueada", "bloqueado");
        String locked = lock == null || Boolean.FALSE.equals(lock) ? null : Boolean.TRUE.equals(lock) ? "" : String.valueOf(lock);
        return new Dialogue.Answer(str(any(m, "text", "texto")), str(m.get("tooltip")),
                list(any(m, "if", "si")), str(any(m, "goto", "ir")), list(any(m, "actions", "acciones")),
                Boolean.TRUE.equals(any(m, "always", "siempre")),
                str(any(m, "icon", "icono")), buttonWidth(id, node, m), locked, look(id, node, m));
    }

    /** estilo / borde / color-borde / color-fondo (en ingles: style / border / border-color / fill-color). */
    private Dialogue.Look look(String id, String node, Map<?, ?> m) {
        String style = str(any(m, "style", "estilo")), border = str(any(m, "border", "borde"));
        String bc = str(any(m, "border-color", "color-borde")), fc = str(any(m, "fill-color", "color-fondo"));
        if (style == null && border == null && bc == null && fc == null) return Dialogue.Look.NONE;
        if (!Buttons.validFill(style)) warn(id, node + ": unknown style '" + style + "' (see the style list in the docs)");
        if (!Buttons.validBorder(border)) warn(id, node + ": unknown border '" + border + "' (" + String.join(", ", Buttons.BORDERS) + ", ninguno)");
        for (String c : new String[]{bc, fc})
            if (c != null && net.kyori.adventure.text.format.TextColor.fromHexString(c) == null)
                warn(id, node + ": bad colour '" + c + "' (use '#RRGGBB')");
        return new Dialogue.Look(style, border, bc, fc);
    }

    /** "size: 1-4" o "width: N" (tamano / ancho). 0 = el ancho de config.yml. */
    private int buttonWidth(String id, String node, Map<?, ?> m) {
        Object w = any(m, "width", "ancho");
        if (w instanceof Number n) {
            if (n.intValue() < 1 || n.intValue() > 1024) warn(id, node + ": width must be 1-1024, got " + n);
            return Math.max(1, Math.min(1024, n.intValue()));
        }
        Object size = any(m, "size", "tamano", "tamaño");
        if (size instanceof Number n) {
            int px = Dialogue.widthForSize(n.intValue());
            if (px == 0) warn(id, node + ": size must be 1, 2, 3 or 4, got " + n);
            return px;
        }
        return 0;
    }

    private static Object any(ConfigurationSection s, String... keys) {
        for (String k : keys) if (s.contains(k)) return s.get(k);
        return null;
    }

    private static Object any(Map<?, ?> m, String... keys) {
        for (String k : keys) if (m.containsKey(k)) return m.get(k);
        return null;
    }

    private static String str(Object o) { return o == null ? null : String.valueOf(o); }
    private static String strOr(Object o, String def) { return o == null ? def : String.valueOf(o); }

    private static List<String> list(Object o) {
        if (o == null) return List.of();
        if (o instanceof List<?> l) {
            List<String> r = new ArrayList<>();
            for (Object x : l) r.add(String.valueOf(x));
            return r;
        }
        return List.of(String.valueOf(o));
    }

    public Dialogue byId(String id) { return id == null ? null : byId.get(id.toLowerCase(Locale.ROOT)); }
    public Dialogue byNpc(String npc) { return npc == null ? null : byNpc.get(npc.toLowerCase(Locale.ROOT)); }
    public Collection<Dialogue> all() { return byId.values(); }
    public int linked() { return byNpc.size(); }
    public int errors() { return errors; }
}

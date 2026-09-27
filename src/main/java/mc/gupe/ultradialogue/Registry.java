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
                for (Object o : l) {
                    if (!(o instanceof Map<?, ?> m)) continue;
                    as.add(new Dialogue.Answer(str(any(m, "text", "texto")), str(m.get("tooltip")),
                            list(any(m, "if", "si")), str(any(m, "goto", "ir")), list(any(m, "actions", "acciones")),
                            Boolean.TRUE.equals(any(m, "always", "siempre")),
                            str(any(m, "icon", "icono")), buttonWidth(id, k, m)));
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
                    any(n, "columns", "columnas") instanceof Number c ? c.intValue() : 0));
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
                for (Dialogue.Answer a : n.answers()) {
                    if (a.text() == null && a.icon() == null) warn(d.id, n.id() + ": an answer has no 'text' nor 'icon'");
                    if (a.icon() != null && Icons.resolve(a.icon()) == null)
                        warn(d.id, n.id() + ": unknown icon '" + a.icon() + "' (see /ud icons)");
                    if (a.goTo() != null) node(d, a.goTo(), n.id() + ".goto");
                    conditions(d, n.id(), a.conditions());
                    actions(d, n.id(), a.actions());
                }
            }
        }
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

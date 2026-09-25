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
                            list(any(m, "if", "si")), str(any(m, "goto", "ir")), list(any(m, "actions", "acciones"))));
                }
            }
            d.nodes.put(k, new Dialogue.Node(k, str(any(n, "speaker", "name", "nombre")),
                    String.join("\n", list(any(n, "text", "texto"))),
                    list(any(n, "on-show", "al-mostrar")), as, str(any(n, "next", "siguiente"))));
        }

        byId.put(id, d);
        for (String npc : npcs) {
            Dialogue before = byNpc.put(npc, d);
            if (before != null) warn(id, "NPC '" + npc + "' was already in '" + before.id + "', this one wins");
        }
    }

    private void validate() {
        for (Dialogue d : byId.values()) {
            for (Dialogue.Rule r : d.start) node(d, r.node(), "start");
            for (Dialogue.Node n : d.nodes.values()) {
                if (n.next() != null) node(d, n.next(), n.id() + ".next");
                actions(d, n.id() + ".on-show", n.onShow());
                for (Dialogue.Answer a : n.answers()) {
                    if (a.text() == null) warn(d.id, n.id() + ": an answer has no 'text'");
                    if (a.goTo() != null) node(d, a.goTo(), n.id() + ".goto");
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
            else if (t[0].equals("dialogue")) {
                String[] p = t[1].split("\\s+");
                Dialogue o = byId.get(p[0].toLowerCase(Locale.ROOT));
                if (o == null) warn(d.id, where + ": there is no dialogue '" + p[0] + "'");
                else if (p.length > 1) node(o, p[1], where);
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

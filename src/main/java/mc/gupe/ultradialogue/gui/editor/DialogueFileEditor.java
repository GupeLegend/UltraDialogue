package mc.gupe.ultradialogue.gui.editor;

import mc.gupe.ultradialogue.model.Dialogue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Writes one node's "answers:"/"nav:" lists and its "layout:" line back into a dialogue .yml file,
 * leaving every other line - the rest of that node, every other node, every comment, the whole
 * header - exactly as it was.
 *
 * The button editor menu (see {@link EditorManager}) keeps its edits in memory and only calls
 * {@link #save} once, when the admin presses "Save". That one call finds the node by its
 * indentation (not with YamlConfiguration, which would drop every comment in the file) and
 * replaces just the lines of its answers, nav and layout with freshly written ones. Everything a
 * button already had - its goto, conditions, actions, tooltip... - is carried over as-is; only
 * text, icon and size/position actually change from what the admin picked in the menu, and only
 * for that one node. Any per-item comments inside the REPLACED lists are lost, but the rest of the
 * file, including every other node, is untouched.
 */
public final class DialogueFileEditor {

    public static final class EditError extends Exception {
        public EditError(String message) { super(message); }
    }

    private DialogueFileEditor() {}

    private static int indent(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') i++;
        return i;
    }

    private static boolean skippable(String s) {
        String t = s.trim();
        return t.isEmpty() || t.startsWith("#");
    }

    private static boolean keyLine(String trimmed, String... keys) {
        for (String k : keys) if (trimmed.matches("^" + Pattern.quote(k) + ":(\\s*(#.*)?)?$")) return true;
        return false;
    }

    private static List<String> readLines(File file) throws IOException {
        return new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
    }

    private record Node(int start, int end, int indent) {}

    private static Node locateNode(List<String> lines, String nodeId) throws EditError {
        int nodesLine = -1;
        for (int i = 0; i < lines.size(); i++)
            if (indent(lines.get(i)) == 0 && keyLine(lines.get(i).trim(), "nodes", "nodos")) { nodesLine = i; break; }
        if (nodesLine < 0) throw new EditError("'nodes:' not found in the file");

        int nodeIndent = -1, nodeStart = -1;
        for (int i = nodesLine + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (skippable(line)) continue;
            int ind = indent(line);
            if (ind == 0) break;
            if (nodeIndent < 0) nodeIndent = ind;
            if (ind != nodeIndent) continue;
            String t = line.trim();
            if (t.equals(nodeId + ":") || t.startsWith(nodeId + ": ") || t.startsWith(nodeId + ":#")) { nodeStart = i; break; }
        }
        if (nodeStart < 0) throw new EditError("node '" + nodeId + "' not found");

        int nodeEnd = lines.size();
        for (int i = nodeStart + 1; i < lines.size(); i++) {
            if (skippable(lines.get(i))) continue;
            if (indent(lines.get(i)) <= nodeIndent) { nodeEnd = i; break; }
        }
        return new Node(nodeStart, nodeEnd, nodeIndent);
    }

    /** The span [start,end) of an existing "key:" block inside the node, or null if it has none. */
    private record Block(int keyLine, int end, int indent) {}

    private static Block findBlock(List<String> lines, Node node, String... keys) {
        int keyLine = -1, keyIndent = -1;
        for (int i = node.start() + 1; i < node.end(); i++) {
            if (skippable(lines.get(i))) continue;
            if (keyLine(lines.get(i).trim(), keys)) { keyLine = i; keyIndent = indent(lines.get(i)); break; }
        }
        if (keyLine < 0) return null;
        int end = node.end();
        for (int i = keyLine + 1; i < node.end(); i++) {
            if (skippable(lines.get(i))) continue;
            if (indent(lines.get(i)) <= keyIndent) { end = i; break; }
        }
        return new Block(keyLine, end, keyIndent);
    }

    /** Unlike {@link #findBlock}, "layout:"/"filas:" always has its value on the same line. */
    private static int findLayoutLine(List<String> lines, Node node) {
        for (int i = node.start() + 1; i < node.end(); i++) {
            if (skippable(lines.get(i))) continue;
            String t = lines.get(i).trim();
            if (t.matches("^(layout|filas):.*$")) return i;
        }
        return -1;
    }

    // ---------------------------------------------------------------------------------- rendering

    private static String scalar(String value) { return "'" + value.replace("'", "''") + "'"; }

    private static void render(List<String> out, int indent, Dialogue.Answer a, boolean nav) {
        String pad = " ".repeat(indent), pad2 = " ".repeat(indent + 2), pad4 = " ".repeat(indent + 4);
        if (nav && a.text() == null && a.icon() == null && a.conditions().isEmpty() && a.actions().isEmpty()) {
            if (Dialogue.BACK.equals(a.goTo())) { out.add(pad + "- back"); return; }
            if (Dialogue.EXIT.equals(a.goTo())) { out.add(pad + "- exit"); return; }
        }
        out.add(pad + "- text: " + (a.text() == null ? "''" : scalar(a.text())));
        if (a.icon() != null) out.add(pad2 + "icon: " + a.icon());
        if (a.tooltip() != null) out.add(pad2 + "tooltip: " + scalar(a.tooltip()));
        if (a.conditions().size() == 1) {
            out.add(pad2 + "if: " + scalar(a.conditions().get(0)));
        } else if (!a.conditions().isEmpty()) {
            out.add(pad2 + "if:");
            for (String c : a.conditions()) out.add(pad4 + "- " + scalar(c));
        }
        if (!Dialogue.BACK.equals(a.goTo()) && !Dialogue.EXIT.equals(a.goTo()) && a.goTo() != null)
            out.add(pad2 + "goto: " + a.goTo());
        if (a.actions().size() == 1) {
            out.add(pad2 + "actions: [" + scalar(a.actions().get(0)) + "]");
        } else if (!a.actions().isEmpty()) {
            out.add(pad2 + "actions:");
            for (String c : a.actions()) out.add(pad4 + "- " + scalar(c));
        }
        if (a.always()) out.add(pad2 + "always: true");
        int size = presetSize(a.width());
        if (size > 0) out.add(pad2 + "size: " + size);
        else if (a.width() > 0) out.add(pad2 + "width: " + a.width());
        if (a.locked() != null) out.add(pad2 + "locked: " + (a.locked().isEmpty() ? "true" : scalar(a.locked())));
    }

    private static int presetSize(int width) {
        for (int size = 1; size <= 4; size++) if (Dialogue.widthForSize(size) == width) return size;
        return 0;
    }

    /** Replaces (or inserts) the node's answers/nav list and layout line with the given final state. */
    public static void save(File file, String nodeId, List<Dialogue.Answer> answers, List<Dialogue.Answer> nav,
                            List<Integer> layout) throws IOException, EditError {
        List<String> lines = readLines(file);

        Node node = locateNode(lines, nodeId);
        Block answersBlock = findBlock(lines, node, "answers", "respuestas");
        int bodyIndent = answersBlock != null ? answersBlock.indent() + 2 : node.indent() + 2;
        List<String> rendered = new ArrayList<>();
        if (!answers.isEmpty()) {
            rendered.add(" ".repeat(answersBlock != null ? answersBlock.indent() : node.indent() + 2) + "answers:");
            for (Dialogue.Answer a : answers) render(rendered, bodyIndent, a, false);
        }
        lines = replace(lines, answersBlock != null ? answersBlock.keyLine() : -1,
                answersBlock != null ? answersBlock.end() : node.end(), node.end(), rendered);

        node = locateNode(lines, nodeId);
        Block navBlock = findBlock(lines, node, "nav", "navegacion", "navegación");
        int navIndent = navBlock != null ? navBlock.indent() + 2 : node.indent() + 2;
        List<String> renderedNav = new ArrayList<>();
        if (!nav.isEmpty()) {
            renderedNav.add(" ".repeat(navBlock != null ? navBlock.indent() : node.indent() + 2) + "nav:");
            for (Dialogue.Answer a : nav) render(renderedNav, navIndent, a, true);
        }
        lines = replace(lines, navBlock != null ? navBlock.keyLine() : -1,
                navBlock != null ? navBlock.end() : node.end(), node.end(), renderedNav);

        if (!layout.isEmpty()) {
            node = locateNode(lines, nodeId);
            int layoutLine = findLayoutLine(lines, node);
            if (layoutLine >= 0) {
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < layout.size(); i++) { if (i > 0) sb.append(", "); sb.append(layout.get(i)); }
                sb.append(']');
                String key = lines.get(layoutLine).trim().split(":", 2)[0].trim();
                lines.set(layoutLine, " ".repeat(indent(lines.get(layoutLine))) + key + ": " + sb);
            }
        }

        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
    }

    /** Replaces lines [from,to) with {@code block} (insertion point {@code insertAt} if from &lt; 0). */
    private static List<String> replace(List<String> lines, int from, int to, int insertAt, List<String> block) {
        List<String> out = new ArrayList<>();
        if (from < 0) {
            out.addAll(lines.subList(0, insertAt));
            out.addAll(block);
            out.addAll(lines.subList(insertAt, lines.size()));
        } else {
            out.addAll(lines.subList(0, from));
            out.addAll(block);
            out.addAll(lines.subList(to, lines.size()));
        }
        return out;
    }
}

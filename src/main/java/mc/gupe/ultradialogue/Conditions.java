package mc.gupe.ultradialogue;

import org.bukkit.entity.Player;

import java.util.List;

/**
 * Todas las lineas de una lista tienen que cumplirse (Y). Dentro de una linea, " || " es O.
 *   permission:node     !permission:node      (tambien permiso:)
 *   flag:name           !flag:name            (tambien marca:)
 *   %placeholder% >= 10     ( ==  !=  >=  <=  >  <  contains )
 *   affinity >= 20          con el personaje de este dialogo   (tambien afinidad)
 *   affinity:kadir >= 20    con otro personaje
 */
public final class Conditions {

    private static final String[] OPS = {">=", "<=", "!=", "==", ">", "<", " contains ", " contiene "};
    private static final java.util.regex.Pattern AFFINITY =
            java.util.regex.Pattern.compile("(?i)^(affinity|afinidad)(?::(\\S+))?\\s*(>=|<=|!=|==|>|<)\\s*(-?\\d+)$");

    private Conditions() {}

    /** @param dialogueId el dialogo desde donde se pregunta (para "affinity" sin id). */
    public static boolean test(Player p, List<String> lines, String dialogueId) {
        for (String l : lines) if (!line(p, l, dialogueId)) return false;
        return true;
    }

    private static boolean line(Player p, String l, String dialogueId) {
        for (String part : l.split("\\s*\\|\\|\\s*")) if (one(p, part.trim(), dialogueId)) return true;
        return false;
    }

    private static boolean one(Player p, String c, String dialogueId) {
        if (c.isEmpty()) return true;
        boolean not = c.startsWith("!") && !c.startsWith("!=");
        String s = not ? c.substring(1).trim() : c;
        String low = s.toLowerCase();
        var am = AFFINITY.matcher(s.trim());
        if (am.matches()) {
            String id = am.group(2) != null ? am.group(2) : dialogueId;
            if (id == null) return false;
            double v = Affinity.get(p, id);
            return not != compare(String.valueOf((int) v), am.group(3), am.group(4));
        }
        for (String pre : new String[]{"permission:", "permiso:"})
            if (low.startsWith(pre)) return not != p.hasPermission(s.substring(pre.length()).trim());
        for (String pre : new String[]{"flag:", "marca:"})
            if (low.startsWith(pre)) return not != Flags.has(p, s.substring(pre.length()).trim());

        String t = Text.ph(p, s);
        for (String op : OPS) {
            int i = t.indexOf(op);
            if (i < 0) continue;
            boolean r = compare(t.substring(0, i).trim(), op.trim(), t.substring(i + op.length()).trim());
            return not != r;
        }
        UltraDialogue.get().getLogger().warning("Condition not understood: '" + c + "'");
        return false;
    }

    private static boolean compare(String a, String op, String b) {
        if (op.equals("contains") || op.equals("contiene")) return a.toLowerCase().contains(b.toLowerCase());
        Double x = num(a), y = num(b);
        if (x != null && y != null) {
            return switch (op) {
                case ">=" -> x >= y;
                case "<=" -> x <= y;
                case ">" -> x > y;
                case "<" -> x < y;
                case "!=" -> !x.equals(y);
                default -> x.equals(y);
            };
        }
        return switch (op) {
            case "==" -> a.equalsIgnoreCase(b);
            case "!=" -> !a.equalsIgnoreCase(b);
            default -> false;
        };
    }

    // Acepta "1.234,5" y "$1,234.50", que es como vienen muchos placeholders de dinero.
    private static Double num(String s) {
        String t = s.replaceAll("[$€\\s%]", "");
        if (!t.matches("-?[0-9][0-9.,]*")) return null;
        if (t.contains(",") && t.contains(".")) {
            t = t.lastIndexOf(',') > t.lastIndexOf('.') ? t.replace(".", "").replace(',', '.') : t.replace(",", "");
        } else {
            t = t.replace(',', '.');
        }
        try { return Double.parseDouble(t); } catch (NumberFormatException e) { return null; }
    }
}

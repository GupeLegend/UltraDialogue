package mc.gupe.ultradialogue;

import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Un dialogo = un archivo de dialogues/. Se arma una vez al cargar y no se modifica despues. */
public final class Dialogue {

    public final String id;
    public final String name;
    public final String portrait;
    public final String sound;
    public final int columns;
    public final List<String> npcs;
    public final List<Rule> start;
    public final Map<String, Node> nodes = new LinkedHashMap<>();

    // Afinidad propia de este personaje (null = usar la de config.yml).
    public Integer affinityMin, affinityMax, affinityPerTalk;
    public Double affinityCooldown;

    public Dialogue(String id, String name, String portrait, String sound, int columns,
                    List<String> npcs, List<Rule> start) {
        this.id = id;
        this.name = name;
        this.portrait = portrait;
        this.sound = sound;
        this.columns = columns;
        this.npcs = npcs;
        this.start = start;
    }

    /** El nodo con el que arranca la charla: la primera regla de start que se cumpla. */
    public String firstNode(Player p) {
        for (Rule r : start) if (Conditions.test(p, r.conditions(), id)) return r.node();
        return nodes.isEmpty() ? null : nodes.keySet().iterator().next();
    }

    public static boolean isEnd(String node) {
        return node != null && (node.equalsIgnoreCase("end") || node.equalsIgnoreCase("fin"));
    }

    public record Rule(List<String> conditions, String node) {}

    /**
     * Un turno del NPC.
     * {@code variants}: frases alternativas; si hay, cada vez sale una al azar (entre las que
     * cumplan su if) en lugar de {@code text}.
     * {@code show}: si es mayor que 0, de las respuestas que no son {@code always} se muestran
     * solo esa cantidad, elegidas al azar cada vez.
     * {@code columns}: botones por fila solo en este nodo (0 = los del dialogo o config.yml).
     */
    public record Node(String id, String speaker, String text, List<Variant> variants, List<String> onShow,
                       List<Answer> answers, String next, int show, int columns) {}

    public record Variant(String text, List<String> conditions) {}

    /**
     * Una respuesta del jugador. {@code goTo} lleva a otro nodo; {@code actions} corren antes.
     * {@code icon}: nombre de un icono de {@link Icons} (null = sin icono).
     * {@code width}: ancho del boton en pixeles (0 = el de config.yml).
     */
    public record Answer(String text, String tooltip, List<String> conditions, String goTo,
                         List<String> actions, boolean always, String icon, int width) {

        public Answer(String text, String tooltip, List<String> conditions, String goTo,
                      List<String> actions, boolean always) {
            this(text, tooltip, conditions, goTo, actions, always, null, 0);
        }
    }

    /** size: 1 a 4 -> los anchos de los botones del menu de pausa de Minecraft. */
    public static int widthForSize(int size) {
        return switch (size) {
            case 1 -> 20;    // el cuadradito de "reportar"
            case 2 -> 98;    // "Opciones...", medio ancho
            case 3 -> 150;
            case 4 -> 204;   // "Desconectar", ancho completo
            default -> 0;
        };
    }
}

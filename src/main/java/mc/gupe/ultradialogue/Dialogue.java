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

    /** Aspecto de los botones RPG en todo el dialogo (lo que no diga, sale de config.yml). */
    public Look look = Look.NONE;

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
     * {@code title}: encabezado de la pantalla solo en este nodo (null = el nombre del personaje).
     * {@code nav}: botones de navegacion (volver, premio...) que van en la ultima fila.
     * {@code navExit}: un "Salir" fijo abajo de todo, el unico lugar fijo que da Minecraft.
     * {@code layout}: botones por fila, fila por fila ([1, 2, 2, 1]). Vacio = la cuadricula de
     * Minecraft. Con layout los botones se dibujan como texto clicable dentro del cuerpo, que no
     * tiene la regla de "todas las filas iguales".
     */
    public record Node(String id, String speaker, String text, List<Variant> variants, List<String> onShow,
                       List<Answer> answers, String next, int show, int columns,
                       String title, List<Answer> nav, boolean navExit, List<Integer> layout, Look look) {}

    /**
     * Aspecto de un boton RPG (modo layout con el pack): fondo, borde y sus colores. Cada campo null
     * = lo hereda del nodo, del dialogo o de config.yml, en ese orden.
     */
    public record Look(String style, String border, String borderColor, String fillColor) {
        public static final Look NONE = new Look(null, null, null, null);

        /** Este aspecto, completando lo que falte con {@code base}. */
        public Look over(Look base) {
            if (base == null) return this;
            return new Look(style != null ? style : base.style, border != null ? border : base.border,
                    borderColor != null ? borderColor : base.borderColor, fillColor != null ? fillColor : base.fillColor);
        }
    }

    public record Variant(String text, List<String> conditions) {}

    /**
     * Una respuesta del jugador. {@code goTo} lleva a otro nodo; {@code actions} corren antes.
     * {@code icon}: nombre de un icono de {@link Icons} (null = sin icono).
     * {@code width}: ancho del boton en pixeles (0 = el de config.yml).
     * {@code locked}: si su if no se cumple, en vez de esconderse sale en gris con este aviso
     * ("" = el aviso del idioma; null = se esconde, como siempre).
     */
    public record Answer(String text, String tooltip, List<String> conditions, String goTo,
                         List<String> actions, boolean always, String icon, int width, String locked, Look look) {

        public Answer(String text, String tooltip, List<String> conditions, String goTo,
                      List<String> actions, boolean always) {
            this(text, tooltip, conditions, goTo, actions, always, null, 0, null, Look.NONE);
        }

        public Answer(String text, String tooltip, List<String> conditions, String goTo,
                      List<String> actions, boolean always, String icon, int width, String locked) {
            this(text, tooltip, conditions, goTo, actions, always, icon, width, locked, Look.NONE);
        }

        /** Boton a ancho completo: en pantallas de varias columnas va solo en la ultima fila. */
        public boolean wide() { return width >= WIDE; }
    }

    /** "goto" del boton Volver: vuelve a la pantalla anterior de esta charla. */
    public static final String BACK = "\u0000back";
    /** "goto" del boton Salir de nav: cierra la charla. */
    public static final String EXIT = "\u0000exit";
    public static final int WIDE = 204;

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

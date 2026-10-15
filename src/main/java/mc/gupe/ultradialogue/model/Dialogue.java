package mc.gupe.ultradialogue.model;

import mc.gupe.ultradialogue.util.Conditions;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One dialogue = one file in dialogues/. Built once when it loads and never changed afterwards. */
public final class Dialogue {

    public final String id;
    public final String name;
    public final String portrait;
    public final String sound;
    public final int columns;
    public final List<String> npcs;
    public final List<Rule> start;
    public final Map<String, Node> nodes = new LinkedHashMap<>();

    /** Appearance of the RPG buttons for this whole dialogue (whatever it doesn't say comes from config.yml). */
    public Look look = Look.NONE;

    // This character's own affinity settings (null = use config.yml's).
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

    /** The node the conversation opens on: the first "start" rule that matches. */
    public String firstNode(Player p) {
        for (Rule r : start) if (Conditions.test(p, r.conditions(), id)) return r.node();
        return nodes.isEmpty() ? null : nodes.keySet().iterator().next();
    }

    public static boolean isEnd(String node) {
        return node != null && (node.equalsIgnoreCase("end") || node.equalsIgnoreCase("fin"));
    }

    public record Rule(List<String> conditions, String node) {}

    /**
     * One turn of the NPC.
     * {@code variants}: alternative lines; if present, one is picked at random each time (among the
     * ones whose "if" passes) instead of {@code text}.
     * {@code show}: if greater than 0, only that many of the non-{@code always} answers are shown,
     * picked at random each time.
     * {@code columns}: buttons per row for this node only (0 = the dialogue's or config.yml's).
     * {@code title}: screen heading for this node only (null = the character's name).
     * {@code nav}: navigation buttons (back, reward...) that go in the last row.
     * {@code navExit}: a fixed "Exit" at the very bottom, the only fixed slot Minecraft gives.
     * {@code layout}: buttons per row, row by row ([1, 2, 2, 1]). Empty = Minecraft's own grid.
     * With a layout the buttons are drawn as clickable text in the body, which skips the
     * "every row must be the same size" rule.
     */
    public record Node(String id, String speaker, String text, List<Variant> variants, List<String> onShow,
                       List<Answer> answers, String next, int show, int columns,
                       String title, List<Answer> nav, boolean navExit, List<Integer> layout, Look look) {}

    /**
     * Appearance of an RPG button (layout mode with the pack): background, border and their
     * colours. Each null field is inherited from the node, the dialogue or config.yml, in that order.
     */
    public record Look(String style, String border, String borderColor, String fillColor) {
        public static final Look NONE = new Look(null, null, null, null);

        /** This look, filling in whatever is missing from {@code base}. */
        public Look over(Look base) {
            if (base == null) return this;
            return new Look(style != null ? style : base.style, border != null ? border : base.border,
                    borderColor != null ? borderColor : base.borderColor, fillColor != null ? fillColor : base.fillColor);
        }
    }

    public record Variant(String text, List<String> conditions) {}

    /**
     * One answer the player can pick. {@code goTo} leads to another node; {@code actions} run first.
     * {@code icon}: the name of an {@link mc.gupe.ultradialogue.gui.Icons} preset (null = no icon).
     * {@code width}: button width in pixels (0 = config.yml's).
     * {@code locked}: if its "if" fails, instead of hiding it shows greyed out with this notice
     * ("" = the language file's default; null = hidden, as usual).
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

        /** A full-width button: on multi-column screens it goes alone in the last row. */
        public boolean wide() { return width >= WIDE; }
    }

    /** "goto" of the nav Back button: returns to this conversation's previous screen. */
    public static final String BACK = "\u0000back";
    /** "goto" of the nav Exit button: closes the conversation. */
    public static final String EXIT = "\u0000exit";
    public static final int WIDE = 204;

    /** size: 1 to 4 -> the button widths from Minecraft's own pause menu. */
    public static int widthForSize(int size) {
        return switch (size) {
            case 1 -> 20;    // the small "report" square
            case 2 -> 98;    // "Options...", half width
            case 3 -> 150;
            case 4 -> 204;   // "Disconnect", full width
            default -> 0;
        };
    }
}

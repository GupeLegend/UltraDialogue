package mc.gupe.ultradialogue.migration;

import mc.gupe.ultradialogue.UltraDialogue;
import mc.gupe.ultradialogue.config.PluginData;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * UltraDialogue started out as a private plugin called "Dialogos" (plugins/Dialogos/, a dialogos/
 * folder, config.yml with Spanish keys). This brings that data over once:
 *   - the .yml files in dialogos/  ->  dialogues/   (read as-is: the parser accepts Spanish keys too)
 *   - config.yml's values, rewritten with the new keys and language: es
 * Nothing in the old folder is deleted. Player flags are bridged separately by {@link mc.gupe.ultradialogue.util.Flags}.
 */
public final class LegacyMigration {

    private static final String MARKER = ".migrado-de-dialogos";

    private LegacyMigration() {}

    public static void run(UltraDialogue pl, PluginData data) {
        File plugins = pl.getDataFolder().getParentFile();
        if (plugins == null) return;
        File old = new File(plugins, "Dialogos");
        if (!old.isDirectory() || data.file(MARKER).exists()) return;

        int n = 0;
        File oldDialogues = new File(old, "dialogos");
        if (oldDialogues.isDirectory()) n = copyMissing(oldDialogues, data.file("dialogues"));

        File oldConfig = new File(old, "config.yml");
        if (oldConfig.isFile() && !data.file("config.yml").exists()) convertConfig(pl, data, oldConfig);

        try { data.file(MARKER).createNewFile(); } catch (IOException ignored) {}
        pl.getLogger().info("Migrated from Dialogos: " + n + " dialogue file(s) copied into dialogues/."
                + " The old folder was not deleted.");
    }

    // old key -> new key (each is unique in the file, so it's replaced by name)
    private static final Map<String, String> KEYS = new LinkedHashMap<>();
    static {
        KEYS.put("modo", "mode");
        KEYS.put("formato", "format");
        KEYS.put("comando-menu", "menu-command");
        KEYS.put("pantalla.ancho-texto", "text-width");
        KEYS.put("pantalla.ancho-botones", "button-width");
        KEYS.put("pantalla.columnas", "columns");
        KEYS.put("pantalla.cerrar-con-esc", "close-with-esc");
        KEYS.put("pantalla.boton-salir", "exit-button");
        KEYS.put("click-izquierdo", "left-click");
        KEYS.put("cooldown-ms", "cooldown-ms");
        KEYS.put("distancia-max", "max-distance");
        KEYS.put("sonido-hablar", "talk-sound");
    }

    // The jar's config.yml text is edited line by line so its comments aren't lost.
    private static void convertConfig(UltraDialogue pl, PluginData data, File oldConfig) {
        data.saveResource("config.yml");
        File f = data.file("config.yml");
        try {
            var old = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(oldConfig);
            String txt = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            txt = put(txt, "language", "es");
            for (var e : KEYS.entrySet()) {
                if (!old.contains(e.getKey())) continue;
                Object v = old.get(e.getKey());
                if (e.getKey().equals("modo")) v = "chat".equalsIgnoreCase(String.valueOf(v)) ? "chat" : "dialog";
                txt = put(txt, e.getValue(), v);
            }
            Files.writeString(f.toPath(), txt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            pl.getLogger().warning("Could not convert the Dialogos config: " + e.getMessage());
        }
    }

    private static String put(String txt, String key, Object value) {
        String v = value instanceof String s ? "'" + s.replace("'", "''") + "'" : String.valueOf(value);
        return txt.replaceFirst("(?m)^(\\s*)" + java.util.regex.Pattern.quote(key) + ":.*$",
                "$1" + key + ": " + java.util.regex.Matcher.quoteReplacement(v));
    }

    private static int copyMissing(File from, File to) {
        File[] kids = from.listFiles();
        if (kids == null) return 0;
        if (!to.isDirectory()) to.mkdirs();
        int n = 0;
        for (File k : kids) {
            File d = new File(to, k.getName());
            if (k.isDirectory()) n += copyMissing(k, d);
            else if (!d.exists()) {
                try { Files.copy(k.toPath(), d.toPath()); n++; } catch (IOException ignored) {}
            }
        }
        return n;
    }
}

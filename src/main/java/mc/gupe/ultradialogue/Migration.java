package mc.gupe.ultradialogue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * UltraDialogue nacio como un plugin privado llamado "Dialogos" (plugins/Dialogos/, carpeta
 * dialogos/, config con claves en español). Esto trae esa data una sola vez:
 *   - los .yml de dialogos/  ->  dialogues/   (se leen tal cual: el parser acepta las claves en español)
 *   - los valores de config.yml, reescritos con las claves nuevas y language: es
 * No borra nada de la carpeta vieja. Las marcas de los jugadores las puentea {@link Flags}.
 */
public final class Migration {

    private static final String MARCA = ".migrado-de-dialogos";

    private Migration() {}

    public static void run(UltraDialogue pl, UltraView view) {
        File plugins = pl.getDataFolder().getParentFile();
        if (plugins == null) return;
        File old = new File(plugins, "Dialogos");
        if (!old.isDirectory() || view.file(MARCA).exists()) return;

        int n = 0;
        File oldDialogs = new File(old, "dialogos");
        if (oldDialogs.isDirectory()) n = copyMissing(oldDialogs, view.file("dialogues"));

        File oldConfig = new File(old, "config.yml");
        if (oldConfig.isFile() && !view.file("config.yml").exists()) convertConfig(pl, view, oldConfig);

        try { view.file(MARCA).createNewFile(); } catch (IOException ignored) {}
        pl.getLogger().info("Migracion desde Dialogos: " + n + " dialogos copiados a UltraView/UltraDialogue/dialogues/."
                + " La carpeta vieja NO se borro.");
    }

    // clave vieja -> clave nueva (todas son unicas en el archivo, asi que se reemplaza por nombre)
    private static final Map<String, String> CLAVES = new LinkedHashMap<>();
    static {
        CLAVES.put("modo", "mode");
        CLAVES.put("formato", "format");
        CLAVES.put("comando-menu", "menu-command");
        CLAVES.put("pantalla.ancho-texto", "text-width");
        CLAVES.put("pantalla.ancho-botones", "button-width");
        CLAVES.put("pantalla.columnas", "columns");
        CLAVES.put("pantalla.cerrar-con-esc", "close-with-esc");
        CLAVES.put("pantalla.boton-salir", "exit-button");
        CLAVES.put("click-izquierdo", "left-click");
        CLAVES.put("cooldown-ms", "cooldown-ms");
        CLAVES.put("distancia-max", "max-distance");
        CLAVES.put("sonido-hablar", "talk-sound");
    }

    // Se edita el texto del config del jar linea por linea para no perder sus comentarios.
    private static void convertConfig(UltraDialogue pl, UltraView view, File oldConfig) {
        view.saveResource("config.yml");
        File f = view.file("config.yml");
        try {
            var viejo = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(oldConfig);
            String txt = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            txt = poner(txt, "language", "es");
            for (var e : CLAVES.entrySet()) {
                if (!viejo.contains(e.getKey())) continue;
                Object v = viejo.get(e.getKey());
                if (e.getKey().equals("modo")) v = "chat".equalsIgnoreCase(String.valueOf(v)) ? "chat" : "dialog";
                txt = poner(txt, e.getValue(), v);
            }
            Files.writeString(f.toPath(), txt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            pl.getLogger().warning("No se pudo convertir el config de Dialogos: " + e.getMessage());
        }
    }

    private static String poner(String txt, String clave, Object valor) {
        String v = valor instanceof String s ? "'" + s.replace("'", "''") + "'" : String.valueOf(valor);
        return txt.replaceFirst("(?m)^(\\s*)" + java.util.regex.Pattern.quote(clave) + ":.*$",
                "$1" + clave + ": " + java.util.regex.Matcher.quoteReplacement(v));
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

package mc.gupe.ultradialogue;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Carpeta compartida de la familia Ultra: plugins/UltraView/<Plugin>/. */
public final class UltraView {

    private final JavaPlugin plugin;
    private final File root;
    private FileConfiguration config;

    public UltraView(JavaPlugin plugin) {
        this.plugin = plugin;
        File plugins = plugin.getDataFolder().getParentFile();
        File compartida = new File(plugins, "UltraView");
        if (!compartida.isDirectory() && !compartida.mkdirs())
            plugin.getLogger().warning("No pude crear plugins/UltraView — se usa la carpeta de siempre.");
        this.root = compartida.isDirectory() ? new File(compartida, plugin.getName())
                                             : plugin.getDataFolder();
        if (!root.isDirectory()) root.mkdirs();
        mudarLoViejo();
    }

    /** La carpeta donde este plugin guarda TODO. Usar esta y no getDataFolder(). */
    public File root() { return root; }

    public File file(String nombre) { return new File(root, nombre); }

    // CONFIG
    /** Como getConfig(), pero desde la carpeta compartida. */
    public FileConfiguration config() {
        if (config == null) reload();
        return config;
    }

    public void reload() {
        File f = file("config.yml");
        if (!f.exists()) saveResource("config.yml");
        config = YamlConfiguration.loadConfiguration(f);
        // Defaults del jar: así las claves nuevas de una actualización existen aunque el archivo
        // del server sea viejo. (No pisa lo que el staff haya cambiado.)
        try (InputStream in = plugin.getResource("config.yml")) {
            if (in != null) config.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8)));
        } catch (IOException ignored) {}
    }

    public void saveConfig() {
        if (config == null) return;
        try { config.save(file("config.yml")); }
        catch (IOException e) { plugin.getLogger().warning("No pude guardar config.yml: " + e.getMessage()); }
    }

    // RECURSOS DEL JAR
    /** Como saveResource(path, false), pero a la carpeta compartida. */
    public void saveResource(String ruta) {
        File destino = file(ruta);
        if (destino.exists()) return;
        File dir = destino.getParentFile();
        if (dir != null && !dir.isDirectory()) dir.mkdirs();
        try (InputStream in = plugin.getResource(ruta.replace(File.separatorChar, '/'))) {
            if (in == null) return;
            Files.copy(in, destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().warning("No pude extraer " + ruta + ": " + e.getMessage());
        }
    }

    // MUDANZA DESDE LA CARPETA VIEJA
    /** Trae lo que hubiera en plugins/<Plugin>/ (la carpeta que Bukkit da por defecto). */
    private void mudarLoViejo() {
        File vieja = plugin.getDataFolder();
        if (vieja.equals(root) || !vieja.isDirectory()) return;
        File marca = file(".mudado");
        if (marca.exists()) return;

        File[] hijos = vieja.listFiles();
        if (hijos == null || hijos.length == 0) return;

        int n = copiarFaltantes(vieja, root);
        try { marca.createNewFile(); } catch (IOException ignored) {}
        if (n > 0) plugin.getLogger().info("Se trajeron " + n + " archivos de "
                + vieja.getName() + "/ a UltraView/" + plugin.getName()
                + "/. La carpeta vieja NO se borro.");
    }

    /** Copia recursivo, salteando lo que ya exista del otro lado. */
    private int copiarFaltantes(File desde, File hacia) {
        File[] hijos = desde.listFiles();
        if (hijos == null) return 0;
        int n = 0;
        for (File h : hijos) {
            File d = new File(hacia, h.getName());
            if (h.isDirectory()) {
                if (!d.isDirectory()) d.mkdirs();
                n += copiarFaltantes(h, d);
            } else if (!d.exists()) {
                try { Files.copy(h.toPath(), d.toPath()); n++; }
                catch (IOException ignored) {}
            }
        }
        return n;
    }
}

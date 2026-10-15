package mc.gupe.ultradialogue.config;

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

/**
 * The plugin's own data folder (plugins/UltraDialogue/) and its config.yml.
 *
 * Versions up to 1.1.2 kept their files in the shared plugins/UltraView/UltraDialogue/ folder.
 * The first time this runs with that folder present and no config.yml of its own yet, everything
 * is moved here (not copied) so nothing is duplicated; if the move fails for some reason, it falls
 * back to copying and leaves the old folder untouched. User data is never deleted.
 */
public final class PluginData {

    private final JavaPlugin plugin;
    private final File root;
    private FileConfiguration config;

    public PluginData(JavaPlugin plugin) {
        this.plugin = plugin;
        this.root = plugin.getDataFolder();
        migrateFromUltraView();
        if (!root.isDirectory()) root.mkdirs();
    }

    /** The folder where this plugin keeps everything. */
    public File root() { return root; }

    public File file(String name) { return new File(root, name); }

    // CONFIG
    public FileConfiguration config() {
        if (config == null) reload();
        return config;
    }

    public void reload() {
        File f = file("config.yml");
        if (!f.exists()) saveResource("config.yml");
        config = YamlConfiguration.loadConfiguration(f);
        // Defaults from the jar: new keys from an update exist even on an older server config.yml
        // (it never overwrites what the staff actually changed).
        try (InputStream in = plugin.getResource("config.yml")) {
            if (in != null) config.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8)));
        } catch (IOException ignored) {}
    }

    public void saveConfig() {
        if (config == null) return;
        try { config.save(file("config.yml")); }
        catch (IOException e) { plugin.getLogger().warning("Could not save config.yml: " + e.getMessage()); }
    }

    // JAR RESOURCES
    public void saveResource(String path) {
        File dest = file(path);
        if (dest.exists()) return;
        File dir = dest.getParentFile();
        if (dir != null && !dir.isDirectory()) dir.mkdirs();
        try (InputStream in = plugin.getResource(path.replace(File.separatorChar, '/'))) {
            if (in == null) return;
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not extract " + path + ": " + e.getMessage());
        }
    }

    // MIGRATION FROM plugins/UltraView/<Plugin>/
    private void migrateFromUltraView() {
        File plugins = root.getParentFile();
        if (plugins == null) return;
        File shared = new File(plugins, "UltraView");
        File old = new File(shared, plugin.getName());
        if (!old.isDirectory() || new File(root, "config.yml").exists()) return;

        boolean moved = !root.exists() && old.renameTo(root);
        int copied = 0;
        if (!moved) {
            if (!root.isDirectory()) root.mkdirs();
            copied = moveContents(old, root);
        }
        if (moved || copied > 0) {
            plugin.getLogger().info("Moved the plugin's data from plugins/UltraView/" + plugin.getName()
                    + "/ to its own plugins/" + plugin.getName() + "/ folder.");
        }

        String[] left = shared.list();
        if (left != null && left.length == 0) shared.delete();
    }

    /** Moves every file and folder from {@code from} into {@code to}; copies instead if a move fails. */
    private int moveContents(File from, File to) {
        File[] kids = from.listFiles();
        if (kids == null) return 0;
        int n = 0;
        for (File k : kids) {
            File d = new File(to, k.getName());
            if (k.isDirectory()) {
                if (!d.isDirectory()) d.mkdirs();
                n += moveContents(k, d);
                k.delete();
            } else if (!d.exists()) {
                try {
                    Files.move(k.toPath(), d.toPath());
                } catch (IOException e) {
                    try { Files.copy(k.toPath(), d.toPath()); } catch (IOException ignored) { continue; }
                }
                n++;
            }
        }
        from.delete();
        return n;
    }
}

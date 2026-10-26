package mc.gupe.ultradialogue.pack;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Hands the button pack to whatever plugin builds the server's pack, so players get one merged pack
 * instead of one per plugin. UltraDialogue never sends packs to players itself.
 *
 * <p>It runs in {@code onLoad} and plugin.yml lists every supported plugin under {@code loadbefore}, so
 * the file is already in place when that plugin starts and builds its pack: nothing to reload on a
 * normal boot. If none of them is installed, the zip is left in {@code plugins/UltraDialogue/resourcepack/}
 * with a README and a warning in the console.
 */
public final class PackInstaller {

    /**
     * One plugin that builds a server pack: where our pack goes and how it rebuilds. {@code onCommand}
     * = it only builds its pack on that command, not when it starts (ItemsAdder, CraftEngine).
     */
    private record Target(String plugin, String path, boolean folder, String rebuild, boolean onCommand) {
    }

    // Order matters when a server has several: ResourcePackManager merges the others' packs too,
    // so it goes first; EcoItems goes last because it's often installed only for its items.
    private static final Target[] TARGETS = {
            new Target("ResourcePackManager", "mixer/UltraDialoguePack.zip", false, "rspm reload", false),
            new Target("Nexo", "pack/external_packs/UltraDialoguePack.zip", false, "nexo reload pack", false),
            new Target("Oraxen", "pack/uploads/UltraDialoguePack.zip", false, "oraxen reload pack", false),
            new Target("ItemsAdder", "contents/ultradialogue/resourcepack", true, "iazip", true),
            new Target("CraftEngine", "resources/ultradialogue/resourcepack", true, "ce reload pack", true),
            new Target("EcoItems", "pack/imports/UltraDialoguePack.zip", false, "ecoitems reload", false),
    };

    /**
     * Where the pack ended up: {@code plugin} is null if it was left for a manual install (or the
     * setting is none). {@code rebuild} is the command that has to run once the server is up because
     * that plugin had already started or only builds its pack on command; null if nothing else is needed.
     */
    public record Result(String plugin, String rebuild) {
        public static final Result NONE = new Result(null, null);
    }

    private static final String ZIP = "UltraDialoguePack.zip";

    private PackInstaller() {
    }

    /** Copies the pack where it belongs. {@code setting}: auto, none or the name of one plugin.
     *  {@code mergedByHand}: the server already added it to its own pack, so no warning if it's left here. */
    public static Result install(JavaPlugin plugin, String setting, boolean mergedByHand) {
        if (setting.equalsIgnoreCase("none")) {
            return Result.NONE;
        }
        try {
            byte[] pack;
            try (InputStream in = plugin.getResource("resourcepack.zip")) {
                if (in == null) {
                    throw new IOException("resourcepack.zip is missing from the jar");
                }
                pack = in.readAllBytes();
            }
            // Older versions extracted the pack here to serve it.
            Files.deleteIfExists(new File(plugin.getDataFolder(), "resourcepack.zip").toPath());

            for (Target t : TARGETS) {
                if (!setting.equalsIgnoreCase("auto") && !setting.equalsIgnoreCase(t.plugin())) {
                    continue;
                }
                Plugin owner = Bukkit.getPluginManager().getPlugin(t.plugin());
                if (owner == null) {
                    continue;
                }
                File dest = new File(owner.getDataFolder(), t.path());
                boolean changed = t.folder() ? unzip(pack, dest) : write(dest, pack);
                if (!changed) {
                    plugin.getLogger().info("Resource pack already in " + t.plugin() + ".");
                    return new Result(t.plugin(), null);
                }
                plugin.getLogger().info("Resource pack installed in " + t.plugin() + " (" + t.path() + ").");
                if (t.folder() && t.plugin().equals("CraftEngine")) {
                    write(new File(dest.getParentFile(), "pack.yml"), CRAFTENGINE_PACK.getBytes(StandardCharsets.UTF_8));
                }
                removeManualCopy(plugin);
                return new Result(t.plugin(), owner.isEnabled() || t.onCommand() ? t.rebuild() : null);
            }

            leaveForManualInstall(plugin, pack, setting, mergedByHand);
        } catch (IOException | RuntimeException e) {
            plugin.getLogger().warning("Couldn't install the resource pack: " + e.getMessage());
        }
        return Result.NONE;
    }

    private static void leaveForManualInstall(JavaPlugin plugin, byte[] pack, String setting, boolean mergedByHand) throws IOException {
        File folder = new File(plugin.getDataFolder(), "resourcepack");
        write(new File(folder, ZIP), pack);
        try (InputStream in = plugin.getResource("resourcepack/README.txt")) {
            if (in != null) {
                write(new File(folder, "README.txt"), in.readAllBytes());
            }
        }
        if (mergedByHand) {
            plugin.getLogger().info("Button pack left in plugins/UltraDialogue/resourcepack/ (merged-by-hand: true).");
            return;
        }
        String why = setting.equalsIgnoreCase("auto")
                ? "No resource pack plugin found (ResourcePackManager, Nexo, Oraxen, ItemsAdder, CraftEngine or EcoItems)."
                : "resource-pack.install is '" + setting + "' but that plugin isn't installed.";
        plugin.getLogger().warning("WARNING: " + why + " Dialogue buttons will show as [ text ] until you add "
                + "plugins/UltraDialogue/resourcepack/" + ZIP + " to your server's resource pack and set "
                + "resource-pack.merged-by-hand: true. See README.txt there.");
    }

    /** The copy left for manual install by an earlier start, before a pack plugin was installed. */
    private static void removeManualCopy(JavaPlugin plugin) throws IOException {
        File folder = new File(plugin.getDataFolder(), "resourcepack");
        Files.deleteIfExists(new File(folder, ZIP).toPath());
        Files.deleteIfExists(new File(folder, "README.txt").toPath());
        Files.deleteIfExists(folder.toPath());
    }

    /** Writes only if the content changed. Returns true if it wrote. */
    private static boolean write(File f, byte[] data) throws IOException {
        if (f.isFile() && Arrays.equals(Files.readAllBytes(f.toPath()), data)) {
            return false;
        }
        Files.createDirectories(f.getParentFile().toPath());
        Files.write(f.toPath(), data);
        return true;
    }

    /** Extracts the pack into a folder (assets only: the host plugin writes its own pack.mcmeta).
     *  A hash file beside it (outside, so it never ends up in the pack) says whether it's already this version. */
    private static boolean unzip(byte[] pack, File dest) throws IOException {
        File stamp = new File(dest.getParentFile(), ".ultradialogue");
        String hash = sha1(pack);
        if (stamp.isFile() && Files.readString(stamp.toPath()).trim().equals(hash)) {
            return false;
        }
        if (dest.exists()) {
            try (Stream<java.nio.file.Path> walk = Files.walk(dest.toPath())) {
                walk.sorted(Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
            }
        }
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(pack))) {
            for (ZipEntry e; (e = zip.getNextEntry()) != null; ) {
                String name = e.getName();
                if (e.isDirectory() || !name.startsWith("assets/") || name.contains("..")) {
                    continue;
                }
                File out = new File(dest, name);
                Files.createDirectories(out.getParentFile().toPath());
                Files.write(out.toPath(), zip.readAllBytes());
            }
        }
        Files.writeString(stamp.toPath(), hash);
        return true;
    }

    private static String sha1(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data)).toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return String.valueOf(Arrays.hashCode(data));
        }
    }

    private static final String CRAFTENGINE_PACK = """
            author: Social Studio
            version: 1.1.4
            description: UltraDialogue buttons (installed by UltraDialogue)
            namespace: ultradialogue
            enable: true
            """;
}

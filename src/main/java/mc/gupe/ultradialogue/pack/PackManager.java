package mc.gupe.ultradialogue.pack;

import mc.gupe.ultradialogue.UltraDialogue;
import mc.gupe.ultradialogue.util.Text;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * The button pack. Sent on join only, from its own port (8083), stacking with UltraRevive's
 * (8081) and UltraBoss's (8082) because each has its own UUID.
 *
 * It also tracks who actually LOADED it: everyone else gets the icons as text symbols
 * (see {@link mc.gupe.ultradialogue.gui.Icons#sprites}) instead of the missing-texture box.
 */
public final class PackManager implements Listener {

    private static final UUID PACK_UUID = UUID.nameUUIDFromBytes("ultradialogue-pack".getBytes());
    private static final String PACK_PATH = "/ultradialogue.zip";

    private final UltraDialogue pl;
    private final Set<UUID> loaded = new HashSet<>();
    private PackServer httpServer;
    private boolean enabled;
    private boolean force;
    private String prompt;
    private String url;
    private byte[] hash;

    public PackManager(UltraDialogue pl) { this.pl = pl; }

    public boolean loaded(Player p) { return loaded.contains(p.getUniqueId()); }

    public void start() {
        FileConfiguration c = pl.config();
        enabled = c.getBoolean("resource-pack.enabled", true);
        if (!enabled) return;
        force = c.getBoolean("resource-pack.force", false);
        String own = c.getString("resource-pack.prompt", "");
        prompt = own == null || own.isEmpty() ? pl.lang().raw("pack-prompt") : own;

        // Always re-extracted: the pack that matters is the one this version's jar carries.
        File zip = pl.data().file("resourcepack.zip");
        zip.delete();
        pl.data().saveResource("resourcepack.zip");
        byte[] data;
        try {
            data = Files.readAllBytes(zip.toPath());
        } catch (Throwable t) {
            pl.getLogger().warning("resourcepack.zip not found; buttons will be drawn as [ text ].");
            enabled = false;
            return;
        }
        hash = sha1(data);

        if (c.getString("resource-pack.mode", "internal").toLowerCase(Locale.ROOT).equals("url")) {
            url = c.getString("resource-pack.url", "");
            if (url == null || url.isEmpty()) {
                pl.getLogger().warning("resource-pack.mode is 'url' but 'url' is empty. Pack disabled.");
                enabled = false;
            }
            return;
        }
        int port = c.getInt("resource-pack.port", 8083);
        String host = c.getString("resource-pack.host", "AUTO");
        if (host == null || host.isEmpty() || host.equalsIgnoreCase("AUTO")) host = detectHost();
        url = "http://" + host + ":" + port + PACK_PATH;
        httpServer = new PackServer();
        try {
            httpServer.start(port, PACK_PATH, data);
            pl.getLogger().info("Button pack served at: " + url);
        } catch (Throwable t) {
            pl.getLogger().warning("Could not open port " + port + " for the button pack (is it allocated?): "
                    + t.getMessage() + ". Buttons will be drawn as [ text ].");
            enabled = false;
        }
    }

    public void stop() {
        if (httpServer != null) httpServer.stop();
    }

    private void sendTo(Player p) {
        if (!enabled || url == null) return;
        try {
            p.setResourcePack(PACK_UUID, url, hash, Text.color(prompt), force);
        } catch (Throwable t) {
            try { p.setResourcePack(url, hash); } catch (Throwable ignored) {}
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        pl.getServer().getScheduler().runTaskLater(pl, () -> { if (p.isOnline()) sendTo(p); }, 40L);
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent e) {
        if (!PACK_UUID.equals(e.getID())) return;   // another plugin's pack
        switch (e.getStatus()) {
            case SUCCESSFULLY_LOADED -> loaded.add(e.getPlayer().getUniqueId());
            case DECLINED, FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD, DISCARDED ->
                    loaded.remove(e.getPlayer().getUniqueId());
            default -> { }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        loaded.remove(e.getPlayer().getUniqueId());
    }

    private String detectHost() {
        try {
            java.net.URLConnection con = new java.net.URL("https://api.ipify.org").openConnection();
            con.setConnectTimeout(2000);
            con.setReadTimeout(2000);
            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(con.getInputStream()))) {
                String ip = r.readLine();
                if (ip != null && !ip.trim().isEmpty()) return ip.trim();
            }
        } catch (Throwable ignored) {}
        String ip = pl.getServer().getIp();
        if (ip != null && !ip.isEmpty() && !ip.equals("0.0.0.0")) return ip;
        try { return InetAddress.getLocalHost().getHostAddress(); } catch (Throwable t) { return "127.0.0.1"; }
    }

    private static byte[] sha1(byte[] d) {
        try { return MessageDigest.getInstance("SHA-1").digest(d); } catch (Throwable t) { return new byte[0]; }
    }
}

package mc.gupe.ultradialogue.config;

import mc.gupe.ultradialogue.UltraDialogue;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Messages from lang/en.yml or lang/es.yml. Missing keys are filled in from the jar. */
public final class Lang {

    private final UltraDialogue pl;
    private FileConfiguration msgs;

    public Lang(UltraDialogue pl) { this.pl = pl; }

    public void load(String language) {
        pl.data().saveResource("lang/en.yml");
        pl.data().saveResource("lang/es.yml");
        String l = language == null ? "en" : language.toLowerCase();
        File f = pl.data().file("lang/" + l + ".yml");
        if (!f.exists()) {
            pl.getLogger().warning("Language '" + language + "' not found, using 'en'.");
            l = "en";
            f = pl.data().file("lang/en.yml");
        }
        msgs = YamlConfiguration.loadConfiguration(f);
        try (InputStream in = pl.getResource("lang/" + l + ".yml")) {
            if (in == null) return;
            FileConfiguration def = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            boolean changed = false;
            for (String k : def.getKeys(true)) {
                if (!msgs.contains(k)) { msgs.set(k, def.get(k)); changed = true; }
            }
            if (changed) msgs.save(f);
        } catch (Exception ignored) {}
    }

    /** Raw text (with & codes), no prefix. pairs = {key}, value, {key}, value...*/
    public String raw(String key, String... pairs) {
        String s = msgs.getString(key, key);
        for (int i = 0; i + 1 < pairs.length; i += 2) s = s.replace(pairs[i], pairs[i + 1]);
        return s;
    }

    /** With its prefix in front: for chat messages. */
    public String msg(String key, String... pairs) {
        return msgs.getString("prefix", "") + raw(key, pairs);
    }
}

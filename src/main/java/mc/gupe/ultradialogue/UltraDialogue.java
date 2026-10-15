package mc.gupe.ultradialogue;

import mc.gupe.ultradialogue.command.Command;
import mc.gupe.ultradialogue.config.Lang;
import mc.gupe.ultradialogue.config.PluginData;
import mc.gupe.ultradialogue.gui.Portraits;
import mc.gupe.ultradialogue.gui.Screen;
import mc.gupe.ultradialogue.gui.editor.EditorManager;
import mc.gupe.ultradialogue.hook.QuestBridge;
import mc.gupe.ultradialogue.listener.Listeners;
import mc.gupe.ultradialogue.listener.NpcHook;
import mc.gupe.ultradialogue.migration.LegacyMigration;
import mc.gupe.ultradialogue.pack.PackManager;
import mc.gupe.ultradialogue.registry.Registry;
import mc.gupe.ultradialogue.util.Actions;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class UltraDialogue extends JavaPlugin {

    private static UltraDialogue instance;

    private PluginData data;
    private Lang lang;
    private Registry registry;
    private Portraits portraits;
    private Screen screen;
    private Actions actions;
    private PackManager pack;
    private EditorManager editor;
    private QuestBridge quests;

    public static UltraDialogue get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        data = new PluginData(this);          // first: everything else writes into its folder
        LegacyMigration.run(this, data);       // before any saveResource, or it gets skipped

        data.reload();
        if (!data.file("dialogues").exists()) {
            data.saveResource("dialogues/example.yml");
            data.saveResource("dialogues/merchant.yml");
            data.saveResource("dialogues/kadir.yml");
        }
        lang = new Lang(this);
        lang.load(config().getString("language", "en"));

        portraits = new Portraits(this);
        registry = new Registry(this);
        actions = new Actions(this);
        screen = new Screen(this);
        editor = new EditorManager(this);
        hooks();          // before reading the dialogues: validation needs to know if quests exist
        registry.load();

        Bukkit.getPluginManager().registerEvents(new Listeners(this), this);
        Bukkit.getPluginManager().registerEvents(editor, this);
        // The icon pack (port 8083). If it can't start, icons show as text symbols instead.
        pack = new PackManager(this);
        Bukkit.getPluginManager().registerEvents(pack, this);
        pack.start();
        NpcHook.register(this);

        PluginCommand cmd = getCommand("ultradialogue");
        if (cmd != null) {
            Command c = new Command(this);
            cmd.setExecutor(c);
            cmd.setTabCompleter(c);
        }

        // The private plugin this one replaced. With both installed, every NPC would open two conversations.
        if (Bukkit.getPluginManager().getPlugin("Dialogos") != null) {
            getLogger().severe("The 'Dialogos' plugin is still installed: remove its jar, UltraDialogue already replaces it.");
        }
    }

    @Override
    public void onDisable() {
        if (screen != null) screen.closeAll();
        if (pack != null) pack.stop();
    }

    public int reload() {
        data.reload();
        lang.load(config().getString("language", "en"));
        return registry.load();
    }

    public FileConfiguration config() { return data.config(); }
    public PluginData data() { return data; }
    public Lang lang() { return lang; }
    public Registry registry() { return registry; }
    public Portraits portraits() { return portraits; }
    public Screen screen() { return screen; }
    public Actions actions() { return actions; }
    public PackManager pack() { return pack; }
    public EditorManager editor() { return editor; }
    public QuestBridge quests() { return quests; }

    /**
     * Integrations that IMPORT another plugin's classes. Each lives in its own package (hook/...)
     * and is loaded by name only if that plugin is enabled: otherwise its class is never touched
     * and there is no NoClassDefFoundError. If something fails (a different version, Paper hiding
     * its classes), it's logged and the rest of UltraDialogue keeps working.
     */
    private void hooks() {
        var pm = Bukkit.getPluginManager();
        if (pm.isPluginEnabled("BeautyQuests")) {
            try {
                quests = (QuestBridge) Class.forName("mc.gupe.ultradialogue.hook.bq.BeautyQuestsHook")
                        .getConstructor(UltraDialogue.class).newInstance(this);
                getLogger().info("Hooked into BeautyQuests: stage type ULTRADIALOGUE registered.");
            } catch (Throwable t) {
                Throwable c = t.getCause() != null ? t.getCause() : t;
                getLogger().warning("Could not hook into BeautyQuests (" + c + "). Quest actions and conditions are disabled.");
                quests = null;
            }
        }
        if (pm.isPluginEnabled("PlaceholderAPI")) {
            try {
                Object exp = Class.forName("mc.gupe.ultradialogue.hook.papi.UltraPlaceholders")
                        .getConstructor(UltraDialogue.class).newInstance(this);
                exp.getClass().getMethod("register").invoke(exp);
                getLogger().info("Placeholders registered: %ultradialogue_affinity_<id>%, %ultradialogue_flag_<flag>%");
            } catch (Throwable t) {
                getLogger().warning("Could not register the placeholders: " + t);
            }
        }
    }
}

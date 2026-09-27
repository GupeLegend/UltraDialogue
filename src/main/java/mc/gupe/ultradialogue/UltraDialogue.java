package mc.gupe.ultradialogue;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class UltraDialogue extends JavaPlugin {

    private static UltraDialogue instance;

    private UltraView view;
    private Lang lang;
    private Registry registry;
    private Portraits portraits;
    private Screen screen;
    private Actions actions;
    private PackManager pack;
    private QuestBridge quests;

    public static UltraDialogue get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        view = new UltraView(this);          // primero: todo lo demas escribe ahi
        Migration.run(this, view);           // antes de cualquier saveResource, o se saltea

        view.reload();
        if (!view.file("dialogues").exists()) {
            view.saveResource("dialogues/example.yml");
            view.saveResource("dialogues/merchant.yml");
            view.saveResource("dialogues/kadir.yml");
        }
        lang = new Lang(this);
        lang.load(config().getString("language", "en"));

        portraits = new Portraits(this);
        registry = new Registry(this);
        actions = new Actions(this);
        screen = new Screen(this);
        hooks();          // antes de leer los dialogos: la validacion necesita saber si hay misiones
        registry.load();

        Bukkit.getPluginManager().registerEvents(new Listeners(this), this);
        // El pack de los iconos (puerto 8083). Si no puede, los iconos salen como simbolos de texto.
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

        // El plugin privado del que salio este. Con los dos puestos, cada NPC abriria dos charlas.
        if (Bukkit.getPluginManager().getPlugin("Dialogos") != null) {
            getLogger().severe("El plugin 'Dialogos' sigue instalado: sacale el jar, UltraDialogue ya lo reemplaza.");
        }
    }

    @Override
    public void onDisable() {
        if (screen != null) screen.closeAll();
        if (pack != null) pack.stop();
    }

    public int reload() {
        view.reload();
        lang.load(config().getString("language", "en"));
        return registry.load();
    }

    public FileConfiguration config() { return view.config(); }
    public UltraView view() { return view; }
    public Lang lang() { return lang; }
    public Registry registry() { return registry; }
    public Portraits portraits() { return portraits; }
    public Screen screen() { return screen; }
    public Actions actions() { return actions; }
    public PackManager pack() { return pack; }
    public QuestBridge quests() { return quests; }

    /**
     * Integraciones que IMPORTAN clases de otro plugin. Cada una vive en su paquete (hook/...) y se
     * carga por nombre solo si ese plugin esta prendido: si no, la clase nunca se toca y no hay
     * NoClassDefFoundError. Si algo falla (otra version, Paper no deja ver sus clases), se avisa y
     * el resto de UltraDialogue sigue igual.
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

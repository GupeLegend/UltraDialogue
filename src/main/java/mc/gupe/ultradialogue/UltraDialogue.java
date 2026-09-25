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
        }
        lang = new Lang(this);
        lang.load(config().getString("language", "en"));

        portraits = new Portraits(this);
        registry = new Registry(this);
        actions = new Actions(this);
        screen = new Screen(this);
        registry.load();

        Bukkit.getPluginManager().registerEvents(new Listeners(this), this);
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
}

package mc.gupe.ultradialogue;

import org.bukkit.entity.Player;

/**
 * Lo que UltraDialogue necesita de un plugin de misiones. La unica implementacion es
 * {@code hook.bq.BeautyQuestsHook}, que se carga por nombre SOLO si BeautyQuests esta prendido:
 * asi ninguna clase principal toca clases de BeautyQuests (sin el plugin, la JVM tiraria
 * NoClassDefFoundError al cargarlas).
 */
public interface QuestBridge {

    /** El jugador esta AHORA en una etapa ULTRADIALOGUE con esa clave. */
    boolean active(Player p, String key);

    /** Termino la mision con ese id. */
    boolean completed(Player p, int questId);

    /** Tiene la mision con ese id empezada y sin terminar. */
    boolean started(Player p, int questId);

    /** Empieza la mision; {@code force} = sin mirar requisitos. false si no existe. */
    boolean start(Player p, int questId, boolean force);

    /** Nombre del plugin, para los mensajes de consola. */
    String name();
}

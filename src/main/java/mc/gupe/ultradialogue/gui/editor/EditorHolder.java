package mc.gupe.ultradialogue.gui.editor;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.function.Consumer;

/**
 * Marks an inventory as one of the editor's own menus and carries one click handler per slot, so a
 * single listener in {@link EditorManager} can serve every menu without a separate Listener class
 * for each screen.
 */
final class EditorHolder implements InventoryHolder {

    @SuppressWarnings("unchecked")
    private final Consumer<org.bukkit.event.inventory.InventoryClickEvent>[] handlers;
    private Inventory inventory;

    EditorHolder(int size) {
        handlers = new Consumer[size];
    }

    void on(int slot, Consumer<org.bukkit.event.inventory.InventoryClickEvent> handler) {
        handlers[slot] = handler;
    }

    Consumer<org.bukkit.event.inventory.InventoryClickEvent> handlerAt(int slot) {
        return slot >= 0 && slot < handlers.length ? handlers[slot] : null;
    }

    void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}

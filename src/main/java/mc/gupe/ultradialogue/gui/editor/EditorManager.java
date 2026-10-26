package mc.gupe.ultradialogue.gui.editor;

import mc.gupe.ultradialogue.util.Tasks;
import io.papermc.paper.event.player.AsyncChatEvent;
import mc.gupe.ultradialogue.UltraDialogue;
import mc.gupe.ultradialogue.gui.Icons;
import mc.gupe.ultradialogue.model.Dialogue;
import mc.gupe.ultradialogue.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The /ud button editor: an inventory GUI for admins to edit a dialogue's buttons - their text,
 * icon and position - without touching the .yml files by hand.
 *
 * Everything else (the NPC's own lines, conditions, actions, affinity...) is still only edited in
 * the files; this menu only ever changes what a button shows and where it sits. Edits are kept in
 * memory (one {@link NodeDraft} per admin) until "Save" writes them to disk through
 * {@link DialogueFileEditor}, which rewrites only that node's answer/nav lists and leaves the rest
 * of the file untouched. Closing the menu without saving discards the draft.
 */
public final class EditorManager implements Listener {

    private static final int SIZE_LIST = 54, SIZE_EDIT = 27, SIZE_SMALL = 27;

    private final UltraDialogue pl;
    private final Map<UUID, NodeDraft> drafts = new ConcurrentHashMap<>();
    private final Map<UUID, ChatCapture> pending = new ConcurrentHashMap<>();

    public EditorManager(UltraDialogue pl) { this.pl = pl; }

    private record ChatCapture(Consumer<String> onInput, Runnable onCancel) {}

    private static final class NodeDraft {
        final String dialogueId, nodeId;
        final List<Dialogue.Answer> answers;
        final List<Dialogue.Answer> nav;
        final List<Integer> layout;
        boolean dirty;

        NodeDraft(String dialogueId, String nodeId, Dialogue.Node n) {
            this.dialogueId = dialogueId;
            this.nodeId = nodeId;
            this.answers = new ArrayList<>(n.answers());
            this.nav = new ArrayList<>(n.nav());
            this.layout = new ArrayList<>(n.layout());
        }
    }

    private static final Map<String, Material> ICON_MATERIAL = Map.ofEntries(
            Map.entry("mision", Material.YELLOW_DYE), Map.entry("principal", Material.LIME_DYE),
            Map.entry("secundaria", Material.LIGHT_BLUE_DYE), Map.entry("acertijo", Material.PURPLE_DYE),
            Map.entry("aceptar", Material.GREEN_DYE), Map.entry("volver", Material.LIME_DYE),
            Map.entry("salir", Material.RED_DYE), Map.entry("premio", Material.ORANGE_DYE),
            Map.entry("cuento", Material.CYAN_DYE), Map.entry("probabilidad", Material.ORANGE_DYE));

    // --------------------------------------------------------------------------------- top menu

    public void open(Player p) {
        drafts.remove(p.getUniqueId());
        List<Dialogue> all = new ArrayList<>(pl.registry().all());
        Inventory inv = create(p, SIZE_LIST, lang("editor-title-list"));
        EditorHolder h = (EditorHolder) inv.getHolder();
        int slot = 9;
        for (Dialogue d : all) {
            if (slot >= 45) break;
            ItemStack it = dialogueItem(d);
            inv.setItem(slot, it);
            h.on(slot, e -> openNodes(p, d));
            slot++;
        }
        if (all.isEmpty()) inv.setItem(22, info(Material.BARRIER, lang("editor-no-dialogues"), List.of()));
        p.openInventory(inv);
    }

    private ItemStack dialogueItem(Dialogue d) {
        ItemStack base = pl.portraits().of(d.portrait, null);
        ItemStack it = base != null ? base.clone() : new ItemStack(Material.BOOK);
        File f = pl.registry().fileOf(d.id);
        String rel = relative(f);
        List<Component> lore = new ArrayList<>();
        lore.add(dim(lang("editor-lore-id", "{id}", d.id)));
        lore.add(dim(lang("editor-lore-nodes", "{n}", String.valueOf(d.nodes.size()))));
        lore.add(dim(lang("editor-lore-file", "{file}", rel)));
        lore.add(dim(lang("editor-lore-npcs", "{npcs}", d.npcs.isEmpty() ? "-" : String.join(", ", d.npcs))));
        lore.add(Component.empty());
        lore.add(hint(lang("editor-lore-click")));
        apply(it, Text.color(d.name), lore);
        return it;
    }

    private String relative(File f) {
        if (f == null) return "?";
        File base = pl.data().file("dialogues");
        String basePath = base.getAbsolutePath();
        String path = f.getAbsolutePath();
        return path.startsWith(basePath) ? "dialogues" + path.substring(basePath.length()).replace('\\', '/') : path;
    }

    // -------------------------------------------------------------------------------- node list

    private void openNodes(Player p, Dialogue d) {
        drafts.remove(p.getUniqueId());
        Inventory inv = create(p, SIZE_LIST, lang("editor-title-nodes", "{id}", d.id));
        EditorHolder h = (EditorHolder) inv.getHolder();
        inv.setItem(0, backItem());
        h.on(0, e -> open(p));
        inv.setItem(4, info(Material.BOOK, lang("editor-help-title"), List.of(dim(lang("editor-help-nodes")))));
        int slot = 9;
        for (Dialogue.Node n : d.nodes.values()) {
            if (slot >= 45) break;
            List<Component> lore = new ArrayList<>();
            if (n.title() != null) lore.add(dim(lang("editor-lore-title", "{title}", plain(n.title()))));
            else if (n.speaker() != null) lore.add(dim(lang("editor-lore-speaker", "{speaker}", plain(n.speaker()))));
            lore.add(dim(lang("editor-lore-answers", "{n}", String.valueOf(n.answers().size()))));
            if (!n.nav().isEmpty()) lore.add(dim(lang("editor-lore-nav", "{n}", String.valueOf(n.nav().size()))));
            if (!n.layout().isEmpty()) lore.add(dim(lang("editor-lore-layout")));
            lore.add(Component.empty());
            lore.add(hint(lang("editor-lore-click")));
            lore.add(hint(lang("editor-lore-right-click-test")));
            ItemStack it = info(Material.PAPER, Component.text(n.id(), NamedTextColor.WHITE), lore);
            inv.setItem(slot, it);
            String nodeId = n.id();
            h.on(slot, e -> {
                if (e.isRightClick()) test(p, d, nodeId);
                else openButtons(p, d, nodeId);
            });
            slot++;
        }
        p.openInventory(inv);
    }

    private void test(Player p, Dialogue d, String nodeId) {
        p.closeInventory();
        Tasks.later(p, () -> pl.screen().open(p, d, nodeId, null, null));
    }

    // ------------------------------------------------------------------------------ button list

    private void openButtons(Player p, Dialogue d, String nodeId) {
        Dialogue.Node n = d.nodes.get(nodeId);
        if (n == null) { openNodes(p, d); return; }
        NodeDraft draft = drafts.computeIfAbsent(p.getUniqueId(), k -> new NodeDraft(d.id, nodeId, n));
        if (!draft.dialogueId.equals(d.id) || !draft.nodeId.equals(nodeId)) {
            draft = new NodeDraft(d.id, nodeId, n);
            drafts.put(p.getUniqueId(), draft);
        }
        renderButtons(p, d, draft);
    }

    private void renderButtons(Player p, Dialogue d, NodeDraft draft) {
        Inventory inv = create(p, SIZE_LIST, lang("editor-title-buttons", "{id}", d.id, "{node}", draft.nodeId));
        EditorHolder h = (EditorHolder) inv.getHolder();
        inv.setItem(0, backItem());
        h.on(0, e -> { drafts.remove(p.getUniqueId()); openNodes(p, d); });
        inv.setItem(4, info(Material.BOOK, lang("editor-help-title"), List.of(dim(lang("editor-help-buttons")))));
        inv.setItem(8, info(Material.SPYGLASS, lang("editor-test"), List.of(dim(lang("editor-test-lore")))));
        h.on(8, e -> test(p, d, draft.nodeId));

        int slot = 9;
        for (int i = 0; i < draft.answers.size() && slot < 45; i++, slot++) {
            int idx = i;
            inv.setItem(slot, buttonItem(draft.answers.get(i), draft, false, idx));
            h.on(slot, e -> openEdit(p, d, draft, false, idx));
        }
        if (!draft.nav.isEmpty() && slot < 45) { inv.setItem(slot, divider()); slot++; }
        for (int i = 0; i < draft.nav.size() && slot < 45; i++, slot++) {
            Dialogue.Answer a = draft.nav.get(i);
            int idx = i;
            boolean readOnly = a.text() == null && a.icon() == null
                    && (Dialogue.BACK.equals(a.goTo()) || Dialogue.EXIT.equals(a.goTo()));
            inv.setItem(slot, buttonItem(a, draft, true, idx));
            if (!readOnly) h.on(slot, e -> openEdit(p, d, draft, true, idx));
            else h.on(slot, e -> p.sendMessage(Text.msg("editor-nav-readonly")));
        }

        boolean canAdd = !draft.answers.isEmpty();
        inv.setItem(48, canAdd ? info(Material.WRITABLE_BOOK, lang("editor-add"), List.of(dim(lang("editor-add-lore"))))
                : info(Material.WRITABLE_BOOK, lang("editor-add"), List.of(dim(lang("editor-no-answers-to-copy")))));
        if (canAdd) h.on(48, e -> openAdd(p, d, draft));

        inv.setItem(50, info(Material.BARRIER, lang("editor-cancel"), List.of(dim(lang("editor-cancel-lore")))));
        h.on(50, e -> { drafts.remove(p.getUniqueId()); openNodes(p, d); });

        inv.setItem(53, info(Material.EMERALD, lang("editor-save"), List.of(dim(lang("editor-save-lore")))));
        h.on(53, e -> save(p, d, draft));

        p.openInventory(inv);
    }

    private ItemStack buttonItem(Dialogue.Answer a, NodeDraft draft, boolean nav, int index) {
        String icon = Icons.resolve(firstIcon(a.icon()));
        Material m = icon != null ? ICON_MATERIAL.getOrDefault(icon, Material.PAPER) : Material.PAPER;
        String label = a.text() != null ? plain(a.text())
                : Dialogue.BACK.equals(a.goTo()) ? lang("button-back-plain")
                : Dialogue.EXIT.equals(a.goTo()) ? lang("button-exit-plain") : lang("editor-no-text");
        List<Component> lore = new ArrayList<>();
        lore.add(dim(lang("editor-lore-position", "{n}", String.valueOf(index + 1))));
        if (a.icon() != null) lore.add(dim(lang("editor-lore-icon", "{icon}", a.icon())));
        if (a.goTo() != null && !Dialogue.BACK.equals(a.goTo()) && !Dialogue.EXIT.equals(a.goTo()))
            lore.add(dim(lang("editor-lore-goto", "{node}", a.goTo())));
        if (!draft.layout.isEmpty() && !nav) lore.add(dim(lang("editor-lore-row", "{row}", String.valueOf(rowOf(draft.layout, index) + 1))));
        lore.add(Component.empty());
        lore.add(hint(lang("editor-lore-click")));
        return info(m, Component.text(label, NamedTextColor.WHITE), lore);
    }

    private static String firstIcon(String icon) {
        if (icon == null) return null;
        String[] parts = icon.trim().split("\\s+");
        return parts.length > 0 ? parts[parts.length - 1] : null;
    }

    private static int rowOf(List<Integer> layout, int index) {
        int seen = 0;
        for (int r = 0; r < layout.size(); r++) {
            seen += layout.get(r);
            if (index < seen) return r;
        }
        return Math.max(0, layout.size() - 1);
    }

    // -------------------------------------------------------------------------------- edit menu

    private void openEdit(Player p, Dialogue d, NodeDraft draft, boolean nav, int index) {
        List<Dialogue.Answer> list = nav ? draft.nav : draft.answers;
        if (index < 0 || index >= list.size()) { renderButtons(p, d, draft); return; }
        Dialogue.Answer a = list.get(index);
        Inventory inv = create(p, SIZE_EDIT, lang("editor-title-edit"));
        EditorHolder h = (EditorHolder) inv.getHolder();
        inv.setItem(4, backItem());
        h.on(4, e -> renderButtons(p, d, draft));

        inv.setItem(10, info(Material.OAK_SIGN, lang("editor-edit-text"),
                List.of(dim(a.text() != null ? plain(a.text()) : lang("editor-no-text")))));
        h.on(10, e -> {
            pending.put(p.getUniqueId(), new ChatCapture(
                    input -> {
                        list.set(index, withText(list.get(index), input));
                        draft.dirty = true;
                        p.sendMessage(Text.msg("done"));
                        openEdit(p, d, draft, nav, index);
                    },
                    () -> { p.sendMessage(Text.msg("editor-chat-cancelled")); openEdit(p, d, draft, nav, index); }));
            p.closeInventory();
            p.sendMessage(Text.msg("editor-chat-prompt"));
        });

        inv.setItem(12, info(Material.ITEM_FRAME, lang("editor-edit-icon"),
                List.of(dim(a.icon() == null ? lang("editor-icon-none") : a.icon()))));
        h.on(12, e -> openStyle(p, d, draft, nav, index));

        int size = presetSize(a.width());
        inv.setItem(14, info(Material.STICK, lang("editor-edit-size"),
                List.of(dim(size == 0 ? lang("editor-size-auto") : lang("editor-size-n", "{n}", String.valueOf(size))))));
        h.on(14, e -> {
            int next = e.isRightClick() ? (size + 4) % 5 : (size + 1) % 5;
            list.set(index, withWidth(list.get(index), next == 0 ? 0 : Dialogue.widthForSize(next)));
            draft.dirty = true;
            openEdit(p, d, draft, nav, index);
        });

        boolean first = index == 0, last = index == list.size() - 1;
        inv.setItem(15, moveItem(!first, "↑ " + lang("editor-move-up")));
        if (!first) h.on(15, e -> { swap(list, index, index - 1); draft.dirty = true; renderButtons(p, d, draft); });
        inv.setItem(16, moveItem(!last, "↓ " + lang("editor-move-down")));
        if (!last) h.on(16, e -> { swap(list, index, index + 1); draft.dirty = true; renderButtons(p, d, draft); });

        if (!nav && draft.layout.size() > 1) {
            inv.setItem(19, info(Material.COMPASS, lang("editor-move-row"), List.of(dim(lang("editor-move-row-lore")))));
            h.on(19, e -> openRow(p, d, draft, index));
        }

        inv.setItem(22, info(Material.REDSTONE, lang("editor-delete"), List.of(dim(lang("editor-delete-lore")))));
        h.on(22, e -> openConfirmDelete(p, d, draft, nav, index));

        p.openInventory(inv);
    }

    private ItemStack moveItem(boolean enabled, String name) {
        return info(enabled ? Material.FEATHER : Material.GRAY_DYE,
                Component.text(name, enabled ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY), List.of());
    }

    private static int presetSize(int width) {
        for (int size = 1; size <= 4; size++) if (Dialogue.widthForSize(size) == width) return size;
        return 0;
    }

    private static void swap(List<Dialogue.Answer> list, int a, int b) {
        Dialogue.Answer t = list.get(a);
        list.set(a, list.get(b));
        list.set(b, t);
    }

    private static Dialogue.Answer withText(Dialogue.Answer a, String text) {
        return new Dialogue.Answer(text, a.tooltip(), a.conditions(), a.goTo(), a.actions(), a.always(),
                a.icon(), a.width(), a.locked(), a.look());
    }

    private static Dialogue.Answer withIcon(Dialogue.Answer a, String icon) {
        return new Dialogue.Answer(a.text(), a.tooltip(), a.conditions(), a.goTo(), a.actions(), a.always(),
                icon, a.width(), a.locked(), a.look());
    }

    private static Dialogue.Answer withWidth(Dialogue.Answer a, int width) {
        return new Dialogue.Answer(a.text(), a.tooltip(), a.conditions(), a.goTo(), a.actions(), a.always(),
                a.icon(), width, a.locked(), a.look());
    }

    // -------------------------------------------------------------------------------- style menu

    private void openStyle(Player p, Dialogue d, NodeDraft draft, boolean nav, int index) {
        // One row for the back arrow, two more for "no icon" plus the 10 approved presets (11 items).
        Inventory inv = create(p, 36, lang("editor-title-style"));
        EditorHolder h = (EditorHolder) inv.getHolder();
        inv.setItem(4, backItem());
        h.on(4, e -> openEdit(p, d, draft, nav, index));

        inv.setItem(9, info(Material.LIGHT_GRAY_DYE, lang("editor-icon-none"), List.of()));
        h.on(9, e -> { setIcon(draft, nav, index, null); openEdit(p, d, draft, nav, index); });

        int slot = 10;
        for (Icons.Preset preset : Icons.presets()) {
            Material m = ICON_MATERIAL.getOrDefault(preset.name(), Material.PAPER);
            inv.setItem(slot, info(m, Component.text(preset.glyph() + " " + preset.name(),
                    net.kyori.adventure.text.format.TextColor.fromHexString(preset.color())), List.of()));
            String name = preset.name();
            h.on(slot, e -> { setIcon(draft, nav, index, name); openEdit(p, d, draft, nav, index); });
            slot++;
        }
        p.openInventory(inv);
    }

    private void setIcon(NodeDraft draft, boolean nav, int index, String icon) {
        List<Dialogue.Answer> list = nav ? draft.nav : draft.answers;
        list.set(index, withIcon(list.get(index), icon));
        draft.dirty = true;
    }

    // ---------------------------------------------------------------------------------- add menu

    private void openAdd(Player p, Dialogue d, NodeDraft draft) {
        Inventory inv = create(p, SIZE_LIST, lang("editor-title-add"));
        EditorHolder h = (EditorHolder) inv.getHolder();
        inv.setItem(0, backItem());
        h.on(0, e -> renderButtons(p, d, draft));
        int slot = 9;
        for (int i = 0; i < draft.answers.size() && slot < 45; i++, slot++) {
            Dialogue.Answer a = draft.answers.get(i);
            inv.setItem(slot, buttonItem(a, draft, false, i));
            h.on(slot, e -> {
                Dialogue.Answer copy = withText(a, lang("editor-new-button-text"));
                draft.answers.add(copy);
                if (!draft.layout.isEmpty()) draft.layout.set(draft.layout.size() - 1, draft.layout.get(draft.layout.size() - 1) + 1);
                draft.dirty = true;
                openEdit(p, d, draft, false, draft.answers.size() - 1);
            });
        }
        p.openInventory(inv);
    }

    // ---------------------------------------------------------------------------------- row menu

    private void openRow(Player p, Dialogue d, NodeDraft draft, int index) {
        Inventory inv = create(p, SIZE_SMALL, lang("editor-title-row"));
        EditorHolder h = (EditorHolder) inv.getHolder();
        inv.setItem(4, backItem());
        h.on(4, e -> openEdit(p, d, draft, false, index));
        int slot = 10;
        int currentRow = rowOf(draft.layout, index);
        for (int r = 0; r < draft.layout.size() && slot < 17; r++, slot++) {
            int count = draft.layout.get(r);
            Component name = Component.text(lang("editor-row-n", "{n}", String.valueOf(r + 1), "{count}", String.valueOf(count)),
                    r == currentRow ? NamedTextColor.GREEN : NamedTextColor.WHITE);
            inv.setItem(slot, info(r == currentRow ? Material.LIME_DYE : Material.PAPER, name, List.of()));
            int target = r;
            if (r != currentRow) h.on(slot, e -> openEdit(p, d, draft, false, moveToRow(draft, index, target)));
        }
        p.openInventory(inv);
    }

    private static int firstIndexOfRow(List<Integer> layout, int row) {
        int sum = 0;
        for (int r = 0; r < row; r++) sum += layout.get(r);
        return sum;
    }

    /**
     * Removes the button from its current row (shrinking or dropping that row) and places it at
     * the start of {@code target}. @return the button's new position in {@code draft.answers}.
     */
    private int moveToRow(NodeDraft draft, int index, int target) {
        int fromRow = rowOf(draft.layout, index);
        Dialogue.Answer a = draft.answers.remove(index);
        draft.layout.set(fromRow, draft.layout.get(fromRow) - 1);
        // The item is already out of the list above, so the layout (fromRow's count included) is
        // consistent with the list as it stands right now: no further index adjustment is needed.
        // "target" is still a valid row index too: it is only used below, before fromRow can shift it.
        int insertAt = Math.max(0, Math.min(firstIndexOfRow(draft.layout, target), draft.answers.size()));
        draft.answers.add(insertAt, a);
        draft.layout.set(target, draft.layout.get(target) + 1);
        if (draft.layout.get(fromRow) <= 0) draft.layout.remove(fromRow);
        draft.dirty = true;
        return insertAt;
    }

    // ------------------------------------------------------------------------------ confirm menu

    private void openConfirmDelete(Player p, Dialogue d, NodeDraft draft, boolean nav, int index) {
        Inventory inv = create(p, SIZE_SMALL, lang("editor-title-confirm"));
        EditorHolder h = (EditorHolder) inv.getHolder();
        inv.setItem(11, info(Material.LIME_WOOL, lang("editor-confirm-yes"), List.of()));
        h.on(11, e -> {
            List<Dialogue.Answer> list = nav ? draft.nav : draft.answers;
            if (!nav && !draft.layout.isEmpty()) {
                int row = rowOf(draft.layout, index);
                draft.layout.set(row, draft.layout.get(row) - 1);
                if (draft.layout.get(row) <= 0) draft.layout.remove(row);
            }
            list.remove(index);
            draft.dirty = true;
            renderButtons(p, d, draft);
        });
        inv.setItem(15, info(Material.RED_WOOL, lang("editor-confirm-no"), List.of()));
        h.on(15, e -> openEdit(p, d, draft, nav, index));
        p.openInventory(inv);
    }

    // --------------------------------------------------------------------------------------- save

    private void save(Player p, Dialogue d, NodeDraft draft) {
        File file = pl.registry().fileOf(d.id);
        if (file == null) { p.sendMessage(Text.msg("editor-save-error", "{error}", "no file")); return; }
        try {
            DialogueFileEditor.save(file, draft.nodeId, draft.answers, draft.nav, draft.layout);
            draft.dirty = false;
            pl.reload();
            p.sendMessage(Text.msg("editor-saved", "{file}", relative(file)));
            Dialogue fresh = pl.registry().byId(d.id);
            if (fresh != null && fresh.nodes.containsKey(draft.nodeId)) openButtons(p, fresh, draft.nodeId);
            else open(p);
        } catch (Exception e) {
            p.sendMessage(Text.msg("editor-save-error", "{error}", String.valueOf(e.getMessage())));
        }
    }

    // --------------------------------------------------------------------------------- listeners

    @EventHandler(priority = EventPriority.LOW)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof EditorHolder h)) return;
        e.setCancelled(true);   // also blocks shift-clicking or dragging items out of the menu
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(top)) return;
        Consumer<InventoryClickEvent> handler = h.handlerAt(e.getRawSlot());
        if (handler != null) handler.accept(e);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof EditorHolder)) return;
        if (!(e.getPlayer() instanceof Player p)) return;
        // Moving from one editor menu to the next also fires this event for the menu being left
        // behind, so this only reacts once nothing editor-related is open a tick later: either the
        // player is mid chat-capture (pending != null) or really closed the inventory for good.
        Tasks.later(p, () -> {
            if (pending.containsKey(p.getUniqueId())) return;
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof EditorHolder) return;
            NodeDraft draft = drafts.remove(p.getUniqueId());
            if (draft != null && draft.dirty) p.sendMessage(Text.msg("editor-discarded"));
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        ChatCapture capture = pending.get(e.getPlayer().getUniqueId());
        if (capture == null) return;
        e.setCancelled(true);
        String text = plain(LegacyComponentSerializer.legacyAmpersand().serialize(e.message()));
        pending.remove(e.getPlayer().getUniqueId());
        Tasks.later(e.getPlayer(), () -> {
            if (text.trim().equalsIgnoreCase("cancel") || text.trim().equalsIgnoreCase("cancelar")) capture.onCancel().run();
            else capture.onInput().accept(text);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        drafts.remove(e.getPlayer().getUniqueId());
        pending.remove(e.getPlayer().getUniqueId());
    }

    // -------------------------------------------------------------------------------- item helpers

    private Inventory create(Player p, int size, String title) {
        EditorHolder h = new EditorHolder(size);
        Inventory inv = Bukkit.createInventory(h, size, Text.color(title));
        h.setInventory(inv);
        return inv;
    }

    private ItemStack backItem() {
        return info(Material.ARROW, lang("editor-back"), List.of());
    }

    private ItemStack divider() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = it.getItemMeta();
        m.displayName(Component.empty());
        it.setItemMeta(m);
        return it;
    }

    private ItemStack info(Material m, String name, List<Component> lore) {
        return info(m, Component.text(name, NamedTextColor.AQUA), lore);
    }

    private ItemStack info(Material m, Component name, List<Component> lore) {
        ItemStack it = new ItemStack(m);
        apply(it, name.decoration(TextDecoration.ITALIC, false), lore);
        return it;
    }

    private void apply(ItemStack it, Component name, List<Component> lore) {
        ItemMeta meta = it.getItemMeta();
        meta.displayName(name.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        meta.lore(lore);
        it.setItemMeta(meta);
    }

    // lore lines come from the language file, so they can carry their own &-colours
    private Component dim(String s) {
        return Text.color(s).colorIfAbsent(NamedTextColor.GRAY);
    }

    private Component hint(String s) {
        return Text.color(s).colorIfAbsent(NamedTextColor.DARK_GRAY);
    }

    private static String plain(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("&#[0-9a-fA-F]{6}", "").replaceAll("&[0-9a-fk-orA-FK-OR]", "").trim();
    }

    private String lang(String key, String... pairs) { return pl.lang().raw(key, pairs); }
}

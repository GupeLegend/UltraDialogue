UltraDialogue - resource pack
=============================

UltraDialoguePack.zip holds the drawn RPG buttons of the dialogue screens: a dark panel with a
double border, coloured by the kind of answer (mission, main, side quest, riddle, reward...).
Without it, the plugin works exactly the same, but every answer shows as a plain "[ text ]"
button.

UltraDialogue doesn't send resource packs to players by itself. On startup it puts this pack
inside the plugin that builds your server's pack, so players keep getting a single pack. It does
that with any of these:

    ResourcePackManager, Nexo, Oraxen, ItemsAdder, CraftEngine, EcoItems

None of them was found on your server, so the pack was left here. You have three ways to load it:

1. Easiest: install one of those plugins and restart.
   ResourcePackManager (free, https://modrinth.com/plugin/resourcepackmanager) merges and hosts
   the packs of all your plugins. UltraDialogue will find it on the next start and install itself.

2. If you already have your own server pack: open UltraDialoguePack.zip and copy its "assets"
   folder into your pack (merge the folders). Everything is under assets/ultradialogue except
   one file, assets/minecraft/textures/gui/inworld_menu_background.png: the darker background
   behind the dialogue window. Then set this in plugins/UltraDialogue/config.yml and restart:
       resource-pack:
         merged-by-hand: true

3. If you don't have a pack: upload UltraDialoguePack.zip somewhere with a direct download link,
   set these in server.properties and restart, then set merged-by-hand: true as in step 2:
       resource-pack=<direct link to the zip>
       resource-pack-sha1=<SHA-1 of the zip>

With merged-by-hand: true, a player gets the drawn buttons once the server's pack has loaded on
their client. Players who decline it keep the "[ text ]" buttons.

This folder is written again on every start, so don't edit the files here.

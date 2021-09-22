# CustomEnderChestToPurpur
Converts enderchest data from CustomEnderChest plugin to Purpur

# How to Use
1. Put the plugin in your plugins folder
2. Start the server
3. Once everything's loaded, stop teh server
4. Edit the plugin's config.yml, relace the database connection info with the same info you used for CustomEnderChests
5. Save the config.yml and start the server
6. The plugin will convert the enderchests on startup. If any player can't be converted, they will be skipped and the skip will be logged in console.
7. Once the conversion has finished, stop the server and remove the plugin.

# Caveats
This only converts data from CustomEnderChest MySQL databases. It won't work for other storage types.
This converts data to vanilla enderchests, designed to be used with Purpur which supports larger enderchests. It may break enderchests if you're not using Purpur.
This will overwrite players' vanilla enderchests with the enderchest contents from the CustomEnderChest database.

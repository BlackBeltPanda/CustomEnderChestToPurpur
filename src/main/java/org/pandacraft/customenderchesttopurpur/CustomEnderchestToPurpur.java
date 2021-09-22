package org.pandacraft.customenderchesttopurpur;

import com.mojang.authlib.GameProfile;
import net.minecraft.ResourceKeyInvalidException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryEnderChest;
import net.minecraft.world.level.World;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;

public final class CustomEnderchestToPurpur extends JavaPlugin {

    private static FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        convertEnder();
    }

    public void convertEnder() {

        Connection conn;
        try {
            //Load Drivers
            Class.forName("com.mysql.jdbc.Driver");

            Properties properties = new Properties();
            properties.setProperty("user", config.getString("database.mysql.user"));
            properties.setProperty("password", config.getString("database.mysql.password"));
            properties.setProperty("autoReconnect", "true");
            properties.setProperty("verifyServerCertificate", "false");
            properties.setProperty("useSSL", String.valueOf(config.getBoolean("database.mysql.ssl")));
            properties.setProperty("requireSSL", String.valueOf(config.getBoolean("database.mysql.ssl")));

            //Connect to database
            conn = DriverManager.getConnection("jdbc:mysql://" + config.getString("database.mysql.host") + ":" + config.getString("database.mysql.port")
                    + "/" + config.getString("database.mysql.databaseName"), properties);

            PreparedStatement preparedUpdateStatement;
            ResultSet result;

            try {
                String sql = "SELECT player_uuid, enderchest_data FROM `" + config.getString("database.mysql.tableName") + "`";
                preparedUpdateStatement = conn.prepareStatement(sql);
                result = preparedUpdateStatement.executeQuery();
                while (result.next()) {
                    try {
                        Inventory mysqlInv = fromBase64(result.getString("enderchest_data"));
                        UUID uuid = UUID.fromString(result.getString("player_uuid"));

                        Bukkit.getLogger().log(Level.INFO, "Converting " + uuid);

                        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
                        if (!offline.hasPlayedBefore()) {
                            Bukkit.getLogger().log(Level.INFO, "Skipping " + uuid);
                            continue;
                        }

                        GameProfile profile = new GameProfile(offline.getUniqueId(),
                                offline.getName() != null ? offline.getName() : offline.getUniqueId().toString());

                        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
                        WorldServer worldServer = server.getWorldServer(World.f);

                        if (worldServer == null) {
                            return;
                        }

                        EntityPlayer entity = null;

                        try {
                            entity = new EntityPlayer(server, worldServer, profile);
                        } catch (ResourceKeyInvalidException ignored) {
                        }

                        if (entity == null) {
                            Bukkit.getLogger().log(Level.INFO, "Skipping " + uuid);
                            continue;
                        }

                        try {
                            Field bukkitEntity = Entity.class.getDeclaredField("bukkitEntity");

                            bukkitEntity.setAccessible(true);

                            bukkitEntity.set(entity, new OpenPlayer(entity.c.server, entity));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }

                        // Get the bukkit entity
                        Player target = entity.getBukkitEntity();
                        // Load data
                        target.loadData();

                        InventoryEnderChest endInv = entity.getEnderChest();

                        for (int i = 0; i < endInv.getSize(); i++) {
                            if (i < mysqlInv.getSize()) {
                                ItemStack item = mysqlInv.getItem(i);
                                if (item != null) {
                                    endInv.setItem(i, CraftItemStack.asNMSCopy(item));
                                }
                            }
                        }

                        endInv.update();
                        target.saveData();

                        Bukkit.getLogger().log(Level.INFO, "Converted " + uuid);

                    } catch (IOException | ClassNotFoundException | SQLException e) {
                        e.printStackTrace();
                    }
                }

                Bukkit.getLogger().log(Level.INFO, "Conversion finished");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Inventory fromBase64(String data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        Inventory inventory = Bukkit.getServer().createInventory(null, dataInput.readInt());

        // Read the serialized inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, (ItemStack) dataInput.readObject());
        }

        dataInput.close();
        return inventory;
    }
}

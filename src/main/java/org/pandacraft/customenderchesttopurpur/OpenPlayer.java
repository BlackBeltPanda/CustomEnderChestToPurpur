package org.pandacraft.customenderchesttopurpur;

import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.level.storage.WorldNBTStorage;
import org.apache.logging.log4j.LogManager;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;

import java.io.File;
import java.io.FileOutputStream;

public class OpenPlayer extends CraftPlayer {

    public OpenPlayer(CraftServer server, EntityPlayer entity) {
        super(server, entity);
    }

    @Override
    public void loadData() {
        // See CraftPlayer#loadData
        NBTTagCompound loaded = this.server.getHandle().r.load(this.getHandle());
        if (loaded != null) {
            readExtraData(loaded);
        }
    }

    @Override
    public void saveData() {
        EntityPlayer player = this.getHandle();
        // See net.minecraft.world.level.storage.WorldNBTStorage#save(EntityHuman)
        try {
            WorldNBTStorage worldNBTStorage = player.c.getPlayerList().r;

            NBTTagCompound playerData = player.save(new NBTTagCompound());
            setExtraData(playerData);

            if (!isOnline()) {
                // Special case: save old vehicle data
                NBTTagCompound oldData = worldNBTStorage.load(player);

                if (oldData != null && oldData.hasKeyOfType("RootVehicle", 10)) {
                    // See net.minecraft.server.PlayerList#a(NetworkManager, EntityPlayer) and net.minecraft.server.EntityPlayer#b(NBTTagCompound)
                    playerData.set("RootVehicle", oldData.getCompound("RootVehicle"));
                }
            }

            File file = new File(worldNBTStorage.getPlayerDir(), player.getUniqueIDString() + ".dat.tmp");
            File file1 = new File(worldNBTStorage.getPlayerDir(), player.getUniqueIDString() + ".dat");

            NBTCompressedStreamTools.a(playerData, new FileOutputStream(file));

            if (file1.exists() && !file1.delete() || !file.renameTo(file1)) {
                LogManager.getLogger().warn("Failed to save player data for {}", player.getDisplayName().getString());
            }

        } catch (Exception e) {
            LogManager.getLogger().warn("Failed to save player data for {}", player.getDisplayName().getString());
        }
    }

}
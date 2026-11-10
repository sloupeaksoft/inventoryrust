package com.rustinv.proxy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class CommonProxy {
    public void preInit() {
    }

    public void init() {
    }

    public void postInit() {
    }

    public Object getClientGui(int id, EntityPlayer player) {
        return null;
    }

    public void handleClothingSync(int entityId, NBTTagCompound data) {
    }
}

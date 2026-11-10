package com.rustinv.network;

import com.rustinv.RustInventory;
import com.rustinv.clothing.ClothingHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class NetworkHandler {
    public static SimpleNetworkWrapper CHANNEL;

    private NetworkHandler() {
    }

    public static void init() {
        CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(RustInventory.MODID);
        CHANNEL.registerMessage(MsgOpenInventory.Handler.class, MsgOpenInventory.class, 0, Side.SERVER);
        CHANNEL.registerMessage(MsgSyncClothing.Handler.class, MsgSyncClothing.class, 1, Side.CLIENT);
    }

    /** Отправить одежду игрока ему самому и всем, кто его видит. */
    public static void syncClothing(EntityPlayer player, ClothingHandler handler) {
        if (player.world.isRemote) return;
        MsgSyncClothing msg = new MsgSyncClothing(player.getEntityId(), handler.serializeNBT());
        if (player instanceof EntityPlayerMP) CHANNEL.sendTo(msg, (EntityPlayerMP) player);
        if (player.world instanceof WorldServer) {
            for (EntityPlayer p : ((WorldServer) player.world).getEntityTracker().getTrackingPlayers(player)) {
                if (p instanceof EntityPlayerMP && p != player) CHANNEL.sendTo(msg, (EntityPlayerMP) p);
            }
        }
    }

    public static void syncClothingTo(EntityPlayer owner, ClothingHandler handler, EntityPlayerMP receiver) {
        CHANNEL.sendTo(new MsgSyncClothing(owner.getEntityId(), handler.serializeNBT()), receiver);
    }
}

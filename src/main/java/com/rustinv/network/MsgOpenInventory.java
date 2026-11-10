package com.rustinv.network;

import com.rustinv.RustInventory;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Клиент -> сервер: открыть Rust-инвентарь. */
public class MsgOpenInventory implements IMessage {
    public MsgOpenInventory() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<MsgOpenInventory, IMessage> {
        @Override
        public IMessage onMessage(MsgOpenInventory message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (player.isSpectator()) return;
                player.openGui(RustInventory.instance, RustInventory.GUI_RUST_INVENTORY, player.world,
                        (int) player.posX, (int) player.posY, (int) player.posZ);
            });
            return null;
        }
    }
}

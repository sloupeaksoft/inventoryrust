package com.rustinv.network;

import com.rustinv.RustInventory;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Сервер -> клиент: одежда игрока с entityId. */
public class MsgSyncClothing implements IMessage {
    public int entityId;
    public NBTTagCompound data;

    public MsgSyncClothing() {
    }

    public MsgSyncClothing(int entityId, NBTTagCompound data) {
        this.entityId = entityId;
        this.data = data;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        data = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        ByteBufUtils.writeTag(buf, data);
    }

    public static class Handler implements IMessageHandler<MsgSyncClothing, IMessage> {
        @Override
        public IMessage onMessage(MsgSyncClothing message, MessageContext ctx) {
            RustInventory.proxy.handleClothingSync(message.entityId, message.data);
            return null;
        }
    }
}

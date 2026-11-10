package com.rustinv.clothing;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nullable;

public final class ClothingCapability {

    @CapabilityInject(ClothingHandler.class)
    public static Capability<ClothingHandler> CLOTHING = null;

    private ClothingCapability() {
    }

    public static void register() {
        CapabilityManager.INSTANCE.register(ClothingHandler.class, new Capability.IStorage<ClothingHandler>() {
            @Nullable
            @Override
            public NBTBase writeNBT(Capability<ClothingHandler> capability, ClothingHandler instance, EnumFacing side) {
                return instance.serializeNBT();
            }

            @Override
            public void readNBT(Capability<ClothingHandler> capability, ClothingHandler instance, EnumFacing side, NBTBase nbt) {
                if (nbt instanceof NBTTagCompound) instance.deserializeNBT((NBTTagCompound) nbt);
            }
        }, ClothingHandler::new);
    }

    @Nullable
    public static ClothingHandler get(Entity entity) {
        if (entity == null || CLOTHING == null) return null;
        return entity.hasCapability(CLOTHING, null) ? entity.getCapability(CLOTHING, null) : null;
    }

    public static class Provider implements ICapabilitySerializable<NBTTagCompound> {
        private final ClothingHandler handler = new ClothingHandler();

        @Override
        public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == CLOTHING;
        }

        @Nullable
        @Override
        public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
            return capability == CLOTHING ? CLOTHING.cast(handler) : null;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            return handler.serializeNBT();
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            handler.deserializeNBT(nbt);
        }
    }
}

package com.rustinv.clothing;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.HashSet;
import java.util.Set;

/** 7 слотов одежды, как в Rust. */
public class ClothingHandler extends ItemStackHandler {
    public static final int SLOTS = 7;

    /** нужно отправить данные клиентам */
    public transient boolean dirty = true;
    /** отложенная синхронизация (тики) */
    public transient int syncDelay = 10;
    /** применённые модификаторы атрибутов */
    public final transient Set<String> appliedAttributes = new HashSet<>();
    public transient boolean attributesDirty = true;

    public ClothingHandler() {
        super(SLOTS);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    // Без @Override — на случай старых сборок Forge без этого метода.
    public boolean isItemValid(int slot, ItemStack stack) {
        return ClothingRules.canEquip(this, slot, stack);
    }

    @Override
    protected void onContentsChanged(int slot) {
        dirty = true;
        attributesDirty = true;
    }

    @Override
    protected void onLoad() {
        dirty = true;
        attributesDirty = true;
    }
}

package com.rustinv.inventory;

import com.rustinv.clothing.ClothingCapability;
import com.rustinv.clothing.ClothingHandler;
import com.rustinv.clothing.ClothingRules;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * Слоты контейнера:
 *  0..6   — одежда (7)
 *  7      — вторая рука
 *  8..31  — инвентарь 6x4
 *  32..37 — пояс 6
 */
public class ContainerRust extends Container {
    public static final int WIDTH = 300;
    public static final int HEIGHT = 162;

    public static final int LEFT_X = 9;
    public static final int GRID_X = 161;
    public static final int GRID_Y = 40;
    public static final int CELL = 22;
    public static final int CLOTH_STEP = 20;
    public static final int CLOTH_Y = GRID_Y + 3 * CELL;   // на уровне последнего ряда инвентаря
    public static final int OFFHAND_Y = CLOTH_Y - 24;
    public static final int BELT_Y = GRID_Y + 4 * CELL + 10;

    public static final int CLOTH_START = 0, CLOTH_END = 7;
    public static final int OFFHAND = 7;
    public static final int GRID_START = 8, GRID_END = 32;
    public static final int BELT_START = 32, BELT_END = 38;

    private final EntityPlayer player;

    public ContainerRust(EntityPlayer player) {
        this.player = player;
        InventoryPlayer inv = player.inventory;
        ClothingHandler clothing = ClothingCapability.get(player);
        if (clothing == null) clothing = new ClothingHandler();

        for (int i = 0; i < ClothingHandler.SLOTS; i++) {
            addSlotToContainer(new SlotClothing(clothing, i, LEFT_X + i * CLOTH_STEP, CLOTH_Y));
        }
        addSlotToContainer(new Slot(inv, 40, LEFT_X, OFFHAND_Y) {
            @Nullable
            @Override
            @SideOnly(Side.CLIENT)
            public String getSlotTexture() {
                return "minecraft:items/empty_armor_slot_shield";
            }
        });
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 6; c++) {
                addSlotToContainer(new Slot(inv, 9 + r * 6 + c, GRID_X + c * CELL, GRID_Y + r * CELL));
            }
        }
        for (int c = 0; c < 6; c++) {
            addSlotToContainer(new Slot(inv, c, GRID_X + c * CELL, BELT_Y));
        }
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();

        if (index < GRID_START) {
            // одежда / вторая рука -> инвентарь, потом пояс
            if (!mergeItemStack(stack, GRID_START, BELT_END, false)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            if (ClothingRules.isClothing(stack)) {
                for (int i = CLOTH_START; i < CLOTH_END; i++) {
                    Slot s = inventorySlots.get(i);
                    if (!s.getHasStack() && s.isItemValid(stack)) {
                        s.putStack(stack.splitStack(1));
                        moved = true;
                        break;
                    }
                }
            }
            if (!moved) {
                if (index < GRID_END) {
                    if (!mergeItemStack(stack, BELT_START, BELT_END, false)) return ItemStack.EMPTY;
                } else {
                    if (!mergeItemStack(stack, GRID_START, GRID_END, false)) return ItemStack.EMPTY;
                }
            }
        }

        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();

        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(playerIn, stack);
        return copy;
    }
}

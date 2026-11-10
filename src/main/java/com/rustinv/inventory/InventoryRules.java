package com.rustinv.inventory;

import com.rustinv.ModConfig;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Раскладка как в Rust: пояс 6 слотов (0-5) + инвентарь 24 слота (9-32).
 * Слоты 6,7,8 (хотбар) и 33,34,35 (инвентарь) заблокированы.
 */
public final class InventoryRules {
    public static final int BELT = 6;
    public static final int[] ALLOWED;

    static {
        ALLOWED = new int[30];
        int n = 0;
        for (int i = 0; i < 6; i++) ALLOWED[n++] = i;
        for (int i = 9; i <= 32; i++) ALLOWED[n++] = i;
    }

    private InventoryRules() {
    }

    public static boolean isLocked(int index) {
        if (!ModConfig.rustSlotLayout) return false;
        return (index >= 6 && index <= 8) || (index >= 33 && index <= 35);
    }

    /** Кладёт предмет только в разрешённые слоты. Возвращает остаток. */
    public static ItemStack insert(InventoryPlayer inv, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int[] slots = ModConfig.rustSlotLayout ? ALLOWED : allVanilla();
        for (int idx : slots) {
            ItemStack cur = inv.getStackInSlot(idx);
            if (!cur.isEmpty() && ItemHandlerHelper.canItemStacksStack(cur, stack)) {
                int max = Math.min(cur.getMaxStackSize(), inv.getInventoryStackLimit());
                int move = Math.min(stack.getCount(), max - cur.getCount());
                if (move > 0) {
                    cur.grow(move);
                    stack.shrink(move);
                    if (stack.isEmpty()) {
                        inv.markDirty();
                        return ItemStack.EMPTY;
                    }
                }
            }
        }
        for (int idx : slots) {
            if (inv.getStackInSlot(idx).isEmpty()) {
                inv.setInventorySlotContents(idx, stack.copy());
                inv.markDirty();
                return ItemStack.EMPTY;
            }
        }
        inv.markDirty();
        return stack;
    }

    private static int[] allVanilla() {
        int[] a = new int[36];
        for (int i = 0; i < 36; i++) a[i] = i;
        return a;
    }
}

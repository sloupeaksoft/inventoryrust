package com.rustinv.inventory;

import com.rustinv.clothing.ClothingHandler;
import com.rustinv.clothing.ClothingRules;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class SlotClothing extends SlotItemHandler {
    private final ClothingHandler clothing;

    public SlotClothing(ClothingHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
        this.clothing = handler;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return ClothingRules.canEquip(clothing, getSlotIndex(), stack);
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return 1;
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        ItemStack s = getStack();
        if (!s.isEmpty() && !player.isCreative() && EnchantmentHelper.hasBindingCurse(s)) return false;
        return super.canTakeStack(player);
    }
}

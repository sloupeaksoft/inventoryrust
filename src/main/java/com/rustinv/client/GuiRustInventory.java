package com.rustinv.client;

import com.rustinv.clothing.ClothingRules;
import com.rustinv.inventory.ContainerRust;
import com.rustinv.inventory.SlotClothing;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.Collections;

/** Инвентарь в стиле Rust. Без модельки игрока. */
public class GuiRustInventory extends GuiContainer {
    private static final int PANEL = 0xC0141414;
    private static final int HEADER = 0xE0262626;
    private static final int SLOT_BG = 0x60B4B4B4;
    private static final int CLOTH_BG = 0x709A8F7A;
    private static final int SELECTED = 0xC02F74C0;

    private final ContainerRust container;
    private final EntityPlayer player;

    public GuiRustInventory(ContainerRust container) {
        super(container);
        this.container = container;
        this.player = container.getPlayer();
        this.xSize = ContainerRust.WIDTH;
        this.ySize = ContainerRust.HEIGHT;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);

        Slot s = getSlotUnderMouse();
        if (s instanceof SlotClothing) {
            ItemStack held = mc.player.inventory.getItemStack();
            IItemHandler handler = ((SlotClothing) s).getItemHandler();
            if (!held.isEmpty() && ClothingRules.isClothing(held) && !s.isItemValid(held)) {
                int c = ClothingRules.findConflict(handler, s.getSlotIndex(), held);
                String name = c >= 0 ? handler.getStackInSlot(c).getDisplayName() : "?";
                drawHoveringText(Collections.singletonList("\u00a7c" + I18n.format("rustinventory.gui.conflict", name)), mouseX, mouseY);
            } else if (!held.isEmpty() && !ClothingRules.isClothing(held)) {
                drawHoveringText(Collections.singletonList("\u00a7c" + I18n.format("rustinventory.gui.not_clothing")), mouseX, mouseY);
            } else if (!s.getHasStack() && held.isEmpty()) {
                drawHoveringText(Collections.singletonList(I18n.format("rustinventory.gui.clothing_slot")), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int x = guiLeft, y = guiTop;
        int split = ContainerRust.GRID_X - 8;

        // левая панель (вместо скина — имя и характеристики)
        drawRect(x, y, x + split - 4, y + ySize, PANEL);
        drawRect(x, y, x + split - 4, y + 20, HEADER);
        // правая панель (инвентарь)
        drawRect(x + split, y + ContainerRust.GRID_Y - 18, x + xSize, y + ySize, PANEL);
        drawRect(x + split, y + ContainerRust.GRID_Y - 18, x + xSize, y + ContainerRust.GRID_Y - 4, HEADER);

        int sel = Math.min(player.inventory.currentItem, 5);
        for (int i = 0; i < container.inventorySlots.size(); i++) {
            Slot s = container.inventorySlots.get(i);
            int sx = x + s.xPos, sy = y + s.yPos;
            if (i < ContainerRust.CLOTH_END) {
                drawRect(sx - 1, sy - 1, sx + 17, sy + 17, CLOTH_BG);
            } else {
                int color = SLOT_BG;
                if (i == ContainerRust.BELT_START + sel) color = SELECTED;
                drawRect(sx - 2, sy - 2, sx + 18, sy + 18, color);
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int white = 0xFFFFFF, gray = 0xB0B0B0;
        fontRenderer.drawString(player.getName(), 8, 6, white);

        int armor = player.getTotalArmorValue();
        double tough = player.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue();
        fontRenderer.drawString(I18n.format("rustinventory.gui.health", (int) Math.ceil(player.getHealth()), (int) player.getMaxHealth()), 8, 28, gray);
        fontRenderer.drawString(I18n.format("rustinventory.gui.armor", armor), 8, 40, gray);
        fontRenderer.drawString(I18n.format("rustinventory.gui.toughness", (int) tough), 8, 52, gray);

        fontRenderer.drawString(I18n.format("rustinventory.gui.clothing"), ContainerRust.LEFT_X + 24, ContainerRust.OFFHAND_Y + 5, white);
        fontRenderer.drawString(I18n.format("rustinventory.gui.inventory"), ContainerRust.GRID_X - 2, ContainerRust.GRID_Y - 14, white);
        fontRenderer.drawString(I18n.format("rustinventory.gui.belt"), ContainerRust.GRID_X - 2, ContainerRust.BELT_Y - 11, gray);
    }
}

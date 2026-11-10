package com.rustinv.client;

import com.rustinv.clothing.ClothingCapability;
import com.rustinv.clothing.ClothingHandler;
import com.rustinv.clothing.ClothingRules;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Рисует на игроке ВСЮ одежду из Rust-слотов, слоями: сначала футболки/штаны,
 * потом куртки, потом броня. Каждый слой — чуть большая модель, чтобы не было мерцания.
 * Используются текстуры и кастомные модели брони из твоего мода (getArmorTexture / getArmorModel).
 */
public class LayerRustClothing extends LayerBipedArmor {
    private final RenderLivingBase<?> rend;
    private final Map<Float, ModelBiped> models = new HashMap<>();

    public LayerRustClothing(RenderLivingBase<?> renderer) {
        super(renderer);
        this.rend = renderer;
    }

    @Override
    public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks,
                              float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        if (!(entity instanceof EntityPlayer)) return;
        ClothingHandler h = ClothingCapability.get(entity);
        if (h == null) return;

        List<ItemStack> worn = new ArrayList<>();
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (!s.isEmpty() && s.getItem() instanceof ItemArmor) worn.add(s);
        }
        if (worn.isEmpty()) return;
        worn.sort(Comparator.comparingInt(ClothingRules::renderLayer));

        for (ItemStack s : worn) {
            renderPiece(entity, s, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderPiece(EntityLivingBase entity, ItemStack stack, float limbSwing, float limbSwingAmount,
                             float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        ItemArmor armor = (ItemArmor) stack.getItem();
        EntityEquipmentSlot slot = armor.armorType;
        float inflation = ClothingRules.inflation(stack);
        ModelBiped base = models.get(inflation);
        if (base == null) {
            base = new ModelBiped(inflation);
            models.put(inflation, base);
        }
        ModelBiped model = getArmorModelHook(entity, stack, slot, base);
        model.setModelAttributes(rend.getMainModel());
        model.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
        setModelSlotVisible(model, slot);

        rend.bindTexture(getArmorResource(entity, stack, slot, null));
        if (armor.hasOverlay(stack)) {
            int color = armor.getColor(stack);
            float r = (float) (color >> 16 & 255) / 255.0F;
            float g = (float) (color >> 8 & 255) / 255.0F;
            float b = (float) (color & 255) / 255.0F;
            GlStateManager.color(r, g, b, 1.0F);
            model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            rend.bindTexture(getArmorResource(entity, stack, slot, "overlay"));
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);

        if (stack.hasEffect()) {
            LayerArmorBase.renderEnchantedGlint(rend, entity, model, limbSwing, limbSwingAmount, partialTicks,
                    ageInTicks, netHeadYaw, headPitch, scale);
        }
    }
}

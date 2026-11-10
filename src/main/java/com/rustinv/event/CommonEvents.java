package com.rustinv.event;

import com.google.common.collect.Multimap;
import com.rustinv.ModConfig;
import com.rustinv.RustInventory;
import com.rustinv.clothing.ClothingCapability;
import com.rustinv.clothing.ClothingHandler;
import com.rustinv.clothing.ClothingRules;
import com.rustinv.inventory.InventoryRules;
import com.rustinv.network.NetworkHandler;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketHeldItemChange;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommonEvents {

    private static final ResourceLocation CAP_KEY = new ResourceLocation(RustInventory.MODID, "clothing");
    private static final int[] LOCKED = {6, 7, 8, 33, 34, 35};

    // ------------------------------------------------------------------ capability

    @SubscribeEvent
    public void attachCaps(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(CAP_KEY, new ClothingCapability.Provider());
        }
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        ClothingHandler oldH = ClothingCapability.get(event.getOriginal());
        ClothingHandler newH = ClothingCapability.get(event.getEntityPlayer());
        if (oldH == null || newH == null) return;
        boolean keep = event.getEntityPlayer().world.getGameRules().getBoolean("keepInventory");
        if (!event.isWasDeath() || keep) newH.deserializeNBT(oldH.serializeNBT());
        newH.dirty = true;
        newH.syncDelay = 10;
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof EntityPlayer && event.getEntityPlayer() instanceof EntityPlayerMP) {
            ClothingHandler h = ClothingCapability.get(event.getTarget());
            if (h != null) {
                NetworkHandler.syncClothingTo((EntityPlayer) event.getTarget(), h, (EntityPlayerMP) event.getEntityPlayer());
            }
        }
    }

    private static void scheduleSync(EntityPlayer p) {
        ClothingHandler h = ClothingCapability.get(p);
        if (h != null) {
            h.dirty = true;
            h.attributesDirty = true;
            h.syncDelay = 10;
        }
    }

    @SubscribeEvent
    public void onLogin(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        scheduleSync(event.player);
    }

    @SubscribeEvent
    public void onRespawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
        scheduleSync(event.player);
    }

    @SubscribeEvent
    public void onDimChange(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event) {
        scheduleSync(event.player);
    }

    // ------------------------------------------------------------------ tick

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer p = event.player;
        ClothingHandler h = ClothingCapability.get(p);
        if (h == null) return;

        // эффекты брони из мода (onArmorTick) работают и для одежды в Rust-слотах
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (!s.isEmpty()) s.getItem().onArmorTick(p.world, p, s);
        }

        if (p.world.isRemote) return;
        InventoryPlayer inv = p.inventory;

        if (ModConfig.rustSlotLayout) {
            // заблокированные слоты всегда пустые
            for (int idx : LOCKED) {
                ItemStack s = inv.getStackInSlot(idx);
                if (!s.isEmpty()) {
                    inv.setInventorySlotContents(idx, ItemStack.EMPTY);
                    ItemStack rem = InventoryRules.insert(inv, s);
                    if (!rem.isEmpty()) p.dropItem(rem, false, true);
                }
            }
            if (inv.currentItem >= InventoryRules.BELT) {
                inv.currentItem = InventoryRules.BELT - 1;
                if (p instanceof EntityPlayerMP) {
                    ((EntityPlayerMP) p).connection.sendPacket(new SPacketHeldItemChange(inv.currentItem));
                }
            }
        }

        // ванильные слоты брони не используются — переносим броню в слоты одежды
        for (int i = 0; i < inv.armorInventory.size(); i++) {
            ItemStack s = inv.armorInventory.get(i);
            if (s.isEmpty() || !(s.getItem() instanceof ItemArmor)) continue; // элитры/тыквы оставляем
            inv.armorInventory.set(i, ItemStack.EMPTY);
            int slot = ClothingRules.findSlotFor(h, s);
            if (slot >= 0 && s.getCount() == 1) {
                h.setStackInSlot(slot, s);
            } else {
                ItemStack rem = InventoryRules.insert(inv, s);
                if (!rem.isEmpty()) p.dropItem(rem, false, true);
            }
        }

        if (h.attributesDirty) {
            h.attributesDirty = false;
            applyAttributes(p, h);
        }

        if (h.syncDelay > 0) {
            h.syncDelay--;
        } else if (h.dirty) {
            h.dirty = false;
            NetworkHandler.syncClothing(p, h);
        }
    }

    private static UUID attrUuid(String name, int op) {
        return UUID.nameUUIDFromBytes(("rustinventory:" + name + ":" + op).getBytes(StandardCharsets.UTF_8));
    }

    /** Суммируем защиту/прочность (и другие атрибуты) всех надетых вещей. */
    private static void applyAttributes(EntityPlayer p, ClothingHandler h) {
        Map<String, double[]> sums = new HashMap<>();
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (s.isEmpty() || !(s.getItem() instanceof ItemArmor)) continue;
            EntityEquipmentSlot es = ((ItemArmor) s.getItem()).armorType;
            Multimap<String, AttributeModifier> mods = s.getAttributeModifiers(es);
            for (Map.Entry<String, AttributeModifier> e : mods.entries()) {
                int op = e.getValue().getOperation();
                if (op < 0 || op > 2) continue;
                sums.computeIfAbsent(e.getKey(), k -> new double[3])[op] += e.getValue().getAmount();
            }
        }
        for (String key : h.appliedAttributes) {
            int sep = key.lastIndexOf('|');
            String name = key.substring(0, sep);
            int op = Integer.parseInt(key.substring(sep + 1));
            IAttributeInstance inst = p.getAttributeMap().getAttributeInstanceByName(name);
            if (inst != null) inst.removeModifier(attrUuid(name, op));
        }
        h.appliedAttributes.clear();
        for (Map.Entry<String, double[]> e : sums.entrySet()) {
            IAttributeInstance inst = p.getAttributeMap().getAttributeInstanceByName(e.getKey());
            if (inst == null) continue;
            for (int op = 0; op < 3; op++) {
                double v = e.getValue()[op];
                if (v == 0) continue;
                UUID id = attrUuid(e.getKey(), op);
                inst.removeModifier(id);
                inst.applyModifier(new AttributeModifier(id, "Rust clothing", v, op).setSaved(false));
                h.appliedAttributes.add(e.getKey() + "|" + op);
            }
        }
    }

    // ------------------------------------------------------------------ подбор предметов (только в 30 слотов)

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPickup(EntityItemPickupEvent event) {
        if (!ModConfig.rustSlotLayout || event.isCanceled()) return;
        EntityPlayer p = event.getEntityPlayer();
        EntityItem ei = event.getItem();
        if (p.world.isRemote || ei.isDead) return;
        event.setCanceled(true);
        ItemStack stack = ei.getItem();
        int before = stack.getCount();
        ItemStack rem = InventoryRules.insert(p.inventory, stack.copy());
        int taken = before - rem.getCount();
        if (taken <= 0) return;
        p.onItemPickup(ei, taken);
        p.world.playSound(null, p.posX, p.posY, p.posZ, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS,
                0.2F, ((p.getRNG().nextFloat() - p.getRNG().nextFloat()) * 0.7F + 1.0F) * 2.0F);
        if (rem.isEmpty()) ei.setDead();
        else ei.setItem(rem);
    }

    // ------------------------------------------------------------------ ПКМ бронёй = надеть в слот одежды

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack st = event.getItemStack();
        if (!(st.getItem() instanceof ItemArmor) || !ClothingRules.isClothing(st)) return;
        EntityPlayer p = event.getEntityPlayer();
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (p.world.isRemote) return;
        ClothingHandler h = ClothingCapability.get(p);
        if (h == null) return;
        int slot = ClothingRules.findSlotFor(h, st);
        if (slot < 0) {
            int c = ClothingRules.findConflict(h, -1, st);
            if (c >= 0) {
                p.sendStatusMessage(new TextComponentTranslation("rustinventory.msg.conflict",
                        h.getStackInSlot(c).getDisplayName()), true);
            } else {
                p.sendStatusMessage(new TextComponentTranslation("rustinventory.msg.full"), true);
            }
            return;
        }
        ItemArmor armor = (ItemArmor) st.getItem();
        h.setStackInSlot(slot, st.splitStack(1));
        p.world.playSound(null, p.posX, p.posY, p.posZ, armor.getArmorMaterial().getSoundEvent(),
                SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------ износ одежды

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.getSource().isUnblockable() || event.getAmount() <= 0) return;
        EntityPlayer p = (EntityPlayer) event.getEntityLiving();
        if (p.world.isRemote) return;
        ClothingHandler h = ClothingCapability.get(p);
        if (h == null) return;
        int dmg = Math.max(1, (int) (event.getAmount() / 4.0F));
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (s.isEmpty() || !(s.getItem() instanceof ItemArmor) || !s.isItemStackDamageable()) continue;
            s.damageItem(dmg, p);
            h.setStackInSlot(i, s.isEmpty() ? ItemStack.EMPTY : s);
        }
    }

    // ------------------------------------------------------------------ смерть: одежда выпадает

    @SubscribeEvent
    public void onDrops(PlayerDropsEvent event) {
        EntityPlayer p = event.getEntityPlayer();
        if (p.world.getGameRules().getBoolean("keepInventory")) return;
        ClothingHandler h = ClothingCapability.get(p);
        if (h == null) return;
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (s.isEmpty()) continue;
            h.setStackInSlot(i, ItemStack.EMPTY);
            if (EnchantmentHelper.hasVanishingCurse(s)) continue;
            EntityItem ei = new EntityItem(p.world, p.posX, p.posY + p.getEyeHeight() - 0.3D, p.posZ, s);
            ei.setPickupDelay(40);
            event.getDrops().add(ei);
        }
    }
}

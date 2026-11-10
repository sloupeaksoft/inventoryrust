package com.rustinv.clothing;

import com.rustinv.ModConfig;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.rustinv.clothing.Coverage.*;

/**
 * Правила одежды как в Rust: вещь занимает набор зон. Нельзя надеть две вещи с общей зоной.
 * Например: футболка (TORSO_UNDER) + куртка (TORSO_JACKET) + металлический нагрудник (TORSO_ARMOR) = можно.
 * Металлический + деревянный нагрудник (оба TORSO_ARMOR) = нельзя.
 */
public final class ClothingRules {

    private static final String P = "minerustaddons:";
    private static final Map<String, EnumSet<Coverage>> DEFAULTS = new HashMap<>();
    private static Map<String, EnumSet<Coverage>> active = null;

    static {
        // ---------- ТОРС: нижний слой (футболки, рубашки) ----------
        def("blacktshirtbody,blacktshirt1body,clothkit2body,greenkitbody,turquoisetshirtbody,yellowtshirtbody,"
                + "orangetshirtandbucketbody,wolfkitbody,easyclothkitbody", TORSO_UNDER);
        def("farmerkitbody", TORSO_UNDER, ARMS_UNDER);
        // ---------- ТОРС: куртки / кофты (надеваются ПОД нагрудник) ----------
        def("blackjacketsnowbody,bucketkitbody,clothkitbody,leatherarmorkitbody,vagabondjacketbody", TORSO_JACKET, ARMS_JACKET);
        // ---------- ТОРС: броня (только одна!) ----------
        def("mhqarmorkitbody,woodenkitbody,roadsignkitbody,bonekitbody", TORSO_ARMOR);
        def("heavyarmorbody", TORSO_ARMOR, TORSO_JACKET, ARMS_JACKET);
        // ---------- ГОЛОВА ----------
        def("blackjacketsnowhelmet,farmerkithelmet,leatherarmorkithelmet,santahathelmet,rabbitcostumehelmet,clothkithelmet", HEAD_OUTER);
        def("clothkit2helmet", HEAD_UNDER, FACE_UNDER);            // балаклава
        def("greenkithelmet", FACE_UNDER);                          // бандана
        def("woodenkithelmet,bonekithelmet,orangetshirtandbuckethelmet,roadsignkithelmet,wolfkithelmet", HEAD_OUTER, HEAD_ARMOR);
        def("mhqarmorkithelmet,heavyarmorhelmet,specialforceshelmethelmet", HEAD_OUTER, HEAD_ARMOR, FACE_ARMOR);
        def("divingsuithelmet", FACE_ARMOR);
        // ---------- НОГИ ----------
        def("blacktshirt1legs,clothkitlegs,easyclothkitlegs,bucketkitlegs,mhqarmorkitlegs,wolfkitlegs", LEGS_UNDER);
        def("woodenkitlegs,roadsignkitlegs,bonekitlegs", LEGS_ARMOR);
        def("heavyarmorlegs,rabbitcostumelegs", LEGS_UNDER, LEGS_ARMOR);
        // ---------- ОБУВЬ ----------
        def("blacktshirt1boots,bucketkitboots,mhqarmorkitboots,roadsignkitboots,clothkitboots,bluehazmatboots,weakhazmatboots", FEET);
        // ---------- РУКИ / СПИНА ----------
        def("hazmatglovesbody,leatherglovesbody", HANDS);
        def("divingsuitbody", BACK);
        // ---------- КОСТЮМЫ (занимают всё) ----------
        def("bluehazmatbody,weakhazmatbody", TORSO_UNDER, TORSO_JACKET, TORSO_ARMOR, ARMS_UNDER, ARMS_JACKET, LEGS_UNDER, LEGS_ARMOR);
        def("bluehazmathelmet,weakhazmathelmet", HEAD_UNDER, HEAD_OUTER, HEAD_ARMOR, FACE_UNDER, FACE_ARMOR);
        def("rabbitcostumebody", TORSO_UNDER, TORSO_JACKET, ARMS_UNDER, ARMS_JACKET);
    }

    private ClothingRules() {
    }

    private static void def(String names, Coverage first, Coverage... rest) {
        for (String n : names.split(",")) {
            DEFAULTS.put(P + n.trim(), EnumSet.of(first, rest));
        }
    }

    public static synchronized void reload() {
        active = null;
    }

    private static synchronized Map<String, EnumSet<Coverage>> map() {
        if (active == null) {
            Map<String, EnumSet<Coverage>> m = new HashMap<>(DEFAULTS);
            for (String line : ModConfig.clothingOverrides) {
                if (line == null) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim().toLowerCase(Locale.ROOT);
                EnumSet<Coverage> set = EnumSet.noneOf(Coverage.class);
                for (String part : line.substring(eq + 1).split(",")) {
                    String p = part.trim().toUpperCase(Locale.ROOT);
                    if (p.isEmpty() || p.equals("NONE")) continue;
                    try {
                        set.add(Coverage.valueOf(p));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                m.put(key, set);
            }
            active = m;
        }
        return active;
    }

    /** Какие зоны занимает предмет. Пустой набор = это не одежда. */
    public static EnumSet<Coverage> getCoverage(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemArmor)) return EnumSet.noneOf(Coverage.class);
        ResourceLocation rl = stack.getItem().getRegistryName();
        if (rl != null) {
            EnumSet<Coverage> s = map().get(rl.toString());
            if (s != null) return s;
        }
        // Неизвестная броня (ванильная или из других модов) — считаем её бронёй своего типа.
        EntityEquipmentSlot slot = ((ItemArmor) stack.getItem()).armorType;
        switch (slot) {
            case HEAD:
                return EnumSet.of(HEAD_OUTER, HEAD_ARMOR);
            case CHEST:
                return EnumSet.of(TORSO_ARMOR);
            case LEGS:
                return EnumSet.of(LEGS_ARMOR);
            case FEET:
                return EnumSet.of(FEET);
            default:
                return EnumSet.noneOf(Coverage.class);
        }
    }

    public static boolean isClothing(ItemStack stack) {
        return !getCoverage(stack).isEmpty();
    }

    /** Индекс слота с конфликтующей вещью, или -1. ignoreSlot — слот, который будет заменён. */
    public static int findConflict(IItemHandler handler, int ignoreSlot, ItemStack stack) {
        EnumSet<Coverage> mine = getCoverage(stack);
        for (int i = 0; i < handler.getSlots(); i++) {
            if (i == ignoreSlot) continue;
            ItemStack other = handler.getStackInSlot(i);
            if (other.isEmpty()) continue;
            for (Coverage c : getCoverage(other)) {
                if (mine.contains(c)) return i;
            }
        }
        return -1;
    }

    public static boolean canEquip(IItemHandler handler, int slot, ItemStack stack) {
        return isClothing(stack) && findConflict(handler, slot, stack) < 0;
    }

    /** Свободный слот одежды для предмета или -1. */
    public static int findSlotFor(IItemHandler handler, ItemStack stack) {
        if (!isClothing(stack) || findConflict(handler, -1, stack) >= 0) return -1;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).isEmpty()) return i;
        }
        return -1;
    }

    public static int renderLayer(ItemStack stack) {
        int l = 0;
        for (Coverage c : getCoverage(stack)) l = Math.max(l, c.layer);
        return l;
    }

    /** Насколько «раздута» модель брони — чтобы верхние слои рисовались поверх нижних без мерцания. */
    public static float inflation(ItemStack stack) {
        int layer = renderLayer(stack);
        boolean legs = stack.getItem() instanceof ItemArmor
                && ((ItemArmor) stack.getItem()).armorType == EntityEquipmentSlot.LEGS;
        if (legs) return new float[]{0.20F, 0.35F, 0.50F, 0.60F}[layer];
        return new float[]{0.35F, 0.65F, 1.00F, 1.10F}[layer];
    }
}

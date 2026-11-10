package com.rustinv;

import com.rustinv.clothing.ClothingRules;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = RustInventory.MODID)
public class ModConfig {

    @Config.Comment({
            "Переопределение слоёв одежды. Формат: modid:item=ЗОНА,ЗОНА,...",
            "Две вещи нельзя надеть вместе, если у них есть хотя бы одна одинаковая зона.",
            "Зоны: HEAD_UNDER, HEAD_OUTER, HEAD_ARMOR, FACE_UNDER, FACE_ARMOR, TORSO_UNDER, TORSO_JACKET,",
            "TORSO_ARMOR, ARMS_UNDER, ARMS_JACKET, HANDS, LEGS_UNDER, LEGS_ARMOR, FEET, BACK",
            "NONE = предмет нельзя надевать в слоты одежды.",
            "Пример: minerustaddons:clothkitbody=TORSO_JACKET,ARMS_JACKET"
    })
    public static String[] clothingOverrides = new String[0];

    @Config.Comment("Блокировать слоты 7-9 хотбара и 3 слота инвентаря (раст: 6 пояс + 24 инвентарь)")
    public static boolean rustSlotLayout = true;

    @Config.Comment("Заменять инвентарь на E (в творческом режиме откроется ванильный, используйте клавишу I)")
    public static boolean replaceInventoryScreen = true;

    @Mod.EventBusSubscriber(modid = RustInventory.MODID)
    public static class Sync {
        @SubscribeEvent
        public static void onChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (RustInventory.MODID.equals(event.getModID())) {
                ConfigManager.sync(RustInventory.MODID, Config.Type.INSTANCE);
                ClothingRules.reload();
            }
        }
    }
}

package com.rustinv.clothing;

import java.util.Locale;

/**
 * Зона тела + слой. Две вещи конфликтуют, если у них есть общая зона.
 * layer: 0 = нижний (футболки, штаны), 1 = средний (куртки, шапки), 2 = броня, 3 = поверх всего.
 */
public enum Coverage {
    HEAD_UNDER(0),
    HEAD_OUTER(1),
    HEAD_ARMOR(2),
    FACE_UNDER(0),
    FACE_ARMOR(2),
    TORSO_UNDER(0),
    TORSO_JACKET(1),
    TORSO_ARMOR(2),
    ARMS_UNDER(0),
    ARMS_JACKET(1),
    HANDS(3),
    LEGS_UNDER(0),
    LEGS_ARMOR(2),
    FEET(2),
    BACK(3);

    public final int layer;

    Coverage(int layer) {
        this.layer = layer;
    }

    public String langKey() {
        return "rustinventory.coverage." + name().toLowerCase(Locale.ROOT);
    }
}

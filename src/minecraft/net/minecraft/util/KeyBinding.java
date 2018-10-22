package net.minecraft.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class KeyBinding
{
    private static final List keybindArray = new ArrayList();
    private static final HashMap<Integer, KeyBinding> map = new HashMap();
    public static final KeyBinding keyBindForward = new KeyBinding(17);
    public static final KeyBinding keyBindLeft = new KeyBinding(30);
    public static final KeyBinding keyBindBack = new KeyBinding(31);
    public static final KeyBinding keyBindRight = new KeyBinding(32);
    public static final KeyBinding keyBindJump = new KeyBinding(57);
    public static final KeyBinding keyBindSneak = new KeyBinding(42);
    public static final KeyBinding keyBindUseItem = new KeyBinding(-99);
    public static final KeyBinding keyBindAttack = new KeyBinding(-100);

    /** because _303 wanted me to call it that(Caironater) */
    private boolean pressed;
    private int presses;

    public static void onTick(int p_74507_0_)
    {
        if (p_74507_0_ != 0)
        {
            KeyBinding var1 = map.get(p_74507_0_);

            if (var1 != null)
            {
                ++var1.presses;
            }
        }
    }

    public static void setKeyBindState(int p_74510_0_, boolean p_74510_1_)
    {
        if (p_74510_0_ != 0)
        {
            KeyBinding var2 = map.get(p_74510_0_);

            if (var2 != null)
            {
                var2.pressed = p_74510_1_;
            }
        }
    }

    public static void unPressAllKeys()
    {
        Iterator var0 = keybindArray.iterator();

        while (var0.hasNext())
        {
            KeyBinding var1 = (KeyBinding)var0.next();
            var1.presses = 0;
            var1.pressed = false;
        }
    }

    private KeyBinding(int p_i45001_2_)
    {
        keybindArray.add(this);
        map.put(p_i45001_2_, this);
    }

    public boolean getIsKeyPressed()
    {
        return this.pressed;
    }

    public boolean isPressed()
    {
        if (this.presses == 0)
        {
            return false;
        }
        else
        {
            --this.presses;
            return true;
        }
    }
}

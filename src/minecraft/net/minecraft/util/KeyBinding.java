package net.minecraft.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class KeyBinding
{
    private static final List<KeyBinding> keybindArray = new ArrayList<KeyBinding>();
    private static final HashMap<Integer, KeyBinding> map = new HashMap<Integer, KeyBinding>();
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

    public static void onTick(int key)
    {
        if (key != 0)
        {
            KeyBinding binding = map.get(key);

            if (binding != null)
            {
                ++binding.presses;
            }
        }
    }

    public static void setKeyBindState(int key, boolean p)
    {
        if (key != 0)
        {
            KeyBinding binding = map.get(key);

            if (binding != null)
            {
                binding.pressed = p;
            }
        }
    }

    public static void unPressAllKeys()
    {
        Iterator<KeyBinding> iter = keybindArray.iterator();

        while (iter.hasNext())
        {
            KeyBinding binding = iter.next();
            binding.presses = 0;
            binding.pressed = false;
        }
    }

    private KeyBinding(int key)
    {
        keybindArray.add(this);
        map.put(key, this);
    }

    public boolean getIsKeyPressed()
    {
        return this.pressed;
    }

    public boolean isPressed()
    {
        if (this.presses > 0)
        {
        	this.presses--;
            return true;
        }
        return false;
    }
}

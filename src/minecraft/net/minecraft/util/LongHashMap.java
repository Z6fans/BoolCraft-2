package net.minecraft.util;

public class LongHashMap<T>
{
    /** the array of all elements in the hash */
    @SuppressWarnings("unchecked")
	private transient LongHashMap.Entry<T>[] hashArray = (LongHashMap.Entry<T>[])(new LongHashMap.Entry[16]);

    /** the number of elements in the hash array */
    private transient int numHashElements;

    /**
     * the maximum amount of elements in the hash (probably 3/4 the size due to meh hashing function)
     */
    private int capacity = 12;

    /**
     * percent of the hasharray that can be used without hash colliding probably
     */
    private final float percentUseable = 0.75F;

    /** count of times elements have been added/removed */
    private transient volatile int modCount;

    /**
     * returns the hashed key given the original key
     */
    private static int getHashedKey(long key)
    {
        return hash((int)(key ^ key >>> 32));
    }

    /**
     * the hash function
     */
    private static int hash(int p_76157_0_)
    {
        p_76157_0_ ^= p_76157_0_ >>> 20 ^ p_76157_0_ >>> 12;
        return p_76157_0_ ^ p_76157_0_ >>> 7 ^ p_76157_0_ >>> 4;
    }

    /**
     * gets the index in the hash given the array length and the hashed key
     */
    private static int getHashIndex(int p_76158_0_, int p_76158_1_)
    {
        return p_76158_0_ & p_76158_1_ - 1;
    }

    public int getNumHashElements()
    {
        return this.numHashElements;
    }

    /**
     * get the value from the map given the key
     */
    public T getValueByKey(long p_76164_1_)
    {
        int var3 = getHashedKey(p_76164_1_);

        for (LongHashMap.Entry<T> var4 = this.hashArray[getHashIndex(var3, this.hashArray.length)]; var4 != null; var4 = var4.nextEntry)
        {
            if (var4.key == p_76164_1_)
            {
                return var4.value;
            }
        }

        return null;
    }

    public boolean containsItem(long p_76161_1_)
    {
    	int var3 = getHashedKey(p_76161_1_);

        for (LongHashMap.Entry<T> var4 = this.hashArray[getHashIndex(var3, this.hashArray.length)]; var4 != null; var4 = var4.nextEntry)
        {
            if (var4.key == p_76161_1_)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Add a key-value pair.
     */
    public void add(long key, T value)
    {
        int hash = getHashedKey(key);
        int index = getHashIndex(hash, this.hashArray.length);

        for (LongHashMap.Entry<T> var6 = this.hashArray[index]; var6 != null; var6 = var6.nextEntry)
        {
            if (var6.key == key)
            {
                var6.value = value;
                return;
            }
        }

        ++this.modCount;

        LongHashMap.Entry<T> nextEntry = this.hashArray[index];
        this.hashArray[index] = new LongHashMap.Entry<T>(hash, key, value, nextEntry);

        if (this.numHashElements++ >= this.capacity)
        {
            int newLength = 2 * this.hashArray.length;
            
            LongHashMap.Entry<T>[] oldArray = this.hashArray;
            int oldLength = oldArray.length;

            if (oldLength == 1073741824)
            {
                this.capacity = Integer.MAX_VALUE;
            }
            else
            {
                @SuppressWarnings("unchecked")
				LongHashMap.Entry<T>[] newArray = (LongHashMap.Entry<T>[])(new LongHashMap.Entry[newLength]);

                for (int i = 0; i < oldArray.length; ++i)
                {
                    LongHashMap.Entry<T> entry = oldArray[i];

                    if (entry != null)
                    {
                        oldArray[i] = null;
                        LongHashMap.Entry<T> tempEntry;

                        do
                        {
                            tempEntry = entry.nextEntry;
                            int hashIndex = getHashIndex(entry.hash, newLength);
                            entry.nextEntry = newArray[hashIndex];
                            newArray[hashIndex] = entry;
                            entry = tempEntry;
                        }
                        while (tempEntry != null);
                    }
                }
                
                this.hashArray = newArray;
                this.capacity = (int)((float)newLength * this.percentUseable);
            }
        }
    }

    /**
     * calls the removeKey method and returns removed object
     */
    public Object remove(long p_76159_1_)
    {
    	int var3 = getHashedKey(p_76159_1_);
        int var4 = getHashIndex(var3, this.hashArray.length);
        LongHashMap.Entry<T> var5 = this.hashArray[var4];
        LongHashMap.Entry<T> var6;
        LongHashMap.Entry<T> var7;

        for (var6 = var5; var6 != null; var6 = var7)
        {
            var7 = var6.nextEntry;

            if (var6.key == p_76159_1_)
            {
                ++this.modCount;
                --this.numHashElements;

                if (var5 == var6)
                {
                    this.hashArray[var4] = var7;
                }
                else
                {
                    var5.nextEntry = var7;
                }

                return var6;
            }

            var5 = var6;
        }
        
        return var6 == null ? null : var6.value;
    }

    private static class Entry<U>
    {
        final long key;
        U value;
        LongHashMap.Entry<U> nextEntry;
        final int hash;

        Entry(int hash, long key, U value, LongHashMap.Entry<U> nextEntry)
        {
            this.value = value;
            this.nextEntry = nextEntry;
            this.key = key;
            this.hash = hash;
        }

        private final long getKey()
        {
            return this.key;
        }

        private final U getValue()
        {
            return this.value;
        }

        public final boolean equals(Object p_equals_1_)
        {
            if (!(p_equals_1_ instanceof LongHashMap.Entry))
            {
                return false;
            }
            else
            {
                @SuppressWarnings("unchecked")
				LongHashMap.Entry<U> var2 = (LongHashMap.Entry<U>)p_equals_1_;
                Long var3 = Long.valueOf(this.getKey());
                Long var4 = Long.valueOf(var2.getKey());

                if (var3 == var4 || var3 != null && var3.equals(var4))
                {
                    Object var5 = this.getValue();
                    Object var6 = var2.getValue();

                    if (var5 == var6 || var5 != null && var5.equals(var6))
                    {
                        return true;
                    }
                }

                return false;
            }
        }

        public final int hashCode()
        {
            return LongHashMap.getHashedKey(this.key);
        }

        public final String toString()
        {
            return this.getKey() + "=" + this.getValue();
        }
    }
}

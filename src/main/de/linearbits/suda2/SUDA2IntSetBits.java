package de.linearbits.suda2;

import java.util.Arrays;

/**
 * A set of rows.
 *
 * @author Fabian Prasser
 */
public class SUDA2IntSetBits extends SUDA2IntSet {

    /** Bits per unit */
    private static final int ADDRESS_BITS_PER_UNIT = 6;

    /** Index mask */
    private static final int BIT_INDEX_MASK        = 63;
    
    /**
     * Returns whether a bitset makes sense
     * @param minimum
     * @param maximum
     * @param size
     */
    public static boolean makesSense(int minimum, int maximum, int size) {
        // TODO: There has to be a more efficient way of doing this
        return (getCapacity(size) << 5) >= maximum - minimum;
    }

    /**
     * Returns the smallest power of two larger than the given size
     * @param size
     * @return
     */
    private static final int getCapacity(int size) {
        --size;
        size |= size >> 1;
        size |= size >> 2;
        size |= size >> 4;
        size |= size >> 8;
        size |= size >> 16;
        return size + 1;
    }

    /** Array */
    private final long[]     array;

    /** Offset */
    private final int        offset;

    /** Number of bits set */
    private int              size;

    /** Min */
    private int              min                   = Integer.MAX_VALUE;

    /** Max */
    private int              max                   = Integer.MIN_VALUE;
    
    /**
     * Creates a new instance
     *
     * @param min
     * @param max
     */
    public SUDA2IntSetBits(int min, int max) {
        
        // Multiple of 64 less than or equal to min
        this.offset = min & (~0x3f);
        this.array = new long[(int) (Math.ceil((double) (max - offset + 1) / 64d))];
    }

    @Override
    public void add(int value) {
        min = Math.min(value, min);
        max = Math.max(value, max);
        value -= offset;
        int offset = value >> ADDRESS_BITS_PER_UNIT;
        this.array[offset] |= 1L << (value & BIT_INDEX_MASK);
        this.size ++; // TODO: Hopefully, we never add the same value twice
    }
    

    @Override
    public boolean contains(int value) {
        value -= this.offset;
        int offset = value >> ADDRESS_BITS_PER_UNIT;
        return (value < 0 || offset >= array.length) ? false : ((array[offset] & (1L << (value & BIT_INDEX_MASK))) != 0);
    }

    @Override
    public boolean containsSpecialRow(SUDA2Item[] items, SUDA2Item referenceItem, int[][] data) {
        int index = this.offset;
        int value = 0;
        for (int offset = 0; offset < this.array.length; offset++) {
            for (int i = 0; i < 64; i++) {
                if (((array[offset] & (1L << (value & BIT_INDEX_MASK))) != 0)) {
                    if (containsSpecialRow(items, referenceItem, data[index - 1])) { 
                        return true; 
                    }
                }
                value ++;
                index ++;
            }
        }
        return false;
    }

    /**
     * Searches for the special row
     * @param items
     * @param referenceItem
     * @param row
     * @return
     */
    private boolean containsSpecialRow(SUDA2Item[] items, SUDA2Item referenceItem, int[] row) {
        for (SUDA2Item item : items) {
            if (!item.isContained(row)) {
                return false;
            }
        }
        if (referenceItem.isContained(row)) {
            return false;
        }
        return true;
    }
    
    @Override
    public SUDA2IntSet intersectWith(SUDA2IntSet other) {

        // No intersection
        if (this.max < other.min() || other.max() < this.min) {
            return new SUDA2IntSetSmall();
        }

        // Intersect two bitsets
        if (other.isBitSet()) {
            
            // Convert and prepare
            SUDA2IntSetBits _other = (SUDA2IntSetBits)other;
            int min = Math.max(this.min, _other.min);
            int max = Math.min(this.max, _other.max);
            
            // Result
            SUDA2IntSetBits result = new SUDA2IntSetBits(min, max);
            result.min = min; // Just an approximation
            result.max = max; // Just an approximation
            
            // Offsets
            int index = offset / 64;
            int _index = _other.offset / 64;
            int resultIndex = result.offset / 64;
            
            // Shift to start at index describing the same offset
            int maxIndex = Math.max(index, Math.max(_index, resultIndex));
            index = maxIndex - index;
            _index = maxIndex - _index;
            resultIndex = maxIndex - resultIndex;
            
            // Pairwise logical and
            while (resultIndex < result.array.length && index < array.length && _index < _other.array.length) {
                long element = array[index++] & _other.array[_index++];
                result.size += Long.bitCount(element);
                result.array[resultIndex++] = element;
            }

            // Return the result
            return result;
            
        // Let the other set probe this set
        } else {
            
            return other.intersectWith(this);
        }
    }

    @Override
    public boolean isSupportRowPresent(SUDA2IntSet other) {

        // No intersection
        if (this.max < other.min() || other.max() < this.min) {
            return false;
        }

        // Intersect two bitsets
        if (other.isBitSet()) {
            
            // Prepare
            SUDA2IntSetBits _other = (SUDA2IntSetBits)other;

            // Offsets
            int index = offset / 64;
            int _index = _other.offset / 64;
            
            // Shift to start at index describing the same offset
            int maxIndex = Math.max(index, _index);
            index = maxIndex - index;
            _index = maxIndex - _index;
                   
            // And count identical bits
            int count = 0;
            while (count <= 1 && index < array.length && _index < _other.array.length) {
                count += Long.bitCount(array[index++] & _other.array[_index++]);
            }
            
            // Return if we found exactly one such row
            return count == 1;

            // Let the other set probe this set
        } else {
            return other.isSupportRowPresent(this);
        }
    }

    @Override
    public int max() {
        return max;
    }

    @Override
    public int min() {
        return min;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean isBitSet() {
        return true;
    }

    @Override
    public String toString() {
        return "Size=" + size + " offset=" + offset + " array=" + Arrays.toString(array);
    }
    
}
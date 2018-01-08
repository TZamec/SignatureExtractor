/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */



/**
 *
 * @author TOm
 */
import java.util.ArrayList;

public class FixedSizeList<T> extends ArrayList<T> {

    public FixedSizeList(int capacity) {
        super(capacity);
        for (int i = 0; i < capacity; i++) {
            super.add(null);
        }
    }

    public FixedSizeList(T[] initialElements) {
        super(initialElements.length);
        for (T loopElement : initialElements) {
            super.add(loopElement);
        }
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Elements may not be cleared from a fixed size List.");
    }

    @Override
    public boolean add(T o) {
        throw new UnsupportedOperationException("Elements may not be added to a fixed size List, use set() instead.");
    }

    @Override
    public void add(int index, T element) {
        throw new UnsupportedOperationException("Elements may not be added to a fixed size List, use set() instead.");
    }

    @Override
    public T remove(int index) {
        throw new UnsupportedOperationException("Elements may not be removed from a fixed size List.");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Elements may not be removed from a fixed size List.");
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("Elements may not be removed from a fixed size List.");
    }
}

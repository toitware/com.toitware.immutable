// Copyright (C) 2019 Toitware ApS. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.toitware.immutable;

import com.toitware.immutable.ImmutableCollection;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.Iterator;
import java.util.ListIterator;

/** A concrete implementation of ImmutableCollection.
 *  @see ImmutableCollection
 */
public class ImmutableArray<E> extends ImmutableCollection<E> {
  protected final long size;

  // A tree that is 199 large has three full trees of 64 each in the leftmost
  // _powers entry.  The next _powers entry has null, and the last points at an
  // array with 7 elements.  If we add one element then the medium _powers
  // entry gets a pointer to a 1-element array that points to the 8-entry leaf.
  private Object _powers[];

  /** Create an empty ImmutableArray. */
  public ImmutableArray() {
    size = 0;
  }

  /** Make an ImmutableArray that is a shallow copy of an array.
   *  @param array The array to be copied
   */
  public ImmutableArray(E array[]) {
    size = array.length;
    _powers = _createBacking(Arrays.asList(array));
  }

  /** Make an ImmutableArray that is a shallow copy of another collection.
   *  @param collection The collection to be copied
   */
  public ImmutableArray(Collection<? extends E> collection) {
    if (collection instanceof ImmutableArray) {
      ImmutableArray other = (ImmutableArray)collection;
      size = other.size;
      _powers = other._powers;
    } else {
      size = collection.size();
      _powers = _createBacking(collection);
    }
  }

  public int size() {
    if (size > Integer.MAX_VALUE) return Integer.MAX_VALUE;
    return (int)size;
  }

  public long longSize() {
    return size;
  }

  public ListIterator<E> listIterator() {
    return new ImmutableArrayListIterator<E>(size, _powers);
  }

  public ListIterator<E> listIterator(int index) {
    return new ImmutableArrayListIterator<E>(size, _powers, index, 0);
  }

  public ListIterator<E> listIterator(long index) {
    return new ImmutableArrayListIterator<E>(size, _powers, index, 0);
  }

  protected ListIterator<E> listIterator(long index, long leftMost) {
    return new ImmutableArrayListIterator<E>(size, _powers, index, leftMost);
  }

  public ImmutableArrayIterator<E> iterator() {
    return new ImmutableArrayIterator<E>(size, _powers);
  }

  protected ImmutableArrayIterator<E> iterator(long startAt) {
    return new ImmutableArrayIterator<E>(size, _powers, startAt);
  }

  private ImmutableArray(long len, Object[] pow) {
    size = len;
    _powers = pow;
  }

  public E get(int index) {
    return get((long)index);
  }

  public E get(long index) {
    long len = size;
    if (index < 0 || index >= len) throw new IndexOutOfBoundsException();
    int length_tribbles = _powers.length - 1;
    while (true) {
      long top_index_digit = index >>> (3 * length_tribbles);
      long top_length_digit = len >>> (3 * length_tribbles);
      if (top_index_digit < top_length_digit) {
        return _get(length_tribbles, index, _powers[length_tribbles]);
      }
      index -= top_index_digit << (3 * length_tribbles);
      len -= top_index_digit << (3 * length_tribbles);
      length_tribbles--;
    }
  }

  private @SuppressWarnings("unchecked") E _get(int tribbles, long index, Object obj) {
    Object array[] = (Object[])obj;
    long idx = index >>> (tribbles * 3);
    if (tribbles == 0) return (E)array[(int)idx];
    return _get(tribbles - 1, index - (idx << (tribbles * 3)), array[(int)idx]);
  }

  static private Object[] _copyBut(Object old[], long index, Object value) {
    Object[] new_array = old == null ? new Object[1] : new Object[old.length];
    for (int i = 0; i < new_array.length; i++) {
      new_array[i] = (i == index) ? value : old[i];
    }
    return new_array;
  }

  // Makes a copy of an array, but at the given index the value is substituted,
  // and to the left of that position, everything is nulled.
  static private Object[] _nullToTheLeft(Object old[], long index, Object value) {
    Object[] new_array = old == null ? new Object[1] : new Object[old.length];
    for (int i = 0; i < new_array.length; i++) {
      new_array[i] =
          (i < index) ?
          null :
          (i == index) ?
              value :
              old[i];
    }
    return new_array;
  }

  public long indexOf(Object needle) {
    long index = 0;
    for (Object obj : this) {
      if ((needle == null && obj == null)|| needle.equals(obj)) return index;
      index++;
    }
    return -1;
  }

  // TODO: Has complexity O(n log n), fix to be O(n).
  protected long indexOf(Object needle, long starting) {
    if (starting > size) throw new IndexOutOfBoundsException();
    for (long index = starting; index < size; index++) {
      Object obj = get(index);
      if ((needle == null && obj == null) || needle.equals(obj)) return index;
    }
    return -1;
  }

  public long lastIndexOf(Object needle) {
    for (long index = size - 1; index >= 0; index--) {
      Object obj = get(index);
      if ((needle == null && obj == null) || needle.equals(obj)) return index;
    }
    return -1;
  }

  protected long lastIndexOf(Object needle, long stopAt) {
    if (stopAt < 0) throw new IndexOutOfBoundsException();
    for (long index = size - 1; index >= stopAt; index--) {
      Object obj = get(index);
      if ((needle == null && obj == null) || needle.equals(obj)) return index;
    }
    return -1;
  }

  public Object clone() {
    return this;
  }

  // Makes a copy of an array, but appends the given values.
  static private Object[] _copyAppend(Object old[], int count, Object value1, Object value2) {
    int len = old == null ? 0 : old.length;
    Object[] new_array = new Object[len + count];
    for (int i = 0; i < len; i++) {
      new_array[i] = old[i];
    }
    new_array[len] = value1;
    if (count == 2) new_array[len + 1] = value2;
    return new_array;
  }

  public ImmutableArray<E> atPut(long index, E value) {
    long len = size;
    if (index < 0 || index >= len) throw new IndexOutOfBoundsException();
    int length_tribbles = _powers.length - 1;
    while (true) {
      long top_index_digit = index >>> (3 * length_tribbles);
      long top_length_digit = len >>> (3 * length_tribbles);
      if (top_index_digit < top_length_digit) {
        return new ImmutableArray<E>(
            size,
            _copyBut(
                _powers,
                length_tribbles,
                _atPut(length_tribbles, value, index, (Object[])_powers[length_tribbles])));
      }
      index -= top_index_digit << (3 * length_tribbles);
      len -= top_index_digit << (3 * length_tribbles);
      length_tribbles--;
    }
  }

  private Object[] _atPut(int tribbles, Object value, long index, Object array[]) {
    long idx = index >>> (tribbles * 3);
    return _copyBut(
        array,
        idx,
        tribbles == 0 ?
            value :
            _atPut(tribbles - 1, value, index - (idx << (tribbles * 3)), (Object[])array[(int)idx]));
  }

  // Null out all entries to the left of the given index. This is used by
  // Immutable Deque to ensure that items to the left of the offset are not
  // retained for GC purposes.
  // Time taken is O(log size).
  protected ImmutableArray<E> trimLeft(long index) {
    long len = size;
    if (index < 0 || index >= len) throw new IndexOutOfBoundsException();
    int length_tribbles = _powers.length - 1;
    while (true) {
      long top_index_digit = index >>> (3 * length_tribbles);
      long top_length_digit = len >>> (3 * length_tribbles);
      if (top_index_digit < top_length_digit) {
        Object new_powers[] = new Object[length_tribbles + 1];
        for (int i = 0; i < length_tribbles; i++) new_powers[i] = _powers[i];
        new_powers[length_tribbles] = _trimLeft(
            length_tribbles,
            index,
            (Object[])_powers[length_tribbles]);
        return new ImmutableArray<E>(len, new_powers);
      }
      index -= top_index_digit << (3 * length_tribbles);
      len -= top_index_digit << (3 * length_tribbles);
      length_tribbles--;
    }
  }

  private Object[] _trimLeft(int tribbles, long index, Object array[]) {
    long idx = index >>> (tribbles * 3);
    return _nullToTheLeft(
        array,
        idx,
        tribbles == 0 ?
            array[(int)idx] :
            _trimLeft(tribbles - 1, index - (idx << (tribbles * 3)), (Object[])array[(int)idx]));
  }

  public ImmutableArray<E> push(E value) {
    return _push(1, value, null);
  }

  public ImmutableArray<E> push(E value1, E value2) {
    if ((size & 1) == 0) {
      return _push(2, value1, value2);
    }
    return push(value1).push(value2);
  }

  private static Object[] _createBacking(Collection collection) {
    int index = 0;
    int size = collection.size();
    Object batch[] = null;
    int batch_index = 0;
    Object powers[] = null;
    for (Object o : collection) {
      if (batch == null) batch = new Object[size - index < 8 ? size - index : 8];
      batch[batch_index++] = o;
      if (batch_index == 8) {
        powers = _pushHelper(powers, index, 8, batch, true);
        index += 8;
        batch = null;
        batch_index = 0;
      }
    }
    if (batch_index != 0) {
      if (powers == null) powers = new Object[1];
      powers[0] = batch;
    }
    return powers;
  }

  public ImmutableArray<E> pushAll(E array[]) {
    return pushAll(Arrays.asList(array));
  }

  public @SuppressWarnings("unchecked") ImmutableArray<E> pushAll(Collection<? extends E> collection) {
    if (collection.isEmpty()) return this;
    ImmutableArray<E> current = this;
    boolean can_reuse = false;
    Object batch[] = null;
    int batch_index = 0;
    int index = 0;
    // Pushes elements 8 at a time for better efficiency.
    for (E o : collection) {
      if (batch == null) {
        if ((current.size & 7) == 0) {
          batch = new Object[8];
          batch_index = 0;
        } else {
          current = current.push(o);
          can_reuse = true;
          continue;
        }
      }
      batch[batch_index++] = o;
      if (batch_index == 8) {
        current = new ImmutableArray<E>(current.size + 8, _pushHelper(current._powers, current.size, 8, batch, can_reuse));
        can_reuse = true;
        batch = new Object[8];
        batch_index = 0;
      }
    }
    for (int i = 0; i < batch_index; i++) {
      current = current.push((E)batch[i]);
    }
    return current;
  }

  private ImmutableArray<E> _push(int count, Object value1, Object value2) {
    if (((size + count) & 7) != 0) {
      return new ImmutableArray<E>(
          size + count,
          _copyBut(
              _powers,
              0,
              _copyAppend(_powers == null ? null : (Object[])_powers[0], count, value1, value2)));
    }
    Object value = _copyAppend((Object[])_powers[0], count, value1, value2);
    return new ImmutableArray<E>(size + count, _pushHelper(_powers, size, count, value, false));
  }

  static private Object[] _pushHelper(Object old_powers[], long size, int count, Object value, boolean can_reuse) {
    assert (size & (count - 1)) == 0;
    Object new_powers[];
    if (_isPowerOf8(size + count)) {
      // Need to grow _powers array.
      if (old_powers == null) {
        new_powers = new Object[2];
      } else {
        new_powers = _copyAppend(old_powers, 1, null, null);
        new_powers[0] = null;
      }
    } else {
      // Don't need to grow _powers array.
      if (can_reuse) {
        new_powers = old_powers;
        assert new_powers[0] == null;
      } else {
        new_powers = _copyBut(old_powers, 0, null);
      }
    }
    for (int i = 1; i < new_powers.length; i++) {
      Object new_value[] = _copyAppend((Object[])new_powers[i], 1, value, null);
      new_powers[i] = new_value;
      if (new_value.length != 8) break;
      value = new_powers[i];
      new_powers[i] = null;
    }
    return new_powers;
  }

  public ImmutableCollection<E> shift() {
    return subList(1);
  }

  public @SuppressWarnings("unchecked") ImmutableCollection<E> subList(long from) {
    if (from == 0) return this;
    if (from == size) return new ImmutableDeque<E>(0, new ImmutableArray());
    return new ImmutableDeque<E>(from, this);
  }

  public @SuppressWarnings("unchecked") ImmutableCollection<E> subList(long from, long to) {
    if (from == 0) return trim(size - to);
    if (from == size) return new ImmutableDeque<E>(0, new ImmutableArray());
    if (to == size) return new ImmutableDeque<E>(from, this);
    return new ImmutableDeque<E>(from, trim(size - to));
  }

  public ImmutableArray<E> trim() {
    return trim(1);
  }

  public ImmutableArray<E> trim(long by) {
    if (by < 0 || by > size) throw new IndexOutOfBoundsException();
    if (by == 0) return this;
    if (by == size) return new ImmutableArray<E>();
    long new_size = size - by;
    int d = _powers.length;
    Object new_powers[] = null;
    Object[] borrow = null;
    // One digit at a time, from most significant to least significant.
    for (int i = d - 1; i >= 0; i--) {
      int old_digit = (int)((size >> (i * 3)) & 7);
      int new_digit = (int)((new_size >> (i * 3)) & 7);
      Object input[] = (Object[])_powers[i];
      int borrow_len = borrow == null ? 0 : borrow.length;
      int input_len = borrow_len + (input == null ? 0 : input.length);
      Object here[];
      if (borrow_len == 0 && new_digit == old_digit) {
        here = input;
      } else {
        here = new_digit == 0 ? null : new Object[new_digit];
        for (int j = 0; true; j++) {
          Object obj = j < borrow_len ? borrow[j] :
                       j < input_len ? input[j - borrow_len] :
                       null;
          if (j == new_digit) {
            if (i != 0) borrow = (Object[])obj;
            break;
          }
          here[j] = obj;
        }
      }
      if (here != null) {
        if (new_powers == null) {
          new_powers = new Object[i + 1];
        }
        new_powers[i] = here;
      }
    }
    return new ImmutableArray<E>(new_size, new_powers);
  }

  static private boolean _isPowerOf8(long i) {
    // Quick check for power of 2.
    if ((i & (i - 1)) != 0) return false;
    // If it's a power of two we have to check for a power of 8.
    long test = 8;
    while (test > 0 && test <= i) {
      if (test == i) return true;
      test <<= 3;  // Can wrap to negative at 2**63 which is -2**63.
    }
    return false;
  }

  protected class ImmutableArrayIterator<E> implements Iterator<E> {
    protected long _remaining;
    protected long _index;
    protected Object _stack[][];
    protected Object _powers[];
    protected int _powers_posn;

    public ImmutableArrayIterator(long length, Object[] powers) {
      _powers = powers;
      _remaining = length;
      _index = 0;
    }

    public ImmutableArrayIterator(long length, Object[] powers, long starting) {
      _powers = powers;
      _remaining = length - starting;
      if (_remaining < 0 || starting < 0 || starting > length) {
        throw new IndexOutOfBoundsException();
      }
      if (starting == length && length != 0) {
        // This is a special annoying case where the current position is
        // one beyond the end.  In this case the _index is set to the
        // correct position, but the arraylets in the _stack are set to
        // to one less (the last real position).
        _index = length - 1;
        _init();  // Eagerly create and initialize.
      }
      _index = starting;
    }

    protected void _init() {
      long starting = _index;
      if (_powers != null) {
        if (starting == 0) {
          _powers_posn = _powers.length - 1;
        } else {
          for (_powers_posn = _powers.length - 1; _powers_posn >= 0; _powers_posn--) {
            if (_powers[_powers_posn] == null) continue;
            long at_this_level = ((Object[])(_powers[_powers_posn])).length << (3 * _powers_posn);
            if (starting < at_this_level) break;
            starting -= at_this_level;
          }
        }
        _stack = new Object[_powers.length][];
        _populate_stack();
      }
    }

    public boolean hasNext() {
      return _remaining > 0;
    }

    public @SuppressWarnings("unchecked") void forEachRemaining(Consumer<? super E> action) {
      if (_remaining != 0) {
        ImmutableArray._forEachHelper(_powers, _index, action);
      }
      _remaining = 0;
    }

    // Get next element in collection and advance by one.
    public @SuppressWarnings("unchecked") E next() {
      // Short version of next, designed to be inlined.
      assert hasNext();
      if (_stack == null) _init();
      int arraylet_posn = (int)(_index & 7);
      Object bottom_arraylet[] = _stack[_powers_posn];
      E result = (E)bottom_arraylet[arraylet_posn];
      _remaining--;
      _index++;
      arraylet_posn++;
      if (arraylet_posn == bottom_arraylet.length) {
        _next();
      }
      return result;
    }

    // Out of line version of _next for when we need to move to the next leaf.
    private @SuppressWarnings("unchecked") void _next() {
      // The easy way to implement this is just to use _index and something
      // like _get(), but this takes logn, giving an nlogn iteration over the
      // whole collection.  Instead we maintain a stack of arraylets at
      // different levels of the tree, which gives O(n) iteration.  The nth
      // position in the _powers array has a tree under it that is n+1 deep,
      // which also sets the size of the explicit stack we need.
      int shift = 0;
      for (int idx = _powers_posn; idx >= 0; idx--) {
        Object[] arraylet = (Object[])(_stack[idx]);
        int arraylet_posn = (int)((_index >> shift) & 7);
        if (arraylet_posn != 0 && arraylet_posn != arraylet.length) {
          _populate_stack(shift, idx, arraylet);
          return;
        }
        shift += 3;
      }
      // Need to move to next position in _powers array.
      int powers_posn = _powers_posn;
      do {
        powers_posn--;
        if (powers_posn < 0) return;  // Hit the end.
      } while (_powers[powers_posn] == null);
      _powers_posn = powers_posn;
      _populate_stack();
    }

    protected void _populate_stack() {
      Object top[] = (Object[])_powers[_powers_posn];
      _stack[0] = top;
      _populate_stack(_powers_posn * 3, 0, top);
    }

    protected void _populate_stack(int shift, int idx, Object arraylet[]) {
      while (idx < _powers_posn) {
        int arraylet_posn = (int)((_index >> shift) & 7);
        idx++;
        arraylet = (Object[])arraylet[arraylet_posn];
        _stack[idx] = arraylet;
        shift -= 3;
      }
    }
  }

  protected class ImmutableArrayListIterator<E> extends ImmutableArrayIterator<E> implements ListIterator<E> {
    private final long _start;

    public ImmutableArrayListIterator(long length, Object[] powers) {
      super(length, powers);
      _start = 0;
    }

    public ImmutableArrayListIterator(long length, Object[] powers, long starting, long leftmost_limit) {
      super(length, powers, starting);
      if (starting < leftmost_limit) throw new IndexOutOfBoundsException();
      _start = leftmost_limit;
    }

    // Get previous element in collection and go back by one.
    public @SuppressWarnings("unchecked") E previous() {
      // Short version of previous, designed to be inlined.
      if (_stack == null) _init();
      assert hasPrevious();
      int arraylet_posn = (int)(_index & 7);
      _index--;
      _remaining++;
      if (arraylet_posn == 0) {
        _previous();
        arraylet_posn = 8;
      }
      arraylet_posn--;
      return (E)_stack[_powers_posn][arraylet_posn];
    }

    // Out of line version of _previous for when we need to move to the previous
    // leaf.
    private @SuppressWarnings("unchecked") void _previous() {
      // The easy way to implement this is just to use _index and something
      // like _get(), but this takes logn, giving an nlogn iteration over the
      // whole collection.  Instead we maintain a stack of arraylets at
      // different levels of the tree, which gives O(n) iteration.  The nth
      // position in the _powers array has a tree under it that is n+1 deep,
      // which also sets the size of the explicit stack we need.
      int shift = 0;
      for (int idx = _powers_posn; idx >= 0; idx--) {
        Object[] arraylet = (Object[])(_stack[idx]);
        int arraylet_posn = (int)((_index >> shift) & 7);
        if (arraylet_posn != 7) {
          _populate_stack(shift, idx, arraylet);
          return;
        }
        shift += 3;
      }
      // Need to move to previous position in _powers array.
      int powers_posn = _powers_posn;
      do {
        powers_posn++;
        // Can't go earlier than the 0th element.
        assert powers_posn != _powers.length;
      } while (_powers[powers_posn] == null);
      _powers_posn = powers_posn;
      _populate_stack();
    }

    public int nextIndex() {
      return (int)(_index);
    }

    public int previousIndex() {
      return (int)(_index - 1);
    }

    public boolean hasPrevious() {
      return _index != _start;
    }

    public void add(E element) {
      throw new UnsupportedOperationException();
    }

    public void set(E element) {
      throw new UnsupportedOperationException();
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  public void forEach(Consumer<? super E> action) {
    forEach(0, action);
  }

  protected void forEach(long startAt, Consumer<? super E> action) {
    if (startAt < 0 || startAt > size) throw new IndexOutOfBoundsException();
    _forEachHelper(_powers, startAt, action);
  }

  private static void _forEachHelper(Object[] powers, long startAt, Consumer action) {
    if (powers == null) return;
    long index = 0;
    for (int p = powers.length - 1; p >= 0; p--) {
      if (powers[p] != null) {
        Object[] power = (Object[])powers[p];
        _forEachHelper(power, p, startAt, index, action);
        index += power.length << (3 * p);
      }
    }
  }

  private static @SuppressWarnings("unchecked") void _forEachHelper(Object[] array, int depth, long startAt, long index, Consumer action) {
    long end = index + array.length << (3 * depth);
    if (end < startAt) return;
    if (depth == 0) {
      int start = startAt <= index ? 0 : (int)(startAt & 7);
      for (int i = start; i < array.length; i++) {
        action.accept(array[i]);
      }
    } else {
      for (int i = 0; i < array.length; i++) {
        long index_after = index + (1 << (3 * depth));
        if (startAt < index_after) {
          _forEachHelper((Object[])array[i], depth - 1, startAt, index, action);
        }
        index = index_after;
      }
    }
  }
}

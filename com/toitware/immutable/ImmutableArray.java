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

// An immutable array with O(log size) access to any element.  A new array that
// differs at one position from this array can be made in O(log size) time.
// The binary tree that backs the immutable array is always kept as shallow as
// possible on the right hand side though its max depth is still bounded by the
// log of the size, rounded up.  This means that a new array, one longer than
// the current one can be made in O(1) time using the push() method.  (This is
// almost true - the push() method is currently O(log size), but you would have
// to have a 128 bit implementation of the Java Language to notice.  You
// don't.)
public class ImmutableArray<E> extends ImmutableCollection<E> {
  // Unlike the size() method this is not limited to the range of an int.
  public final long size;

  // A tree that is 199 large has three full trees of 64 each in the leftmost
  // _powers entry.  The next _powers entry has null, and the last
  // points at an array with 7 elements.  If we add one element then the
  // medium _powers entry gets a pointer to a 1-element array that points to
  // the 8-entry leaf.
  private Object _powers[];

  // Create an empty ImmutableArray.
  public ImmutableArray() {
    size = 0;
  }

  // Make an ImmutableArray that is a shallow copy of an array.
  public ImmutableArray(E array[]) {
    size = array.length;
    _powers = _createBacking(Arrays.asList(array));
  }

  // Make an ImmutableArray that is a shallow copy of another collection.
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

  // To conform to the AbstractCollection interface this returns an int, but
  // see also the property 'size' and the method longSize().
  public int size() {
    if (size > Integer.MAX_VALUE) return Integer.MAX_VALUE;
    return (int)size;
  }

  public long longSize() {
    return size;
  }

  public ImmutableArrayIterator<E> iterator() {
    return new ImmutableArrayIterator<E>(size, _powers);
  }

  public ImmutableArrayIterator<E> iterator(long startAt) {
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

  // Makes a copy of an array, but at the given index the value is substituted.
  static private Object[] _copyBut(Object old[], long index, Object value) {
    Object[] new_array = old == null ? new Object[1] : new Object[old.length];
    for (int i = 0; i < new_array.length; i++) {
      new_array[i] = (i == index) ? value : old[i];
    }
    return new_array;
  }

  // Returns the first index whose element is equal to the needle, using
  // equals().  Returns -1 if the needle is not found.
  // Unlike the method of the same name on ArrayList, this one returns long.
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

  // Returns the last index whose element is equal to the needle, using
  // equals().  Returns -1 if the needle is not found.
  // TODO: Has complexity O(n log n), fix to be O(n).
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

  // Since the collection is immutable there is no reason to actually build a
  // clone.  Returns itself.
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

  // Unlike set() on ArrayList, this returns a new immutable collection that differs
  // from the original one at position index, having value at that position instead.
  // Time taken is O(log size).
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

  // Unlike add() on ArrayList, this returns a new immutable collection that differs
  // from the original one only by having an extra element.
  // Time taken is O(1), almost, but it has to allocate and initialize an object that
  // has log2(size)/3 words.
  public ImmutableArray<E> push(E value) {
    return _push(1, value, null);
  }

  // Push two values at once.  This is more efficient than two calls to push(E
  // value) if the array has an even size.
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

  // Return a new ImmutableArray that is the current array with all elements of
  // an array added to the end.  May return itself if the provided array is
  // empty.
  public ImmutableArray<E> pushAll(E array[]) {
    return pushAll(Arrays.asList(array));
  }

  // Return a new ImmutableArray that is the current array with all elements of
  // a given collection added to the end.  May return itself if the provided
  // collection is empty.
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
    assert((size & (count - 1)) == 0);
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
        assert(new_powers[0] == null);
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
    private long _remaining;
    private Object _stack[][];
    private int _positions[];
    private Object _powers[];
    private int _powers_posn;

    public ImmutableArrayIterator(long length, Object[] powers) {
      _powers = powers;
      _remaining = length;
      _init(0);
    }

    public ImmutableArrayIterator(long length, Object[] powers, long starting) {
      _powers = powers;
      _remaining = length - starting;
      _init(starting);
    }

    private void _init(long starting) {
      if (_powers != null) {
        for (_powers_posn = _powers.length - 1; _powers_posn >= 0; _powers_posn--) {
          if (_powers[_powers_posn] == null) continue;
          long at_this_level = ((Object[])(_powers[_powers_posn])).length << (3 * _powers_posn);
          if (starting < at_this_level) break;
          starting -= at_this_level;
        }
        _stack = new Object[_powers.length][];
        _positions = new int[_powers.length];
        _populate_stack(starting);
      }
    }

    public boolean hasNext() {
      return _remaining > 0;
    }

    // Get next element in collection and advance by one.
    public @SuppressWarnings("unchecked") E next() {
      // Short version of next, designed to be inlined.
      assert hasNext();
      if (_positions[_powers_posn] != _stack[_powers_posn].length - 1) {
        _remaining--;
        return (E)_stack[_powers_posn][++_positions[_powers_posn]];
      }
      return _next();
    }

    // Out of line version of _next for when we need to move to the next leaf.
    private @SuppressWarnings("unchecked") E _next() {
      // The easy way to implement this is just to use _remaining and _at(), but this
      // takes logn, giving an nlogn iteration over the whole collection.
      // Instead we maintain a stack of positions at different levels of the
      // tree, which gives O(n) iteration.
      // The nth position in the _powers array has a tree under it that is n+1
      // deep, which also sets the size of the explicit stack we need.
      int idx = _powers_posn;
      while (idx >= 0 && _positions[idx] == _stack[idx].length - 1) idx--;
      if (idx != -1) {
        _positions[idx]++;
        for (; idx < _powers_posn; idx++) {
          _stack[idx + 1] = (Object[])_stack[idx][_positions[idx]];
          _positions[idx + 1] = 0;
        }
        _remaining--;
        return (E)_stack[idx][_positions[idx]];
      }
      // Need to move to the next element in the powers array.
      do {
        _powers_posn--;
      } while (_powers[_powers_posn] == null);
      _populate_stack(0);
      return next();
    }

    private void _populate_stack(long from) {
      _stack[0] = (Object[])_powers[_powers_posn];
      int shift = _powers_posn * 3;
      for (int idx = 0; idx < _powers_posn; idx++) {
        int progress = (int)((from >> shift) & 7);
        shift -= 3;
        _positions[idx] = progress;
        _stack[idx + 1] = (Object[])_stack[idx][_positions[idx]];
      }
      _positions[_powers_posn] = (int)((from & 7) - 1);
    }
  }

  public void forEach(Consumer<? super E> action) {
    forEach(0, action);
  }

  public void forEach(long startAt, Consumer<? super E> action) {
    if (startAt < 0 || startAt > size) throw new IndexOutOfBoundsException();
    if (_powers == null) return;
    long index = 0;
    for (int p = _powers.length - 1; p >= 0; p--) {
      if (_powers[p] != null) {
        Object[] power = (Object[])_powers[p];
        _forEachHelper(power, p, startAt, index, action);
        index += power.length << (3 * p);
      }
    }
  }

  private @SuppressWarnings("unchecked") void _forEachHelper(Object[] array, int depth, long startAt, long index, Consumer<? super E> action) {
    long end = index + array.length << (3 * depth);
    if (end < startAt) return;
    if (depth == 0) {
      int start = startAt <= index ? 0 : (int)(startAt & 7);
      for (int i = start; i < array.length; i++) {
        action.accept((E)array[i]);
      }
    } else {
      for (int i = 0; i < array.length; i++) {
        _forEachHelper((Object[])array[i], depth - 1, startAt, index, action);
        index += 1 << (3 * depth);
      }
    }
  }
}

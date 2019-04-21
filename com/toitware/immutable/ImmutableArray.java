// Copyright (C) 2019 Toitware ApS. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.toitware.immutable;

import java.util.AbstractCollection;

// An immutable array with O(log size) access to any element.  A new array that
// differs at one position from this array can be made in O(log size) time.
// A new array, one longer than the current one can be made in O(1) time using the
// push() method.  (This is almost true - the push() method is currently O(log size),
// but you would have to have a 128 bit implementation of the Java Language to notice
// You don't.)
public class ImmutableArray<E> extends AbstractCollection<E> implements Iterable<E> {
  // Unlike the size() method this is not limited to the range of an int.
  public final long size;
  private Object _powers[];

  // Create an empty ImmutableArray.
  public ImmutableArray() {
    size = 0;
  }

  // To conform to the AbstractCollection interface this returns an int, but
  // see also the property 'size'.
  public int size() {
    if (size > Integer.MAX_VALUE) return Integer.MAX_VALUE;
    return (int)size;
  }

  public ImmutableArrayIterator<E> iterator() {
    return new ImmutableArrayIterator<E>(size, _powers);
  }

  private ImmutableArray(long len, Object[] pow) {
    size = len;
    _powers = pow;
  }

  // A tree that is 199 large has three full trees of 64 each in the leftmost
  // _powers entry.  The next _powers entry has null, and the last
  // points at an array with 7 elements.  If we add one element then the
  // medium _powers entry gets a pointer to a 1-element array that points to
  // the 8-entry leaf.

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
      if (needle == null || needle.equals(obj)) return index;
      index++;
    }
    return -1;
  }

  // Returns the last index whose element is equal to the needle, using
  // equals().  Unlike the method of the same name on ArrayList, this one
  // returns long.  Returns -1 if the needle is not found.
  // TODO: Currently takes O(nlogn).
  public long lastIndexOf(Object needle) {
    for (long index = size - 1; index >= 0; index--) {
      Object obj = get(index);
      if (needle == null || needle.equals(obj)) return index;
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
    Object new_powers[];
    if (_isPowerOf8(size + count)) {
      // Need to grow _powers array.
      new_powers = _copyAppend(_powers, 1, null, null);
      new_powers[0] = null;
    } else {
      // Don't need to grow _powers array.
      new_powers = _copyBut(_powers, 0, null);
    }
    for (int i = 1; i < new_powers.length; i++) {
      Object new_value[] = _copyAppend((Object[])new_powers[i], 1, value, null);
      new_powers[i] = new_value;
      if (new_value.length != 8) break;
      value = new_powers[i];
      new_powers[i] = null;
    }
    return new ImmutableArray<E>(size + count, new_powers);
  }

  private boolean _isPowerOf8(long i) {
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
}

// Copyright (C) 2019 Toitware ApS. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.toitware.immutable;

import com.toitware.immutable.ImmutableCollection;
import com.toitware.immutable.RebuildIterator;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.function.Consumer;

/** A concrete implementation of ImmutableCollection.
 *  @see ImmutableCollection
 */
public class ImmutableArray<E> extends ImmutableCollection<E> {
  protected final long size;

  // Branching factor.
  static final int M = 16;
  static final int MASK = 15;
  static final int SHIFT = 4;
  static final int SHIFTSHIFT = 2;  // Only used if SHIFT is a power of two.

  // A tree that is 199 large has three full trees of 64 each in the leftmost
  // _powers entry.  The next _powers entry points at a zero length array, and
  // the last points at an array with M - 1 elements.  If we add one element
  // then the medium _powers entry gets a pointer to a 1-element array that
  // points to the M-entry leaf.
  private Object _powers[];  // Should really have type Object[][].
  private Object _powers0[];

  static private final Object _zero[] = new Object[0];

  /** Create an empty ImmutableArray. */
  public ImmutableArray() {
    size = 0;
    _powers = _zero;
    _powers0 = _zero;
  }

  /** Make an ImmutableArray that is a shallow copy of an array.
   *  @param array The array to be copied
   */
  public ImmutableArray(E array[]) {
    size = array.length;
    if (size == 0) {
      _powers = _zero;
      _powers0 = _zero;
    } else {
      _powers = _pushHelper(_zero, _zero, 0, Arrays.asList(array), size);
      _powers0 = (Object[])_powers[0];
      _powers[0] = null;
      if (_powers.length == 1) _powers = _zero;
    }
  }

  /** Make an ImmutableArray that is a shallow copy of another collection.
   *  @param collection The collection to be copied
   */
  public ImmutableArray(Collection<? extends E> collection) {
    if (collection instanceof ImmutableArray) {
      ImmutableArray other = (ImmutableArray)collection;
      size = other.size;
      _powers = other._powers;
      _powers0 = other._powers0;
    } else {
      size = collection.size();
      if (size == 0) {
        _powers = _zero;
        _powers0 = _zero;
        return;
      }
      _powers = _pushHelper(_zero, _zero, 0, collection, size);
      _powers0 = (Object[])_powers[0];
      _powers[0] = null;
      if (_powers.length == 1) _powers = _zero;
    }
  }

  public int size() {
    return _longTruncator(size);
  }

  public long longSize() {
    return size;
  }

  public ListIterator<E> listIterator() {
    return new ImmutableArrayListIterator<E>(size, _powers, _powers0);
  }

  public ListIterator<E> listIterator(int index) {
    return new ImmutableArrayListIterator<E>(size, _powers, _powers0, index, 0);
  }

  public ListIterator<E> listIterator(long index) {
    return new ImmutableArrayListIterator<E>(size, _powers, _powers0, index, 0);
  }

  protected ListIterator<E> listIterator(long index, long leftMost) {
    return new ImmutableArrayListIterator<E>(size, _powers, _powers0, index, leftMost);
  }

  public ImmutableArrayIterator<E> iterator() {
    return new ImmutableArrayIterator<E>(size, _powers, _powers0);
  }

  protected ImmutableArrayIterator<E> iterator(long startAt) {
    return new ImmutableArrayIterator<E>(size, _powers, _powers0, startAt);
  }

  private ImmutableArray(long len, Object[] pow, Object[] pow0) {
    size = len;
    _powers = pow;
    _powers0 = pow0;
  }

  public E get(int index) {
    return get((long)index);
  }

  private static int _powerPosn(long index) {
    if (M == 8) {
      // Multiplying by 43 and shifting down by 7 is just a way to divide by 3.
      return ((63 - Long.numberOfLeadingZeros(index)) * 43) >> 7;
    } else {
      assert SHIFT == 1 << SHIFTSHIFT;
      return (63 - Long.numberOfLeadingZeros(index)) >> SHIFTSHIFT;
    }
  }

  @SuppressWarnings("unchecked")
  public E get(long index) {
    long len = size;
    if (index < 0 || index >= len) throw new IndexOutOfBoundsException();
    int power_posn = _powerPosn(index ^ size);
    if (power_posn == 0) return (E)_powers0[(int)(index & MASK)];
    long mask = (1 << ((power_posn + 1) * SHIFT)) - 1;
    return _get(power_posn, index & mask, _powers[power_posn]);
  }

  @SuppressWarnings("unchecked")
  private E _get(int tribbles, long index, Object obj) {
    Object array[] = (Object[])obj;
    if (tribbles == 0) return (E)array[(int)index];
    long idx = index >>> (tribbles * SHIFT);
    return _get(tribbles - 1, index - (idx << (tribbles * SHIFT)), array[(int)idx]);
  }

  static private Object[] _copyBut(Object old[], int index, Object value) {
    Object[] new_array = Arrays.copyOf(old, old.length);
    for (int i = old.length; i < index; i++) new_array[i] = _zero;
    new_array[index] = value;
    return new_array;
  }

  // Makes a copy of an array, but at the given index the value is substituted,
  // and to the left of that position, everything is zeroed.
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

  public int indexOf(Object needle) {
    // Not worth creating an iterator for small collections.
    if (size < 64) return (int)indexOf(needle, 0);
    long index = 0;
    for (Object obj : this) {
      if ((needle == null && obj == null)|| needle.equals(obj)) return _longTruncator(index);
      index++;
    }
    return -1;
  }

  protected long indexOf(Object needle, long starting) {
    if (starting > size) throw new IndexOutOfBoundsException();
    // Not worth creating an iterator for small collections.
    if (size < 64) {
      for (long index = starting; index < size; index++) {
        Object obj = get(index);
        if ((needle == null && obj == null) || needle.equals(obj)) return index;
      }
    } else {
      ListIterator<E> lit = listIterator(starting);
      long index = starting;
      while (lit.hasNext()) {
        Object obj = lit.next();
        if ((needle == null && obj == null) || needle.equals(obj)) return _longTruncator(index);
        index++;
      }
    }
    return -1;
  }

  public int lastIndexOf(Object needle) {
    return _longTruncator(lastIndexOf(needle, 0));
  }

  protected long lastIndexOf(Object needle, long stopAt) {
    if (stopAt < 0) throw new IndexOutOfBoundsException();
    // Not worth creating an iterator for small collections.
    if (size - stopAt < 64) {
      for (long index = size - 1; index >= stopAt; index--) {
        Object obj = get(index);
        if ((needle == null && obj == null) || needle.equals(obj)) return index;
      }
    } else {
      ListIterator<E> lit = listIterator(size);
      long index = size;
      while (index > stopAt) {
        Object obj = lit.previous();
        index--;
        if ((needle == null && obj == null) || needle.equals(obj)) return _longTruncator(index);
      }
    }
    return -1;
  }

  public Object clone() {
    return this;
  }

  // Makes a copy of an array, but appends the given values.
  static private Object[] _copyAppend(Object old[], int count, Object value1, Object value2) {
    int len = old.length;
    Object[] new_array = Arrays.copyOf(old, len + count);
    new_array[len] = value1;
    if (count == 2) new_array[len + 1] = value2;
    return new_array;
  }

  public ImmutableArray<E> atPut(long index, E value) {
    long len = size;
    if (index < 0 || index >= len) throw new IndexOutOfBoundsException();
    int power_posn = _powerPosn(index ^ size);
    if (power_posn == 0) {
      return new ImmutableArray<E>(
          size,
          _powers,
          _copyBut(_powers0, (int)(index & MASK), value));
    }
    long mask = (1 << ((power_posn + 1) * SHIFT)) - 1;
    return new ImmutableArray<E>(
        size,
        _copyBut(
            _powers,
            power_posn,
            _atPut(power_posn, value, index & mask, (Object[])_powers[power_posn])),
        _powers0);
  }

  private Object[] _atPut(int tribbles, Object value, long index, Object array[]) {
    int idx = (int)(index >>> (tribbles * SHIFT));
    // We need to check for subtrees that have been deleted because they are
    // the backing of a Deque.
    if (array[(int)idx] == null) array[(int)idx] = new Object[M];
    return _copyBut(
        array,
        idx,
        tribbles == 0 ?
            value :
            _atPut(tribbles - 1, value, index - (idx << (tribbles * SHIFT)), (Object[])array[(int)idx]));
  }

  protected ImmutableArray<E> _newWithSpaceOnLeft() {
    if (size <= M - 1) {
      Object new_tail[] = new Object[(int)(size + 1)];
      for (int i = 0; i < size; i++) new_tail[i + 1] = _powers0[i];
      if (size == M - 1) {
        Object new_powers[] = new Object[] { null, new Object[] { new_tail } };
        return new ImmutableArray<E>(size + 1, new_powers, _zero);
      } else {
        return new ImmutableArray<E>(size + 1, _powers, new_tail);
      }
    }
    assert _powers.length >= 2;
    int shift = _powers.length - 1;
    Object arraylet[] = (Object[])_powers[shift];
    Object new_arraylet[] = new Object[arraylet.length + 1];
    for (int i = 0; i < arraylet.length; i++) {
      new_arraylet[i + 1] = arraylet[i];
    }
    long extra_space = 1L << (shift * SHIFT);
    Object new_powers[];
    if (arraylet.length == M - 1)  {
      // We have to extend the powers array.
      new_powers = _copyAppend(_powers, 1, new Object[] { new_arraylet }, null);
      new_powers[shift] = _zero;
    } else {
      // There is space in the top of the powers array for a new entry.
      new_powers = _copyBut(_powers, shift, new_arraylet);
    }
    return new ImmutableArray<E>(size + extra_space, new_powers, _powers0);
  }

  // Null out all entries to the left of the given index. This is used by
  // Immutable Deque to ensure that items to the left of the offset are not
  // retained for GC purposes.
  // Time taken is O(log size).
  protected ImmutableArray<E> trimLeft(long index) {
    assert 0 < index && index < size;
    if ((size ^ index) < M) {
      Object[] tail = new Object[(int)(size - index)];
      for (int i = 0; i < tail.length; i++) {
        tail[i] = _powers0[(int)(i + (index & MASK))];
      }
      return new ImmutableArray<E>(size - index, _powers, tail);
    }
    assert _powers.length >= 2;
    long len = size;
    int length_tribbles = _powerPosn(size);
    while (true) {
      long top_index_digit = index >>> (SHIFT * length_tribbles);
      long top_length_digit = len >>> (SHIFT * length_tribbles);
      if (top_index_digit < top_length_digit) {
        Object new_powers[] = Arrays.copyOf(_powers, length_tribbles + 1);
        for (int i = 0; i < length_tribbles; i++) new_powers[i] = _powers[i];
        new_powers[length_tribbles] = _trimLeft(
            length_tribbles,
            index,
            (Object[])_powers[length_tribbles]);
        return new ImmutableArray<E>(len, new_powers, _powers0);
      }
      index -= top_index_digit << (SHIFT * length_tribbles);
      len -= top_index_digit << (SHIFT * length_tribbles);
      length_tribbles--;
    }
  }

  private Object[] _trimLeft(int tribbles, long index, Object array[]) {
    long idx = index >>> (tribbles * SHIFT);
    return _nullToTheLeft(
        array,
        idx,
        tribbles == 0 ?
            array[(int)idx] :
            _trimLeft(tribbles - 1, index - (idx << (tribbles * SHIFT)), (Object[])array[(int)idx]));
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

  public ImmutableArray<E> pushAll(E array[]) {
    return _pushAll(Arrays.asList(array), array.length);
  }

  public ImmutableArray<E> pushAll(Collection<? extends E> collection) {
    return _pushAll(collection, collection.size());
  }

  public ImmutableArray<E> pushAll(ImmutableCollection<? extends E> collection) {
    return _pushAll(collection, collection.longSize());
  }

  private ImmutableArray<E> _pushAll(Collection collection, long length) {
    if (length == 0) return this;
    Object new_powers[] = _pushHelper(_powers, _powers0, size, collection, length);
    Object new_powers0[] = (Object[])new_powers[0];
    new_powers[0] = null;
    return new ImmutableArray<E>(size + length, new_powers.length == 1 ? _zero : new_powers, new_powers0);
  }

  // Uses a mutable powers array and pushes items in groups of powers of M in
  // order to make things more efficient.
  // Takes the powers and powers0 arrays separately, but returns everything in
  // one powers array, with powers0 in the 0th position.
  private static Object[] _pushHelper(Object old_powers[], Object old_powers0[], long size, Collection collection, long remaining) {
    assert(remaining != 0);
    Object[] powers = old_powers.length == 0 ? new Object[] { _zero } : Arrays.copyOf(old_powers, old_powers.length);
    Iterator it = collection.iterator();
    int mod = (int)(size & MASK);
    if (mod != 0) {
      int add = M - mod > remaining ? (int)remaining : M - mod;
      int old_size = old_powers0.length;
      Object arraylet[] = Arrays.copyOf(old_powers0, old_size + add);
      for (int i = 0; i < add; i++) arraylet[old_size + i] = it.next();
      if (arraylet.length == M) {
        powers = _insertSubtree(powers, arraylet, 1);
        powers[0] = _zero;
      } else {
        powers[0] = arraylet;
      }
      remaining -= add;
      size += add;
      if (remaining == 0) return powers;
    } else {
      powers[0] = _zero;
    }
    assert powers[0] != null;
    // We have a power of M size.  Try to push M at a time, or M*M, or M*M*M...
    long at_a_time = M;
    int shift = 1;
    while (at_a_time >= M) {
      if (at_a_time <= remaining) {
        long next_aat = at_a_time << SHIFT;
        if (next_aat <= remaining && ((size & (next_aat - 1)) == 0)) {
          at_a_time = next_aat;
          shift++;
        } else {
          Object subtree[] = _createSubtree(at_a_time, it);
          powers = _insertSubtree(powers, subtree, shift);
          remaining -= at_a_time;
          size += at_a_time;
        }
      } else {
        at_a_time >>= SHIFT;
        shift--;
      }
    }
    if (remaining > 0) {
      assert remaining < M;
      Object arraylet[] = new Object[(int)remaining];
      for (int i = 0; i < remaining; i++) arraylet[i] = it.next();
      powers[0] = arraylet;
    }
    assert powers[0] != null;
    return powers;
  }

  private static Object[] _createSubtree(long at_a_time, Iterator it) {
    Object arraylet[] = new Object[M];
    for (int i = 0; i < M; i++) {
      if (at_a_time == M) {
        arraylet[i] = it.next();
      } else {
        arraylet[i] = _createSubtree(at_a_time >> SHIFT, it);
      }
    }
    return arraylet;
  }

  private ImmutableArray<E> _push(int count, Object value1, Object value2) {
    Object powers[] = _powers;
    Object powers0[] = _powers0;
    Object tail[] = _copyAppend(powers0, count, value1, value2);
    if (tail.length == M) {
      powers = Arrays.copyOf(_powers, _powers.length);
      powers = _insertSubtree(powers, tail, 1);
      tail = _zero;
    }
    return new ImmutableArray<E>(size + count, powers, tail);
  }

  static private Object[] _insertSubtree(Object powers[], Object value, int shift) {
    assert shift != 0;
    if (powers.length < shift + 1) {
      int zero_start = powers.length;
      powers = Arrays.copyOf(powers, shift + 1);
      for (int i = zero_start; i < powers.length; i++) powers[i] = _zero;
      assert powers[0] != null;
    }
    for (int i = shift; i < powers.length; i++) {
      Object new_value[] = _copyAppend((Object[])powers[i], 1, value, null);
      powers[i] = new_value;
      if (new_value.length != M) return powers;
      value = powers[i];
      powers[i] = _zero;
    }
    // Need to grow powers array.
    powers = Arrays.copyOf(powers, powers.length + 1);
    powers[powers.length - 1] = new Object[] { value };
    powers[powers.length - 2] = _zero;
    return powers;
  }

  public ImmutableCollection<E> unshift(E value) {
    if (size < 16) {
      // For small collections, the ImmutableArray is more memory efficient
      // than the ImmutableDeque.
      return new ImmutableArray<E>().push(value).pushAll(this);
    }
    return new ImmutableDeque<E>(0, this).unshift(value);
  }

  public ImmutableCollection<E> unshiftAll(E array[]) {
    if (size + array.length <= 16) {
      // For small collections, the ImmutableArray is more memory efficient
      // than the ImmutableDeque.
      return new ImmutableArray<E>(array).pushAll(this);
    }
    return new ImmutableDeque<E>(0, this).unshiftAll(array);
  }

  @SuppressWarnings("unchecked")
  public ImmutableCollection<E> unshiftAll(Collection<? extends E> collection) {
    if (size + collection.size() <= 16) {
      // For small collections, the ImmutableArray is more memory efficient
      // than the ImmutableDeque.
      if (collection instanceof ImmutableArray) {
        return ((ImmutableArray)collection).pushAll(this);
      }
      return new ImmutableArray(collection).pushAll(this);
    }
    if ((size >> 2) < collection.size()) {
      // Reverse the operation unless we are prepending a small thing onto a
      // much larger thing.
      if (collection instanceof ImmutableCollection) {
        return ((ImmutableCollection)collection).pushAll(this);
      } else {
        return new ImmutableArray<E>(collection).pushAll(this);
      }
    }
    return new ImmutableDeque<E>(0, this).unshiftAll(collection);
  }

  public ImmutableCollection<E> shift() {
    return subList(1);
  }

  public ImmutableCollection<E> subList(long from) {
    if (from == 0) return this;
    if (from == size) return new ImmutableArray<E>();
    return new ImmutableDeque<E>(from, this);
  }

  public ImmutableCollection<E> subList(long from, long to) {
    if (from == 0) return trim(size - to);
    if (from == size) return new ImmutableArray<E>();
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
    if (by < (size & MASK)) {
      return new ImmutableArray<E>(new_size, _powers, Arrays.copyOf(_powers0, (int)((size - by) & MASK)));
    } else if (by == (size & MASK)) {
      return new ImmutableArray<E>(new_size, _powers, _zero);
    }
    // At this point we know that powers needs to change.
    int d = _powers.length;
    Object new_powers[] = null;
    Object[] borrow = _zero;
    // One digit at a time, from most significant to least significant.
    for (int i = d - 1; i >= 0; i--) {
      int old_digit = (int)((size >> (i * SHIFT)) & MASK);
      int new_digit = (int)((new_size >> (i * SHIFT)) & MASK);
      Object input[] = i == 0 ? _powers0 : (Object[])_powers[i];
      int borrow_len = borrow.length;
      int input_len = borrow_len + input.length;
      Object here[];
      if (borrow_len == 0 && new_digit == old_digit) {
        here = input;
      } else {
        here = new_digit == 0 ? _zero : new Object[new_digit];
        for (int j = 0; true; j++) {
          Object obj = j < borrow_len ? borrow[j] :
                       j < input_len ? input[j - borrow_len] :
                       _zero;
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
    Object[] new_powers0 = (Object[])new_powers[0];
    new_powers[0] = null;
    return new ImmutableArray<E>(new_size, new_powers, new_powers0);
  }

  protected class ImmutableArrayIterator<E> implements Iterator<E> {
    protected long _remaining;
    protected long _index;
    protected Object _stack[][];
    protected Object _powers[];
    protected Object _powers0[];
    protected int _powers_posn;

    public ImmutableArrayIterator(long length, Object[] powers, Object[] powers0) {
      _powers = powers;
      _powers0 = powers0;
      _remaining = length;
      _index = 0;
    }

    public ImmutableArrayIterator(long length, Object[] powers, Object[] powers0, long starting) {
      _powers = powers;
      _powers0 = powers0;
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
      if (size != 0) {
        int len = _powerPosn(size);
        if (starting == 0) {
          _powers_posn = len;
        } else {
          for (_powers_posn = len; _powers_posn >= 0; _powers_posn--) {
            Object arraylet[] = _powers_posn == 0 ? _powers0 : (Object[])_powers[_powers_posn];
            long at_this_level = arraylet.length << (SHIFT * _powers_posn);
            if (starting < at_this_level) break;
            starting -= at_this_level;
          }
        }
        _stack = new Object[len + 1][];
        _populate_stack();
      }
    }

    public boolean hasNext() {
      return _remaining > 0;
    }

    public void forEachRemaining(Consumer<? super E> action) {
      if (_remaining != 0) {
        ImmutableArray._forEachHelper(_powers, _powers0, _index, _index + _remaining, action);
      }
      _remaining = 0;
    }

    // Get next element in collection and advance by one.
    @SuppressWarnings("unchecked")
    public E next() {
      // Short version of next, designed to be inlined.
      assert hasNext();
      if (_stack == null) _init();
      int arraylet_posn = (int)(_index & MASK);
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
    private void _next() {
      // The easy way to implement this is just to use _index and something
      // like _get(), but this takes logn, giving an nlogn iteration over the
      // whole collection.  Instead we maintain a stack of arraylets at
      // different levels of the tree, which gives O(n) iteration.  The nth
      // position in the _powers array has a tree under it that is n+1 deep,
      // which also sets the size of the explicit stack we need.
      int shift = 0;
      for (int idx = _powers_posn; idx >= 0; idx--) {
        Object[] arraylet = (Object[])(_stack[idx]);
        int arraylet_posn = (int)((_index >> shift) & MASK);
        if (arraylet_posn != 0 && arraylet_posn != arraylet.length) {
          _populate_stack(shift, idx, arraylet);
          return;
        }
        shift += SHIFT;
      }
      // Need to move to next position in _powers array.
      if (_powers_posn == 0) return;  // Hit the end.
      int powers_posn = _powers_posn;
      do {
        powers_posn--;
        if (powers_posn == 0) {
          if (_powers0.length != 0) break;
          return;  // Hit the end.
        }
      } while (((Object[])_powers[powers_posn]).length == 0);
      _powers_posn = powers_posn;
      _populate_stack();
    }

    protected void _populate_stack() {
      Object top[] = _powers_posn <= 0 ? _powers0 : (Object[])_powers[_powers_posn];
      _stack[0] = top;
      _populate_stack(_powers_posn * SHIFT, 0, top);
    }

    protected void _populate_stack(int shift, int idx, Object arraylet[]) {
      while (idx < _powers_posn) {
        int arraylet_posn = (int)((_index >> shift) & MASK);
        idx++;
        arraylet = (Object[])arraylet[arraylet_posn];
        _stack[idx] = arraylet;
        shift -= SHIFT;
      }
    }
  }

  protected class ImmutableArrayListIterator<E> extends ImmutableArrayIterator<E> implements ListIterator<E> {
    private final long _start;

    public ImmutableArrayListIterator(long length, Object[] powers, Object[] powers0) {
      super(length, powers, powers0);
      _start = 0;
    }

    public ImmutableArrayListIterator(long length, Object[] powers, Object[] powers0, long starting, long leftmost_limit) {
      super(length, powers, powers0, starting);
      if (starting < leftmost_limit) throw new IndexOutOfBoundsException();
      _start = leftmost_limit;
    }

    // Get previous element in collection and go back by one.
    @SuppressWarnings("unchecked")
    public E previous() {
      // Short version of previous, designed to be inlined.
      if (_stack == null) _init();
      assert hasPrevious();
      int arraylet_posn = (int)(_index & MASK);
      _index--;
      _remaining++;
      if (arraylet_posn == 0) {
        _previous();
        arraylet_posn = M;
      }
      arraylet_posn--;
      return (E)_stack[_powers_posn][arraylet_posn];
    }

    // Out of line version of _previous for when we need to move to the previous
    // leaf.
    private void _previous() {
      // The easy way to implement this is just to use _index and something
      // like _get(), but this takes logn, giving an nlogn iteration over the
      // whole collection.  Instead we maintain a stack of arraylets at
      // different levels of the tree, which gives O(n) iteration.  The nth
      // position in the _powers array has a tree under it that is n+1 deep,
      // which also sets the size of the explicit stack we need.
      int shift = 0;
      for (int idx = _powers_posn; idx >= 0; idx--) {
        Object[] arraylet = (Object[])(_stack[idx]);
        int arraylet_posn = (int)((_index >> shift) & MASK);
        if (arraylet_posn != M - 1) {
          _populate_stack(shift, idx, arraylet);
          return;
        }
        shift += SHIFT;
      }
      // Need to move to previous position in _powers array.
      int powers_posn = _powers_posn;
      do {
        powers_posn++;
        // Can't go earlier than the 0th element.
        assert powers_posn != _powers.length;
      } while (((Object[])_powers[powers_posn]).length == 0);
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
    _forEachHelper(_powers, _powers0, startAt, size, action);
  }

  private static void _forEachHelper(Object[] powers, Object[] powers0, long startAt, long size, Consumer action) {
    long index = 0;
    int start = _powerPosn(size);
    for (int p = start; p >= 0; p--) {
      Object[] power = p == 0 ? powers0 : (Object[])powers[p];
      _forEachHelper(power, p, startAt, index, action);
      index += power.length << (SHIFT * p);
    }
  }

  @SuppressWarnings("unchecked")
  private static void _baseForEach(Object array[], Consumer action) {
    for (int i = 0; i < array.length; i++) {
      Object[] sub = (Object[])array[i];
      for (int j = 0; j < sub.length; j++) {
        action.accept(sub[j]);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static void _forEachHelper(Object[] array, int depth, long startAt, long index, Consumer action) {
    long end = index + array.length << (SHIFT * depth);
    if (end < startAt) return;
    if (depth == 1 && startAt <= index) {
      // Having this fast case makes peak 25-30% faster.
      _baseForEach(array, action);
      return;
    }
    if (depth == 0) {
      int start = startAt <= index ? 0 : (int)(startAt & MASK);
      for (int i = start; i < array.length; i++) {
        action.accept(array[i]);
      }
    } else {
      for (int i = 0; i < array.length; i++) {
        long index_after = index + (1 << (SHIFT * depth));
        if (startAt < index_after) {
          _forEachHelper((Object[])array[i], depth - 1, startAt, index, action);
        }
        index = index_after;
      }
    }
  }
}

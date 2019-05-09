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
  private Object _tail[];

  static private final Object _zero[] = new Object[0];

  /** Create an empty ImmutableArray. */
  public ImmutableArray() {
    size = 0;
    _powers = _zero;
    _tail = _zero;
  }

  /** Make an ImmutableArray that is a shallow copy of an array.
   *  @param array The array to be copied
   */
  public ImmutableArray(E array[]) {
    size = array.length;
    _tail = (size == 0) ?
        _zero :
        Arrays.copyOfRange(array, array.length & ~MASK, array.length);
    _powers = _newPushHelper(
        _zero, 0, Arrays.asList(array).iterator(), size & ~MASK);
  }

  /** Make an ImmutableArray that is a shallow copy of another collection.
   *  @param collection The collection to be copied
   */
  public ImmutableArray(Collection<? extends E> collection) {
    if (collection instanceof ImmutableArray) {
      ImmutableArray other = (ImmutableArray)collection;
      size = other.size;
      _powers = other._powers;
      _tail = other._tail;
    } else {
      size = collection.size();
      Iterator<?> it = collection.iterator();
      _powers = _newPushHelper(_zero, 0, it, size & ~MASK);
      if ((size & MASK) == 0) {
        _tail = _zero;
      } else {
        _tail = new Object[(int)(size & MASK)];
        for (int i = 0; i < _tail.length; i++) _tail[i] = it.next();
      }
    }
  }

  public int size() {
    return _longTruncator(size);
  }

  public long longSize() {
    return size;
  }

  public ListIterator<E> listIterator() {
    return new ImmutableArrayListIterator<E>(size, _powers, _tail);
  }

  public ListIterator<E> listIterator(int index) {
    return new ImmutableArrayListIterator<E>(size, _powers, _tail, index, 0);
  }

  public ListIterator<E> listIterator(long index) {
    return new ImmutableArrayListIterator<E>(size, _powers, _tail, index, 0);
  }

  protected ListIterator<E> listIterator(long index, long leftMost) {
    return new ImmutableArrayListIterator<E>(size, _powers, _tail, index, leftMost);
  }

  public ImmutableArrayIterator<E> iterator() {
    return new ImmutableArrayIterator<E>(size, _powers, _tail);
  }

  protected ImmutableArrayIterator<E> iterator(long startAt) {
    return new ImmutableArrayIterator<E>(size, _powers, _tail, startAt);
  }

  private ImmutableArray(long len, Object[] pow, Object[] pow0) {
    size = len;
    _powers = pow;
    _tail = pow0;
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
    if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
    int power_posn = _powerPosn(index ^ size);
    if (power_posn == 0) return (E)_tail[(int)(index & MASK)];
    Object array[] = (Object[])_powers[power_posn - 1];
    int shift = power_posn * SHIFT;
    while (true) {
      if (shift == 0) return (E)array[(int)(index & MASK)];
      int idx = (int)(index >>> shift);
      array = (Object[])array[idx & MASK];
      shift -= SHIFT;
    }
  }

  static private Object[] _copyPad(Object old[], int new_length) {
    Object[] new_array = Arrays.copyOf(old, new_length);
    for (int i = old.length; i < new_length; i++) new_array[i] = _zero;
    return new_array;
  }

  static private Object[] _copyBut(Object old[], int index, Object value) {
    Object[] new_array = Arrays.copyOf(old, old.length);
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
          _copyBut(_tail, (int)(index & MASK), value));
    }
    long mask = (1 << ((power_posn + 1) * SHIFT)) - 1;
    return new ImmutableArray<E>(
        size,
        _copyBut(
            _powers,
            power_posn - 1,
            _atPut(power_posn, value, index & mask, (Object[])_powers[power_posn - 1])),
        _tail);
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
      for (int i = 0; i < size; i++) new_tail[i + 1] = _tail[i];
      if (size == M - 1) {
        Object new_powers[] = new Object[] { new Object[] { new_tail } };
        return new ImmutableArray<E>(size + 1, new_powers, _zero);
      } else {
        return new ImmutableArray<E>(size + 1, _powers, new_tail);
      }
    }
    assert _powers.length >= 1;
    int shift = _powers.length;
    Object arraylet[] = (Object[])_powers[shift - 1];
    Object new_arraylet[] = new Object[arraylet.length + 1];
    for (int i = 0; i < arraylet.length; i++) {
      new_arraylet[i + 1] = arraylet[i];
    }
    long extra_space = 1L << (shift * SHIFT);
    Object new_powers[];
    if (arraylet.length == M - 1)  {
      // We have to extend the powers array.
      new_powers = _copyAppend(_powers, 1, new Object[] { new_arraylet }, null);
      new_powers[shift - 1] = _zero;
    } else {
      // There is space in the top of the powers array for a new entry.
      new_powers = _copyBut(_powers, shift - 1, new_arraylet);
    }
    return new ImmutableArray<E>(size + extra_space, new_powers, _tail);
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
        tail[i] = _tail[(int)(i + (index & MASK))];
      }
      return new ImmutableArray<E>(size - index, _powers, tail);
    }
    assert _powers.length >= 1;
    long len = size;
    int length_tribbles = _powerPosn(size);
    while (true) {
      long top_index_digit = index >>> (SHIFT * length_tribbles);
      long top_length_digit = len >>> (SHIFT * length_tribbles);
      if (top_index_digit < top_length_digit) {
        Object new_powers[] = Arrays.copyOf(_powers, length_tribbles);
        new_powers[length_tribbles - 1] = _trimLeft(
            length_tribbles,
            index,
            (Object[])_powers[length_tribbles - 1]);
        return new ImmutableArray<E>(len, new_powers, _tail);
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

  private ImmutableArray<E> _pushAll(Collection<? extends E> collection, long length) {
    if (length == 0) return this;
    Iterator<? extends E> it = collection.iterator();
    if (length == 1) return push(it.next());
    long new_size = size;
    Object new_powers[];
    if ((size & MASK) != 0 || length < MASK) {
      long mod = M - (size & MASK);
      if (mod > length) mod = length;
      Object new_tail[] = Arrays.copyOf(_tail, (int)(_tail.length + mod));
      for (int i = 0; i < mod; i++) {
        new_tail[_tail.length + i] = it.next();
      }
      if (new_tail.length < M) {
        return new ImmutableArray<E>(size + length, _powers, new_tail);
      }
      new_powers = _copyPad(_powers, _powerPosn(size + length));
      new_powers = _insertSubtree(new_powers, new_tail, 0);
      new_size += mod;
      length -= mod;
    } else {
      new_powers = _powers;
    }
    // We have extended to a multiple of M.
    new_powers = _newPushHelper(new_powers, new_size, it, length & ~MASK);
    // Take care of the rest.
    Object new_tail[] = _zero;
    if ((length & MASK) != 0) {
      new_tail = new Object[(int)(length & MASK)];
      for (int i = 0; i < new_tail.length; i++) new_tail[i] = it.next();
    }
    return new ImmutableArray<E>(new_size + length, new_powers, new_tail);
  }

  // Creates a mutable powers array and pushes items in groups of powers of M in
  // order to make things more efficient.  The collection always has a size
  // divisible with M before and after.
  private static Object[] _newPushHelper(Object old_powers[], long size, Iterator it, long remaining) {
    if (remaining == 0) return old_powers;
    int new_powers_length = _powerPosn(size + remaining);
    Object[] powers = _copyPad(old_powers, new_powers_length);
    // We have a power of M size.  Try to push M at a time, or M*M, or M*M*M...
    long at_a_time = M;
    int shift = 1;
    while (remaining != 0) {
      assert(at_a_time != 1);
      if (at_a_time <= remaining) {
        long next_aat = at_a_time << SHIFT;
        if (next_aat <= remaining && ((size & (next_aat - 1)) == 0)) {
          at_a_time = next_aat;
          shift++;
        } else {
          Object subtree[] = _createSubtree(at_a_time, it);
          powers = _insertSubtree(powers, subtree, shift - 1);
          remaining -= at_a_time;
          size += at_a_time;
        }
      } else {
        at_a_time >>= SHIFT;
        shift--;
      }
    }
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
    Object tail[] = _copyAppend(_tail, count, value1, value2);
    if (tail.length == M) {
      powers = _copyPad(_powers, _powerPosn(size + count));
      powers = _insertSubtree(powers, tail, 0);
      tail = _zero;
    }
    return new ImmutableArray<E>(size + count, powers, tail);
  }

  static private Object[] _insertSubtree(Object powers[], Object value, int shift) {
    assert shift >= 0;
    if (powers.length < shift + 1) {
      powers = _copyPad(powers, shift + 1);
    }
    for (int i = shift; i < powers.length; i++) {
      Object new_value[] = _copyAppend((Object[])powers[i], 1, value, null);
      powers[i] = new_value;
      if (new_value.length != M) return powers;
      value = powers[i];
      powers[i] = _zero;
    }
    // We are always passed a large enough array.
    assert(false);
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
      return new ImmutableArray<E>(new_size, _powers, Arrays.copyOf(_tail, (int)((size - by) & MASK)));
    } else if (by == (size & MASK)) {
      return new ImmutableArray<E>(new_size, _powers, _zero);
    }
    // At this point we know that powers needs to change.
    Object new_powers[] = null;
    Object[] new_tail = _tail;
    Object[] borrow = _zero;
    // One digit at a time, from most significant to least significant.
    for (int i = _powers.length; i >= 0; i--) {
      int old_digit = (int)((size >> (i * SHIFT)) & MASK);
      int new_digit = (int)((new_size >> (i * SHIFT)) & MASK);
      Object input[] = i == 0 ? _tail : (Object[])_powers[i - 1];
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
          new_powers = new Object[i];
        }
        if (i == 0) {
          new_tail = here;
        } else {
          new_powers[i - 1] = here;
        }
      }
    }
    return new ImmutableArray<E>(new_size, new_powers, new_tail);
  }

  protected class ImmutableArrayIterator<E> implements Iterator<E> {
    protected long _remaining;
    protected long _index;
    protected Object _stack[][];
    protected Object _powers[];
    protected Object _tail[];
    protected int _powers_posn;

    public ImmutableArrayIterator(long length, Object[] powers, Object[] tail) {
      _powers = powers;
      _tail = tail;
      _remaining = length;
      _index = 0;
      _init();
    }

    public ImmutableArrayIterator(long length, Object[] powers, Object[] tail, long starting) {
      _powers = powers;
      _tail = tail;
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
        _init();  // Set stack for the last position it makes sense.
        _index = starting;
      } else {
        _index = starting;
        _init();
      }
    }

    protected void _init() {
      long starting = _index;
      if (size != 0) {
        int len = _powerPosn(size);
        if (starting == 0) {
          _powers_posn = len;
        } else {
          for (_powers_posn = len; _powers_posn >= 0; _powers_posn--) {
            Object arraylet[] = _powers_posn == 0 ? _tail : (Object[])_powers[_powers_posn - 1];
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
        ImmutableArray._forEachHelper(_powers, _tail, _index, _index + _remaining, action);
      }
      _remaining = 0;
    }

    // Get next element in collection and advance by one.
    @SuppressWarnings("unchecked")
    public E next() {
      // Short version of next, designed to be inlined.
      assert hasNext();
      //if (_stack == null) _init();
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
      int shift = SHIFT;
      for (int idx = _powers_posn - 1; idx >= 0; idx--) {
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
      _powers_posn = _powerPosn(_index ^ (_index + _remaining));
      _populate_stack();
    }

    protected void _populate_stack() {
      Object top[] = _powers_posn <= 0 ? _tail : (Object[])_powers[_powers_posn - 1];
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

    public ImmutableArrayListIterator(long length, Object[] powers, Object[] tail) {
      super(length, powers, tail);
      _start = 0;
    }

    public ImmutableArrayListIterator(long length, Object[] powers, Object[] tail, long starting, long leftmost_limit) {
      super(length, powers, tail, starting);
      if (starting < leftmost_limit) throw new IndexOutOfBoundsException();
      _start = leftmost_limit;
    }

    // Get previous element in collection and go back by one.
    @SuppressWarnings("unchecked")
    public E previous() {
      // Short version of previous, designed to be inlined.
      //if (_stack == null) _init();
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
      _powers_posn = _powerPosn(_index ^ (_index + _remaining));
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
    _forEachHelper(_powers, _tail, startAt, size, action);
  }

  private static void _forEachHelper(Object[] powers, Object[] tail, long startAt, long size, Consumer action) {
    long index = 0;
    int start = _powerPosn(size);
    for (int p = start; p >= 0; p--) {
      Object[] power = p == 0 ? tail : (Object[])powers[p - 1];
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

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
    _powers = _pushHelper(null, 0, Arrays.asList(array), size);
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
      _powers = _pushHelper(null, 0, collection, size);
    }
  }

  public int size() {
    return _longTruncator(size);
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

  private int _powerPosn(long index) {
    // Multiplying by 43 and shifting down by 7 is just a way to divide by 3.
    return ((63 - Long.numberOfLeadingZeros(index ^ size)) * 43) >> 7;
  }

  public E get(long index) {
    long len = size;
    if (index < 0 || index >= len) throw new IndexOutOfBoundsException();
    int power_posn = _powerPosn(index);
    long mask = (1 << ((power_posn + 1) * 3)) - 1;
    return _get(power_posn, index & mask, _powers[power_posn]);
  }

  @SuppressWarnings("unchecked")
  private E _get(int tribbles, long index, Object obj) {
    Object array[] = (Object[])obj;
    if (tribbles == 0) return (E)array[(int)index];
    long idx = index >>> (tribbles * 3);
    return _get(tribbles - 1, index - (idx << (tribbles * 3)), array[(int)idx]);
  }

  static private Object[] _copyBut(Object old[], int index, Object value) {
    if (old == null) {
      Object[] new_array = new Object[index + 1];
      new_array[index] = value;
      return new_array;
    } else {
      Object[] new_array = new Object[old.length];
      for (int i = 0; i < new_array.length; i++) {
        new_array[i] = (i == index) ? value : old[i];
      }
      return new_array;
    }
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
    int power_posn = _powerPosn(index);
    long mask = (1 << ((power_posn + 1) * 3)) - 1;
    return new ImmutableArray<E>(
        size,
        _copyBut(
            _powers,
            power_posn,
            _atPut(power_posn, value, index & mask, (Object[])_powers[power_posn])));
  }

  private Object[] _atPut(int tribbles, Object value, long index, Object array[]) {
    int idx = (int)(index >>> (tribbles * 3));
    if (array == null) array = new Object[8];
    return _copyBut(
        array,
        idx,
        tribbles == 0 ?
            value :
            _atPut(tribbles - 1, value, index - (idx << (tribbles * 3)), (Object[])array[(int)idx]));
  }

  protected ImmutableArray<E> _newWithSpaceOnLeft() {
    if (size == 0) {
      // Handle specially the case where the backing has a null powers array.
      return push(null);
    }
    int shift = _powers.length - 1;
    Object arraylet[] = (Object[])_powers[shift];
    Object new_arraylet[] = new Object[arraylet.length + 1];
    for (int i = 0; i < arraylet.length; i++) new_arraylet[i + 1] = arraylet[i];
    long extra_space = 1L << (shift * 3);
    Object new_powers[];
    if (arraylet.length == 7)  {
      // We have to extend the powers array.
      new_powers = _copyAppend(_powers, 1, new Object[] { new_arraylet }, null);
      new_powers[shift] = null;
    } else {
      // There is space in the top of the powers array for a new entry.
      new_powers = _copyBut(_powers, shift, new_arraylet);
    }
    return new ImmutableArray<E>(size + extra_space, new_powers);
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
    return new ImmutableArray<E>(size + length, _pushHelper(_powers, size, collection, length));
  }

  private static Object[] _freshPowers(Object old_powers[], long additions) {
    int powers_size;
    if (old_powers == null) {
      long po8 = 8;
      powers_size = 1;
      while (additions >= po8) {
        powers_size++;
        po8 <<= 3;
      }
    } else {
      powers_size = old_powers.length;
    }
    Object[] powers = new Object[powers_size];
    if (old_powers != null) {
      for (int i = 0; i < powers_size; i++) powers[i] = old_powers[i];
    }
    return powers;
  }

  private static Object[] _pushHelper(Object old_powers[], long size, Collection collection, long remaining) {
    Object[] powers = _freshPowers(old_powers, remaining);
    Iterator it = collection.iterator();
    int mod = (int)(size & 7);
    if (mod != 0) {
      int add = 8 - mod > remaining ? (int)remaining : 8 - mod;
      Object old[] = (Object[])powers[0];
      int old_size = old == null ? 0 : old.length;
      Object arraylet[] = new Object[old_size + add];
      for (int i = 0; i < old_size; i++) arraylet[i] = old[i];
      for (int i = 0; i < add; i++) arraylet[old_size + i] = it.next();
      if (arraylet.length == 8) {
        powers[0] = null;
        powers = _insertSubtree(powers, arraylet, 1);
      } else {
        powers[0] = arraylet;
      }
      remaining -= add;
      size += add;
      if (remaining == 0) return powers;
    }
    // We have a power of 8 size.  Try to push 8 at a time, or 8*8, or 8*8*8...
    long at_a_time = 8;
    int shift = 1;
    while (at_a_time >= 8) {
      if (at_a_time <= remaining) {
        long next_aat = at_a_time << 3;
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
        at_a_time >>= 3;
        shift--;
      }
    }
    if (remaining > 0) {
      assert remaining < 8;
      Object arraylet[] = new Object[(int)remaining];
      for (int i = 0; i < remaining; i++) arraylet[i] = it.next();
      powers[0] = arraylet;
    }
    return powers;
  }

  private static Object[] _createSubtree(long at_a_time, Iterator it) {
    Object arraylet[] = new Object[8];
    for (int i = 0; i < 8; i++) {
      if (at_a_time == 8) {
        arraylet[i] = it.next();
      } else {
        arraylet[i] = _createSubtree(at_a_time >> 3, it);
      }
    }
    return arraylet;
  }

  private ImmutableArray<E> _push(int count, Object value1, Object value2) {
    Object powers[] = _freshPowers(_powers, count);
    Object tail[] = _copyAppend((Object[])powers[0], count, value1, value2);
    if (tail.length == 8) {
      powers[0] = null;
      powers = _insertSubtree(powers, tail, 1);
    } else {
      powers[0] = tail;
    }
    return new ImmutableArray<E>(size + count, powers);
  }

  static private Object[] _insertSubtree(Object powers[], Object value, int shift) {
    for (int i = shift; i < powers.length; i++) {
      Object new_value[] = _copyAppend((Object[])powers[i], 1, value, null);
      powers[i] = new_value;
      if (new_value.length != 8) return powers;
      value = powers[i];
      powers[i] = null;
    }
    // Need to grow powers array.
    powers = new Object[powers.length + 1];
    Object arraylet[] = new Object[1];
    arraylet[0] = value;
    powers[powers.length - 1] = arraylet;
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
  public ImmutableCollection<E> unshiftAll(Collection<? super E> collection) {
    if (size + collection.size() <= 16) {
      // For small collections, the ImmutableArray is more memory efficient
      // than the ImmutableDeque.
      if (collection instanceof ImmutableArray) {
        return ((ImmutableArray)collection).pushAll(this);
      }
      return new ImmutableArray(collection).pushAll(this);
    }
    if (collection instanceof ImmutableCollection &&
        size < collection.size()) {
      // Reverse the operation if we are prepending a large thing onto a
      // smaller thing.
      return ((ImmutableCollection)collection).pushAll(this);
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

    public void forEachRemaining(Consumer<? super E> action) {
      if (_remaining != 0) {
        ImmutableArray._forEachHelper(_powers, _index, action);
      }
      _remaining = 0;
    }

    // Get next element in collection and advance by one.
    @SuppressWarnings("unchecked")
    public E next() {
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
    @SuppressWarnings("unchecked")
    public E previous() {
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

  @SuppressWarnings("unchecked")
  private static void _forEachHelper(Object[] array, int depth, long startAt, long index, Consumer action) {
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

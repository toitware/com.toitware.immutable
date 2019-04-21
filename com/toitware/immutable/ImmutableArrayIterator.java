package com.toitware.immutable;

import java.util.Iterator;

public class ImmutableArrayIterator<E> implements Iterator<E> {
  private long _remaining;
  private Object _stack[][];
  private int _positions[];
  private Object _powers[];
  private int _powers_posn;

  public ImmutableArrayIterator(ImmutableArray array, Object[] powers) {
    _powers = powers;
    _remaining = array.length;
    if (powers != null) {
      _powers_posn = powers.length - 1;
      _stack = new Object[powers.length][];
      _positions = new int[powers.length];
      Object a[] = (Object[])powers[_powers_posn];
      for (int i = 0; i < _stack.length; i++) {
        _stack[i] = a;
        _positions[i] = 1;
        if (i < _stack.length - 1) {
          a = (Object[])a[0];
        }
      }
      _positions[_powers_posn] = 0;
    }
  }

  public boolean hasNext() {
    return _remaining > 0;
  }

  // Get next element in collection and advance by one.
  public @SuppressWarnings("unchecked") E next() {
    // Short version of next, designed to be inlined.
    assert hasNext();
    if (_positions[_powers_posn] != _stack[_powers_posn].length) {
      _remaining--;
      return (E)_stack[_powers_posn][_positions[_powers_posn]++];
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
    while (idx >= 0 && _positions[idx] == _stack[idx].length) idx--;
    if (idx != -1) {
      _positions[idx]++;
      for (; idx < _powers_posn; idx++) {
        _stack[idx + 1] = (Object[])_stack[idx][_positions[idx] - 1];
        _positions[idx + 1] = 1;
      }
      _positions[idx] = 0;
      _remaining--;
      return (E)_stack[idx][_positions[idx]++];
    }
    // Need to move to the next element in the powers array.
    do {
      _powers_posn--;
    } while (_powers[_powers_posn] == null);
    _stack[0] = (Object[])_powers[_powers_posn];
    _positions[0] = 1;
    for (idx = 0; idx < _powers_posn; idx++) {
      _stack[idx + 1] = (Object[])_stack[idx][_positions[idx] - 1];
      _positions[idx + 1] = 1;
    }
    _positions[_powers_posn] = 0;
    return next();
  }
}

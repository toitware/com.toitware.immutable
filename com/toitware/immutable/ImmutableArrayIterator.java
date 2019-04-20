package com.toitware.immutable;

import java.util.Iterator;

public class ImmutableArrayIterator<E> implements Iterator<E> {
  private long _index;
  private ImmutableArray _array;

  public ImmutableArrayIterator(ImmutableArray array) {
    _array = array;
    _index = 0;
  }

  public boolean hasNext() {
    return _index < _array.length;
  }

  public @SuppressWarnings("unchecked") E next() {
    return (E)_array.at(_index++);
  }
}

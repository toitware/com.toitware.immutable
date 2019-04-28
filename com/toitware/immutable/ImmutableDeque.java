// Copyright (C) 2019 Toitware ApS. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.toitware.immutable;

import com.toitware.immutable.ImmutableCollection;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.Iterator;

public class ImmutableDeque<E> extends ImmutableCollection<E> {
  private long _offset;
  private ImmutableArray<E> _backing;

  protected ImmutableDeque(long offset, ImmutableArray<E> backing) {
    if (offset < 0 || (offset != 0 && offset >= backing.size)) throw new IndexOutOfBoundsException();
    _offset = offset;
    _backing = backing;
  }

  public int size() {
    long s = _backing.size - _offset;
    if (s > Integer.MAX_VALUE) return Integer.MAX_VALUE;
    return (int)s;
  }

  public long longSize() {
    return _backing.size - _offset;
  }

  public String toString() {
    return "ImmutableDeque " + _offset + " " + _backing;
  }

  public E get(int index) {
    return _backing.get(index + _offset);
  }

  public E get(long index) {
    return _backing.get(index + _offset);
  }

  public ImmutableDeque<E> atPut(long index, E value) {
    if (index < 0) throw new IndexOutOfBoundsException();
    return new ImmutableDeque<E>(_offset, _backing.atPut(index + _offset, value));
  }

  public ImmutableDeque<E> push(E value) {
    return new ImmutableDeque<E>(_offset, _backing.push(value));
  }

  public ImmutableDeque<E> push(E value1, E value2) {
    return new ImmutableDeque<E>(_offset, _backing.push(value1, value2));
  }

  public ImmutableDeque<E> pushAll(E array[]) {
    return new ImmutableDeque<E>(_offset, _backing.pushAll(array));
  }

  public ImmutableDeque<E> pushAll(Collection<? extends E> collection) {
    return new ImmutableDeque<E>(_offset, _backing.pushAll(collection));
  }

  public long indexOf(E object) {
    long result = _backing.indexOf(object, _offset);
    return result == -1 ? -1 : result - _offset;
  }

  public long lastIndexOf(E object) {
    long result = _backing.lastIndexOf(object, _offset);
    return result == -1 ? -1 : result - _offset;
  }

  public ImmutableDeque<E> trim() {
    return trim(1);
  }

  public ImmutableDeque<E> trim(long by) {
    if (by > longSize()) throw new IndexOutOfBoundsException();
    if (by == longSize()) return new ImmutableDeque<E>(0, new ImmutableArray<E>());
    return new ImmutableDeque<E>(_offset, _backing.trim(by));
  }

  public Iterator<E> iterator() {
    return _backing.iterator(_offset);
  }

  public void forEach(Consumer<? super E> action) {
    _backing.forEach(_offset, action);
  }

  public ImmutableDeque<E> shift() {
    return subList(1);
  }

  public ImmutableDeque<E> subList(long from) {
    if (_offset + from > _backing.size) throw new IndexOutOfBoundsException();
    if (_offset + from == _backing.size) return new ImmutableDeque<E>(0, new ImmutableArray<E>());
    return new ImmutableDeque<E>(_offset + from, _backing);
  }

  public ImmutableDeque<E> subList(long from, long to) {
    if (to == longSize()) return subList(from);
    return new ImmutableDeque<E>(_offset + from, _backing.trim(longSize() - to));
  }
}

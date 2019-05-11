// Copyright (C) 2019 Toitware ApS. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.toitware.immutable;

import com.toitware.immutable.ImmutableCollection;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ImmutableDeque<E> extends ImmutableCollection<E> {
  private long _offset;
  private ImmutableArray<E> _backing;

  protected ImmutableDeque(long offset, ImmutableArray<E> backing) {
    if (offset < 0 || (offset != 0 && offset >= backing.size)) throw new IndexOutOfBoundsException();
    long old_backing_size = backing.size;
    _backing = offset == 0 ? backing : backing.trimLeft(offset);
    _offset = offset + _backing.size - old_backing_size;
  }

  private ImmutableDeque(long offset, ImmutableArray<E> backing, boolean notrim) {
    _backing = backing;
    _offset = offset;
  }

  public int size() {
    long s = _backing.size - _offset;
    if (s > Integer.MAX_VALUE) return Integer.MAX_VALUE;
    return (int)s;
  }

  public long longSize() {
    return _backing.size - _offset;
  }

  public E get(int index) {
    return _backing.get(index + _offset);
  }

  public E get(long index) {
    return _backing.get(index + _offset);
  }

  public ImmutableDeque<E> atPut(long index, E value) {
    if (index < 0) throw new IndexOutOfBoundsException();
    return new ImmutableDeque<E>(_offset, _backing.atPut(index + _offset, value), true);
  }

  public ImmutableDeque<E> push(E value) {
    return new ImmutableDeque<E>(_offset, _backing.push(value), true);
  }

  public ImmutableDeque<E> push(E value1, E value2) {
    return new ImmutableDeque<E>(_offset, _backing.push(value1, value2), true);
  }

  public ImmutableDeque<E> pushAll(E array[]) {
    return new ImmutableDeque<E>(_offset, _backing.pushAll(array), true);
  }

  public ImmutableDeque<E> pushAll(Collection<? extends E> collection) {
    return new ImmutableDeque<E>(_offset, _backing.pushAll(collection), true);
  }

  public ImmutableDeque<E> unshift(E value) {
    if (_offset != 0) {
      // There is space in the trimmed left hand side of the backing, so we
      // can just write it there.
      return new ImmutableDeque<E>(_offset - 1, _backing.atPut(_offset - 1, value), true);
    }

    ImmutableArray<E> new_backing = _backing._newWithSpaceOnLeft();
    long extra_space = new_backing.size - _backing.size;
    return new ImmutableDeque<E>(extra_space - 1, new_backing.atPut(extra_space - 1, value), true);
  }

  public ImmutableDeque<E> unshiftAll(E array[]) {
    return unshiftAll(Arrays.asList(array));
  }

  @SuppressWarnings("unchecked")
  public ImmutableDeque<E> unshiftAll(Collection<? extends E>collection) {
    if (collection.isEmpty()) return this;
    List<? extends E> list = new ArrayList<>(collection);
    ListIterator<? extends E> lit = list.listIterator(list.size());
    ImmutableDeque<E> current = this;
    while (lit.hasPrevious()) {
      current = current.unshift((E)lit.previous());
    }
    return current;
  }

  public int indexOf(E object) {
    long result = _backing.indexOf(object, _offset);
    return result == -1 ? -1 : ImmutableCollection._longTruncator(result - _offset);
  }

  public int lastIndexOf(E object) {
    long result = _backing.lastIndexOf(object, _offset);
    return result == -1 ? -1 : ImmutableCollection._longTruncator(result - _offset);
  }

  public Object clone() {
    return this;
  }

  public ImmutableDeque<E> trim() {
    return trim(1);
  }

  public ImmutableDeque<E> trim(long by) {
    if (by > longSize()) throw new IndexOutOfBoundsException();
    if (by == longSize()) return new ImmutableDeque<E>(0, new ImmutableArray<E>());
    return new ImmutableDeque<E>(_offset, _backing.trim(by), true);
  }

  public ListIterator<E> listIterator() {
    return _backing.listIterator(_offset, _offset);
  }

  public ListIterator<E> listIterator(int index) {
    if (index < 0) throw new IndexOutOfBoundsException();
    return _backing.listIterator(index + _offset, _offset);
  }

  public ListIterator<E> listIterator(long index) {
    if (index < 0) throw new IndexOutOfBoundsException();
    return _backing.listIterator(index + _offset, _offset);
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
    if (from < 0 || _offset + from > _backing.size) throw new IndexOutOfBoundsException();
    if (_offset + from == _backing.size) return new ImmutableDeque<E>(0, new ImmutableArray<E>());
    return new ImmutableDeque<E>(_offset + from, _backing);
  }

  public ImmutableDeque<E> subList(int from, int to) {
    return subList((long)from, to);
  }

  public ImmutableDeque<E> subList(long from, long to) {
    if (to == longSize()) return subList(from);
    return new ImmutableDeque<E>(_offset + from, _backing.trim(longSize() - to));
  }
}

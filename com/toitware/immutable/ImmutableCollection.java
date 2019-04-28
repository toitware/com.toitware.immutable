// Copyright (C) 2019 Toitware ApS. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.toitware.immutable;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.function.Consumer;

public abstract class ImmutableCollection<E> extends AbstractCollection<E> implements Iterable<E> {
  abstract public long longSize();
  abstract public E get(int index);
  abstract public E get(long index);
  abstract public long indexOf(E value);
  abstract public long lastIndexOf(E value);
  abstract public ImmutableCollection<E> atPut(long index, E value);
  abstract public ImmutableCollection<E> push(E value);
  abstract public ImmutableCollection<E> push(E value1, E value2);
  abstract public ImmutableCollection<E> pushAll(E array[]);
  abstract public ImmutableCollection<E> pushAll(Collection<? extends E> collection);
  abstract public ImmutableCollection<E> trim();
  abstract public ImmutableCollection<E> trim(long by);
  abstract public ImmutableCollection<E> shift();
  abstract public ImmutableCollection<E> subList(long from);
  abstract public ImmutableCollection<E> subList(long from, long to);
}

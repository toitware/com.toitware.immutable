// Copyright (C) 2019 Toitware ApS. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.toitware.immutable;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.Iterator;
import java.util.ListIterator;

/** An immutable (fully persistent) list with O(log size) access to any
 *  element.  A new list that differs at one position from this
 *  ImmutableCollection can be made in O(log size) time.  The b tree that backs
 *  the ImmutableCollectionis always kept as shallow as possible on the right
 *  hand side though its max depth is still bounded by the log of the size,
 *  rounded up.  This means that a new list, one longer than the current one
 *  can be made in amortized O(1) time using the push() method.  (This is
 *  almost true - the push() method is currently O(log size), but you would
 *  have to have a 128 bit implementation of the Java Language to notice.  You
 *  don't.)<p>
 *
 *  The instances are GC safe in the sense that they do not keep elements alive
 *  that cannot be reached after atPut(), trim() or subList().  The subList
 *  method is fast, as if it was merely creating a view over the original
 *  collection, but unlike subList() on java.util.ArrayList, the elements
 *  referenced by the original ArrayList are not kept alive.  It is the
 *  intention that subList() be used to iterate over a part of the
 *  ImmutableCollection.<p>
 *
 *  Methods that modify ArrayList do not have similarly named methods in this
 *  class.  This is to remind you that you need to use the return value, which
 *  is a new ImmutableCollection.<p>
 *
 *  This abstract class defines the interface, but new instances will normally
 *  be created by the constructors of ImmutableArray.  If you only append or
 *  trim from the right hand side of the ImmutableArray then you will continue
 *  to get ImmutableArray elements, but if you remove elements from the left
 *  hand side then you will get instances of ImmutableDeque, which are slightly
 *  less efficient to create, having an extra level of indirection.
 *
 *  @see ImmutableArray
 */
public abstract class ImmutableCollection<E> extends AbstractCollection<E> implements Iterable<E> {
  /** The number of elements in the ImmutableCollection.
   *  To conform to the AbstractCollection interface this returns an int, 
   *  see also the method longSize().
   *  @return The number of elements in the ImmutableCollection or
   *      Integer.MAX_VALUE if the ImmutableCollection is too large.
   */
  abstract public int size();

  /** The number of elements in the ImmutableCollection.
   *  @return The number of elements in the ImmutableCollection
   */
  abstract public long longSize();

  /** Create an iterator over the entire ImmutableCollection.  Since the
   *  ImmutableCollection cannot be mutated, there is no issue of what happens
   *  if the underlying collection is mutated during iteration.
   *  @return A fresh iterator that does not implement remove().
   */
  abstract public Iterator<E> iterator();

  /** Create a list iterator over the entire ImmutableCollection.  Since the
   *  ImmutableCollection cannot be mutated, there is no issue of what happens
   *  if the underlying collection is mutated during iteration.
   *  @return A fresh list iterator that does not implement remove().
   */
  abstract public ListIterator<E> listIterator();

  /** Create a list iterator over the entire ImmutableCollection.  Since the
   *  ImmutableCollection cannot be mutated, there is no issue of what happens
   *  if the underlying collection is mutated during iteration.
   *  @param index The starting point, which must be between 0 and the size,
   *      inclusive().
   *  @return A fresh list iterator that does not implement remove().
   */
  abstract public ListIterator<E> listIterator(int index);

  /** Create a list iterator over the entire ImmutableCollection.  Since the
   *  ImmutableCollection cannot be mutated, there is no issue of what happens
   *  if the underlying collection is mutated during iteration.
   *  @param index The starting point, which must be between 0 and the size,
   *      inclusive().
   *  @return A fresh list iterator that does not implement remove().
   */
  abstract public ListIterator<E> listIterator(long index);

  /** Get an arbitrary element of the ImmutableCollection. Takes an average
   *  time of O(log size).
   *  @param index The index of the required element.
   *  @return The element.
   */
  abstract public E get(int index);

  /** Get an arbitrary element of the ImmutableCollection in an average time of
   *  O(log size).
   *  @param index The index of the required element.
   *  @return The element.
   */
  abstract public E get(long index);

  /** Find the first element that is equal to the needle, using equals().
   *  Unlike the method of the same name on ArrayList, this one returns long.
   *  @param needle The element to be found
   *  @return The index of the found element, or -1 if the needle is not found.
   */
  abstract public long indexOf(E needle);

  /** Find the last element that is equal to the needle, using equals().
   *  Unlike the method of the same name on ArrayList, this one returns long.
   *  TODO: Has complexity O(n log n), fix to be O(n).
   *  @param needle The element to be found
   *  @return The index of the found element, or -1 if the needle is not found.
   */
  abstract public long lastIndexOf(E needle);

  /** Since the collection is immutable there is no reason to actually build a
   *  clone.
   *  @return The receiver.
   */
  abstract public Object clone();

  /** Makes a copy of an ImmutableCollection, but at the given index the given
   *  value is substituted.  Takes on average O(log size) time unless the
   *  element is the last one, in which case it is O(1).  Note that the
   *  original ImmutableCollection is left unchanged, so if you ignore the
   *  return value there is no effect.
   *  @param index Offset of the element where substitution should happen
   *  @param value The new element to be placed at offset index
   *  @return A new ImmutableCollection differing at one position from this one.
   */
  abstract public ImmutableCollection<E> atPut(long index, E value);

  /** Create a new ImmutableCollection that differs from the original one only
   *  by having an extra element.  On average, time taken is O(1), almost, but
   *  it has to allocate and initialize an object that has log2(size)/3 machine
   *  words.
   *  @param value The new element to be placed at the end of the new
   *      ImmutableCollection
   *  @return A new ImmutableCollection, one element larger
   */
  abstract public ImmutableCollection<E> push(E value);

  /** Create a new ImmutableCollection that differs from the original one only
   *  by having two extra elements.  On average, time taken is O(1), almost,
   *  but it has to allocate and initialize an object that has log2(size)/3
   *  machine words.  This is more efficient than calling push(E value) twice
   *  if the ImmutableCollection has even size.
   *  @param value1 The new element to be placed almost at the end of the new
   *      ImmutableCollection
   *  @param value2 The new element to be placed the end of the new
   *      ImmutableCollection
   *  @return A new ImmutableCollection, two elements larger
   */
  abstract public ImmutableCollection<E> push(E value1, E value2);

  /** Create a new ImmutableCollection that is the current ImmutableCollection
   *  with all elements of an array pushed on the end.  May return itself if
   *  the provided array is empty.  Takes time O(array.length).
   *  @param array The array whose elements should be appended to this
   *      ImmutableCollection.
   *  @return A new ImmutableCollection with the extra elements.
   */
  abstract public ImmutableCollection<E> pushAll(E array[]);

  /** Create a new ImmutableCollection that is the current ImmutableCollection
   *  with all elements of a collection pushed on the end.  May return itself
   *  if the provided collection is empty.  Takes time O(array.length).
   *  @param collection The collection whose elements should be appended to
   *      this ImmutableCollection.
   *  @return A new ImmutableCollection with the extra elements.
   */
  abstract public ImmutableCollection<E> pushAll(Collection<? extends E> collection);

  /** Create a new ImmutableCollection without the last element of this.
   *  Time taken is on average O(1).  GC safe in the sense that the discarded
   *  last element is not kept alive by the new ImmutableCollection.
   *  @return A new ImmutableCollection without the last element of this
   */
  abstract public ImmutableCollection<E> trim();

  /** Create a new ImmutableCollection without the last n elements of this.
   *  Time taken is on average O(log by).  GC safe in the sense that the
   *  discarded elements are not kept alive by the new ImmutableCollection.
   *  @param by The number of elments to discard from the end of this
   *  @return A new ImmutableCollection without the last n elements of this
   */
  abstract public ImmutableCollection<E> trim(long by);

  /** Create a new ImmutableCollection without the first element of this.
   *  Time taken is on average O(log size).  GC safe in the sense that the
   *  discarded first element is not kept alive by the new ImmutableCollection.
   *  @return A new ImmutableCollection without the first element of this.
   */
  abstract public ImmutableCollection<E> shift();

  /** Create a new ImmutableCollection without the first n elements of this.
   *  Time taken is on average O(log size).  GC safe in the sense that the
   *  discarded elements are not kept alive by the new ImmutableCollection.
   *  @param from The number of elements to discard from the start of this.
   *  @return A new ImmutableCollection without the first n elements of this.
   */
  abstract public ImmutableCollection<E> subList(long from);

  /** Create a new ImmutableCollection without the first n and last m elements
   *  of this.  Time taken is on average O(log size).  GC safe in the
   *  sense that the discarded elements are not kept alive by the new
   *  ImmutableCollection.
   *  @param from The number of elements to discard from the start of this.
   *  @param to Elements at this position and later will be discarded.
   *  @return A new ImmutableCollection without the first n and last m elements
   *      of this.
   */
  abstract public ImmutableCollection<E> subList(long from, long to);
}

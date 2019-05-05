// Copyright (C) 2019 Toitware ApS. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.toitware.immutable;

import com.toitware.immutable.ImmutableCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.Iterator;

/** A class for creating new ImmutableCollections.  The iterators of
 *  immutable collections do not implement remove() and set().  Instead, create
 *  an instance of RebuildIterator with ImmutableCollection.rebuildIterator().
 *  This iterator implements remove(), set(), insert() and insertAll(), but
 *  they have no effect on the original collection.  Instead, when the
 *  iteration is complete, a new immutable collection can be created from this
 *  RebuildIterator with build().  This iterator is itself a collection so as
 *  an alternative to the build() method you can build other collections by
 *  giving it as an argument to their constructors.  Building an
 *  ImmutableCollection in this way may be less efficient than using build().
 */
public class RebuildIterator<E> extends AbstractCollection<E> implements Iterator<E> {
  // Size of the new collection.
  private long _size = 0;

  public int size() {
    return ImmutableCollection._longTruncator(_size);
  }

  // The collection whose parts we are using to build the new one.
  private final Collection<E> _bricks;

  // Instructions for building the new collection.
  private byte _instructions[];
  private int _instructionsPosn = 0;

  // New things to insert into the new collection.
  private ArrayList<Object> _newBricks;

  private static final int COPY = 0;
  private static final int SKIP = 1;
  private static final int INSERT = 2;
  private static final int INSERT_ALL = 3;

  // The iterator over the bricks.
  private Iterator<E> _iterator;

  public RebuildIterator(Collection<E> bricks) {
    _bricks = bricks;
    _iterator = bricks.iterator();
  }

  private void _push(int instruction) {
    if (_instructions == null) {
      _instructions = new byte[16];
    } else if (_instructionsPosn >= _instructions.length) {
      _instructions = Arrays.copyOf(_instructions, (int)(_instructions.length * 1.4));
    }
    _instructions[_instructionsPosn++] = (byte)instruction;
  }

  private void _revise(int instruction) {
    int last = _instructionsPosn - 1;
    assert _instructions[last] == COPY;
    _instructions[last] = (byte)instruction;
  }

  private void _addNewBrick(Object o) {
    if (_newBricks == null) _newBricks = new ArrayList<Object>();
    _newBricks.add(o);
  }

  public E next() {
    _size++;
    _push(COPY);
    return _iterator.next();
  }

  public boolean hasNext() { return _iterator.hasNext(); }

  public void forEachRemaining(Consumer<? super E> action) {
    _iterator.forEachRemaining((x)-> {
      _size++;
      _push(COPY);
      action.accept(x);
    });
  }

  /** Removes the element most recently returned by next().  The element is
   *  removed from the collection being built by this rebuilder, not from the
   *  original collection. You can only call this if you have called next at
   *  least once, and you have not called set() or remove() since the last call
   *  to next().
   */
  public void remove() {
    _size--;
    _revise(SKIP);
  }

  /** Replaces the element most recently returned by next().  The given element
   *  is used for the collection being built, instead of the element returned
   *  by next().  You can only call this if you have called next at least once,
   *  and you have not called set() or remove() since the last call to next().
   *  @param replacement The element which should replace the result of the last
   *  call to next() at this point in the result of build().
   */
  public void set(E replacement) {
    _revise(SKIP);
    _push(INSERT);
    _addNewBrick(replacement);
  }

  /** Inserts a new element into the collection being built.  This can be called
   *  at any time.  The inserted element appears after the one most recently
   *  returned by next() or at the start if next() has not yet been called.
   *  @param element The element to be inserted into the result of build().
   */
  public void insert(E element) {
    _size++;
    _push(INSERT);
    _addNewBrick(element);
  }

  /** Inserts a collection of new elements into the collection being built.
   *  This can be called at any time.  The inserted elements appear after the
   *  one most recently returned by next() or at the start if next() has not
   *  yet been called.
   *  @param collection The collection which will be flattened into the result
   *  of build() at this point.
   */
  public void insertAll(Collection<? super E> collection) {
    int extra = collection.size();
    if (extra != 0) {
      _size += extra;
      _push(INSERT_ALL);
      _addNewBrick(collection);
    }
  }

  /** Inserts an array of new elements into the collection being built.
   *  This can be called at any time.  The inserted elements appear after the
   *  one most recently returned by next() or at the start if next() has not
   *  yet been called.
   *  @param array The array which will be flattened into the result at of
   *  build() at this point.
   */
  public void insertAll(E array[]) {
    _size += array.length;
    _push(INSERT_ALL);
    _addNewBrick(Arrays.asList(array));
  }

  /** Create an iterator over result of the builder.
   *  @return An iterator that can be used to build a new collection based on
   *  the iteration.
   */
  public Iterator<E> iterator() {
    return new BuilderIterator<E>(this);
  }

  /** Create a new immutable array based on the calls to remove(), set(),
   *  insert() and insertAll() done on this iterator.
   *  @return A new ImmutableCollection based on the items iterated over.
   */
  public ImmutableArray<E> build() {
    return new ImmutableArray<E>(this);
  }

  private class BuilderIterator<E> implements Iterator<E> {
    private long _instructionIndex = 0;
    private long _index = 0;
    private RebuildIterator<E> _buildIterator;

    // For iterating within INSERT_ALL brick collections.
    private Iterator<E> _insertAllIterator;
    private Iterator<E> _brickIterator;
    private Iterator<E> _newBrickIterator;

    BuilderIterator(RebuildIterator<E> builder) {
      _buildIterator = builder;
    }

    public boolean hasNext() {
      return _insertAllIterator != null ||
             _instructionIndex < _buildIterator._instructionsPosn;
    }

    @SuppressWarnings("unchecked")
    public E next() {
      if (_insertAllIterator != null) {
        E result = _insertAllIterator.next();
        if (!_insertAllIterator.hasNext()) _insertAllIterator = null;
        return result;
      }
      if (_brickIterator == null) _brickIterator = _buildIterator._bricks.iterator();
      int instruction = _instructions[(int)(_instructionIndex++)];
      while (instruction == SKIP) {
        _brickIterator.next();
        instruction = _instructions[(int)(_instructionIndex++)];
      }
      if (instruction == COPY) {
        return _brickIterator.next();
      } else {
        if (_newBrickIterator == null) {
          _newBrickIterator = (Iterator<E>)_buildIterator._newBricks.iterator();
        }
        if (instruction == INSERT) {
          return _newBrickIterator.next();
        } else {
          assert instruction == INSERT_ALL;
          _insertAllIterator = ((Collection<E>)_newBrickIterator.next()).iterator();
          E result = _insertAllIterator.next();
          if (!_insertAllIterator.hasNext()) _insertAllIterator = null;
          return result;
        }
      }
    }
  }
}

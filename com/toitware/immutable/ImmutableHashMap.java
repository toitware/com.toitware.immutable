// Copyright (C) 2019 Toitware ApS. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.toitware.immutable;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.BiConsumer;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

// An efficient immutable HashMap implemented using an ImmutableArray and an
// AtomicIntegerArray as backing.  The AtomicIntegerArray is a mutable
// datastructure, but the interface presented by this HashMap is immutable.
// Iteration is always in insertion order.
public class ImmutableHashMap<K, V> {
  public ImmutableHashMap() {
    _size = 0;
    _backing = null;
    _index = null;
  }

  private ImmutableHashMap(int size, ImmutableArray<Object> backing, AtomicIntegerArray index) {
    _size = size;
    _backing = backing;
    _index = index;
    assert(((index.length() - 1) & (index.length()  - 2)) == 0);
  }

  public Object clone() {
    return this;
  }

  public int size() { return _size; }
  private final int _size;
  // Immutable alternating keys and values in insertion order.
  private final ImmutableArray<Object> _backing;
  // An array of atomic integers, where each slot is a combination of hash
  // code and index into the _backing array.  The first entry is reserved for
  // the number of slots that are in use.
  private AtomicIntegerArray _index;

  private static final int _MAX_ENTRIES = 0xffff;
  private static final int _HASH_MASK = 0xffff;
  private static final int _MAX_INDEX_SIZE = 0x20000;
  private static final int _FREE = 0;
  private static final Object _DELETED_KEY = new ImmutableHashMap();

  private int _combination(int hash, int index) {
    assert(index < _MAX_ENTRIES);
    return (hash << 16) | (index + 1);
  }
  private int _hashAt(int slot) { return _index.get(slot + 1) >>> 16; }
  private int _indexAt(int slot) { return (_index.get(slot + 1) & 0xffff) - 1; }
  private int _indexMask() {
    int result = _index.length() - 2;
    assert((result & (result + 1)) == 0);
    return result;
  }
  private boolean _isFree(int slot) { return _index.get(slot + 1) == _FREE; }
  private boolean _matches(K key, int hash, int slot) {
    assert(!_isFree(slot));
    if ((hash & _HASH_MASK) != _hashAt(slot)) return false;
    //System.out.println("hash matched");
    int index = _indexAt(slot) * 2;
    //System.out.println("Trying index " + index + " on backing sized " + _backing.size);
    if (index >= _backing.size) return false;
    //System.out.println("index in range");
    if (!_backing.get(index).equals(key)) return false;
    //System.out.println("equals succeded");
    return true;
  }
  private boolean _takeFreeSlot(int index, int hash, int slot) {
    boolean success = _index.compareAndSet(slot + 1, 0, _combination(hash, index));
    //System.out.println("Trying to take free slot at " + slot + " success " + success);
    if (success) _index.incrementAndGet(0);
    return success;
  }
  private int _usedSlots() {
    return _index.get(0);
  }

  public boolean isEmpty() {
    return _size == 0;
  }

  public V get(K key) {
    return getOrDefault(key, null);
  }

  public @SuppressWarnings("unchecked") V getOrDefault(K key, V default_value) {
    if (_index == null) return default_value;  // Empty map.
    int hash = key.hashCode();
    int slot = hash & _indexMask();
    int step = 1;
    while (true) {
      if (_isFree(slot)) return default_value;  // No match.
      if (_matches(key, hash, slot)) {
        return (V)_backing.get(_indexAt(slot) * 2 + 1);
      }
      int new_slot = (slot + step) & _indexMask();
      step++;
      if (slot == new_slot) return default_value;  // Searched full table.
      slot = new_slot;
    }
  }

  public boolean containsKey(K key) {
    if (_index == null) return false;  // Empty map.
    int hash = key.hashCode();
    int slot = hash & _indexMask();
    int step = 1;
    while (true) {
      //System.out.println("containsKey probes " + slot);
      if (_isFree(slot)) return false;  // No match.
      if (_matches(key, hash, slot)) return true;
      int new_slot = (slot + step) & _indexMask();
      step++;
      if (slot == new_slot) return false;  // Searched full table.
      slot = new_slot;
    }
  }

  public ImmutableHashMap<K, V> remove(K key) {
    if (_index == null) return this;  // Empty map.
    int hash = key.hashCode();
    int slot = hash & _indexMask();
    int step = 1;
    while (true) {
      if (_isFree(slot)) return this;  // No match.
      if (_matches(key, hash, slot)) {
        // Backing is immutable so we need to create a new one.  This is an
        // O(log size) operation.
        ImmutableArray<Object> new_backing = _backing.atPut(_indexAt(slot) * 2, _DELETED_KEY);
        new_backing = new_backing.atPut(_indexAt(slot) * 2 + 1, null);
        return new ImmutableHashMap<K, V>(_size - 1, new_backing, _index);
      }
      int new_slot = (slot + step) & _indexMask();
      step++;
      if (slot == new_slot) return this;  // Searched full table.
      slot = new_slot;
    }
  }

  public ImmutableHashMap<K, V> put(K key, V value) {
    return _put(key, value, false, false);
  }

  public ImmutableHashMap<K, V> putIfAbsent(K key, V value) {
    return _put(key, value, true, false);
  }

  public ImmutableHashMap<K, V> replace(K key, V value) {
    return _put(key, value, false, true);
  }

  static final long REBUILD = 0;       // Rebuild index and retry.
  static final long APPEND = 1;        // Append key-value pair.
  static final long DO_NOTHING = 2;    // Return this (no change).
  static final long INDEX_OFFSET = 3;  // Value was overwritten at given index.

  private ImmutableHashMap<K, V> _put(K key, V value, boolean only_if_absent, boolean only_if_present) {
    long result = _insert(_backing == null ? 0 : _backing.size, key, value, only_if_absent, only_if_present, true);
    if (result == REBUILD) {
      return _rebuild_index()._put(key, value, only_if_absent, only_if_present);
    } else if (result == APPEND) {
      // Backing is an immutable array, create a new one.  This is almost an O(1) operation.
      ImmutableArray<Object> new_backing = _backing.push(key, value);
      return new ImmutableHashMap<K, V>(_size + 1, new_backing, _index);
    } else if (result == DO_NOTHING) {
      return this;
    } else {
      long index = result - INDEX_OFFSET;
      ImmutableArray<Object> new_backing = _backing.atPut(index * 2 + 1, value);
      return new ImmutableHashMap<K, V>(_size, new_backing, _index);
    }
  }

  private long _insert(long backing_size, K key, V value, boolean only_if_absent, boolean only_if_present, boolean check_for_oversized_backing) {
    if (_index == null) {
      if (only_if_present) return DO_NOTHING;
      // A new ImmutableHashMap with no slots must be 'rebuilt' before entries
      // can be added.
      //System.out.println("Empty map, rebuilding");
      return REBUILD;
    }
    int used = _usedSlots();
    if (check_for_oversized_backing) {
      if (used + (used >> 2) >= _index.length()
          || (backing_size > used + 2 && backing_size > used * 3)
          || backing_size == _MAX_ENTRIES * 2) {
        // If there is not 1.25 times as much space as we need, rebuild with more space.
        // Also rebuild when the backing is clogged with deleted entries.
        //if (used + (used >> 2) >= _index.length()) System.out.println("Used of " + used + " too much for index length of " + _index.length());
        //if (backing_size > used * 3) System.out.println("backing_size of " + backing_size + " too much for used of " + used);
        return REBUILD;
      }
    }
    int hash = key.hashCode();
    int slot = hash & _indexMask();
    int step = 1;
    while (true) {
      //System.out.println("Probing for key " + key + " slot " + slot + " indexMask " + _indexMask());
      if (_isFree(slot)) {
        if (only_if_present) return DO_NOTHING;
        // Found free slot for new entry.
        int index = (int)(backing_size >>> 1);
        // Try to add entry.
        boolean success = _takeFreeSlot(index, hash, slot);
        if (!success) {
          // If the compare and swap didn't work then some other thread is
          // intensively updating the index.  We make a new index, which is
          // ours alone and retry.
          //System.out.println("CAS failed");
          return REBUILD;
        }
        if (_size == _MAX_ENTRIES) {
          throw new UnsupportedOperationException();
        }
        // Backing is an immutable array, create a new one.  This is almost an O(1) operation.
        return APPEND;
      }
      if (_matches(key, hash, slot)) {
        if (only_if_absent) return DO_NOTHING;
        return INDEX_OFFSET + _indexAt(slot);
      }
      int new_slot = (slot + step) & _indexMask();
      step++;
      if (slot == new_slot || used != _usedSlots()) {
        // Searched full table or a slot was taken while we searched.  This
        // must mean some other thread is taking free slots.  We make a new
        // index, which is ours alone and retry.
        //if (slot == new_slot) System.out.println("Search wrapped around");
        //if (used != _usedSlots()) System.out.println("Someone interfered with the usedness");
        return REBUILD;
      }
      slot = new_slot;
    }
  }

  // Called entrySet to match the method in java.util.HashMap, but this is just a
  // collection, which you can iterate over.  Iteration is in insertion order.
  // Removing a key and reinserting it puts it at the end of the iteration order.
  public Collection<Map.Entry<K, V>> entrySet() {
    return new KeyValues<K, V>(this);
  }

  // Called keySet to match the method in java.util.HashMap, but this is just a
  // collection, which you can iterate over.  Iteration is in insertion order.
  // Removing a key and reinserting it puts it at the end of the iteration order.
  public Collection<K> keySet() {
    return new Keys<K>(this);
  }

  public Collection<V> values() {
    return new Values<V>(this);
  }

  public boolean containsValue(V value) {
    if (_index == null) return false;
    boolean k = true;
    for (Object o : _backing) {
      if (k) {
        k = false;
      } else {
        if ((o == null && value == null) || o.equals(value)) return true;
        k = true;
      }
    }
    return false;
  }

  private @SuppressWarnings("unchecked") ImmutableHashMap<K, V> _rebuild_index() {
    // Get a power of 2 1.7 to 3.4 times larger.
    int index_size = (int)((_size + 2) * 1.7);
    index_size |= index_size >> 1;
    index_size |= index_size >> 2;
    index_size |= index_size >> 4;
    index_size |= index_size >> 8;
    index_size |= index_size >> 16;
    index_size++;
    assert(index_size >= _size);
    if (index_size > _MAX_INDEX_SIZE) throw new UnsupportedOperationException();
    // Determine whether there are so many deleted elements in the backing
    // store that we need to rebuild it to squeeze them out.
    boolean squeeze = _backing == null || (_backing.size > _size * 2 + 4 && _backing.size > (long)(_size * 2 * 1.2));
    AtomicIntegerArray new_index = new AtomicIntegerArray(index_size + 1);
    ImmutableHashMap<K, V> new_map = squeeze ?
        new ImmutableHashMap<K, V>(0, new ImmutableArray<>(), new_index) :
        new ImmutableHashMap<K, V>(_size, _backing, new_index);
    long count = 0;
    K key = null;
    if (_backing != null) {
      for (Object o : _backing) {
        if ((count & 1) == 0) {
          key = (K)o;
        } else {
          if (_DELETED_KEY != key) {
            if (squeeze) {
              // This should never call _rebuild_index because the index is big
              // enough and there is no contention, since no other threads have
              // access to the new index yet.
              new_map = new_map.put(key, (V)o);
            } else {
              long action = new_map._insert(count, key, (V)o, false, false, false);
              // We are reusing the backing so the key and value are already appended.
              assert(action == APPEND);
            }
          }
        }
        count++;
      }
    }
    return new_map;
  }

  private class Keys<K> extends AbstractCollection <K> {
    private ImmutableHashMap<K, ?> _map;
    public Keys(ImmutableHashMap<K, ?> map) {
      _map = map;
    }
    public int size() { return _map.size(); }
    public Iterator<K> iterator() { return new KeyIterator<K>(this); }
  }

  private class KeyIterator<K> implements Iterator<K> {
    private int _index;
    private int _limit;
    private Iterator<Object> _backing_iterator;

    public KeyIterator(Keys<K> keys) {
      _index = 0;
      _limit = keys._map.size();
      ImmutableArray<Object> backing = keys._map._backing;
      _backing_iterator = backing == null ? null : backing.iterator();
    }

    public boolean hasNext() {
      return _index < _limit;
    }

    public @SuppressWarnings("unchecked") K next() {
      while (true) {
        K key = (K)_backing_iterator.next();
        _backing_iterator.next();  // Consume value.
        if (_DELETED_KEY != key) {
          _index++;
          return key;
        }
      }
    }
  }

  private class Values<V> extends AbstractCollection <V> {
    private ImmutableHashMap<?, V> _map;
    public Values(ImmutableHashMap<?, V> map) {
      _map = map;
    }
    public int size() { return _map.size(); }
    public Iterator<V> iterator() { return new ValueIterator<V>(this); }
  }

  private class ValueIterator<V> implements Iterator<V> {
    private int _index;
    private int _limit;
    private Iterator<Object> _backing_iterator;

    public ValueIterator(Values<V> values) {
      _index = 0;
      _limit = values._map.size();
      ImmutableArray<Object> backing = values._map._backing;
      _backing_iterator = backing == null ? null : backing.iterator();
    }

    public boolean hasNext() {
      return _index < _limit;
    }

    public @SuppressWarnings("unchecked") V next() {
      while (true) {
        Object key = _backing_iterator.next();
        V value = (V)_backing_iterator.next();
        if (_DELETED_KEY != key) {
          _index++;
          return value;
        }
      }
    }
  }

  private class KeyValues<K, V> extends AbstractCollection<Map.Entry<K, V>> {
    private ImmutableHashMap<K, V> _map;
    public KeyValues(ImmutableHashMap<K, V> map) {
      _map = map;
    }
    public int size() { return _map.size(); }
    public Iterator<Map.Entry<K, V>> iterator() { return new KeyValueIterator<K, V>(this); }
  }

  private class KeyValueIterator<K, V> implements Iterator<Map.Entry<K, V>> {
    private int _index;
    private int _limit;
    private Iterator<Object> _backing_iterator;

    public KeyValueIterator(KeyValues<K, V> keyValues) {
      _index = 0;
      _limit = keyValues._map.size();
      ImmutableArray<Object> backing = keyValues._map._backing;
      _backing_iterator = backing == null ? null : backing.iterator();
    }

    public boolean hasNext() {
      return _index < _limit;
    }

    public @SuppressWarnings("unchecked") Map.Entry<K, V> next() {
      while (true) {
        K key = (K)_backing_iterator.next();
        V value = (V)_backing_iterator.next();
        if (_DELETED_KEY != key) {
          _index++;
          return new AbstractMap.SimpleImmutableEntry<K, V>(key, value);
        }
      }
    }
  }

  public @SuppressWarnings("unchecked") void forEach(BiConsumer<? super K, ? super V> action) {
    long count = 0;
    K key = null;
    for (Object o : _backing) {
      if ((count & 1) == 0) {
        key = (K)o;
      } else {
        if (_DELETED_KEY != key) {
          action.accept(key, (V)o);
        }
      }
      count++;
    }
  }
}

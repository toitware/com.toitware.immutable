// Copyright (C) 2019 Toitware ApS. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.toitware.immutable_test;
import com.toitware.immutable.ImmutableArray;
import com.toitware.immutable.ImmutableArrayIterator;

class ImmutableArrayTest {
  public static void main(String args[]) {
    ImmutableArray<Integer> empty = new ImmutableArray<Integer>();
    assert(empty.size() == 0);
    assert(empty.isEmpty());
    for (int i : empty) {
      assert(false);
    }
    ImmutableArray<Integer> ft = empty.push(42);
    assert(!empty.contains(42));
    assert(empty.indexOf(42) == -1);
    assert(ft.size() == 1);
    assert(ft.size == 1);
    assert(!ft.isEmpty());
    assert(ft.get(0) == 42);
    assert(ft.contains(42));
    assert(ft.indexOf(42) == 0);
    for (int i : ft) {
      assert(i == 42);
    }
    assert(empty.size() == 0);
    assert(empty.size == 0);
    ImmutableArray<Integer> two_a = ft.push(103);
    ImmutableArray<Integer> two_b = ft.push(7);
    assert(two_a.size() == 2);
    assert(two_a.size == 2);
    assert(two_b.size() == 2);
    assert(two_b.size == 2);
    assert(two_a.get(0) == 42);
    assert(two_b.get(0) == 42);
    assert(two_a.get(1) == 103);
    assert(two_b.get(1) == 7);
    ImmutableArray<Integer> p = new ImmutableArray<Integer>();
    for (int i = 0; i < 10; i++) {
      p = p.push(i * i);
    }
    assert(p.size() == 10);
    assert(p.size == 10);
    Object[] as_array = p.toArray();
    Integer[] typed_array = new Integer[11];
    p.toArray(typed_array);
    for (int i = 0; i < 10; i++) {
      assert(p.get(i) == i * i);
      assert(i * i == (Integer)as_array[i]);
      assert(i * i == typed_array[i]);
    }
    for (int i = 10; i < 100; i++) {
      p = p.push(i * i);
    }
    assert(p.indexOf(36) == 6);
    assert(p.size() == 100);
    for (int i = 0; i < 100; i++) {
      assert(p.get(i) == i * i);
    }
    for (int i = 100; i < 1000; i++) {
      p = p.push(i * i);
    }
    assert(p.size() == 1000);
    for (int i = 0; i < 1000; i++) {
      assert(p.get(i) == i * i);
    }
    assert(p.indexOf(196) == 14);
    ImmutableArray<Integer> p2 = p.atPut(42, 42);
    assert(p2.size() == p.size());
    assert(p2.get(42) == 42);
    ImmutableArray<Integer> p3 = p;
    for (int i = 0; i < 1000; i++) {
      assert(p2.get(i) == (i == 42 ? 42 : i * i));
      assert(p.get(i) == i * i);
      p3 = p3.atPut(i, i * i * i);
    }
    for (int i = 0; i < 1000; i++) {
      assert(p3.get(i) == i * i * i);
    }
    int j = 0;
    for (int i : p3) {
      assert(i == j * j * j);
      j++;
    }
    for (int i = 1000; i < 100000; i++) {
      p = p.push(i * i);
    }
    assert(p.size() == 100000);
    assert(p.size == 100000);
    for (int i = 0; i < 100000; i++) {
      assert(p.get(i) == i * i);
    }

    j = 0;
    for (int i : p) {
      assert(i == j * j);
      j++;
    }
  }
}

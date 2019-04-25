// Copyright (C) 2019 Toitware ApS. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.toitware.immutable_test;
import com.toitware.immutable.ImmutableArray;
import java.util.Arrays;
import java.util.Random;

class ImmutableArrayTest {
  public static void main(String args[]) {
    simple_test();
    random_test();
    push_all_test();
  }

  private static void random_test() {
    final int ITERATIONS = 1000;
    Random random = new Random(1034210342);
    for (int z = 0; z < ITERATIONS; z++) {
      int len = random.nextInt(100);
      ImmutableArray<Integer> a = new ImmutableArray<Integer>();
      for (int i = 0; i < len; i++) {
        a = a.push(random.nextInt(120));
      }
      ImmutableArray<Integer> trimmed = a.trim(random.nextInt((int)(a.size + 1)));
      for (int i = 0; i < trimmed.size; i++) {
        assert(trimmed.get(i) == a.get(i));
      }
    }
  }

  private static void simple_test() {
    ImmutableArray<Integer> empty = new ImmutableArray<>();
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
    ImmutableArray<Integer> p = new ImmutableArray<>();
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

    for (int i = 0; i <= 10; i++) {
      ImmutableArray trimmed = p.trim(i);
      assert(trimmed.size == p.size - i);
      for (int j = 0; j < trimmed.size; j++) {
        assert(trimmed.get(j) == p.get(j));
      }
    }

    int iBox[] = new int[1];
    p.forEach((x)-> {
      assert(x == iBox[0] * iBox[0]);
      iBox[0]++;
    });
    assert(iBox[0] == 10);

    for (int i = 10; i < 100; i++) {
      p = p.push(i * i);
    }
    assert(p.indexOf(36) == 6);
    assert(p.size() == 100);
    for (int i = 0; i < 100; i++) {
      assert(p.get(i) == i * i);
    }
    for (int i = 100; i < 1000; i += 2) {
      p = p.push(i * i, (i + 1) * (i + 1));
    }
    assert(p.size() == 1000);
    for (int i = 0; i < 1000; i++) {
      assert(p.get(i) == i * i);
    }

    iBox[0] = 0;
    p.forEach((x)-> {
      assert(x == iBox[0] * iBox[0]);
      iBox[0]++;
    });
    assert(iBox[0] == 1000);

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

  private static ImmutableArray<Integer> factory(int n) {
    ImmutableArray<Integer> a = new ImmutableArray<>();
    for (int i = 0; i < n; i++) a = a.push(i * i);
    return a;
  }

  private static void push_all_test() {
    for (int x = 0; x < 4; x++) {
      for (int i = 0; i <= 17; i++) {
        for (int j = 0; j <= 17; j++) {
          push_all_pair(factory(i), factory(j), (x & 1) == 0, (x & 2) == 0);
          if (i != 0 & j != 0) {
            push_all_pair(factory(i * 8 - 1), factory(j * 8 - 1), (x & 1) == 0, (x & 2) == 0);
            push_all_pair(factory(i * 8 - 1), factory(j * 8 + 0), (x & 1) == 0, (x & 2) == 0);
            push_all_pair(factory(i * 8 - 1), factory(j * 8 + 1), (x & 1) == 0, (x & 2) == 0);
            push_all_pair(factory(i * 8 + 0), factory(j * 8 - 1), (x & 1) == 0, (x & 2) == 0);
            push_all_pair(factory(i * 8 + 0), factory(j * 8 + 0), (x & 1) == 0, (x & 2) == 0);
            push_all_pair(factory(i * 8 + 0), factory(j * 8 + 1), (x & 1) == 0, (x & 2) == 0);
            push_all_pair(factory(i * 8 + 1), factory(j * 8 - 1), (x & 1) == 0, (x & 2) == 0);
            push_all_pair(factory(i * 8 + 1), factory(j * 8 + 0), (x & 1) == 0, (x & 2) == 0);
            push_all_pair(factory(i * 8 + 1), factory(j * 8 + 1), (x & 1) == 0, (x & 2) == 0);
          }
        }
      }
    }
  }

  private static void push_all_pair(ImmutableArray<Integer> a1, ImmutableArray<Integer> a2, boolean a1_via_array, boolean a2_via_array) {
    ImmutableArray<Integer> both;
    if (!a1_via_array && !a2_via_array) {
      both = a1.pushAll(a2);
    } else if (a1_via_array && !a2_via_array) {
      Integer array1[] = new Integer[(int)a1.size];
      a1.toArray(array1);
      both = new ImmutableArray<>(Arrays.asList(array1)).pushAll(a2);
    } else if (!a1_via_array && a2_via_array) {
      Integer array2[] = new Integer[(int)a2.size];
      a2.toArray(array2);
      both = a1.pushAll(Arrays.asList(array2));
    } else {
      Integer array1[] = new Integer[(int)a1.size];
      a1.toArray(array1);
      Integer array2[] = new Integer[(int)a2.size];
      a2.toArray(array2);
      both = new ImmutableArray<>(array1).pushAll(array2);
    }
    assert(both.size == a1.size + a2.size);
    long idx = 0;
    for (Integer i : a1) {
      assert(both.get(idx++) == i);
    }
    for (Integer i : a2) {
      assert(both.get(idx++) == i);
    }
  }
}

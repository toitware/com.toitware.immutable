// Copyright (C) 2019 Toitware ApS. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.toitware.immutable_test;
import com.toitware.immutable.ImmutableArray;
import com.toitware.immutable.ImmutableCollection;
import com.toitware.immutable.ImmutableDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Random;

class ImmutableArrayTest {
  public static void main(String args[]) {
    simple_test();
    random_test();
    random_test2();
    push_all_test();
    leak_deque_test();
  }

  private static void random_test() {
    final int ITERATIONS = 1000;
    Random random = new Random(1034210342);
    for (int z = 0; z < ITERATIONS; z++) {
      int len = random.nextInt(100);
      ImmutableCollection<Integer> a = new ImmutableArray<Integer>();
      for (int i = 0; i < len; i++) {
        a = a.push(random.nextInt(120));
      }
      ImmutableCollection<Integer> trimmed = a.trim(random.nextInt((int)(a.longSize() + 1)));
      for (int i = 0; i < trimmed.longSize(); i++) {
        assert(trimmed.get(i) == a.get(i));
      }
    }
  }

  private static void random_test2() {
    final int ITERATIONS = 1000;
    final int ARRAYS = 20;
    ArrayList<ArrayList<Integer>> control = new ArrayList<ArrayList<Integer>>();
    ImmutableArray<ImmutableCollection<Integer>> arrays = new ImmutableArray<>();
    for (int i = 0; i < ARRAYS; i++) {
      control.add(new ArrayList<Integer>());
      arrays = arrays.push(new ImmutableArray<Integer>());
    }

    Random random = new Random(1034210342);
    for (int z = 0; z < ITERATIONS; z++) {
      int src = random.nextInt(ARRAYS);
      int dest = random.nextInt(ARRAYS);
      int amount = random.nextInt(4) + 1;
      switch (random.nextInt(7)) {
        case 0: {
          // Push.
          //System.out.println("Push " + amount + " on " + src + " and store to " + dest);
          arrays = arrays.atPut(dest, arrays.get(src));
          control.set(dest, new ArrayList<Integer>(control.get(src)));
          for (int i = 0; i < amount; i++) {
            int value1 = random.nextInt(100);
            int value2 = random.nextInt(100);
            if (random.nextBoolean()) {
              arrays = arrays.atPut(dest, arrays.get(dest).push(value1, value2));
            } else {
              arrays = arrays.atPut(dest, arrays.get(dest).push(value1));
              arrays = arrays.atPut(dest, arrays.get(dest).push(value2));
            }
            control.get(dest).add(value1);
            control.get(dest).add(value2);
          }
          break;
        }
        case 1: {
          // Pop.
          //System.out.println("Pop " + amount + " from " + src + " and store to " + dest);
          control.set(dest, new ArrayList<Integer>(control.get(src)));
          int i = 0;
          for ( ; i < amount && control.get(dest).size() != 0; i++) {
            control.get(dest).remove(control.get(dest).size() - 1);
          }
          arrays = arrays.atPut(dest, arrays.get(src).trim(i));
          break;
        }
        case 2: {
          // Remove from start.
          control.set(dest, new ArrayList<Integer>(control.get(src)));
          int i = 0;
          for ( ; i < amount && control.get(dest).size() != 0; i++) {
            control.get(dest).remove(0);
          }
          //System.out.println("Trim " + i + " from " + src + " and store to " + dest);
          ImmutableCollection<Integer> old = arrays.get(src);
          arrays = arrays.atPut(dest, old.subList(i));
          // Test forEachRemaining.
          Iterator<Integer> it = old.iterator();
          for (int j = 0; j < i; j++) {
            assert(it.hasNext());
            it.next();  // Skip the first i.
          }
          // The remaining ones should match the new collection.
          int jBox[] = new int[] { 0 };
          ImmutableCollection<Integer> noo = arrays.get(dest);
          it.forEachRemaining((x)-> {
            assert(x.equals(noo.get(jBox[0]++)));
          });
          assert(jBox[0] == noo.size());
          ListIterator<Integer> lit = noo.listIterator(noo.size());
          for (int j = noo.size() - 1; j >= 0; j--) {
            assert(lit.hasPrevious());
            assert(lit.previous() == noo.get(j));
          }
          assert(!lit.hasPrevious());
          break;
        }
        case 3: {
          if (control.get(dest).size() > 897) {
            //System.out.println("Truncate " + dest);
            control.set(dest, new ArrayList<Integer>());
            arrays = arrays.atPut(dest, new ImmutableArray<Integer>());
          } else {
            // Concat.
            //System.out.println("Concat " + src + " onto " + dest);
            control.get(dest).addAll(control.get(src));
            if (random.nextBoolean()) {
              arrays = arrays.atPut(dest, arrays.get(dest).pushAll(arrays.get(src)));
            } else {
              ImmutableCollection<Integer> source = arrays.get(src);
              Integer as_array[] = new Integer[source.size()];
              source.toArray(as_array);
              arrays = arrays.atPut(dest, arrays.get(dest).pushAll(as_array));
            }
            break;
          }
        }
        case 4: {
          int len = control.get(src).size();
          if (len != 0) {
            int offset = random.nextInt(len);
            int value = random.nextInt(100);
            //System.out.println("Len " + len + ", Set " + offset + " to " + value + " from " + src + " to " + dest);
            arrays = arrays.atPut(dest, arrays.get(src).atPut(offset, value));
            control.set(dest, new ArrayList<Integer>(control.get(src)));
            control.get(dest).set(offset, value);
          }
          break;
        }
        case 5: {
          int len = control.get(src).size();
          if (len > 4) {
            int fromStart = random.nextInt(len >> 2);
            int fromEnd = random.nextInt(len >> 2);
            //System.out.println("Cut chunk from " + fromStart + " to " + (len - fromEnd) + " of " + src + " putting result in " + dest);
            control.set(dest, new ArrayList<Integer>(control.get(src).subList(fromStart, len - fromEnd)));
            arrays = arrays.atPut(dest, arrays.get(src).subList(fromStart, len - fromEnd));
          }
        }
        case 6: {
          if (control.get(dest).size() > 897) {
            //System.out.println("Truncate " + dest);
            control.set(dest, new ArrayList<Integer>());
            arrays = arrays.atPut(dest, new ImmutableArray<Integer>());
          } else {
            // Prepend.
            //System.out.println("Prepend " + src + " onto " + dest);
            ArrayList<Integer> old_dest = control.get(dest);
            ArrayList<Integer> old_source = control.get(src);
            control.set(dest, new ArrayList<Integer>());
            control.get(dest).addAll(old_source);
            control.get(dest).addAll(old_dest);
            if (random.nextBoolean()) {
              arrays = arrays.atPut(dest, arrays.get(dest).unshiftAll(arrays.get(src)));
            } else {
              ImmutableCollection<Integer> source = arrays.get(src);
              Integer as_array[] = new Integer[source.size()];
              source.toArray(as_array);
              arrays = arrays.atPut(dest, arrays.get(dest).unshiftAll(as_array));
            }
            break;
          }
        }
      }
      for (int i = 0; i < ARRAYS; i++) {
        assert(control.get(i).size() == arrays.get(i).size());
        for (int j = 0; j < control.get(i).size(); j++) {
          assert(control.get(i).get(j).equals(arrays.get(i).get(j)));
        }
        int j = 0;
        for (int element : arrays.get(i)) {
          assert(control.get(i).get(j++).equals(element));
        }
        int needle = random.nextInt(100);
        assert(control.get(i).indexOf(needle) == arrays.get(i).indexOf(needle));
        assert(control.get(i).lastIndexOf(needle) == arrays.get(i).lastIndexOf(needle));
      }
    }
  }

  // A deque is implemented as a view on an array, but we need to ensure that
  // the elements to the left of the view do not retain garbage.  This is done
  // by the trimLeft method of Immutable Array, which nulls out that part of
  // the tree.
  private static void leak_deque_test() {
    final int ITERATIONS = 1000;
    ImmutableArray<Object> accumulate = new ImmutableArray<>();
    Random random = new Random(1034210342);
    for (int z = 0; z < ITERATIONS; z++) {
      int size = random.nextInt(1000) + 2;
      int cut = random.nextInt(size - 1);
      ImmutableArray<Object[]> backing = new ImmutableArray<>();
      int i = 0;
      for ( ; i < cut; i++) {
        backing = backing.push(new Object[10000]);
      }
      for ( ; i < size; i++) {
        backing = backing.push(null);
      }
      accumulate = accumulate.push(backing.subList(cut));
    }
  }

  private static void ft_test(ImmutableCollection<Integer> ft, ImmutableCollection<Integer> empty) {
    Iterator<Integer> it = ft.iterator();
    assert(it.hasNext());
    ListIterator<Integer> lit = ft.listIterator();
    assert(lit.hasNext());
    assert(!lit.hasPrevious());
    assert(it.next() == 42);
    assert(!it.hasNext());
    assert(lit.next() == 42);
    assert(!lit.hasNext());
    assert(lit.hasPrevious());
    assert(lit.previous() == 42);
    lit = ft.listIterator(1);  // One past the end.
    assert(lit.hasPrevious());
    assert(!lit.hasNext());
    assert(lit.previous() == 42);
    assert(!empty.contains(42));
    assert(empty.indexOf(42) == -1);
    assert(ft.size() == 1);
    assert(ft.longSize() == 1);
    assert(!ft.isEmpty());
    assert(ft.get(0) == 42);
    assert(ft.contains(42));
    assert(ft.indexOf(42) == 0);
    for (int i : ft) {
      assert(i == 42);
    }
    assert(empty.size() == 0);
    assert(empty.longSize() == 0);
  }

  private static void simple_test() {
    ImmutableCollection<Integer> empty = new ImmutableArray<>();
    assert(empty.size() == 0);
    assert(empty.isEmpty());
    for (int i : empty) {
      assert(false);
    }
    Iterator<Integer> it = empty.iterator();
    assert(!it.hasNext());
    ListIterator<Integer> lit = empty.listIterator();
    assert(!lit.hasNext());
    assert(!lit.hasPrevious());

    ImmutableCollection<Integer> ft = empty.push(42);
    ft_test(ft, empty);
    ImmutableCollection<Integer> ft2 = empty.unshift(42);
    ft_test(ft2, empty);

    ImmutableCollection<Integer> two_a = ft.push(103);
    ImmutableCollection<Integer> two_b = ft.push(7);
    assert(two_a.size() == 2);
    assert(two_a.longSize() == 2);
    assert(two_b.size() == 2);
    assert(two_b.longSize() == 2);
    assert(two_a.get(0) == 42);
    assert(two_b.get(0) == 42);
    assert(two_a.get(1) == 103);
    assert(two_b.get(1) == 7);

    ImmutableCollection<Integer> love = two_a.unshift(3000);
    ImmutableCollection<Integer> futures = two_b.unshift(14000605);

    assert(love.size() == 3);
    assert(futures.size() == 3);
    it = love.iterator();
    lit = love.listIterator();
    assert(it.hasNext());
    assert(lit.hasNext());
    assert(it.next() == 3000);
    assert(lit.next() == 3000);
    assert(it.hasNext());
    assert(lit.hasNext());
    assert(it.next() == 42);
    assert(lit.next() == 42);
    assert(it.hasNext());
    assert(lit.hasNext());
    assert(it.next() == 103);
    assert(lit.next() == 103);
    assert(!it.hasNext());
    assert(!lit.hasNext());

    ImmutableCollection<Integer> p = new ImmutableArray<>();
    for (int i = 0; i < 10; i++) {
      p = p.push(i * i);
    }
    assert(p.size() == 10);
    assert(p.longSize() == 10);
    Object[] as_array = p.toArray();
    Integer[] typed_array = new Integer[11];
    p.toArray(typed_array);
    it = p.iterator();
    lit = p.listIterator();
    for (int i = 0; i < 10; i++) {
      assert(it.hasNext());
      assert(lit.hasNext());
      assert(lit.hasPrevious() == (i != 0));
      int element1 = it.next();
      int element2 = lit.next();
      int element3 = lit.previous();
      int element4 = lit.next();
      assert(element1 == element2);
      assert(element1 == element3);
      assert(element1 == element4);
      assert(p.get(i) == i * i);
      assert(i * i == (Integer)as_array[i]);
      assert(i * i == typed_array[i]);
    }
    assert(!it.hasNext());
    assert(!lit.hasNext());
    assert(lit.nextIndex() == 10);
    assert(lit.previousIndex() == 9);

    for (int i = 0; i <= 10; i++) {
      ImmutableCollection trimmed = p.trim(i);
      assert(trimmed.longSize() == p.longSize() - i);
      for (int j = 0; j < trimmed.longSize(); j++) {
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
    it = p.iterator();
    lit = p.listIterator();
    for (int i = 0; i < 100; i++) {
      assert(p.get(i) == i * i);
      assert(it.next() == i * i);
      assert(lit.next() == i * i);
      assert(lit.previous() == i * i);
      assert(lit.next() == i * i);
    }
    assert(!it.hasNext());
    assert(!lit.hasNext());
    assert(lit.nextIndex() == 100);
    assert(lit.previousIndex() == 99);

    for (int i = 100; i < 1000; i += 2) {
      p = p.push(i * i, (i + 1) * (i + 1));
    }
    assert(p.size() == 1000);
    it = p.iterator();
    lit = p.listIterator();
    for (int i = 0; i < 1000; i++) {
      assert(p.get(i) == i * i);
      assert(i == lit.nextIndex());
      assert(it.next() == i * i);
      assert(lit.next() == i * i);
      assert(i == lit.previousIndex());
      assert(lit.previous() == i * i);
      assert(lit.next() == i * i);
    }
    assert(!it.hasNext());
    assert(!lit.hasNext());
    assert(lit.nextIndex() == 1000);
    assert(lit.previousIndex() == 999);

    iBox[0] = 0;
    p.forEach((x)-> {
      assert(x == iBox[0] * iBox[0]);
      iBox[0]++;
    });
    assert(iBox[0] == 1000);

    ImmutableCollection<Integer> d = p.subList(123);
    iBox[0] = 123;
    d.forEach((x)-> {
      assert(x == iBox[0] * iBox[0]);
      iBox[0]++;
    });
    assert(iBox[0] == 1000);

    assert(p.indexOf(196) == 14);
    ImmutableCollection<Integer> p2 = p.atPut(42, 42);
    assert(p2.size() == p.size());
    assert(p2.get(42) == 42);
    ImmutableCollection<Integer> p3 = p;
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
    assert(p.longSize() == 100000);
    for (int i = 0; i < 100000; i++) {
      assert(p.get(i) == i * i);
    }

    j = 0;
    for (int i : p) {
      assert(i == j * j);
      j++;
    }
  }

  private static ImmutableCollection<Integer> factory(int n) {
    ImmutableCollection<Integer> a = new ImmutableArray<>();
    for (int i = 0; i < n; i++) a = a.push(i * i);
    ListIterator<Integer> at_end = a.listIterator(n);
    if (n != 0) assert(at_end.previous() == (n - 1) * (n - 1));
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

  private static void push_all_pair(ImmutableCollection<Integer> a1, ImmutableCollection<Integer> a2, boolean a1_via_array, boolean a2_via_array) {
    ImmutableCollection<Integer> both;
    if (!a1_via_array && !a2_via_array) {
      both = a1.pushAll(a2);
    } else if (a1_via_array && !a2_via_array) {
      Integer array1[] = new Integer[(int)a1.longSize()];
      a1.toArray(array1);
      both = new ImmutableArray<>(Arrays.asList(array1)).pushAll(a2);
    } else if (!a1_via_array && a2_via_array) {
      Integer array2[] = new Integer[(int)a2.longSize()];
      a2.toArray(array2);
      both = a1.pushAll(Arrays.asList(array2));
    } else {
      Integer array1[] = new Integer[(int)a1.longSize()];
      a1.toArray(array1);
      Integer array2[] = new Integer[(int)a2.longSize()];
      a2.toArray(array2);
      both = new ImmutableArray<>(array1).pushAll(array2);
    }
    assert(both.longSize() == a1.longSize() + a2.longSize());
    long idx = 0;
    for (Integer i : a1) {
      assert(both.get(idx++) == i);
    }
    for (Integer i : a2) {
      assert(both.get(idx++) == i);
    }
  }
}

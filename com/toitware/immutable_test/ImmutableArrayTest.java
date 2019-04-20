package com.toitware.immutable_test;
import com.toitware.immutable.ImmutableArray;

class ImmutableArrayTest {
  public static void main(String args[]) {
    ImmutableArray empty = new ImmutableArray();
    assert(empty.length == 0);
    ImmutableArray ft = empty.push(42);
    assert(ft.length == 1);
    assert((Integer)ft.at(0) == 42);
    assert(empty.length == 0);
    ImmutableArray two_a = ft.push(103);
    ImmutableArray two_b = ft.push(7);
    assert(two_a.length == 2);
    assert(two_b.length == 2);
    assert((Integer)two_a.at(0) == 42);
    assert((Integer)two_b.at(0) == 42);
    assert((Integer)two_a.at(1) == 103);
    assert((Integer)two_b.at(1) == 7);
    ImmutableArray p = new ImmutableArray();
    for (int i = 0; i < 10; i++) {
      p = p.push(i * i);
    }
    assert(p.length == 10);
    for (int i = 0; i < 10; i++) {
      assert((Integer)p.at(i) == i * i);
    }
    for (int i = 10; i < 100; i++) {
      p = p.push(i * i);
    }
    assert(p.length == 100);
    for (int i = 0; i < 100; i++) {
      assert((Integer)p.at(i) == i * i);
    }
    for (int i = 100; i < 1000; i++) {
      p = p.push(i * i);
    }
    assert(p.length == 1000);
    for (int i = 0; i < 1000; i++) {
      assert((Integer)p.at(i) == i * i);
    }
    ImmutableArray p2 = p.at_put(42, 42);
    assert(p2.length == p.length);
    assert((Integer)p2.at(42) == 42);
    ImmutableArray p3 = p;
    for (int i = 0; i < 1000; i++) {
      assert((Integer)p2.at(i) == (i == 42 ? 42 : i * i));
      assert((Integer)p.at(i) == i * i);
      p3 = p3.at_put(i, i * i * i);
    }
    for (int i = 1000; i < 100000; i++) {
      p = p.push(i * i);
    }
    for (int i = 0; i < 100000; i++) {
      assert((Integer)p.at(i) == i * i);
    }
  }
}

// Copyright (C) 2019 Toitware ApS. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.toitware.immutable_test;

import org.organicdesign.fp.collections.ImList;
import org.organicdesign.fp.collections.PersistentVector;
import org.organicdesign.fp.collections.RrbTree;
import com.toitware.immutable.ImmutableArray;
import com.toitware.immutable.ImmutableCollection;
import com.toitware.immutable.ImmutableDeque;
import com.toitware.immutable.RebuildIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Random;
import org.pcollections.TreePVector;

abstract class ImmutableMemoryUse {
  public static void main(String args[]) {
    new ImmutableArrayMemoryUse(0).runs();
    new ImmutableArrayMemoryUse(1).runs();
    new ImmutableArrayMemoryUse(4).runs();
    new ImmutableArrayMemoryUse(16).runs();
    new ImmutableArrayMemoryUse(64).runs();
    new ImmutableArrayMemoryUse(256).runs();
    new PCollectionsMemoryUse(0).runs();
    new PCollectionsMemoryUse(1).runs();
    new PCollectionsMemoryUse(4).runs();
    new PCollectionsMemoryUse(16).runs();
    new PCollectionsMemoryUse(64).runs();
    new PCollectionsMemoryUse(256).runs();
    new PaguroMemoryUse(0).runs();
    new PaguroMemoryUse(1).runs();
    new PaguroMemoryUse(4).runs();
    new PaguroMemoryUse(16).runs();
    new PaguroMemoryUse(64).runs();
    new PaguroMemoryUse(256).runs();
    new RrbTreeMemoryUse(0).runs();
    new RrbTreeMemoryUse(1).runs();
    new RrbTreeMemoryUse(4).runs();
    new RrbTreeMemoryUse(16).runs();
    new RrbTreeMemoryUse(64).runs();
    new RrbTreeMemoryUse(256).runs();
    /*new KruVectorMemoryUse(0).runs();
    new KruVectorMemoryUse(1).runs();
    new KruVectorMemoryUse(4).runs();
    new KruVectorMemoryUse(16).runs();
    new KruVectorMemoryUse(64).runs();
    new KruVectorMemoryUse(256).runs();*/
  }

  static protected final int SIZE = 10000;

  protected Object holder[];

  void runs() {
    long smallHeapSize = 10000000000000000L;
    setup(SIZE, SIZE * 2);
    for (int i = 0; i < 4; i++) {
      churn();
      System.gc();
      long heapSize = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(); 
      if (heapSize < smallHeapSize) smallHeapSize = heapSize;
    }

    long largeHeapSize = 10000000000000000L;
    setup(SIZE * 2, SIZE * 2);
    for (int i = 0; i < 4; i++) {
      churn();
      System.gc();
      long heapSize = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(); 
      if (heapSize < largeHeapSize) largeHeapSize = heapSize;
    }
    long heapSize = largeHeapSize - smallHeapSize;
    int perItem = (int)(heapSize / (1.0 * SIZE));
    System.out.println("" + name() + " " + perItem + " bytes");
  }

  abstract void churn();
  abstract void setup(int size, int backing_size);
  abstract String name();

  private static class ImmutableArrayMemoryUse extends ImmutableMemoryUse {
    int elements;
    int length;

    ImmutableArrayMemoryUse(int e) {
      elements = e;
    }

    public void setup(int size, int backing_size) {
      holder = new Object[backing_size];
      length = size;
      churn();
    }

    public void churn() {
      Integer x = 42;
      for (int i = 0; i < length; i++) {
        ImmutableArray<Integer> a = new ImmutableArray<>();
        for (int j = 0; j < elements; j++) {
          a = a.push(x);
        }
        holder[i] = a;
      }
    }

    public String name() { return "ImmutableArray[" + elements + "]"; }
  }

  private static class PCollectionsMemoryUse extends ImmutableMemoryUse {
    int elements;
    int length;

    PCollectionsMemoryUse(int e) {
      elements = e;
    }

    public void setup(int size, int backing_size) {
      holder = new Object[backing_size];
      length = size;
      churn();
    }

    public void churn() {
      Integer x = 42;
      for (int i = 0; i < length; i++) {
        TreePVector<Integer> a = TreePVector.<Integer>empty();
        for (int j = 0; j < elements; j++) {
          a = a.plus(x);
        }
        holder[i] = a;
      }
    }

    public String name() { return "TreePVector[" + elements + "]"; }
  }

  private static class PaguroMemoryUse extends ImmutableMemoryUse {
    int elements;
    int length;

    PaguroMemoryUse(int e) {
      elements = e;
    }

    public void setup(int size, int backing_size) {
      holder = new Object[backing_size];
      length = size;
      churn();
    }

    public void churn() {
      Integer x = 42;
      for (int i = 0; i < length; i++) {
        ImList<Integer> a = PersistentVector.<Integer>empty();
        for (int j = 0; j < elements; j++) {
          a = a.append(x);
        }
        holder[i] = a;
      }
    }

    public String name() { return "PersistentVector[" + elements + "]"; }
  }

  private static class RrbTreeMemoryUse extends ImmutableMemoryUse {
    int elements;
    int length;

    RrbTreeMemoryUse(int e) {
      elements = e;
    }

    public void setup(int size, int backing_size) {
      holder = new Object[backing_size];
      length = size;
      churn();
    }

    public void churn() {
      Integer x = 42;
      for (int i = 0; i < length; i++) {
        ImList<Integer> a = RrbTree.ImRrbt.<Integer>empty();
        for (int j = 0; j < elements; j++) {
          a = a.append(x);
        }
        holder[i] = a;
      }
    }

    public String name() { return "RrrbTree[" + elements + "]"; }
  }

  /*
   * The version of Karl Krukows port of CLJ data structures that is
   * available on Github no longer compiles due to language changes,
   * so this is commented out.  The changes needed are pretty minimal
   * (mostly just variables called _.
   *
  private static class KruVectorMemoryUse extends ImmutableMemoryUse {
    int elements;
    int length;

    KruVectorMemoryUse(int e) {
      elements = e;
    }

    public void setup(int size, int backing_size) {
      holder = new Object[backing_size];
      length = size;
      churn();
    }

    public void churn() {
      Integer x = 42;
      for (int i = 0; i < length; i++) {
        com.github.krukow.clj_ds.PersistentVector<Integer> a = com.github.krukow.clj_ds.Persistents.<Integer>vector();
        for (int j = 0; j < elements; j++) {
          a = a.plus(x);
        }
        holder[i] = a;
      }
    }

    public String name() { return "KruVector[" + elements + "]"; }
  }
  */
}

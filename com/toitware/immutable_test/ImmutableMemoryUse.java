// Copyright (C) 2019 Toitware ApS. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.toitware.immutable_test;
import com.toitware.immutable.ImmutableArray;
import com.toitware.immutable.ImmutableCollection;
import com.toitware.immutable.ImmutableDeque;
import com.toitware.immutable.RebuildIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Random;

abstract class ImmutableMemoryUse {
  public static void main(String args[]) {
    new ImmutableArrayMemoryUse(0).runs();
    new ImmutableArrayMemoryUse(1).runs();
    new ImmutableArrayMemoryUse(4).runs();
    new ImmutableArrayMemoryUse(16).runs();
    new ImmutableArrayMemoryUse(64).runs();
    new ImmutableArrayMemoryUse(256).runs();
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
}

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

abstract class ImmutableBenchmark {
  public static void main(String args[]) {
    new ForInBench().runs();
    new ForEachBench().runs();
    new IntLoopBench().runs();
    new ForInDequeBench().runs();
    new ForEachDequeBench().runs();
    new IntLoopDequeBench().runs();
    new ArrayListForInBench().runs();
    new ArrayListForEachBench().runs();
    new ArrayListIntLoopBench().runs();
    new ForInBench().runs();
    new ForEachBench().runs();
    new IntLoopBench().runs();
    new ArrayListForInBench().runs();
    new ArrayListForEachBench().runs();
    new ArrayListIntLoopBench().runs();
    new ForInDequeBench().runs();
    new ForEachDequeBench().runs();
    new IntLoopDequeBench().runs();
  }

  void runs() {
    long now = System.nanoTime();
    setup();

    while (System.nanoTime() - now < 2000000000) {
      run();
    }

    long start = System.nanoTime();
    long iterations = 1;
    while (System.nanoTime() - start < 2000000000) {
      run();
      iterations++;
    }
    long end = System.nanoTime();

    System.out.println("" + name() + " " + (end - start) / (iterations * 1000L) + "us");
  }

  abstract void run();
  void setup() {}
  abstract String name();

  private static abstract class IterationBench extends ImmutableBenchmark {
    private long _sum;
    public long sum() { return _sum; }
    protected ImmutableArray<ImmutableCollection<Integer>> _top;

    public void setup() {
      Random random = new Random(1034210342);
      _top = new ImmutableArray<>();
      long sum = 0;
      for (int i = 0; i < 1000; i++) {
        ImmutableArray<Integer> a = new ImmutableArray<>();
        for (int j = 0; j < 1000; j++) {
          int x = random.nextInt(123);
          a = a.push(x);
          sum += x;
        }
        _top = _top.push(a);
        _sum = sum;
      }
    }

    protected void dequeify() {
      for (int i = 0; i < _top.size(); i++) {
        _top = _top.atPut(i, _top.get(i).unshift(0));
      }
    }
  }

  private static class ForInBench extends IterationBench {
    public String name() { return "ForIn     "; }

    public void run() {
      long answer = sumForIn();
      if (answer != sum()) throw new RuntimeException();
    }

    protected long sumForIn() {
      long sum = 0;
      for (ImmutableCollection<Integer> array : _top) {
        for (int x : array) {
          sum += x;
        }
      }
      return sum;
    }
  }

  private static class ForInDequeBench extends ForInBench {
    public String name() { return "ForIn-Dq  "; }

    public void setup() {
      super.setup();
      dequeify();
    }
  }

  private static class ForEachBench extends IterationBench {
    public String name() { return "ForEach   "; }

    long s;

    public void run() {
      long answer = sumForEach();
      if (answer != sum()) throw new RuntimeException();
    }

    protected long sumForEach() {
      s = 0;
      _top.forEach((array)-> {
        array.forEach((x)-> {
          s += x;
        });
      });
      return s;
    }
  }

  private static class ForEachDequeBench extends ForEachBench {
    public String name() { return "ForEach-Dq"; }

    public void setup() {
      super.setup();
      dequeify();
    }
  }

  private static class IntLoopBench extends IterationBench {
    public String name() { return "IntLoop   "; }

    public void run() {
      long answer = sumIntLoop();
      if (answer != sum()) throw new RuntimeException();
    }

    protected long sumIntLoop() {
      long sum = 0;
      for (int i = 0; i < _top.size(); i++) {
        ImmutableCollection<Integer> a = _top.get(i);
        for (int j = 0; j < a.size(); j++) {
          sum += a.get(j);
        }
      }
      return sum;
    }
  }

  private static class IntLoopDequeBench extends IntLoopBench {
    public String name() { return "IntLoop-Dq"; }

    public void setup() {
      super.setup();
      dequeify();
    }
  }

  private static abstract class ArrayListIterationBench extends ImmutableBenchmark {
    private long _sum;
    public long sum() { return _sum; }
    protected ArrayList<ArrayList<Integer>> _top;

    public void setup() {
      Random random = new Random(1034210342);
      _top = new ArrayList<>();
      long sum = 0;
      for (int i = 0; i < 1000; i++) {
        ArrayList<Integer> a = new ArrayList<>();
        _top.add(a);
        for (int j = 0; j < 1000; j++) {
          int x = random.nextInt(123);
          a.add(x);
          sum += x;
        }
        _sum = sum;
      }
    }
  }

  private static class ArrayListForInBench extends ArrayListIterationBench {
    public String name() { return "ForIn-AL  "; }

    public void run() {
      long answer = sumForIn();
      if (answer != sum()) throw new RuntimeException();
    }

    protected long sumForIn() {
      long sum = 0;
      for (ArrayList<Integer> array : _top) {
        for (int x : array) {
          sum += x;
        }
      }
      return sum;
    }
  }

  private static class ArrayListForEachBench extends ArrayListIterationBench {
    public String name() { return "ForEach-AL"; }

    long s;

    public void run() {
      long answer = sumForEach();
      if (answer != sum()) throw new RuntimeException();
    }

    protected long sumForEach() {
      s = 0;
      _top.forEach((array)-> {
        array.forEach((x)-> {
          s += x;
        });
      });
      return s;
    }
  }

  private static class ArrayListIntLoopBench extends ArrayListIterationBench {
    public String name() { return "IntLoop-AL"; }

    public void run() {
      long answer = sumIntLoop();
      if (answer != sum()) throw new RuntimeException();
    }

    protected long sumIntLoop() {
      long sum = 0;
      for (int i = 0; i < _top.size(); i++) {
        ArrayList<Integer> a = _top.get(i);
        for (int j = 0; j < a.size(); j++) {
          sum += a.get(j);
        }
      }
      return sum;
    }
  }

}

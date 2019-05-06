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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Set;

abstract class ImmutableBenchmark {
  public static void main(String args[]) {
    new FilterAllBench().runs();
    new FilterAllWithSetBench().runs();
    new FilterAllListBench().runs();
    new FilterAllListWithSetBench().runs();
    new Push1AtATimeBench().runs();
    new PushAllBench().runs();
    new PushAllFromListBench().runs();
    new PushAllListList().runs();
    new Unshifting1AtATimeBench().runs();
    new UnshiftingAllBench().runs();
    new UnshiftAllFromList().runs();
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

    System.out.println("" + name() + " " + (end - start) / (iterations * 1000L) + "us" + "    " + (end - start) / (iterations * 1000000L) + "ms");
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

  private static abstract class BuildingBench extends ImmutableBenchmark {
    protected ImmutableArray<ImmutableArray<Integer>> _top;
    protected ImmutableArray<Long> _sums;
    protected List<List<Integer>> _lists = new ArrayList<>();
    protected List<Set<Integer>> _sets = new ArrayList<>();

    public void setup() {
      Random random = new Random(1034210342);
      _top = new ImmutableArray<>();
      _sums = new ImmutableArray<>();
      for (int i = 0; i < 31; i++) {
        long sum = 0;
        int size = random.nextInt(1000) + 500;
        ImmutableArray<Integer> a = new ImmutableArray<>();
        for (int j = 0; j < size; j++) {
          int x = random.nextInt(123);
          sum += x;
          a = a.push(x);
        }
        _top = _top.push(a);
        _sums = _sums.push(sum);
      }
      for (int i = 0; i < _top.size(); i++) {
        _lists.add(new ArrayList<Integer>(_top.get(i)));
        _sets.add(new HashSet<Integer>(_top.get(i)));
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static class Push1AtATimeBench extends BuildingBench {
    public String name() { return "Push1AtATimeBench     "; }
    public void run() {
      int x = 0;
      for (ImmutableCollection<Integer> a1 : _top) {
        int y = 0;
        for (ImmutableCollection<Integer> a2 : _top) {
          ImmutableCollection<Integer> both[] = new ImmutableCollection[1];
          both[0] = a1;
          a2.forEach((e)-> {
            both[0] = both[0].push(e);
          });
          int sum[] = new int[1];
          both[0].forEach((e) -> {
            sum[0] += e;
          });
          if (sum[0] != _sums.get(x) + _sums.get(y)) {
            throw new RuntimeException();
          }
          y++;
        }
        x++;
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static class Unshifting1AtATimeBench extends BuildingBench {
    public String name() { return "Unshifting1AtATimeBench  "; }
    public void run() {
      int x = 0;
      for (ImmutableCollection<Integer> a1 : _top) {
        int y = 0;
        for (ImmutableCollection<Integer> a2 : _top) {
          ImmutableCollection<Integer> both[] = new ImmutableCollection[1];
          both[0] = a1;
          a2.forEach((e)-> {
            both[0] = both[0].unshift(e);
          });
          int sum[] = new int[1];
          both[0].forEach((e) -> {
            sum[0] += e;
          });
          if (sum[0] != _sums.get(x) + _sums.get(y)) {
            throw new RuntimeException();
          }
          y++;
        }
        x++;
      }
    }
  }

  private static class PushAllBench extends BuildingBench {
    public String name() { return "PushAllBench          "; }
    public void run() {
      int x = 0;
      for (ImmutableCollection<Integer> a1 : _top) {
        int y = 0;
        for (ImmutableCollection<Integer> a2 : _top) {
          ImmutableCollection<Integer> both = a1.pushAll(a2);
          int sum[] = new int[1];
          both.forEach((e) -> {
            sum[0] += e;
          });
          if (sum[0] != _sums.get(x) + _sums.get(y)) {
            throw new RuntimeException();
          }
          y++;
        }
        x++;
      }
    }
  }

  private static class UnshiftingAllBench extends BuildingBench {
    public String name() { return "UnshiftingAllBench      "; }
    public void run() {
      int x = 0;
      for (ImmutableCollection<Integer> a1 : _top) {
        int y = 0;
        for (ImmutableCollection<Integer> a2 : _top) {
          ImmutableCollection<Integer> both = a1.unshiftAll(a2);
          int sum[] = new int[1];
          both.forEach((e) -> {
            sum[0] += e;
          });
          if (sum[0] != _sums.get(x) + _sums.get(y)) {
            throw new RuntimeException();
          }
          y++;
        }
        x++;
      }
    }
  }

  private static class PushAllFromListBench extends BuildingBench {
    public String name() { return "PushAllFromListBench   "; }
    public void run() {
      int x = 0;
      for (ImmutableCollection<Integer> a1 : _top) {
        int y = 0;
        for (List<Integer> a2 : _lists) {
          ImmutableCollection<Integer> both = a1.pushAll(a2);
          int sum[] = new int[1];
          both.forEach((e) -> {
            sum[0] += e;
          });
          if (sum[0] != _sums.get(x) + _sums.get(y)) {
            throw new RuntimeException();
          }
          y++;
        }
        x++;
      }
    }
  }

  private static class UnshiftAllFromList extends BuildingBench {
    public String name() { return "UnshiftAllFromList      "; }
    public void run() {
      int x = 0;
      for (ImmutableCollection<Integer> a1 : _top) {
        int y = 0;
        for (List<Integer> a2 : _lists) {
          ImmutableCollection<Integer> both = a1.unshiftAll(a2);
          int sum[] = new int[1];
          both.forEach((e) -> {
            sum[0] += e;
          });
          if (sum[0] != _sums.get(x) + _sums.get(y)) {
            throw new RuntimeException();
          }
          y++;
        }
        x++;
      }
    }
  }

  private static class PushAllListList extends BuildingBench {
    public String name() { return "PushAllListListBench   "; }
    public void run() {
      int x = 0;
      for (List<Integer> a1 : _lists) {
        int y = 0;
        for (List<Integer> a2 : _lists) {
          List<Integer> both = new ArrayList<>();
          both.addAll(a1);
          both.addAll(a2);
          int sum[] = new int[1];
          both.forEach((e) -> {
            sum[0] += e;
          });
          if (sum[0] != _sums.get(x) + _sums.get(y)) {
            throw new RuntimeException();
          }
          y++;
        }
        x++;
      }
    }
  }

  private static class FilterAllBench extends BuildingBench {
    public String name() { return "FilterAllBench        "; }
    public void run() {
      int x = 0;
      for (ImmutableCollection<Integer> a1 : _top) {
        int y = 0;
        for (ImmutableCollection<Integer> a2 : _top) {
          ImmutableCollection<Integer> a1_without_a2 = a1.filterAll(a2);
          ImmutableCollection<Integer> a1_withonly_a2 = a1.selectAll(a2);
          int sum[] = new int[1];
          a1_without_a2.forEach((e) -> { sum[0] += e; });
          a1_withonly_a2.forEach((e) -> { sum[0] += e; });
          a2.forEach((e) -> { sum[0] += e; });
          if (sum[0] != _sums.get(x) + _sums.get(y)) {
            throw new RuntimeException();
          }
          y++;
        }
        x++;
      }
    }
  }

  private static class FilterAllWithSetBench extends BuildingBench {
    public String name() { return "FilterAllWithSetBench "; }
    public void run() {
      int x = 0;
      for (ImmutableCollection<Integer> a1 : _top) {
        int y = 0;
        for (Set<Integer> a2 : _sets) {
          ImmutableCollection<Integer> a1_without_a2 = a1.filterAll(a2);
          ImmutableCollection<Integer> a1_withonly_a2 = a1.selectAll(a2);
          int sum[] = new int[1];
          a1_without_a2.forEach((e) -> { sum[0] += e; });
          a1_withonly_a2.forEach((e) -> { sum[0] += e; });
          _lists.get(y).forEach((e) -> { sum[0] += e; });
          if (sum[0] != _sums.get(x) + _sums.get(y)) {
            throw new RuntimeException();
          }
          y++;
        }
        x++;
      }
    }
  }

  private static class FilterAllListBench extends BuildingBench {
    public String name() { return "FilterAllListBench    "; }
    public void run() {
      int x = 0;
      for (List<Integer> a1 : _lists) {
        int y = 0;
        for (List<Integer> a2 : _lists) {
          List<Integer> a1_without_a2 = new ArrayList<>(a1);
          a1_without_a2.removeAll(a2);
          List<Integer> a1_withonly_a2 = new ArrayList<>(a1);
            a1_withonly_a2.retainAll(a2);
          int sum[] = new int[1];
          a1_without_a2.forEach((e) -> { sum[0] += e; });
          a1_withonly_a2.forEach((e) -> { sum[0] += e; });
          a2.forEach((e) -> { sum[0] += e; });
          if (sum[0] != _sums.get(x) + _sums.get(y)) {
            throw new RuntimeException();
          }
          y++;
        }
        x++;
      }
    }
  }

  private static class FilterAllListWithSetBench extends BuildingBench {
    public String name() { return "FilterAllListWithSet  "; }
    public void run() {
      int x = 0;
      for (List<Integer> a1 : _lists) {
        int y = 0;
        for (Set<Integer> a2 : _sets) {
          List<Integer> a1_without_a2 = new ArrayList<>(a1);
          a1_without_a2.removeAll(a2);
          List<Integer> a1_withonly_a2 = new ArrayList<>(a1);
            a1_withonly_a2.retainAll(a2);
          int sum[] = new int[1];
          a1_without_a2.forEach((e) -> { sum[0] += e; });
          a1_withonly_a2.forEach((e) -> { sum[0] += e; });
          _lists.get(y).forEach((e) -> { sum[0] += e; });
          if (sum[0] != _sums.get(x) + _sums.get(y)) {
            throw new RuntimeException();
          }
          y++;
        }
        x++;
      }
    }
  }
}

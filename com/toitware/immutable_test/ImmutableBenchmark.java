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

import org.pcollections.PCollection;
import org.pcollections.TreePVector;
import org.pcollections.PVector;

import org.organicdesign.fp.collections.ImList;
import org.organicdesign.fp.collections.PersistentVector;
import org.organicdesign.fp.collections.RrbTree;
import org.organicdesign.fp.collections.UnmodIterable;
import org.organicdesign.fp.collections.UnmodList;

abstract class ImmutableBenchmark {
  public static void main(String args[]) {
    new ForInBench().runs();
    //new KruForInBench().runs();
    new PForInBench().runs();
    new PagForInBench().runs();
    new RrbForInBench().runs();
    new ForInDequeBench().runs();
    new ForEachBench().runs();
    new PForEachBench().runs();
    new PagForEachBench().runs();
    new RrbForEachBench().runs();
    new ForEachDequeBench().runs();
    new IntLoopBench().runs();
    new PIntLoopBench().runs();
    new PagIntLoopBench().runs();
    new RrbIntLoopBench().runs();
    new IntLoopDequeBench().runs();
    new ArrayListForInBench().runs();
    new ArrayListForEachBench().runs();
    new ArrayListIntLoopBench().runs();
    new ArrayListForInBench().runs();
    new ArrayListForEachBench().runs();
    new ArrayListIntLoopBench().runs();
    new ForInBench().runs();
    new PForInBench().runs();
    new PagForInBench().runs();
    new RrbForInBench().runs();
    new ForInDequeBench().runs();
    new ForEachBench().runs();
    new PForEachBench().runs();
    new PagForEachBench().runs();
    new RrbForEachBench().runs();
    new ForEachDequeBench().runs();
    new IntLoopBench().runs();
    new PIntLoopBench().runs();
    new PagIntLoopBench().runs();
    new RrbIntLoopBench().runs();
    new IntLoopDequeBench().runs();

    new Push1AtATimeBench().runs();
    //new KruPush1AtATimeBench().runs();
    new PPush1AtATimeBench().runs();
    new PagPush1AtATimeBench().runs();
    new RrbPush1AtATimeBench().runs();
    new Push2AtATimeBench().runs();
    new PagPush2AtATimeBench().runs();
    new PushAllBench().runs();
    new PPushAllBench().runs();
    new PagPushAllBench().runs();
    new RrbPushAllBench().runs();
    new RrbJoinAllBench().runs();
    new PushAllFromListBench().runs();
    new PPushAllFromListBench().runs();
    new PagPushAllFromListBench().runs();
    new RrbPushAllFromListBench().runs();
    new PushAllListList().runs();
    new Insert1AtATimeBench().runs();
    new PInsert1AtATimeBench().runs();
    new RrbInsert1AtATimeBench().runs();
    new InsertAllBench().runs();
    new PInsertAllBench().runs();
    new PagInsertAllBench().runs();
    new Unshifting1AtATimeBench().runs();
    new PUnshifting1AtATimeBench().runs();
    new RrbUnshifting1AtATimeBench().runs();
    new UnshiftingAllBench().runs();
    new PUnshiftingAllBench().runs();
    new UnshiftAllFromList().runs();
    new PUnshiftAllFromList().runs();

    new FilterAllBench().runs();
    new FilterAllWithSetBench().runs();
    new FilterAllListBench().runs();
    new FilterAllListWithSetBench().runs();
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
    protected PVector<PVector<Integer>> _pvectors;
    protected ImList<ImList<Integer>> _paguro;
    protected RrbTree.ImRrbt<RrbTree.ImRrbt<Integer>> _rrbs;
    //protected ArrayList<com.github.krukow.clj_ds.PersistentVector<Integer>> _krukows;

    public void setup() {
      Random random = new Random(1034210342);
      _top = new ImmutableArray<>();
      _pvectors = TreePVector.<PVector<Integer>>empty();
      _paguro = PersistentVector.<ImList<Integer>>empty();
      _rrbs = RrbTree.ImRrbt.<RrbTree.ImRrbt<Integer>>empty();
      //_krukows = new ArrayList<com.github.krukow.clj_ds.PersistentVector<Integer>>();
      long sum = 0;
      for (int i = 0; i < 1000; i++) {
        ImmutableArray<Integer> a = new ImmutableArray<>();
        PVector<Integer> p = TreePVector.<Integer>empty();
        ImList<Integer> l = PersistentVector.<Integer>empty();
        RrbTree.ImRrbt<Integer> r = RrbTree.ImRrbt.<Integer>empty();
        //com.github.krukow.clj_ds.PersistentVector<Integer> k = com.github.krukow.clj_ds.Persistents.<Integer>vector();
        for (int j = 0; j < 1000; j++) {
          int x = random.nextInt(123);
          a = a.push(x);
          p = p.plus(x);
          l = l.append(x);
          r = r.append(x);
          //k = k.plus(x);
          sum += x;
        }
        _top = _top.push(a);
        _pvectors = _pvectors.plus(p);
        _paguro = _paguro.append(l);
        _rrbs = _rrbs.append(r);
        //_krukows.add(k);
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

  private static class PForInBench extends IterationBench {
    public String name() { return "PForIn    "; }

    public void run() {
      long answer = sumForIn();
      if (answer != sum()) throw new RuntimeException();
    }

    protected long sumForIn() {
      long sum = 0;
      for (PVector<Integer> array : _pvectors) {
        for (int x : array) {
          sum += x;
        }
      }
      return sum;
    }
  }

  private static class PagForInBench extends IterationBench {
    public String name() { return "PagForIn  "; }

    public void run() {
      long answer = sumForIn();
      if (answer != sum()) throw new RuntimeException();
    }

    protected long sumForIn() {
      long sum = 0;
      for (ImList<Integer> array : _paguro) {
        for (int x : array) {
          sum += x;
        }
      }
      return sum;
    }
  }

  private static class RrbForInBench extends IterationBench {
    public String name() { return "RrbForIn  "; }

    public void run() {
      long answer = sumForIn();
      if (answer != sum()) throw new RuntimeException();
    }

    protected long sumForIn() {
      long sum = 0;
      for (RrbTree.ImRrbt<Integer> array : _rrbs) {
        for (int x : array) {
          sum += x;
        }
      }
      return sum;
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

  private static class PForEachBench extends IterationBench {
    public String name() { return "PForEach   "; }

    long s;

    public void run() {
      long answer = sumForEach();
      if (answer != sum()) throw new RuntimeException();
    }

    protected long sumForEach() {
      s = 0;
      _pvectors.forEach((array)-> {
        array.forEach((x)-> {
          s += x;
        });
      });
      return s;
    }
  }

  private static class PagForEachBench extends IterationBench {
    public String name() { return "PagForEach "; }

    long s;

    public void run() {
      long answer = sumForEach();
      if (answer != sum()) throw new RuntimeException();
    }

    protected long sumForEach() {
      s = 0;
      _paguro.forEach((array)-> {
        array.forEach((x)-> {
          s += x;
        });
      });
      return s;
    }
  }

  private static class RrbForEachBench extends IterationBench {
    public String name() { return "RrbForEach "; }

    long s;

    public void run() {
      long answer = sumForEach();
      if (answer != sum()) throw new RuntimeException();
    }

    protected long sumForEach() {
      s = 0;
      _rrbs.forEach((array)-> {
        array.forEach((x)-> {
          s += x;
        });
      });
      return s;
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

  private static class PIntLoopBench extends IterationBench {
    public String name() { return "PIntLoop   "; }

    public void run() {
      long answer = sumIntLoop();
      if (answer != sum()) throw new RuntimeException();
    }

    protected long sumIntLoop() {
      long sum = 0;
      for (int i = 0; i < _pvectors.size(); i++) {
        PVector<Integer> a = _pvectors.get(i);
        for (int j = 0; j < a.size(); j++) {
          sum += a.get(j);
        }
      }
      return sum;
    }
  }

  private static class PagIntLoopBench extends IterationBench {
    public String name() { return "PagIntLoop "; }

    public void run() {
      long answer = sumIntLoop();
      if (answer != sum()) throw new RuntimeException();
    }

    protected long sumIntLoop() {
      long sum = 0;
      for (int i = 0; i < _paguro.size(); i++) {
        ImList<Integer> a = _paguro.get(i);
        for (int j = 0; j < a.size(); j++) {
          sum += a.get(j);
        }
      }
      return sum;
    }
  }

  private static class RrbIntLoopBench extends IterationBench {
    public String name() { return "RrbIntLoop "; }

    public void run() {
      long answer = sumIntLoop();
      if (answer != sum()) throw new RuntimeException();
    }

    protected long sumIntLoop() {
      long sum = 0;
      for (int i = 0; i < _rrbs.size(); i++) {
        RrbTree.ImRrbt<Integer> a = _rrbs.get(i);
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
    protected PVector<PVector<Integer>> _pvectors;
    protected ImList<ImList<Integer>> _paguro;
    protected RrbTree.ImRrbt<RrbTree.ImRrbt<Integer>> _rrbs;
    protected ImmutableArray<Long> _sums;
    protected List<List<Integer>> _lists = new ArrayList<>();
    protected List<Set<Integer>> _sets = new ArrayList<>();
    //protected List<com.github.krukow.clj_ds.PersistentVector<Integer>> _krukows = new ArrayList<>();

    public void setup() {
      Random random = new Random(1034210342);
      _top = new ImmutableArray<>();
      _pvectors = TreePVector.<PVector<Integer>>empty();
      _paguro = PersistentVector.<ImList<Integer>>empty();
      _rrbs = RrbTree.ImRrbt.<RrbTree.ImRrbt<Integer>>empty();
      _sums = new ImmutableArray<>();
      for (int i = 0; i < 31; i++) {
        long sum = 0;
        int size = (random.nextInt(1000) & ~1) + 500;
        ImmutableArray<Integer> a = new ImmutableArray<>();
        PVector<Integer> p = TreePVector.<Integer>empty();
        ImList<Integer> l = PersistentVector.<Integer>empty();
        RrbTree.ImRrbt<Integer> r = RrbTree.ImRrbt.<Integer>empty();
        //com.github.krukow.clj_ds.PersistentVector<Integer> k = com.github.krukow.clj_ds.Persistents.<Integer>vector();
        for (int j = 0; j < size; j++) {
          int x = random.nextInt(123);
          sum += x;
          a = a.push(x);
          p = p.plus(x);
          l = l.append(x);
          r = r.append(x);
          //k = k.plus(x);
        }
        _top = _top.push(a);
        _pvectors = _pvectors.plus(p);
        _paguro = _paguro.append(l);
        _rrbs = _rrbs.append(r);
        //_krukows.add(k);
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
  private static class Push2AtATimeBench extends BuildingBench {
    public String name() { return "Push2AtATimeBench     "; }
    public void run() {
      int x = 0;
      for (ImmutableCollection<Integer> a1 : _top) {
        int y = 0;
        for (ImmutableCollection<Integer> a2 : _top) {
          ImmutableCollection<Integer> both[] = new ImmutableCollection[1];
          both[0] = a1;
          Integer prev[] = new Integer[] { null };
          a2.forEach((e)-> {
            if (prev[0] == null) {
              prev[0] = e;
            } else {
              both[0] = both[0].push(prev[0], e);
              prev[0] = null;
            }
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
  private static class PagPush1AtATimeBench extends BuildingBench {
    public String name() { return "PagPush1AtATimeBench     "; }
    public void run() {
      int x = 0;
      for (ImList<Integer> a1 : _paguro) {
        int y = 0;
        for (ImList<Integer> a2 : _paguro) {
          ImList<Integer> both[] = new ImList[1];
          both[0] = a1;
          a2.forEach((e)-> {
            both[0] = both[0].append(e);
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
  private static class PagPush2AtATimeBench extends BuildingBench {
    public String name() { return "PagPush2AtATimeBench     "; }
    public void run() {
      int x = 0;
      for (ImList<Integer> a1 : _paguro) {
        int y = 0;
        for (ImList<Integer> a2 : _paguro) {
          ImList<Integer> both[] = new ImList[] { a1 };
          Integer prev[] = new Integer[] { null };
          a2.forEach((e)-> {
            if (prev[0] == null) {
              prev[0] = e;
            } else {
              both[0] = both[0].append(prev[0]).append(e);
              prev[0] = null;
            }
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
  private static class RrbPush1AtATimeBench extends BuildingBench {
    public String name() { return "RrbPush1AtATimeBench     "; }
    public void run() {
      int x = 0;
      for (RrbTree.ImRrbt<Integer> a1 : _rrbs) {
        int y = 0;
        for (RrbTree.ImRrbt<Integer> a2 : _rrbs) {
          RrbTree.ImRrbt<Integer> both[] = new RrbTree.ImRrbt[1];
          both[0] = a1;
          a2.forEach((e)-> {
            both[0] = both[0].append(e);
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
  private static class Insert1AtATimeBench extends BuildingBench {
    public String name() { return "Insert1AtATimeBench   "; }
    public void run() {
      int x = 0;
      for (ImmutableCollection<Integer> a1 : _top) {
        int y = 0;
        for (ImmutableCollection<Integer> a2 : _top) {
          ImmutableCollection<Integer> both[] = new ImmutableCollection[1];
          both[0] = a1;
          a2.forEach((e)-> {
            int position = both[0].size() >> 1;
            ImmutableCollection<Integer> left = both[0].subList(0, position);
            left = left.push(e);
            left = left.pushAll(both[0].subList(position, both[0].size()));
            both[0] = left;
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
  private static class PPush1AtATimeBench extends BuildingBench {
    public String name() { return "PPush1AtATimeBench    "; }
    public void run() {
      int x = 0;
      for (PVector<Integer> a1 : _pvectors) {
        int y = 0;
        for (PVector<Integer> a2 : _pvectors) {
          PVector<Integer> both[] = new TreePVector[1];
          both[0] = a1;
          a2.forEach((e)-> {
            both[0] = both[0].plus(e);
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
  private static class PInsert1AtATimeBench extends BuildingBench {
    public String name() { return "PInsert1AtATimeBench    "; }
    public void run() {
      int x = 0;
      for (PVector<Integer> a1 : _pvectors) {
        int y = 0;
        for (PVector<Integer> a2 : _pvectors) {
          PVector<Integer> both[] = new TreePVector[1];
          both[0] = a1;
          a2.forEach((e)-> {
            int position = both[0].size() >> 1;
            both[0] = both[0].plus(position, e);
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
  private static class RrbInsert1AtATimeBench extends BuildingBench {
    public String name() { return "RrbInsert1AtATimeBench  "; }
    public void run() {
      int x = 0;
      for (RrbTree<Integer> a1 : _rrbs) {
        int y = 0;
        for (RrbTree<Integer> a2 : _rrbs) {
          RrbTree<Integer> both[] = new RrbTree[1];
          both[0] = a1;
          a2.forEach((e)-> {
            int position = both[0].size() >> 1;
            both[0] = both[0].insert(position, e);
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

  @SuppressWarnings("unchecked")
  private static class PUnshifting1AtATimeBench extends BuildingBench {
    public String name() { return "PUnshifting1AtATimeBench "; }
    public void run() {
      int x = 0;
      for (PVector<Integer> a1 : _pvectors) {
        int y = 0;
        for (PVector<Integer> a2 : _pvectors) {
          PVector<Integer> both[] = new TreePVector[1];
          both[0] = a1;
          a2.forEach((e)-> {
            both[0] = both[0].plus(0, e);
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
  private static class RrbUnshifting1AtATimeBench extends BuildingBench {
    public String name() { return "RrbUnshifting1AtATimeBench "; }
    public void run() {
      int x = 0;
      for (RrbTree<Integer> a1 : _rrbs) {
        int y = 0;
        for (RrbTree<Integer> a2 : _rrbs) {
          RrbTree<Integer> both[] = new RrbTree.ImRrbt[1];
          both[0] = a1;
          a2.forEach((e)-> {
            both[0] = both[0].insert(0, e);
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

  private static class PagPushAllBench extends BuildingBench {
    public String name() { return "PagPushAllBench       "; }
    public void run() {
      int x = 0;
      for (ImList<Integer> a1 : _paguro) {
        int y = 0;
        for (ImList<Integer> a2 : _paguro) {
          ImList<Integer> both = a1.concat(a2);
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

  private static class RrbPushAllBench extends BuildingBench {
    public String name() { return "RrbPushAllBench       "; }
    public void run() {
      int x = 0;
      for (RrbTree.ImRrbt<Integer> a1 : _rrbs) {
        int y = 0;
        for (RrbTree.ImRrbt<Integer> a2 : _rrbs) {
          RrbTree.ImRrbt<Integer> both = a1.concat(a2);
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

  private static class RrbJoinAllBench extends BuildingBench {
    public String name() { return "RrbJoinAllBench       "; }
    public void run() {
      int x = 0;
      for (RrbTree.ImRrbt<Integer> a1 : _rrbs) {
        int y = 0;
        for (RrbTree.ImRrbt<Integer> a2 : _rrbs) {
          RrbTree<Integer> both = a1.join(a2);
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

  private static class InsertAllBench extends BuildingBench {
    public String name() { return "InsertAllBench          "; }
    public void run() {
      int x = 0;
      for (ImmutableCollection<Integer> a1 : _top) {
        int y = 0;
        for (ImmutableCollection<Integer> a2 : _top) {
          int position = a1.size() >> 1;
          ImmutableCollection<Integer> both = a1.subList(0, position);
          both = both.pushAll(a2);
          both = both.pushAll(a1.subList(position, a1.size()));
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

  private static class PagInsertAllBench extends BuildingBench {
    public String name() { return "PagInsertAllBench       "; }
    public void run() {
      int x = 0;
      for (ImList<Integer> a1 : _paguro) {
        int y = 0;
        for (ImList<Integer> a2 : _paguro) {
          int position = a1.size() >> 1;
          UnmodList<Integer> both = a1.subList(0, position);
          UnmodIterable<Integer> both2 = both.concat(a2);
          both2 = both2.concat(a1.subList(position, a1.size()));
          int sum[] = new int[1];
          both2.forEach((e) -> {
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

  private static class PInsertAllBench extends BuildingBench {
    public String name() { return "PInsertAllBench          "; }
    public void run() {
      int x = 0;
      for (PVector<Integer> a1 : _pvectors) {
        int y = 0;
        for (PVector<Integer> a2 : _pvectors) {
          int position = a1.size() >> 1;
          PVector<Integer> both = a1.plusAll(position, a2);
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

  private static class PPushAllBench extends BuildingBench {
    public String name() { return "PPushAllBench         "; }
    public void run() {
      int x = 0;
      for (PVector<Integer> a1 : _pvectors) {
        int y = 0;
        for (PVector<Integer> a2 : _pvectors) {
          PVector<Integer> both = a1.plusAll(a2);
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

  private static class PUnshiftingAllBench extends BuildingBench {
    public String name() { return "PUnshiftingAllBench      "; }
    public void run() {
      int x = 0;
      for (PVector<Integer> a1 : _pvectors) {
        int y = 0;
        for (PVector<Integer> a2 : _pvectors) {
          PVector<Integer> both = a1.plusAll(0, a2);
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

  private static class PagPushAllFromListBench extends BuildingBench {
    public String name() { return "PagPushAllFromListBench "; }
    public void run() {
      int x = 0;
      for (ImList<Integer> a1 : _paguro) {
        int y = 0;
        for (List<Integer> a2 : _lists) {
          ImList<Integer> both = a1.concat(a2);
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

  private static class RrbPushAllFromListBench extends BuildingBench {
    public String name() { return "RrbPushAllFromListBench "; }
    public void run() {
      int x = 0;
      for (RrbTree.ImRrbt<Integer> a1 : _rrbs) {
        int y = 0;
        for (List<Integer> a2 : _lists) {
          RrbTree.ImRrbt<Integer> both = a1.concat(a2);
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

  private static class PPushAllFromListBench extends BuildingBench {
    public String name() { return "PPushAllFromListBench  "; }
    public void run() {
      int x = 0;
      for (PVector<Integer> a1 : _pvectors) {
        int y = 0;
        for (List<Integer> a2 : _lists) {
          PVector<Integer> both = a1.plusAll(a2);
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

  private static class PUnshiftAllFromList extends BuildingBench {
    public String name() { return "PUnshiftAllFromList     "; }
    public void run() {
      int x = 0;
      for (PVector<Integer> a1 : _pvectors) {
        int y = 0;
        for (List<Integer> a2 : _lists) {
          PVector<Integer> both = a1.plusAll(0, a2);
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

  /*
   * The version of Karl Krukows port of CLJ data structures that is
   * available on Github no longer compiles due to language changes,
   * so this is commented out.  The changes needed are pretty minimal
   * (mostly just variables called _.
   *
  private static class KruForInBench extends IterationBench {
    public String name() { return "KruForIn  "; }

    public void run() {
      long answer = sumForIn();
      if (answer != sum()) throw new RuntimeException();
    }

    protected long sumForIn() {
      long sum = 0;
      for (com.github.krukow.clj_ds.PersistentVector<Integer> array : _krukows) {
        for (int x : array) {
          sum += x;
        }
      }
      return sum;
    }
  }

  @SuppressWarnings("unchecked")
  private static class KruPush1AtATimeBench extends BuildingBench {
    public String name() { return "KruPush1AtATimeBench  "; }
    public void run() {
      int x = 0;
      for (com.github.krukow.clj_ds.PersistentVector<Integer> a1 : _krukows) {
        int y = 0;
        for (com.github.krukow.clj_ds.PersistentVector<Integer> a2 : _krukows) {
          com.github.krukow.clj_ds.PersistentVector<Integer> both[] = new com.github.krukow.clj_ds.PersistentVector[1];
          both[0] = a1;
          a2.forEach((e)-> {
            both[0] = both[0].plus(e);
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

  private static class KruPushAllBench extends BuildingBench {
    public String name() { return "KruPushAllBench       "; }
    public void run() {
      int x = 0;
      for (com.github.krukow.clj_ds.PersistentVector<Integer> a1 : _krukows) {
        int y = 0;
        for (com.github.krukow.clj_ds.PersistentVector<Integer> a2 : _krukows) {
          com.github.krukow.clj_ds.PersistentVector<Integer> both = a1.plusAll(a2);
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
  */
}

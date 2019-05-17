// Copyright (C) 2019 Toitware ApS. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.toitware.immutable_test;
import com.toitware.immutable.ImmutableHashMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;

class ImmutableHashMapTest {
  public static void main(String args[]) {
    simpleTest();
    randomTest(true);
    randomTest(false);
    iteratorTest();
  }

  static private void check_empty(ImmutableHashMap<String, Object> empty) {
    assert(empty.isEmpty());
    assert(empty.size() == 0);
    assert(!empty.containsKey("foo"));
    assert(!empty.containsValue("foo"));
    assert(empty.get("foo") == null);
    assert(empty.getOrDefault("foo", "bar") == "bar");
  }

  static private void check_foo_bar(ImmutableHashMap<String, Object> foo_bar) {
    assert(!foo_bar.isEmpty());
    assert(foo_bar.size() == 1);
    assert(foo_bar.containsKey("foo"));
    assert(foo_bar.containsValue("bar"));
    assert(foo_bar.get("foo") == "bar");
    assert(foo_bar.getOrDefault("foo", "baz") == "bar");
    assert(foo_bar.get("bar") == null);
    assert(foo_bar.getOrDefault("bar", "baz") == "baz");
  }

  static private void check_fbfb(ImmutableHashMap<String, Object> fbfb) {
    assert(!fbfb.isEmpty());
    assert(fbfb.size() == 2);
    assert(fbfb.containsKey("foo"));
    assert(fbfb.containsValue("bar"));
    assert(fbfb.containsKey("fizz"));
    assert(fbfb.containsValue("buzz"));
    assert(fbfb.get("foo") == "bar");
    assert(fbfb.get("fizz") == "buzz");
    assert(fbfb.getOrDefault("foo", "baz") == "bar");
    assert(fbfb.getOrDefault("fizz", "baz") == "buzz");
    assert(fbfb.get("bar") == null);
    assert(fbfb.get("buzz") == null);
    assert(fbfb.getOrDefault("bar", "baz") == "baz");
    assert(fbfb.getOrDefault("buzz", "baz") == "baz");
  }

  static private void check_fbet(ImmutableHashMap<String, Object> fbet) {
    assert(!fbet.isEmpty());
    assert(fbet.size() == 2);
    assert(fbet.containsKey("foo"));
    assert(fbet.containsValue("bar"));
    assert(fbet.containsKey("en"));
    assert(fbet.containsValue("to"));
    assert(fbet.get("foo") == "bar");
    assert(fbet.get("en") == "to");
    assert(fbet.getOrDefault("foo", "baz") == "bar");
    assert(fbet.getOrDefault("en", "baz") == "to");
    assert(fbet.get("bar") == null);
    assert(fbet.get("to") == null);
    assert(fbet.getOrDefault("bar", "baz") == "baz");
    assert(fbet.getOrDefault("to", "baz") == "baz");
  }

  private static void simpleTest() {
    ImmutableHashMap<String, Object> empty = new ImmutableHashMap<>();
    check_empty(empty);

    ImmutableHashMap<String, Object> foo_bar = empty.put("foo", "bar");
    check_foo_bar(foo_bar);
    check_empty(empty);

    ImmutableHashMap<String, Object> fbfb = foo_bar.put("fizz", "buzz");
    check_fbfb(fbfb);
    check_foo_bar(foo_bar);
    check_empty(empty);

    ImmutableHashMap<String, Object> fbet = foo_bar.put("en", "to");
    check_fbet(fbet);
    check_fbfb(fbfb);
    check_foo_bar(foo_bar);
    check_empty(empty);

    ImmutableHashMap<String, Object> back_to_foo_bar = fbet.remove("en");
    check_foo_bar(back_to_foo_bar);
    check_fbet(fbet);

    ImmutableHashMap<String, Object> back_to_fbet = fbet.put("en", "to");
    check_fbet(back_to_fbet);
    check_foo_bar(back_to_foo_bar);
    check_fbet(fbet);
  }

  private @SuppressWarnings("unchecked") static void randomTest(boolean with_deletion) {
    final int MAPS = 10;
    final int KEYS = 30;
    final int ITERATIONS = 50000;
    ImmutableHashMap<String, Object> maps[] = new ImmutableHashMap[MAPS];
    for (int i = 0; i < MAPS; i++) maps[i] = new ImmutableHashMap<String, Object>();
    HashMap<String, Object> control_maps[] = new HashMap[MAPS];
    for (int i = 0; i < MAPS; i++) control_maps[i] = new HashMap<String, Object>();
    Random random = new Random(1034210342);
    for (int z = 0; z < ITERATIONS; z++) {
      int source = random.nextInt(MAPS);
      int dest;
      do {
        dest = random.nextInt(MAPS);
      } while (dest == source);
      String key = "" + random.nextInt(KEYS);
      String value = random.nextBoolean() ? key : "value" + key;
      control_maps[dest] = new HashMap<String, Object>(control_maps[source]);
      if (with_deletion) {
        if (random.nextInt(3) < 2) {
          // Add entry.
          maps[dest] = maps[source].put(key, value);
          control_maps[dest].put(key, value);
        } else {
          maps[dest] = maps[source].remove(key);
          control_maps[dest].remove(key);
        }
      } else {
        if (random.nextInt(30) == 0) {
          maps[dest] = new ImmutableHashMap<>();
          control_maps[dest] = new HashMap<>();
        } else {
          // Add entry.
          maps[dest] = maps[source].put(key, value);
          control_maps[dest].put(key, value);
        }
      }
      for (int i = 0; i < MAPS; i++) {
        assert(maps[i].size() == control_maps[i].size());
        for (int k = 0; k < KEYS; k++) {
          String key2 = "" + k;
          if (control_maps[i].containsKey(key2)) {
            assert(maps[i].containsKey(key2));
            assert(maps[i].get(key2) == control_maps[i].get(key2));
          } else {
            assert(!maps[i].containsKey(key2));
            assert(null == control_maps[i].get(key2));
          }
        }
      }
    }
  }

  static public void iteratorTest() {
    ImmutableHashMap<String, Object> empty = new ImmutableHashMap<>();
    check_empty(empty);

    for (String k : empty.keySet()) {
      assert(false);
    }

    ImmutableHashMap<String, Object> foo_bar = empty.put("foo", "bar");
    check_foo_bar(foo_bar);

    int count = 0;
    for (String k : foo_bar.keySet()) {
      assert(k == "foo");
      assert(foo_bar.get(k) == "bar");
      count++;
    }
    assert(count == 1);

    ImmutableHashMap<String, Object> ten_build = empty;
    for (int i = 0; i < 10; i++) {
      ten_build = ten_build.put("" + i, "value" + i);
    }
    ImmutableHashMap<String, Object> ten = ten_build;
    assert(ten.size() == 10);
    int i = '0';
    for (String k : ten.keySet()) {
      assert(k.length() == 1);
      assert(k.charAt(0) == i++);
      assert(ten.get(k).equals("value" + k));
    }
    assert(i == '0' + 10);
    for (Object v : ten.values()) {
      String s = (String)v;
      assert(s.length() == 6);
      assert(s.indexOf("value") == 0);
    }

    i = '0';
    for (Object v : ten.values()) {
      String s = (String)v;
      assert(s.length() == 6);
      assert(s.equals("value" + (i - '0')));
      assert(s.charAt(5) == i++);
    }
    assert(i == '0' + 10);

    ImmutableHashMap<String, Object> removed = ten.remove("4");
    assert(removed.size() == 9);
    i = '0';
    for (String k : removed.keySet()) {
      assert(k.length() == 1);
      if (i == '4') i++;
      assert(k.charAt(0) == i++);
      assert(ten.get(k).equals("value" + k));
    }
    assert(i == '0' + 10);

    int j[] = { 0 };
    removed.keySet().forEach((k) -> {
      assert ("" + j[0]).equals(k);
      if (j[0] == 3) j[0]++;
      j[0]++;
    });
    assert(j[0] == 10);
    j[0] = 0;
    removed.keySet().iterator().forEachRemaining((k) -> {
      assert ("" + j[0]).equals(k);
      if (j[0] == 3) j[0]++;
      j[0]++;
    });
    assert(j[0] == 10);
    j[0] = 0;
    removed.values().forEach((k) -> {
      assert ("value" + j[0]).equals(k);
      if (j[0] == 3) j[0]++;
      j[0]++;
    });
    assert(j[0] == 10);
    j[0] = 0;
    removed.values().iterator().forEachRemaining((k) -> {
      assert ("value" + j[0]).equals(k);
      if (j[0] == 3) j[0]++;
      j[0]++;
    });
    assert(j[0] == 10);

    i = '0';
    for (Object v : removed.values()) {
      String s = (String)v;
      assert(s.length() == 6);
      if (i == '4') i++;
      assert(s.equals("value" + (i - '0')));
      assert(s.charAt(5) == i++);
    }
    assert(i == '0' + 10);

    i = '0';
    for (Map.Entry<String, Object> entry : ten.entrySet()) {
      String k = entry.getKey();
      String v = (String)(entry.getValue());
      assert(k.length() == 1);
      assert(k.charAt(0) == i);
      assert(ten.get(k).equals("value" + k));
      assert(ten.get(k).equals(v));
      assert(v.length() == 6);
      assert(v.equals("value" + (i - '0')));
      assert(v.charAt(5) == i++);
    }
    assert(i == '0' + 10);

    i = '0';
    for (Map.Entry<String, Object> entry : removed.entrySet()) {
      String k = entry.getKey();
      String v = (String)(entry.getValue());
      if (i == '4') i++;
      assert(k.length() == 1);
      assert(k.charAt(0) == i);
      assert(removed.get(k).equals("value" + k));
      assert(removed.get(k).equals(v));
      assert(v.length() == 6);
      assert(v.equals("value" + (i - '0')));
      assert(v.charAt(5) == i);
      i++;
    }
    assert(i == '0' + 10);

    // Iterate the entries in most-recently-added first order.
    ListIterator<Map.Entry<String, Object>> iterator =
        removed.entries().listIterator(removed.size());
    ListIterator<String> key_iterator = removed.keys().listIterator(removed.size());
    ListIterator<Object> val_iterator = removed.values().listIterator(removed.size());
    while (iterator.hasPrevious()) {
      Map.Entry<String, Object> entry = iterator.previous();
      String k = entry.getKey();
      String v = (String)(entry.getValue());
      i--;
      if (i == '4') i--;
      assert(k.equals(key_iterator.previous()));
      assert(v.equals(val_iterator.previous()));
      assert(k.length() == 1);
      assert(k.charAt(0) == i);
      assert(removed.get(k).equals("value" + k));
      assert(removed.get(k).equals(v));
      assert(v.length() == 6);
      assert(v.equals("value" + (i - '0')));
      assert(v.charAt(5) == i);
    }
    assert(i == '0');
    assert(!key_iterator.hasPrevious());
    assert(!val_iterator.hasPrevious());

    int iBox[] = new int[1];
    iBox[0] = '0';
    ten.forEach((k, o)-> {
      String v = (String)o;
      assert(k.length() == 1);
      assert(k.charAt(0) == iBox[0]);
      assert(ten.get(k).equals("value" + k));
      assert(ten.get(k).equals(v));
      assert(v.length() == 6);
      assert(v.equals("value" + (iBox[0] - '0')));
      assert(v.charAt(5) == iBox[0]++);
    });
    assert(iBox[0] == '0' + 10);

    iBox[0] = '0';
    removed.forEach((k, o)-> {
      String v = (String)o;
      assert(k.length() == 1);
      if (iBox[0] == '4') iBox[0]++;
      assert(k.charAt(0) == iBox[0]);
      assert(removed.get(k).equals("value" + k));
      assert(removed.get(k).equals(v));
      assert(v.length() == 6);
      assert(v.equals("value" + (iBox[0] - '0')));
      assert(v.charAt(5) == iBox[0]++);
    });
    assert(iBox[0] == '0' + 10);
  }
}

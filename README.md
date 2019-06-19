# com.toitware.immutable

## Immutable data structures for Java

Here are some efficient and convenient fully persistent (immutable) data structures implemented for Java.

By "fully persistent" we mean that every updating operation creates a new instance of the collection without changing
the previous version of the collection.  For most languages, String is the only class that has this property.  You can
a new string, eg by appending a character, but the old string still exists, is unchanged, and can be used to create
other strings.

Because fully persistent data types are never modified after creation they are particularly easy to use in a
safe way in multithreaded programs.

## Classes

### ImmutableCollection

This is an array-like class that you can often use instead of ArrayList.

    ImmutableCollection<String> a = new ImmutableArray<>();  // Empty array of strings.
    a = empty.push("foo");                                   // Array with ["foo"]
    a = a.push("bar");                                       // Array with ["foo", "bar"]
    a = a.atPut(0, "fizz")                                   // Array with ["fizz", "bar"]
    
ImmutableArray is optimized for the append/push operation:

Operation | Complexity (average) | Explanation
----------|----------------------|------------
push      | O(1)                 | Append to end
atPut     | O(log size)          | One random element changed
subList   | O(log size)          | Create a slice of the original array
trim      | O(1)                 | Remove last (pop)
forEach   | O(size)              | Iterate over the whole list using forEach()
for       | O(size)              | Iterate using for : syntax (using an Iterator)
get       | O(log size)          | Random access to an element
get(a.size() - 1) | O(1)         | Access to last element
for (int..| O(size * log size)   | Iterate using an integer index

### ImmutableHashMap

This is a hash map that preserves insertion order, which can be used instead of HashMap or LinkedHashMap.
It is fully persistent, so every operation creates a new hash map with the relevant entry added, modified or removed.

    ImmutableMap<String, Integer> map = new ImmutableMap<>();  // Empty map.
    map = map.put("foo", 42);                                  // Map { "foo": 42 }.
    map = map.put("bar", 103);                                 // Map { "foo": 42, "bar": 103 }.
    map.containsKey("foo");                                    // True.

ImmutableHashMap is optimized for adding and iterating.

Operation | Complexity (average) | Explanation
----------| -------------------- | -----------
put       | O(1)                 | Add or update an entry.
containsKey | O(1)               | Check for presence of a key.
remove    | O(1)                 | Remove a key.
forEach   | O(size)              | Iterate over key-value pairs in insertion order
for( ...entrySet() | O(size)     | Iterate over key-value pairs using for : syntax

### Example

Here is an example showing that we can 'modify' a map while iterating over it.  Each 'modification' creates a new
map, independent of the immutable one we are iterating over.

    ImmutableMap<String, Integer> clean(ImmutableMap<String, Integer> map, int limit) {
      for (Map.Entry<String, Integer> entry : map.entrySet()) {
        if (entry.getValue() > limit) map = map.remove(entry.getKey());
      return map;
    }

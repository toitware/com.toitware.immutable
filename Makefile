# Copyright (C) 2019 Toitware ApS. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

PPATH=../pcollections/src/main/java
PAGPATH=../Paguro/src/main/java

all: doc bench test mem

doc:
	(mkdir -p docs; cd docs; CLASSPATH=..:../$(PPATH):../$(PAGPATH) javadoc -public com.toitware.immutable org.pcollections org.organicdesign.fp.collections)

bench:
	CLASSPATH=.:$(PPATH):$(PAGPATH) javac -Xlint:unchecked com/toitware/immutable_test/ImmutableBenchmark.java
	CLASSPATH=.:$(PPATH):$(PAGPATH) java com.toitware.immutable_test.ImmutableBenchmark

test:
	CLASSPATH=. javac -Xlint:unchecked com/toitware/immutable_test/ImmutableArrayTest.java
	CLASSPATH=. java -ea com.toitware.immutable_test.ImmutableArrayTest
	CLASSPATH=.:../pcollections/src/main/java javac -Xlint:unchecked com/toitware/immutable_test/ImmutableBenchmark.java
	CLASSPATH=. java com.toitware.immutable_test.ImmutableBenchmark
	CLASSPATH=. javac -Xlint:unchecked com/toitware/immutable_test/ImmutableHashMapTest.java
	CLASSPATH=. java -ea com.toitware.immutable_test.ImmutableHashMapTest

mem:
	CLASSPATH=.:$(PPATH):$(PAGPATH) javac -Xlint:unchecked com/toitware/immutable_test/ImmutableMemoryUse.java
	CLASSPATH=.:$(PPATH):$(PAGPATH) java -XX:+UseCompressedOops com.toitware.immutable_test.ImmutableMemoryUse

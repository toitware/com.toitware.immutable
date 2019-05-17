# Copyright (C) 2019 Toitware ApS. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

PPATH=../pcollections/src/main/java
PAGPATH=../Paguro/src/main/java
KRUPATH=../clj-ds/src/main/java

all: doc bench test mem

doc:
	(mkdir -p docs; cd docs; CLASSPATH=..:../$(PPATH):../$(PAGPATH):../$(KRUPATH) javadoc -public com.toitware.immutable org.pcollections org.organicdesign.fp.collections)

bench:
	CLASSPATH=.:$(PPATH):$(PAGPATH):$(KRUPATH) javac -Xlint:unchecked com/toitware/immutable_test/ImmutableBenchmark.java
	CLASSPATH=.:$(PPATH):$(PAGPATH):$(KRUPATH) java com.toitware.immutable_test.ImmutableBenchmark

test:
	CLASSPATH=. javac -Xlint:unchecked com/toitware/immutable_test/ImmutableHashMapTest.java
	CLASSPATH=. java -ea com.toitware.immutable_test.ImmutableHashMapTest
	CLASSPATH=. javac -Xlint:unchecked com/toitware/immutable_test/ImmutableArrayTest.java
	CLASSPATH=. java -ea com.toitware.immutable_test.ImmutableArrayTest

mem:
	CLASSPATH=.:$(PPATH):$(PAGPATH):$(KRUPATH) javac -Xlint:unchecked com/toitware/immutable_test/ImmutableMemoryUse.java
	CLASSPATH=.:$(PPATH):$(PAGPATH):$(KRUPATH) java -XX:+UseCompressedOops com.toitware.immutable_test.ImmutableMemoryUse

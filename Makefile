# Copyright (C) 2019 Toitware ApS. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

all:
	CLASSPATH=. javac -Xlint:unchecked com/toitware/immutable_test/ImmutableBenchmark.java
	CLASSPATH=. java com.toitware.immutable_test.ImmutableBenchmark
	CLASSPATH=. javac -Xlint:unchecked com/toitware/immutable_test/ImmutableArrayTest.java
	CLASSPATH=. java -ea com.toitware.immutable_test.ImmutableArrayTest
	CLASSPATH=. javac -Xlint:unchecked com/toitware/immutable_test/ImmutableHashMapTest.java
	CLASSPATH=. java -ea com.toitware.immutable_test.ImmutableHashMapTest
	(mkdir -p docs; cd docs; CLASSPATH=.. javadoc -public com.toitware.immutable)

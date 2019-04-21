# Copyright (C) 2019 Toitware ApS. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

all:
	CLASSPATH=/Users/erik/immutable javac -Xlint:unchecked com/toitware/immutable_test/ImmutableArrayTest.java 
	CLASSPATH=/Users/erik/immutable java -ea com.toitware.immutable_test.ImmutableArrayTest
	CLASSPATH=/Users/erik/immutable javac -Xlint:unchecked com/toitware/immutable_test/ImmutableHashMapTest.java 
	CLASSPATH=/Users/erik/immutable java -ea com.toitware.immutable_test.ImmutableHashMapTest

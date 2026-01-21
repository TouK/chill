[![Maven Central Version](https://img.shields.io/maven-central/v/pl.touk/chill_2.13)](https://central.sonatype.com/artifact/pl.touk/chill_2.13/versions)

# About this fork

This is a stripped down fork of xebialabs/chill, which in turn is a fork of twitter/chill.
It was made to provide base Scala serialization for [TouK/flink-scala](https://github.com/TouK/flink-scala), please don't depend on it in your projects.

Build warnings and uses of deprecated APIs are expected, we don't touch them for now.

Publishing:

1. Change version in _version.sbt_, commit changes
2. Add version tag (`git tag v...`)
3. Push changes to GitHub
4. Manually run the [_Publish_ workflow in GitHub Actions](https://github.com/TouK/chill/actions/workflows/publish.yml)
5. Change version to a `-SNAPSHOT` version in build.sbt
6. Commit and push changes

# Chill

Extensions for the [Kryo serialization library](https://github.com/EsotericSoftware/kryo) including
serializers and a set of classes to ease configuration of Kryo in systems like Hadoop, Storm,
Akka, etc.

### Building Chill

```bash
> sbt
sbt:chill-all>  compile # to build chill
sbt:chill-all>  publishM2 # to publish chill to your local .m2 repo
sbt:chill-all>  publishLocal # publish to local ivy repo.
```

## Chill-Java

The chill-java package includes the `KryoInstantiator` class (factory for Kryo instances)
and the `IKryoRegistrar` interface (adds Serializers to a given Kryo). These two are composable
to build instantiators that create instances of Kryo that have the options and serializers you
need. The benefit of this over a direct Kryo instance is that a Kryo instance is mutable and not
serializable, which limits the safety and reusability of code that works directly with them.

To deserialize or serialize easily, look at `KryoPool`:

```java
int POOL_SIZE = 10;
KryoPool kryo = KryoPool.withByteArrayOutputStream(POOL_SIZE, new KryoInstantiator());
byte[] ser = kryo.toBytesWithClass(myObj);
Object deserObj = kryo.fromBytes(myObj);
```

The KryoPool is a thread-safe way to share Kryo instances and temporary output buffers.

## Chill in Scala

Scala classes often have a number of properties that distinguish them from usual Java classes. Often
scala classes are immutable, and thus have no zero argument constructor. Secondly, `object` in scala is
a singleton that needs to be carefully serialized. Additionally, scala classes often have synthetic
(compiler generated) fields that need to be serialized, and by default Kryo does not serialize
those.

In addition to a `ScalaKryoInstantiator` which generates Kryo instances with options suitable for
scala, chill provides a number of Kryo serializers for standard scala classes (see below).

### The MeatLocker

Many existing systems use Java serialization. MeatLocker is an object that wraps a given instance
using Kryo serialization internally, but the MeatLocker itself is Java serializable.
The MeatLocker allows you to box Kryo-serializable objects and deserialize them lazily on the first call to `get`:

```scala
import com.twitter.chill.MeatLocker

val boxedItem = MeatLocker(someItem)

// boxedItem is java.io.Serializable no matter what it contains.
val box = roundTripThroughJava(boxedItem)
box.get == boxedItem.get // true!
```

To retrieve the boxed item without caching the deserialized value, use `meatlockerInstance.copy`.

### Serializers for Scala classes

These are found in the `chill-scala` directory in the chill jar (originally this project was
only scala serializers).  Chill provides support for singletons, scala Objects and the following types:

* Scala primitives
  * scala.Enumeration values
  * scala.Symbol
  * scala.reflect.Manifest
  * scala.reflect.ClassManifest
  * scala.Function[0-22] closure cleaning (removing unused `$outer` references).
* Collections and sequences
  * scala.collection.immutable.Map
  * scala.collection.immutable.List
  * scala.collection.immutable.Vector
  * scala.collection.immutable.Set
  * scala.collection.mutable.{Map, Set, Buffer, WrappedArray}
  * all 22 scala tuples

## Documentation

To learn more and find links to tutorials and information around the web, check out the original [Chill Wiki](https://github.com/twitter/chill/wiki).

## Original authors

* Oscar Boykin <https://twitter.com/posco>
* Mike Gagnon <https://twitter.com/MichaelNGagnon>
* Sam Ritchie <https://twitter.com/sritchie>

## License

Copyright 2012 Twitter, Inc.

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

[![CodeFactor](https://www.codefactor.io/repository/github/dmitriy-iliyov/hibernate-uuidv7-generator/badge)](https://www.codefactor.io/repository/github/dmitriy-iliyov/hibernate-uuidv7-generator)
[![codecov](https://codecov.io/github/dmitriy-iliyov/hibernate-uuidv7-generator/branch/main/graph/badge.svg)](https://codecov.io/github/dmitriy-iliyov/hibernate-uuidv7-generator)
[![CI](https://github.com/dmitriy-iliyov/hibernate-uuidv7-generator/actions/workflows/ci.yaml/badge.svg)](https://github.com/dmitriy-iliyov/hibernate-uuidv7-generator/actions/workflows/ci.yaml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.dmitriy-iliyov.hbrntuuidv7gen/hibernate-uuidv7-generator.svg?label=maven-central&color=blue)](https://central.sonatype.com/artifact/io.github.dmitriy-iliyov.hbrntuuidv7gen/hibernate-uuidv7-generator)
[![javadoc](https://javadoc.io/badge2/io.github.dmitriy-iliyov.hbrntuuidv7gen/hibernate-uuidv7-generator/javadoc.svg)](https://javadoc.io/doc/io.github.dmitriy-iliyov.hbrntuuidv7gen/hibernate-uuidv7-generator)
![GitHub Release](https://img.shields.io/github/v/release/dmitriy-iliyov/hibernate-uuidv7-generator?include_prereleases)
![GitHub last commit](https://img.shields.io/github/last-commit/dmitriy-iliyov/hibernate-uuidv7-generator)

## Overview

Time-ordered UUID (RFC 9562, version 7) primary keys for Hibernate ORM, behind a single annotation.  No `@GeneratedValue`, no database default, no sequence table. The identifier is assigned in memory
right before the `INSERT`, so it is available as soon as `persist()` returns.

## Quick Start

1. Add dependency in pom.xml
```xml
<dependency>
    <groupId>io.github.dmitriy-iliyov.hbrntuuidv7gen</groupId>
    <artifactId>hibernate-uuidv7-generator</artifactId>
    <version>1.0.0</version>
</dependency>
```

2. Annotate a `UUID` identifier. Nothing else is required - no configuration property, no Spring bean, no `persistence.xml` entry. Hibernate discovers the generator through `@IdGeneratorType` while building the `SessionFactory`.

```java
@Entity
@Table(name = "notes")
public class Note {

    @Id
    @UUIDv7
    private UUID id;

    @Column(nullable = false)
    private String title;

    protected Note() { }

    public Note(String title) {
        this.title = title;
    }

    public UUID getId() {
        return id;
    }
}
```

## Why UUIDv7

A UUIDv7 puts a 48-bit Unix millisecond timestamp in its most significant bits and fills the rest
with randomness. Consecutive values therefore sort by creation time, and inserts land at the right
edge of the primary-key B-tree instead of scattering across it the way random UUIDv4 keys do — less
page splitting, a warmer buffer cache, and `ORDER BY id` that follows insertion order.

Ordering holds *between* milliseconds: two values created inside the same millisecond share a
timestamp and are ordered arbitrarily relative to each other. Uniqueness is unaffected.

## Requirements

|                    |                                                                  |
|--------------------|------------------------------------------------------------------|
| Java               | 21+                                                              |
| Hibernate ORM      | 6.6.x (built against `6.6.26.Final`, verified on `6.6.53.Final`) |
| Runtime dependency | `com.github.f4b6a3:uuid-creator` (pulled in transitively)        |

`hibernate-core` is declared as `provided` - the library uses whichever Hibernate your application
already brings, e.g. the one from `spring-boot-starter-data-jpa`.

### Custom generator

`@UUIDv7` delegates the actual value to a `UuidGenerator`. Point the annotation at your own
implementation when you need a different source - a monotonic counter in tests, a node-id-tagged
variant, a custom clock:

```java
public class SequencedUuidGenerator implements UuidGenerator {

    private final AtomicLong counter = new AtomicLong();
  
    // a no-arg constructor is required
    public SequencedUuidGenerator() { }

    @Override
    public UUID generate() {
        return new UUID(0x0000_0000_0000_7000L, 0x8000_000000000000L | counter.incrementAndGet());
    }
}
```

```java
@Id
@UUIDv7(generator = SequencedUuidGenerator.class)
private UUID id;
```

The implementation must be a concrete class with an accessible no-argument constructor; anything
else fails at `SessionFactory` bootstrap with a `HibernateException`, not on the first insert.

## Public API

| Type                   | Role                                                                |
|------------------------|---------------------------------------------------------------------|
| `@UUIDv7`              | Marks the identifier field; optional `generator()` attribute        |
| `UuidGenerator`        | Functional interface producing the `UUID` values                    |
| `DefaultUuidGenerator` | Default implementation, RFC 9562 v7 via `uuid-creator`              |
| `UUIDv7Generator`      | Hibernate-facing generator; not referenced directly by applications |

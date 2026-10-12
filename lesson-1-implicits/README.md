# Cats Training

Internal training series on functional programming in Scala with the
[Cats](https://typelevel.org/cats/) library. Each session lives on its
own branch so you can check out the exact code that matches the
recording and slides.

## Sessions

| # | Topic | Branch | Scala |
|---|-------|--------|-------|
| 1 | Cats Intro & Implicits | [`lesson-1-implicits`](https://github.com/PabloJSartirana/cats-training/tree/lesson-1-implicits) | 2.13 |
| 2 | Type Classes | [`lesson-2-typeClasses`](https://github.com/PabloJSartirana/cats-training/tree/lesson-2-typeClasses) | 3.3 |

## Getting started

```bash
git clone git@github.com:PabloJSartirana/cats-training.git
cd cats-training
git switch <branch-name>   # e.g. lesson-2-typeClasses
sbt compile
```

The first `sbt compile` will download Scala, cats-core, and any other
dependencies — give it a minute the first time.

## Prerequisites

- JDK 17 or newer (JDK 21 recommended)
- sbt 1.11.7 (specified in `project/build.properties` — sbt fetches
  this version automatically)
- IntelliJ IDEA with the Scala plugin, or VS Code with Metals

## Session 1 — Cats Intro & Implicits

**Branch:** `lesson-1-implicits` (Scala 2.13)

Topics covered:

- Cats introduction
- Implicit classes
- Implicit values
- Implicit arguments
- Worked example: JSON serializer

## Session 2 — Type Classes

**Branch:** `lesson-2-typeClasses` (Scala 3.3)

Topics covered:

- The type-class pattern from first principles
  (trait, instances, interface object, interface syntax)
- Cats' `Show` type class — pretty-printing done properly
- Cats' `Eq` type class — type-safe equality

## Session recordings

Recordings and any supplementary materials for each session live in
the shared Training folder on Google Drive. Ask Pablo for access if
you need it.

# Cats Training

Internal training series on functional programming in Scala with the
[Cats](https://typelevel.org/cats/) library. Each session lives on its
own branch so you can check out the exact code that matches the
recording and slides.

## Sessions

| # | Topic | Branch | Scala |
|---|-------|--------|-------|
| 1 | Implicits | [`lesson-1-implicits`](https://github.com/PabloJSartirana/cats-training/tree/lesson-1-implicits) | 2.13 |
| 2 | Type Classes | [`lesson-2-typeClasses`](https://github.com/PabloJSartirana/cats-training/tree/lesson-2-typeClasses) | 3.3 |
| 3 | Monoids, Functors and Monads | [`lesson-3-monads`](https://github.com/PabloJSartirana/cats-training/tree/lesson-3-monads) | 3.3 |

## Getting started

```bash
git clone git@github.com:PabloJSartirana/cats-training.git
cd cats-training
git switch <branch-name>   # e.g. lesson-2-typeClasses
sbt compile
```


## Session 1: Implicits

**Branch:** `lesson-1-implicits` (Scala 2.13)

Topics covered:

- Implicit classes
- Implicit values
- Implicit arguments
- Worked example: JSON serializer

## Session 2: Type Classes

**Branch:** `lesson-2-typeClasses` (Scala 3.3)

Topics covered:

- The type-class pattern from first principles
  (trait, instances, interface object, interface syntax)
- Cats' `Show` type class, pretty-printing
- Cats' `Eq` type class, type-safe equality

## Session 3: Monoids, Functors and Monads

**Branch:** `lesson-3-monads` (Scala 3.3)

Topics covered, in class order:

- `Monoids.scala`: Monoid and Semigroup, the laws, Cats instances, `|+|` syntax
- `Functors.scala`: the std-library map vs Cats' Functor, `F[_]`, the Functor
  signature and laws, Functor syntax, writing our own instance, compose
- `Monads.scala`: map vs flatMap with counterexamples, fail fast,
  for comprehensions, Option/Either/List/Future, the Monad type class

## Session recordings

Recordings for each session live in the shared Training folder on Google Drive.

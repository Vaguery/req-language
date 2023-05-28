# DEPRECATED

A new version, with a substantially different memory model and underlying data structure(s), is being developed.

# ReQ (first version)

`ReQ`---pronounced "re-queue" or "wreck", depending on how long you've stared at it---is a new language intended for genetic programming applications. It's a queue-based (as opposed to stack-based) language, with a strong and extensible type system, tight integration to Clojure (in which it's written), polymorphic monadic functional style, and surprisingly promiscuous computational dynamics.

In other words, it's _not for people_. My design goals are simple, even if the result isn't: the language is relatively small but extensible, and the path for extending the core libraries to work in a specific domain is as simple as I can manage to make it. This simplicity of setup comes with some trade-offs, however....

The `ReQ` interpreter queue is loaded with the initial program tokens, and in each processing cycle the interpreter:

1. pop the next item from the queue, placing it in the "hot seat"
2. check to see whether the item in the hot seat is an `Imperative`, and if so it applies the function specified to the running Interpreter itself (`GOTO 1`)
3. check to see if the item in the hot seat is a `Nullary`, and if so append the results of calling its function to the queue (`GOTO 1`)
2. decide whether the item in the hot seat _wants_ the new top item on the queue; if it does want that item as an argument, it _consumes_ it, immediately pushing the result(s) to the tail of the queue
3. if the item in the hot seat doesn't want the top item, decide whether it _is wanted by_ the top item on the queue; if it is wanted, then that item _consumes_ the one in the hot seat, immediately pushing the result(s) to the tail of the queue
4. if neither of the item in the hot seat and the top queue item _wants_ the other, pop the top item and send it to the tail of the queue, leaving the item in the hot seat, and `GOTO 2 or 5`
5. if the item in the hot seat doesn't interact with _any_ items found on the queue (that is, if the queue pops through a full cycle), send the item in the hot seat to the tail and carry on
6. update any `Channel` states, if there are connections from the outside world

That's it.

The sense by which one `ReQ` item "wants" another as an _argument_. Functions like `«+»` or `«dup»` "want" any argument they could potentially act on in any of their polymorphic meanings; for example, `«+»` can add numbers, points, vectors or matrices (of the right size), or it can concatenate strings or collections, or produce the union of two sets. The "pure" function therefore _wants_ all those types of item as a potential argument, but when it _consumes_ an argument it becomes a new _partially applied function_ (a `Qlosure`) with a new set of "wants". If a particular `«+»` item consumes an `81`, the resulting `Qlosure` item will want numbers only---not strings or sets or songs. If instead `«+»` consumes a vector `[3.2 9.1]`, the resulting `Qlosure` item can "want" several things, in a preferred order: it can do "vector addition" if it finds another 2-element numerical vector first, or "concatenation" if it finds some other collection (of any sort) first.

When the last argument of a `Qlosure` is assigned, the result is produced. So to add `17+2` with `«+»`, the process involves _four_ distinct `ReQ` items going through two distinct stages:

```text
«+» + 17 -> «17+_»   ;; function «+» wants 17, consumes it, produces Qlosure «17+_»
«17+_» + 2 -> 19     ;; Qlosure «17+_» wants 2, consumes it, produces 19
```

A few important observations:

- `ReQ` scripts never "terminate" in the sense other computational systems are expected to. A queue filled with non-interacting literals will cycle forever; even an empty queue can be thought of as "cycling".
- Communication with the "outside world" occurs through special `Channel` items. These can hold a single value, and can be read and written by instructions in the same way other `ReQ` items interact with one another.
- Because of the unusual "sorting" behavior of the queue, the _partial_ token order determines the detailed dynamics of the program. The same tokens in slightly different orders can have dramatically different meanings, but will almost certainly sort themselves out into _some_ result in any case.

## Status

This version has been mothballed. A new version is being sketched in 2019, and a release called `ReQ` (vs "ReQ" is coming) in first quarter 2019.

### Done

- `ReQ` interpreter
- `Qlosure` items
- `Nullary` items: `Qlosure` items with no arguments
- `Immortal` items: wrapper around any standard `ReQ` item, protecting it from being consumed when used as an argument
- `Channel` items: `Immortal` items which can be written and read by "the outside world"
- convenience functions for producing problem-specific `ReQ` items
- the `⬍SELF⬍` item, which is a special `Immortal` item that acts as a proxy for the running `Interpreter` state as an argument
- `Imperative` items: `Qlosure` items which affect the running `Interpreter` iself
- some functions that act on collections and wrapped-collections (e.g. `Interpreter` instances)

### Active development

- writing to and taking from `Channel` items
- channel list explicitly stored in the interpreter record
- core instruction and type set:
  - numbers
  - booleans
  - vectors
  - strings
  - sets
  - control structures
  - higher-order functions
- instruction definitions stored in interpreter record
- `Gatherer` items: `Qlosure` items that build collection types
- convenience methods for quickly defining mixed-type `Qlosure` items

### To Do

- `«fork»` and other instructions for concurrency
- search:
    - random code generation
    - rubrics
    - crossover
    - mutation
    - hillclimbing
    - lexicase selection

### Minimum viable release:

A working interpreter with: Channels; convenient ways to define new instructions and types; works for arbitrary instruction Qlosures (any number or type of arguments and return types); problem definition; training and test data; 

### Some day

- return type for Qlosures
- nested Qlosures (consuming Qlosures as arguments, and capturing arguments)


## Qlosures and how they interact

The interaction between various types of `Qlosure` items is where "user modeling" happens. Against a backdrop of core functionality one expects from any algorithmic system (arithmetic, logic, string-handling, collections), the user can define new domain-specific types, and more importantly a suite of _functions_ which connect these new types to the existing core through new `Qlosure` definitions.

A `Qlosure` item represents a function with positional arguments: that is, if (as with arithmetic functions) there are two `Number` arguments, the pure `«+»` function will only _want_ the first of those. The `Qlosure` that results when it consumes a number will _want_ the second, and so forth.

In the case of functions that take multiple arguments of multiple types, arguments are assigned in a strict left-to-right order, for each type. So for example if we had a silly hypothetical function `«foo»` that wanted three numbers and two strings `foo(n1, n2, n3, s1, s2)`, there are only two "real" wants: a number for `n1` and a string for `s1`. There's no way (at present) for the interpreter itself to act in any other way, though it's feasible with the Clojure functions already in place to define especially convoluted `Qlosure` intermediate steps that somehow filled in arguments in some other order. Why you'd want that, I don't actually know.

This is not to say that once a function grabs an argument, a deterministic process kicks in: a `Qlosure` is a `ReQ` item like any other, and there are other items that could want it as an argument. Not least `«delete»`, which will blithely destroy anything at all, but also a `Gatherer` could come along and stick the `Qlosure` into a collection (where in general it won't see the hot seat), or it could be made `Immortal` and persist as it is now, even after it consumes the next item in its wants list.

In other words, even though program flow is (mostly) deterministic, it's the detailed interaction _between_ `ReQ` items that truly determines the particular outcome of a program---not a programmer's sense of "flow". Please keep that in mind.

### some sketches of simple scripts running

In these sketches, I've shown the queue as a square-bracketed collection of tokens, with the "hot seat" at the left and the "tail" at the right.

#### some simple arithmetic

[TBD]

#### collection-gatherers

[TBD]

#### modifiers

[TBD]

#### "return values" and channels (and fitness calculations)

[TBD]

#### problem-specific `ReQ`

[TBD]

## tests

The project uses [Midje](https://github.com/marick/Midje/).

### How to run the tests

`lein midje` will run all tests.

`lein midje namespace.*` will run only tests beginning with "namespace.".

`lein midje :autotest` will run all the tests indefinitely. It sets up a
watcher on the code files. If they change, only the relevant tests will be
run again.

# ReQ

`ReQ` (pronounced "wreck" or "re-queue", depending on how long you've tried to use it) is a language for genetic programming. Its... well, it's odd, see.

The ReQ interpreter uses a single queue for running programs. All ReQ "items" (or "tokens" I guess?) act a bit like messages, and a bit like objects.

The interpreter cycle is quite simple:

1. pop the next item off the queue
2. determine whether that item _addresses_ or _is addressed by_ the interpreter itself (some instructions, for instance, affect interpreter state); if it does, do what's expected
3. if the item and the interpreter do not address one another, determine whether the popped item addresses or is addressed by the next item on the queue; if so, do what's expected, and push the result onto the (tail of the) queue
4. if the popped item does not interact with any items found on the queue (that is, if the queue undergoes a full cycle), requeue it

That's it.

## items "addressing" one another

- a number addresses certain functions and produces a partial or complete result
  - `77`∘`+` -> `77+«num»->«num»`
  - `77`∘`neg` -> `-77`
  - `0`∘`sin` -> `0.0`
- some functions address a number to produce a partial or complete result
  - `+`∘`77` -> `«num»+77->«num»`
  - `neg`∘`77` -> `-77`
  - `cos`∘`π` -> `-1`
- a collection can address functions which apply to it
  - `(1 2 3)`∘`shatter` -> `1`,`2`,`3` (three results)
  - `[2 4 6]`∘`contain?` -> `[2 4 6].contain?(«any»)->«bool»`
  - `{"a" "b" "b"}`∘`union` -> `{"a" "b" "b"}.union(«set»)->«set»`
- some functions can also address collections to produce a partial or complete result
  - `+`∘`(7 1 2)` -> `«seq»+(7 1 2)`
  - `reverse`∘`[2 1 0]` -> `[0 1 2]`
  - `map`∘`(1..99)` -> `map(«num»->«T»,(1..99)->«(T)»`

(As you may have noticed, I'm using a type system very similar to that found in Apple's Swift language here).

### some sketches of simple scripts running

In these sketches, I've shown the queue as a square-bracketed collection of tokens, with the "head" at the left and the "tail" at the right.

#### some simple arithmetic

#### some more complicated interpreter-affecting instructions

#### collection-gatherers

#### adverbs and adjectives

## tests

The project uses [Midje](https://github.com/marick/Midje/).

### How to run the tests

`lein midje` will run all tests.

`lein midje namespace.*` will run only tests beginning with "namespace.".

`lein midje :autotest` will run all the tests indefinitely. It sets up a
watcher on the code files. If they change, only the relevant tests will be
run again.

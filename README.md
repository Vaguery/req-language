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

```text
[1 2 + * 3 4 - 5 6 ÷]
1 [2 + * 3 4 - 5 6 ÷]
1 [+ * 3 4 - 5 6 ÷ 2]
[* 3 4 - 5 6 ÷ 2 1+«num»->«num»]
* [3 4 - 5 6 ÷ 2 1+«num»->«num»]
[4 - 5 6 ÷ 2 1+«num»->«num» «num»*3->«num»]
4 [- 5 6 ÷ 2 1+«num»->«num» «num»*3->«num»]
[5 6 ÷ 2 1+«num»->«num» «num»*3->«num» 4-«num»->«num»]
5 [6 ÷ 2 1+«num»->«num» «num»*3->«num» 4-«num»->«num»]
5 [÷ 2 1+«num»->«num» «num»*3->«num» 4-«num»->«num» 6]
[2 1+«num»->«num» «num»*3->«num» 4-«num»->«num» 6 5÷«num»->«num»]
2 [1+«num»->«num» «num»*3->«num» 4-«num»->«num» 6 5÷«num»->«num»]
[«num»*3->«num» 4-«num»->«num» 6 5÷«num»->«num» 3]
«num»*3->«num» [4-«num»->«num» 6 5÷«num»->«num» 3]
[6 5÷«num»->«num» 3 4-(«num»*3)->«num»]
6 [5÷«num»->«num» 3 4-(«num»*3)->«num»]
[3 4-(«num»*3)->«num» 5/6]
3 [4-(«num»*3)->«num» 5/6]
[5/6 -5]
[repeat forever]
```

#### some more complicated interpreter-affecting instructions

```text
[1 land 6 * swap skip + jump 5 x times pause]
1 [land 6 * swap skip + jump 5 x times pause]
1 [6 * swap skip + jump 5 x times pause land]
1 [* swap skip + jump 5 x times pause land 6]
[swap skip + jump 5 x times pause land 6 1*«num»->«num»]
swap [skip + jump 5 x times pause land 6 1*«num»->«num»]
[+ skip jump 5 x times pause land 6 1*«num»->«num»]
+ [skip jump 5 x times pause land 6 1*«num»->«num»]
[jump 5 x times pause land 6 1*«num»->«num» +]
jump [5 x times pause land 6 1*«num»->«num» +]
jump [x times pause land 6 1*«num»->«num» + 5]
jump [times pause land 6 1*«num»->«num» + 5 x]
jump [pause land 6 1*«num»->«num» + 5 x times]
jump [land 6 1*«num»->«num» + 5 x times pause]
[6 1*«num»->«num» + 5 x times pause]
6 [1*«num»->«num» + 5 x times pause]
[+ 5 x times pause 6]
+ [5 x times pause 6]
[x times pause 6 «num»+5->«num»]
x [times pause 6 «num»+5->«num»]
[times pause 6 «num»+5->«num» 991]  # gets x from environment
times [pause 6 «num»+5->«num» 991]
times [6 «num»+5->«num» 991 pause]
[«num»+5->«num» 991 pause 6_times]
«num»+5->«num» [991 pause 6_times]
[pause 6_times 996]
pause [6_times 996]
[paused]
```

#### collection-gatherers

```text
[1 ) + ( 3 swap 5 false ) 6 ÷]
1 [) + ( 3 swap 5 false ) 6 ÷]
1 [+ ( 3 swap 5 false ) 6 ÷ )]
[( 3 swap 5 false ) 6 ÷ ) 1+«num»->«num»]
( [3 swap 5 false ) 6 ÷ ) 1+«num»->«num»]
[swap 5 false ) 6 ÷ ) 1+«num»->«num» (3,«any»)->«list»]
swap [5 false ) 6 ÷ ) 1+«num»->«num» (3,«any»)->«list»]
[false 5 ) 6 ÷ ) 1+«num»->«num» (3,«any»)->«list»]
false [5 ) 6 ÷ ) 1+«num»->«num» (3,«any»)->«list»]
...
[5 ) 6 ÷ ) 1+«num»->«num» (3,«any»)->«list» false]  # no response
5 [6 ÷ ) 1+«num»->«num» (3,«any»)->«list» false )]
5 [÷ ) 1+«num»->«num» (3,«any»)->«list» false ) 6]
[) 1+«num»->«num» (3,«any»)->«list» false ) 6 5÷«num»->«num»]
) [1+«num»->«num» (3,«any»)->«list» false ) 6 5÷«num»->«num»]
) [(3,«any»)->«list» false ) 6 5÷«num»->«num» 1+«num»->«num»]
[false ) 6 5÷«num»->«num» 1+«num»->«num» (3)]     # collector is closed
false [) 6 5÷«num»->«num» 1+«num»->«num» (3)]
[) 6 5÷«num»->«num» 1+«num»->«num» (3) false]
) [6 5÷«num»->«num» 1+«num»->«num» (3) false]
[6 5÷«num»->«num» 1+«num»->«num» (3) false )]
6 [5÷«num»->«num» 1+«num»->«num» (3) false )]
[1+«num»->«num» (3) false ) 5/6]
1+«num»->«num» [(3) false ) 5/6]
1+«num»->«num» [false ) 5/6 (3)]
1+«num»->«num» [) 5/6 (3) false]
1+«num»->«num» [5/6 (3) false )]
[(3) false ) 11/6]
[begin loop]
```

#### adverbs and adjectives

~~~ text
# (exploring the ⥀ “don’t consume args” modifier)
# type hints are hidden
[1 dup 2 ⥀ + ⥀ * 3 4 - 5 ⥀ 6 ÷]
1 [dup 2 ⥀ + ⥀ * 3 4 - 5 ⥀ 6 ÷]
1 [2 ⥀ + ⥀ * 3 4 - 5 ⥀ 6 ÷ dup]
1 [⥀ + ⥀ * 3 4 - 5 ⥀ 6 ÷ dup 2]
[+ ⥀ * 3 4 - 5 ⥀ 6 ÷ dup 2 1⥀]
+ [⥀ * 3 4 - 5 ⥀ 6 ÷ dup 2 1⥀]
[* 3 4 - 5 ⥀ 6 ÷ dup 2 1⥀ +⥀]
* [3 4 - 5 ⥀ 6 ÷ dup 2 1⥀ +⥀]
[4 - 5 ⥀ 6 ÷ dup 2 1⥀ +⥀ _*3]
4 [- 5 ⥀ 6 ÷ dup 2 1⥀ +⥀ _*3]
[5 ⥀ 6 ÷ dup 2 1⥀ +⥀ _*3 4-_]
5 [⥀ 6 ÷ dup 2 1⥀ +⥀ _*3 4-_]
[6 ÷ dup 2 1⥀ +⥀ _*3 4-_ 5⥀]
6 [÷ dup 2 1⥀ +⥀ _*3 4-_ 5⥀]
[dup 2 1⥀ +⥀ _*3 4-_ 5⥀ 6÷_]
dup [2 1⥀ +⥀ _*3 4-_ 5⥀ 6÷_]
[2 2 1⥀ +⥀ _*3 4-_ 5⥀ 6÷_]
2 [2 1⥀ +⥀ _*3 4-_ 5⥀ 6÷_]
2 [1⥀ +⥀ _*3 4-_ 5⥀ 6÷_ 2]
2 [+⥀ _*3 4-_ 5⥀ 6÷_ 2 1⥀]
[_*3 4-_ 5⥀ 6÷_ 2 1⥀ 2+_⥀]
[5⥀ 6÷_ 2 1⥀ 2+_⥀ (4-_)+3]
5⥀ [6÷_ 2 1⥀ 2+_⥀ (4-_)+3]
[2 1⥀ 2+_⥀ (4-_)+3 6/5 5⥀]
2 [1⥀ 2+_⥀ (4-_)+3 6/5 5⥀]
2 [2+_⥀ (4-_)+3 6/5 5⥀ 1⥀]
[(4-_)+3 6/5 5⥀ 1⥀ 4 2+_⥀]
[5⥀ 1⥀ 4 2+_⥀ 5.8]
5⥀ [1⥀ 4 2+_⥀ 5.8]
5⥀ [4 2+_⥀ 5.8 1⥀]
5⥀ [2+_⥀ 5.8 1⥀ 4]
[5.8 1⥀ 4 7 5⥀ 2+_⥀]
5.8 [1⥀ 4 7 5⥀ 2+_⥀]
5.8 [4 7 5⥀ 2+_⥀ 1⥀]
5.8 [7 5⥀ 2+_⥀ 1⥀ 4]
5.8 [5⥀ 2+_⥀ 1⥀ 4 7]
5.8 [2+_⥀ 1⥀ 4 7 5⥀]
[1⥀ 4 7 5⥀ 7.8 2+_⥀]
1⥀ [4 7 5⥀ 7.8 2+_⥀]
…
1⥀ [2+_⥀ 4 7 5⥀ 7.8]
[4 7 5⥀ 7.8 3 1⥀ 2+_⥀]
4 [7 5⥀ 7.8 3 1⥀ 2+_⥀]
…
4 [2+_⥀ 7 5⥀ 7.8 3 1⥀]
[7 5⥀ 7.8 3 1⥀ 6 2+_⥀]
7 [5⥀ 7.8 3 1⥀ 6 2+_⥀]
…
7 [2+_⥀ 5⥀ 7.8 3 1⥀ 6]
[5⥀ 7.8 3 1⥀ 6 9 2+_⥀]
5⥀ [7.8 3 1⥀ 6 9 2+_⥀]
…
5⥀ [2+_⥀ 7.8 3 1⥀ 6 9]
[7.8 3 1⥀ 6 9 7 5⥀ 2+_⥀]
[and so on]
~~~


## tests

The project uses [Midje](https://github.com/marick/Midje/).

### How to run the tests

`lein midje` will run all tests.

`lein midje namespace.*` will run only tests beginning with "namespace.".

`lein midje :autotest` will run all the tests indefinitely. It sets up a
watcher on the code files. If they change, only the relevant tests will be
run again.

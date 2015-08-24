# ReQ

`ReQ` (pronounced "wreck" or "re-queue", depending on how long you've tried to use it) is a language for genetic programming. Its... well, it's odd, see.

The ReQ interpreter uses a single queue for running programs. All ReQ "items" (or "tokens" or ... OK, I'm going to call them "Qlosures" because they're not all closures but some are and they can almost all make closures in one way or another, and Clojure has `Closure` as a protected word...) act a bit like messages, and a bit like objects.

The interpreter cycle is quite simple:

1. pop the next item off the queue
2. determine whether that item _addresses_ or _is addressed by_ the interpreter itself (some instructions, for instance, affect interpreter state); if it does, do what's expected
3. if the item and the interpreter do not address one another, determine whether the popped item addresses or is addressed by the next item on the queue; if so, do what's expected, and push the result onto the (tail of the) queue
4. if the popped item does not interact with any items found on the queue (that is, if the queue undergoes a full cycle), requeue it
5. update any `channel` states, if there are connections from the outside world

That's it.

## Status

Not working yet. Close, but the design is still emerging.

## Qlosures and how they interact

Because I'm not a Computer Sciencer, I'm going to use the word "address" when speaking of what one token does to another in the `ReQ` language. This is something like "being compatible", and something like "being an argument for" and something like "recognizing", but to be honest I don't have a good word for the concept as sketched. Also I would be happy to accept improvements and suggestions.

The basic concept is this: Functions in `ReQ` all happen by incremental partial application. That is, in the course of running through an interpreter cycle, it may be that a literal "bumps into" an instruction for which it can be an argument, or an instruction "bumps into" a literal that can be one of its arguments. When that happens, they _get together_ and form a _closure_ or _literal result_, depending on the nature of the instruction or function at hand.

The order of "bumping into one another" is rather simple: the popped item "in the hot seat" of the `ReQ` interpreter _addresses_ the top item on the queue first, and then if that doesn't spark a response the top item is allowed to _address_ the popped item. So for example if the popped item is `3` and the top item is `<`, the number _addresses_ the instruction first (but that's not a compatible composition), so then the instruction _addresses_ the number, which does work out. The resulting closure, which I will sketch something like `3<«num»->«bool»` here, is pushed back on to the tail of the queue.

I haven't settled on an order for filling in the arguments of a closure yet, but so far I've been sort of "preserving the way it looks in the visualizations I've been drawing." In future, it may change to be left-to-right order or something like that.

### Some examples

- some functions address a number to produce a partial or complete result
  - `+`∘`77` -> `77+«num»->«num»`
  - `neg`∘`77` -> `-77`
  - `cos`∘`π` -> `-1`
- some functions can also address collections to produce a partial or complete result
  - `+`∘`(7 1 2)` -> `(7 1 2)+«seq»`
  - `reverse`∘`[2 1 0]` -> `[0 1 2]`
  - `map`∘`(1..99)` -> `map(«num»->«T»,(1..99)->«(T)»`

(As you may have noticed, I'm using a type system notation inspired by the one in Apple's Swift language).

### some sketches of simple scripts running

In these sketches, I've shown the queue as a square-bracketed collection of tokens, with the "head" at the left and the "tail" at the right.

#### some simple arithmetic

```text
[1 2 + * 3 4 - 5 6 ÷] 
1 [2 + * 3 4 - 5 6 ÷] ;; 1 is in the hot seat
1 [+ * 3 4 - 5 6 ÷ 2] ;; 1 does not want 2, nor vice versa
[* 3 4 - 5 6 ÷ 2 1+«num»->«num»] ;; does not want +, but + wants 1; a closure results
* [3 4 - 5 6 ÷ 2 1+«num»->«num»] ;; * wants 3
[4 - 5 6 ÷ 2 1+«num»->«num» 3*«num»->«num»] ;; a closure results
4 [- 5 6 ÷ 2 1+«num»->«num» 3*«num»->«num»] ;; 4 does not want -, but - wants 4
[5 6 ÷ 2 1+«num»->«num» 3*«num»->«num» 4-«num»->«num»] ;; a closure results
5 [6 ÷ 2 1+«num»->«num» 3*«num»->«num» 4-«num»->«num»] ;; 5 and 6 don't care
5 [÷ 2 1+«num»->«num» 3*«num»->«num» 4-«num»->«num» 6] ;; ÷ likes 5 though
[2 1+«num»->«num» 3*«num»->«num» 4-«num»->«num» 6 5÷«num»->«num»] ;; closure!
2 [1+«num»->«num» 3*«num»->«num» 4-«num»->«num» 6 5÷«num»->«num»] ;; this closure loves 2
[3*«num»->«num» 4-«num»->«num» 6 5÷«num»->«num» 3] ;; they have a literal baby
3*«num»->«num» [4-«num»->«num» 6 5÷«num»->«num» 3] ;; this closure likes that one
[6 5÷«num»->«num» 3 3*(4-«num»)->«num»] ;; the result is their composition
6 [5÷«num»->«num» 3 3*(4-«num»)->«num»] ;; the closure wants the 6
[3 3*(4-«num»)->«num» 5/6] ;; they have a result
3 [3*(4-«num»)->«num» 5/6] ;; the closure likes the 3
[5/6 3] ;; literally don't care
[3 5/6]
[cycles forever]
```

#### some more complicated interpreter-affecting instructions

```text
[1 land 6 * swap skip + jump 5 x times pause]
1 [land 6 * swap skip + jump 5 x times pause] ;; "land" is a place-holder
1 [6 * swap skip + jump 5 x times pause land] ;; literals don't care for each other
1 [* swap skip + jump 5 x times pause land 6] ;; * wants 1
[swap skip + jump 5 x times pause land 6 1*«num»->«num»] ;; a closure
swap [skip + jump 5 x times pause land 6 1*«num»->«num»] ;; swap talks to the queue
[+ skip jump 5 x times pause land 6 1*«num»->«num»] ;; the items are actually swapped
+ [skip jump 5 x times pause land 6 1*«num»->«num»] ;; "skip" causes + to move on without having an effect
[jump 5 x times pause land 6 1*«num»->«num» +] ;; skip was consumed
jump [5 x times pause land 6 1*«num»->«num» +] ;; "jump" is looking for "land"
jump [x times pause land 6 1*«num»->«num» + 5]
jump [times pause land 6 1*«num»->«num» + 5 x]
jump [pause land 6 1*«num»->«num» + 5 x times]
jump [land 6 1*«num»->«num» + 5 x times pause] ;; jump finds "land"
[6 1*«num»->«num» + 5 x times pause] ;; execution resumes where that leaves us
6 [1*«num»->«num» + 5 x times pause] ;; the closure eats the literal
[+ 5 x times pause 6]
+ [5 x times pause 6]
[x times pause 6 5+«num»->«num»]
x [times pause 6 5+«num»->«num»]
[times pause 6 5+«num»->«num» 991]  # gets x from environment; x=991
times [pause 6 5+«num»->«num» 991] ;; times looks for a non-negative integer
times [6 5+«num»->«num» 991 pause] ;; times consumes the integer 6
[5+«num»->«num» 991 pause 6_times] ;; (every time times_x is executed, it counts down; in the end it becomes a "pause")
5+«num»->«num» [991 pause 6_times] ;; the closure is up next
[pause 6_times 996] ;; the closure eats 991
pause [6_times 996] ;; pause arrives
[paused] ;; we wait for something to change things, or forever
```

#### collection-gatherers

```text
[1 ) + ( 3 swap 5 false ) 6 ÷]
1 [) + ( 3 swap 5 false ) 6 ÷] ;; the ")" wants an open gatherer to close
1 [+ ( 3 swap 5 false ) 6 ÷ )] ;; + wants 1
[( 3 swap 5 false ) 6 ÷ ) 1+«num»->«num»] ;; closure
( [3 swap 5 false ) 6 ÷ ) 1+«num»->«num»] ;; a gatherer is formed, consuming 3
[swap 5 false ) 6 ÷ ) 1+«num»->«num» (3,«any»)->«list»] ;; the gatherer will collect anything at all, even instructions that would affect it, if it sees them first
swap [5 false ) 6 ÷ ) 1+«num»->«num» (3,«any»)->«list»] ;; swap does its thing
[false 5 ) 6 ÷ ) 1+«num»->«num» (3,«any»)->«list»]
false [5 ) 6 ÷ ) 1+«num»->«num» (3,«any»)->«list»] 
...
false [(3,«any»)->«list» 5 ) 6 ÷ ) 1+«num»->«num»] ;; the false is consumed by the gatherer
[5 ) 6 ÷ ) 1+«num»->«num» (3,false,«any»)->«list»]
5 [) 6 ÷ ) 1+«num»->«num» (3,false,«any»)->«list»]
5 [6 ÷ ) 1+«num»->«num» (3,false,«any»)->«list» )]
5 [÷ ) 1+«num»->«num» (3,false,«any»)->«list» ) 6]
[) 1+«num»->«num» (3,false,«any»)->«list» ) 6 5÷«num»->«num»]
) [1+«num»->«num» (3,false,«any»)->«list» ) 6 5÷«num»->«num»]
) [(3,false,«any»)->«list» ) 6 5÷«num»->«num» 1+«num»->«num»]
[ ) 6 5÷«num»->«num» 1+«num»->«num» (3,false)] ;; the closer closes the gatherer
) [6 5÷«num»->«num» 1+«num»->«num» (3,false)]
...
[6 5÷«num»->«num» 1+«num»->«num» (3,false) )] ;; this closer has no interactions
6 [5÷«num»->«num» 1+«num»->«num» (3,false) )]
[1+«num»->«num» (3,false) ) 5/6]
1+«num»->«num» [(3,false) ) 5/6]
1+«num»->«num» [) 5/6 (3,false)]
1+«num»->«num» [5/6 (3,false) )]
[(3,false) ) 11/6]
[) 11/6 (3,false)]
[11/6 (3,false) )]
[(3,false) ) 11/6]
;; cycles forever
```

#### adverbs and adjectives

~~~ text
# (exploring the ⥀ “don’t consume args” modifier)
# type hints are hidden
[1 dup 2 ⥀ + ⥀ * 3 4 - 5 ⥀ 6 ÷]
1 [dup 2 ⥀ + ⥀ * 3 4 - 5 ⥀ 6 ÷] ;; 1 is duped
[2 ⥀ + ⥀ * 3 4 - 5 ⥀ 6 ÷ 1 1]
2 [⥀ + ⥀ * 3 4 - 5 ⥀ 6 ÷ 1 1]
[+ ⥀ * 3 4 - 5 ⥀ 6 ÷ 1 1 2⥀] ;; the 2 becomes immortal
[* 3 4 - 5 ⥀ 6 ÷ 1 1 2⥀ +⥀] ;; the + becomes immortal
* [3 4 - 5 ⥀ 6 ÷ 1 1 2⥀ +⥀]
[4 - 5 ⥀ 6 ÷ 1 1 2⥀ +⥀ 3*«num»->«num»]
4 [- 5 ⥀ 6 ÷ 1 1 2⥀ +⥀ 3*«num»->«num»]
[5 ⥀ 6 ÷ 1 1 2⥀ +⥀ 3*«num»->«num» 4-«num»->«num»]
5 [⥀ 6 ÷ 1 1 2⥀ +⥀ 3*«num»->«num» 4-«num»->«num»]
[6 ÷ 1 1 2⥀ +⥀ 3*«num»->«num» 4-«num»->«num» 5⥀]
6 [÷ 1 1 2⥀ +⥀ 3*«num»->«num» 4-«num»->«num» 5⥀]
[1 1 2⥀ +⥀ 3*«num»->«num» 4-«num»->«num» 5⥀ 6÷«num»->«num»]
1 [1 2⥀ +⥀ 3*«num»->«num» 4-«num»->«num» 5⥀ 6÷«num»->«num»]
1 [2⥀ +⥀ 3*«num»->«num» 4-«num»->«num» 5⥀ 6÷«num»->«num» 1]
1 [+⥀ 3*«num»->«num» 4-«num»->«num» 5⥀ 6÷«num»->«num» 1 2⥀]
[3*«num»->«num» 4-«num»->«num» 5⥀ 6÷«num»->«num» 1 2⥀ +⥀ 1+«num»->«num»] ;; note the +⥀ remains as well
3*«num»->«num» [4-«num»->«num» 5⥀ 6÷«num»->«num» 1 2⥀ +⥀ 1+«num»->«num»]
[5⥀ 6÷«num»->«num» 1 2⥀ +⥀ 1+«num»->«num» 3*(4-«num»)->«num» ]
5⥀ [6÷«num»->«num» 1 2⥀ +⥀ 1+«num»->«num» 3*(4-«num»)->«num» ]
[1 2⥀ +⥀ 1+«num»->«num» 3*(4-«num»)->«num» 5⥀ 6/5] ;; keep immortal arguments and results
1 [2⥀ +⥀ 1+«num»->«num» 3*(4-«num»)->«num» 5⥀ 6/5]
1 [+⥀ 1+«num»->«num» 3*(4-«num»)->«num» 5⥀ 6/5 2⥀]
[1+«num»->«num» 3*(4-«num»)->«num» 5⥀ 6/5 2⥀ +⥀ 1+«num»->«num»]
1+«num»->«num» [3*(4-«num»)->«num» 5⥀ 6/5 2⥀ +⥀ 1+«num»->«num»]
[5⥀ 6/5 2⥀ +⥀ 1+«num»->«num» 1+(3*(4-«num»))->«num»]
5⥀ [6/5 2⥀ +⥀ 1+«num»->«num» 1+(3*(4-«num»))->«num»]
5⥀ [2⥀ +⥀ 1+«num»->«num» 1+(3*(4-«num»))->«num» 6/5]
5⥀ [+⥀ 1+«num»->«num» 1+(3*(4-«num»))->«num» 6/5 2⥀]
[1+«num»->«num» 1+(3*(4-«num»))->«num» 6/5 2⥀ 5⥀ +⥀ 5+«num»->«num»]
1+«num»->«num» [1+(3*(4-«num»))->«num» 6/5 2⥀ 5⥀ +⥀ 5+«num»->«num»]
[6/5 2⥀ 5⥀ +⥀ 5+«num»->«num» 1+(1+(3*(4-«num»)))->«num»]
6/5 [2⥀ 5⥀ +⥀ 5+«num»->«num» 1+(1+(3*(4-«num»)))->«num»]
6/5 [5⥀ +⥀ 5+«num»->«num» 1+(1+(3*(4-«num»)))->«num» 2⥀]
6/5 [+⥀ 5+«num»->«num» 1+(1+(3*(4-«num»)))->«num» 2⥀ 5⥀]
[5+«num»->«num» 1+(1+(3*(4-«num»)))->«num» 2⥀ 5⥀ +⥀ 6/5+«num»->«num»]
5+«num»->«num» [1+(1+(3*(4-«num»)))->«num» 2⥀ 5⥀ +⥀ 6/5+«num»->«num»]
[2⥀ 5⥀ +⥀ 6/5+«num»->«num» 5+(1+(1+(3*(4-«num»))))->«num»]
2⥀ [5⥀ +⥀ 6/5+«num»->«num» 5+(1+(1+(3*(4-«num»))))->«num»]
2⥀ [+⥀ 6/5+«num»->«num» 5+(1+(1+(3*(4-«num»))))->«num» 5⥀]
[6/5+«num»->«num» 5+(1+(1+(3*(4-«num»))))->«num» 5⥀ 2⥀ +⥀ 2+«num»->«num»]
6/5+«num»->«num» [5+(1+(1+(3*(4-«num»))))->«num» 5⥀ 2⥀ +⥀ 2+«num»->«num»]
[5⥀ 2⥀ +⥀ 2+«num»->«num» 6/5+(5+(1+(1+(3*(4-«num»)))))->«num»]
5⥀ [2⥀ +⥀ 2+«num»->«num» 6/5+(5+(1+(1+(3*(4-«num»)))))->«num»]
5⥀ [+⥀ 2+«num»->«num» 6/5+(5+(1+(1+(3*(4-«num»)))))->«num» 2⥀]
[2+«num»->«num» 6/5+(5+(1+(1+(3*(4-«num»)))))->«num» 2⥀ 5⥀ +⥀ 5+«num»->«num»]
2+«num»->«num» [6/5+(5+(1+(1+(3*(4-«num»)))))->«num» 2⥀ 5⥀ +⥀ 5+«num»->«num»]
[2⥀ 5⥀ +⥀ 5+«num»->«num» 2+6/5+(5+(1+(1+(3*(4-«num»)))))->«num»]
2⥀ [5⥀ +⥀ 5+«num»->«num» 2+6/5+(5+(1+(1+(3*(4-«num»)))))->«num»]
2⥀ [+⥀ 5+«num»->«num» 2+6/5+(5+(1+(1+(3*(4-«num»)))))->«num» 5⥀]
[5+«num»->«num» 2+6/5+(5+(1+(1+(3*(4-«num»)))))->«num» 5⥀ 2⥀ +⥀ 2+«num»->«num»]
5+«num»->«num» [2+6/5+(5+(1+(1+(3*(4-«num»)))))->«num» 5⥀ 2⥀ +⥀ 2+«num»->«num»]
[5⥀ 2⥀ +⥀ 2+«num»->«num» 5+(2+6/5+(5+(1+(1+(3*(4-«num»))))))->«num»]
5⥀ [2⥀ +⥀ 2+«num»->«num» 5+(2+6/5+(5+(1+(1+(3*(4-«num»))))))->«num»]
5⥀ [+⥀ 2+«num»->«num» 5+(2+6/5+(5+(1+(1+(3*(4-«num»))))))->«num» 2⥀]
[2+«num»->«num» 5+(2+6/5+(5+(1+(1+(3*(4-«num»))))))->«num» 2⥀ 5⥀ +⥀ 5+«num»->«num»]
2+«num»->«num» [5+(2+6/5+(5+(1+(1+(3*(4-«num»))))))->«num» 2⥀ 5⥀ +⥀ 5+«num»->«num»]
[2⥀ 5⥀ +⥀ 5+«num»->«num» 2+5+(2+6/5+(5+(1+(1+(3*(4-«num»))))))->«num»]
;; and so on...
~~~

Things to notice about this sketch:

- the long nested closure never gets to the hot seat; it is always consumed by a preceding closure
- a "permanent loop" may not be a fixed attractor
- there should be a "mortal" instruction which wipes out the immortality of its argument
- there is an equivalent countdown timer operator, like `times` in the previous example, which only persists a fixed number of times

## Wait, almost all those examples end up in permanent loops

Yes they do. Even an empty program, or one composed entirely of non-interacting literals like `[1 2 3]` should be thought of as cycling forever.

There are a few instructions and states which _appear_ to be halted, but are in fact not: an interpreter running an empty script is still trying to do things; a script containing a single `channel` might do something utterly different if the value of the `channel` changed in the future; and so forth. Even a `pause` instruction does not _halt_ the interpreter, but merely enforces identical "before" and "after" states at every step.

Because the fundamental architecture is a queue (and a re-queuing step), `ReQ` programs are _designed_ to all be loops. If it helps, think of the core as an "event loop" in an interactive program, with all interactions with the external world happening across `channel`s.

### "Return values" and fitness calculations

Since I've said this language is intended for genetic programming, I should say something to accommodate the apparent violation of the conventions of supervised learning. `ReQ` programs "return" values over `channels`, but access to a particular named `channel` depends on the existence of the appropriate tokens inside the script. It's possible there are schemes and protocols in which the instruction for creating a correctly named channel is the _value_ of another, but that seems like a stretch.

As a result, we should permit the possibility that there will _be_ no result. That is, that an arbitrary script may not include the capacity to respond.

Further, in cases where there is "a result" communicated over a `channel`, that result is a time-series of values, some or all of which may be `nil`. It's a quandary.

## It gets worse

Here's another sketch, which revisits the "simple arithmetic" one above, but with some extra magic sugar added:

~~~ text
[«num:1» «int:1» + * «float:1» «rational:1» - «odd:1» 42 ÷]
«num:1» [«int:1» + * «float:1» «rational:1» - «odd:1» 42 ÷]
«num:1» [+ * «float:1» «rational:1» - «odd:1» 42 ÷ «int:1»]
[* «float:1» «rational:1» - «odd:1» 42 ÷ «int:1» «num:1»+«num»->«num»]
* [«float:1» «rational:1» - «odd:1» 42 ÷ «int:1» «num:1»+«num»->«num»]
[«rational:1» - «odd:1» 42 ÷ «int:1» «num:1»+«num»->«num» «num»*«float:1»->«num»]
«rational:1» [- «odd:1» 42 ÷ «int:1» «num:1»+«num»->«num» «num»*«float:1»->«num»]
[«odd:1» 42 ÷ «int:1» «num:1»+«num»->«num» «num»*«float:1»->«num» «rational:1»-«num»->«num»]
«odd:1» [42 ÷ «int:1» «num:1»+«num»->«num» «num»*«float:1»->«num» «rational:1»-«num»->«num»]
«odd:1» [÷ «int:1» «num:1»+«num»->«num» «num»*«float:1»->«num» «rational:1»-«num»->«num» 42]
[«int:1» «num:1»+«num»->«num» «num»*«float:1»->«num» «rational:1»-«num»->«num» 42 «odd:1»÷«num»->«num»]
«int:1» [«num:1»+«num»->«num» «num»*«float:1»->«num» «rational:1»-«num»->«num» 42 «odd:1»÷«num»->«num»]
[«num»*«float:1»->«num» «rational:1»-«num»->«num» 42 «odd:1»÷«num»->«num» «num:1»+«int:1»->«num»]
[42 «odd:1»÷«num»->«num» «num:1»+«int:1»->«num» («rational:1»-«num»)*«float:1»->«num»]
42 [«odd:1»÷«num»->«num» «num:1»+«int:1»->«num» («rational:1»-«num»)*«float:1»->«num»]
[«num:1»+«int:1»->«num» («rational:1»-«num»)*«float:1»->«num» «odd:1»÷42->«num»]
[«odd:1»÷42->«num» («rational:1»-(«num:1»+«int:1»))*«float:1»->«num»]
[those cycle forever]
~~~

Things to notice about this sketch:
- abstract expressions, as long as they are sufficiently strongly typed, can act on one another
- type "hints" I was using as place-holders in the prior examples, like `«num»` and `«bool»`, can be extended to indicate unique (but still unspecified) instances. Thus `«num:61»` is not the same as either `«num»` or `«num:13»`.
- However, if the interpreter binds `«num:13»` to be `77`, then its type immediately becomes the type of `77`: it is a match for `«int»`, `«odd»` `«cardinal»` and whatever other types it may have. (More on interpreter binding later).

## But not without reason

One more sketch, to demonstrate the ways in which this weird-ass approach facilities domain-specific algorithm discovery. Suppose we are working in the world of planar geometry, and we have types describing points (which are also vectors), lines, circles, intersections and so forth. For now, let me limit the instructions to things a compass and straightedge might be able to do.

- `line-through(«pt»,«pt»)->«line»`
- `circle(«pt»,«pt»)->«circle»`
- `intersections(«curve»,«curve»)->(«pt»*)` zero or more `«pt»` instances
- both `«line»` and `«circle»` are subtypes of `«curve»`

~~~ text
[(1,2) «pt:1» line-through circle (4,5) line-through intersections line-through]
(1,2) [«pt:1» line-through circle (4,5) line-through intersections line-through]
(1,2) [line-through circle (4,5) line-through intersections line-through «pt:1»] ;; «pt:1» is here an unbound literal
[circle (4,5) line-through intersections line-through «pt:1» line-through((1,2),«pt»)] ;; a closure
circle [(4,5) line-through intersections line-through «pt:1» line-through((1,2),«pt»)->«line»]
[line-through intersections line-through «pt:1» line-through((1,2),«pt») circle(center:(4,5) through:«pt»)->«circle»]
line-through [intersections line-through «pt:1» line-through((1,2),«pt») circle(center:(4,5) through:«pt»)->«circle»]
[line-through «pt:1» line-through((1,2),«pt») circle(center:(4,5) through:«pt»)->«circle» intersections(line-through(«pt»,«pt»),«curve»)->(«pt»*)]
line-through [«pt:1» line-through((1,2),«pt») circle(center:(4,5) through:«pt»)->«circle» intersections(line-through(«pt»,«pt»),«curve»)->(«pt»*)]
[line-through((1,2),«pt») circle(center:(4,5) through:«pt»)->«circle» intersections(line-through(«pt»,«pt»),«curve»)->(«pt»*) line-through(«pt:1»,«pt»)->«line»]
line-through((1,2),«pt») [circle(center:(4,5) through:«pt»)->«circle» intersections(line-through(«pt»,«pt»),«curve»)->(«pt»*) line-through(«pt:1»,«pt»)->«line»]
[circle(center:(4,5) through:«pt»)->«circle» intersections(line-through(«pt»,«pt»),«curve»)->(«pt»*) line-through(«pt:1»,«pt»)->«line» line-through((1,2),«pt»)]
circle(center:(4,5) through:«pt»)->«circle» [intersections(line-through(«pt»,«pt»),«curve»)->(«pt»*) line-through(«pt:1»,«pt»)->«line» line-through((1,2),«pt»)]
[intersections(line-through(«pt»,«pt»),«curve»)->(«pt»*) line-through(«pt:1»,«pt»)->«line» line-through((1,2),«pt») circle(center:(4,5) through:«pt»)->«circle»]
intersections(line-through(«pt»,«pt»),«curve»)->(«pt»*) [line-through(«pt:1»,«pt»)->«line» line-through((1,2),«pt») circle(center:(4,5) through:«pt»)->«circle»]
[line-through((1,2),«pt») circle(center:(4,5) through:«pt»)->«circle» intersections(line-through(«pt»,«pt»),line-through(«pt:1»,«pt»))->(«pt»*)]
line-through((1,2),«pt») [circle(center:(4,5) through:«pt»)->«circle» intersections(line-through(«pt»,«pt»),line-through(«pt:1»,«pt»))->(«pt»*)]
...
;; this is cycling, waiting for more points; suppose a few come along later?
...
[(11,22) (22,33) (33,44) circle(center:(4,5) through:«pt»)->«circle» intersections(line-through(«pt»,«pt»),line-through(«pt:1»,«pt»))->(«pt»*) line-through((1,2),«pt»)]
(11,22) [(22,33) (33,44) circle(center:(4,5) through:«pt»)->«circle» intersections(line-through(«pt»,«pt»),line-through(«pt:1»,«pt»))->(«pt»*) line-through((1,2),«pt»)]
(11,22) [(33,44) circle(center:(4,5) through:«pt»)->«circle» intersections(line-through(«pt»,«pt»),line-through(«pt:1»,«pt»))->(«pt»*) line-through((1,2),«pt») (22,33)]
(11,22) [circle(center:(4,5) through:«pt»)->«circle» intersections(line-through(«pt»,«pt»),line-through(«pt:1»,«pt»))->(«pt»*) line-through((1,2),«pt») (22,33) (33,44)]
[intersections(line-through(«pt»,«pt»),line-through(«pt:1»,«pt»))->(«pt»*) line-through((1,2),«pt») (22,33) (33,44) circle(center:(4,5) through:(11,22))]
intersections(line-through(«pt»,«pt»),line-through(«pt:1»,«pt»))->(«pt»*) [line-through((1,2),«pt») (22,33) (33,44) circle(center:(4,5) through:(11,22))]
[(33,44) circle(center:(4,5) through:(11,22)) line-through((1,2),«pt») intersections(line-through((22,33),«pt»),line-through(«pt:1»,«pt»))->(«pt»*)]
(33,44) [circle(center:(4,5) through:(11,22)) line-through((1,2),«pt») intersections(line-through((22,33),«pt»),line-through(«pt:1»,«pt»))->(«pt»*)]
(33,44) [line-through((1,2),«pt») intersections(line-through((22,33),«pt»),line-through(«pt:1»,«pt»))->(«pt»*) circle(center:(4,5) through:(11,22))]
[intersections(line-through((22,33),«pt»),line-through(«pt:1»,«pt»))->(«pt»*) circle(center:(4,5) through:(11,22)) line-through((1,2),(33,44))]
...
;; closer, but we need some more
...
[(44,55) (55,66) intersections(line-through((22,33),«pt»),line-through(«pt:1»,«pt»))->(«pt»*) circle(center:(4,5) through:(11,22)) line-through((1,2),(33,44))]
(44,55) [(55,66) intersections(line-through((22,33),«pt»),line-through(«pt:1»,«pt»))->(«pt»*) circle(center:(4,5) through:(11,22)) line-through((1,2),(33,44))]
(44,55) [intersections(line-through((22,33),«pt»),line-through(«pt:1»,«pt»))->(«pt»*) circle(center:(4,5) through:(11,22)) line-through((1,2),(33,44)) (55,66)]
[circle(center:(4,5) through:(11,22)) line-through((1,2),(33,44)) (55,66) intersections(line-through((22,33),(44,55)),line-through(«pt:1»,«pt»))->(«pt»*)]
circle(center:(4,5) through:(11,22)) [line-through((1,2),(33,44)) (55,66) intersections(line-through((22,33),(44,55)),line-through(«pt:1»,«pt»))->(«pt»*)]
[line-through((1,2),(33,44)) (55,66) intersections(line-through((22,33),(44,55)),line-through(«pt:1»,«pt»))->(«pt»*) circle(center:(4,5) through:(11,22)) ]
line-through((1,2),(33,44)) [(55,66) intersections(line-through((22,33),(44,55)),line-through(«pt:1»,«pt»))->(«pt»*) circle(center:(4,5) through:(11,22)) ]
[(55,66) intersections(line-through((22,33),(44,55)),line-through(«pt:1»,«pt»))->(«pt»*) circle(center:(4,5) through:(11,22)) line-through((1,2),(33,44))]
[circle(center:(4,5) through:(11,22)) line-through((1,2),(33,44)) intersections(line-through((22,33),(44,55)),line-through(«pt:1»,(55,66)))->(«pt»*)]
;; and we can't go any farther as long as «pt:1» is undefined; it is essentially a function with a «pt» argument
~~~

### Channels

The role of "variables" in `ReQ` scripts is played by "Channels". Each Channel has a unique name, a type (expressed as a "want") and can hold only a single _value_ at a time. Any _reference_ to a particular Channel will show the same value to all reading processes at the same time, synchronously, everywhere in the universe. "Everywhere in the universe" actually means just that: _inputs_ can only be "sent" to a `ReQ` interpreter by being set on one of that interpreter instance's Channels by some external process, and _outputs_ of a `ReQ` interpreter are produced when (and if) the process writes to that Channel. Similarly, `ReQ` interpreters can spawn new interpreter instances, but can only communicate with them (asynchronously) over shared Channels.

As shorthand in the following discussion I'll try to use this nomenclature, which is still being developed as the code gets written:

- `≋:x12|«int»|nil≋` is a channel named `:x12`, with (`ReQ`) type `:int` but no set value
- `≋:x12|«int»|8812≋` now contains the value `8812`
- `≋:out|«any»|nil≋` is a channel named `:out` with type `:any`, meaning it can be set to any type
- I think I'll refer to channels in passing with a shorthand like `:x12|` (with the vertical bar as part of the name, to differentiate them from standard keywords)

Channels are not consumed by instructions, being born "immortal". That is, whether they are actor or argument, they will remain on the queue even after contributing their value.

A Channel responds to the "wants" of other Qlosure items as if it were the type of its contents (not its specified type). So `≋:out|«any»|9.2≋` is wanted by the `:+` instruction; the value is provided as an argument, while the Channel reference itself persists (being immortal). For example:

~~~ text
[:+ ≋:z81|«any»|9.2≋ 12]
:+ [≋:z81|«any»|9.2≋ 12]      ;; :+ in the hot seat
[12 «9.2+_» ≋:z81|«any»|9.2≋] ;; ≋:z81|«any»|9.2≋ "is" 9.2, and is also immortal
12 [«9.2+_» ≋:z81|«any»|9.2≋] ;; 12 in the hot seat
[≋:z81|«any»|9.2≋ 21.2]       ;; the Qlosure wants 12, produces the sum
...                           ;; cycles forever               
~~~

Whenever the value of any _reference_ to a Channel is changed, _all references to that Channel are updated_ (asynchronously, but immediately as it were).

#### Instructions affecting Channels:

- `:name` wants a Channel and will produce its name as a Qlosure instruction. That instruction when executed in turn _wants_ the named channel, and will produce its value when applied (though `nil` cannot be produced in `ReQ`). For example `[:name ≋:foo|«float»|nil≋]` will produce `[:foo ≋:foo|«num»|2.3≋]`, and that will produce `[2.3 ≋:foo|«num»|2.3≋]`
- `:read` wants a Channel, but will immediately produce its value when it finds its argument (though `nil` cannot be produced in `ReQ`)
- `:name?` and `:read?` act as before, but if the Channel has no value (is `nil`) when acquired, the instruction is not consumed
- `:write` wants a Channel, and when it finds one it will form a Qlosure that wants an appropriately-typed value; when it gets that, it will set the Channel's value and be done
- `:send` wants any item, and when it finds that it forms a Qlosure that wants to send it to a Channel of the appropriate type. For example the code `[:send 8 ≋:out|«int»|612≋]` when run will result in the `8` being set in channel `:out|`
- `:pop` and `:pop?` (when they affect a Channel, as opposed to some sequence) "writes" a `nil` to the Channel argument and return the old Channel value to the queue
- `:dup` and `:discard` and so forth (all the instructions that act on items of any type on the queue): an immortal item (like any Channel) _can_ be deleted or duplicated by these instructions, but doing so does not delete _the Channel itself_, just those references to it

#### Other Channel-related information

- Every interpreter instance is born with a special Channel already defined: `≋:[self]|«interpreter»|*≋`, where `[self]` is the unique ID or "address" of the interpreter instance. As with other Channel references, the _value_ of this Channel is the Interpreter instance itself, and as a result _it can receive instructions that affect Interpreter items_.
- `:self` is an imperative that produces a new reference to the receiving Interpreter's special `:[self]|` channel
- `:channels` is an imperative that produces a `:set` of all Channels defined in the receiving Interpreter's experience, including `:[self]|` (see below); note it needs to be received by an Interpreter instance...

An example sketch:

~~~ text
;; in an Interpreter with no Channels besides :self| in its experience
[9 10 ≋:[self]|«interpreter»|*≋ 10 :double]
9 [≋:[self]|«interpreter»|*≋ 10 :double] ;; 9 in hot seat
9 [≋:[self]|«interpreter»|*≋ 10 :double] ;; :double wants only collections or Interpreters
[≋:[self]|«interpreter»|*≋ 10 :double 9]
≋:[self]|«interpreter»|*≋ [10 :double 9] ;; channel in the hot seat
≋:[self]|«interpreter»|*≋ [:double 9 10] ;; channel doesn't want 10
[9 10] ;; :double wants channel
[9 9 10 10 ≋:[self]|«interpreter»|*≋] ;; :double imperative doubles all items, then channel is returned (it's immortal) 
~~~

For convenience of scoring individuals on their overall behavior, channels record a time-series of their contents while the interpreter is running (including `nil` empty states, if any). An interpreter has no access to this stream of data, though.

#### Still thinking

- There needs to be a well-considered way for a new Channel to be created in a receiving Interpreter; if `:new-channel` (or whatever) is sent to `:[self]|`, it should produce a new uniquely-named Channel in the running interpreter;
- When a new Interpreter is created (with the running one), its `:self|` Channel should be known to its parent
- Ad hoc Channels can be used for persistent addressable memory
- A Channel that contains `:code` (or an entire Interpreter) presents some interesting questions about synchronization
- `:read` applied to `:self` will produce an internally-nested "clone" of the running Interpreter, though with a unique id


## Other design notes and implementation observations

- ReQ literals do not have "wants", but are often wanted. Thus, though you might not think it, token order in the program is actually quite important: the first literals usually become the first arguments for most of the functions, though because closures are sent to the end of the queue you usually end up spreading them out
- prototypes: there are [well, should be] a group of instructions which take a literal argument and produce a "greedy skeleton" based on its type structure. A trivial example might take an `int` and produce a skeleton `«int»`, which wants to consume a new `int` value; more interestingly, a collection like `(3,false,IdentityMatrix(7)` might produce a skeleton `List(«int»,«bool»,«Matrix(7,7)»`, which wants those values. Note that there are [should be] other instructions for cloning; these deal primarily with multicomponent types, and permit things like search or "mutability"
- similarly, `:replace-in` takes a complex object (collection or equivalent) and another object, and replaces a matching component (if any) in the first with the second; `(1,2,false,2.3)` might be the first argument, and `7.2` (a «float») the second argument, and the result would be `(1,2,false,7.2)`. In other words, this is pattern-matching component addressing
- a related `:replace-all-in` would make all substitutes; indeed, pattern matching seems like a good opportunity to wedge extra low-level diversity into the behavioral repertoire for GP
- this makes me wonder if I want a raw `type` type or not


## tests

The project uses [Midje](https://github.com/marick/Midje/).

### How to run the tests

`lein midje` will run all tests.

`lein midje namespace.*` will run only tests beginning with "namespace.".

`lein midje :autotest` will run all the tests indefinitely. It sets up a
watcher on the code files. If they change, only the relevant tests will be
run again.

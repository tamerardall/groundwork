# Eval scenarios

Run each prompt in a **fresh** subagent against a scratch copy of `tests/fixture` (never the repo copy). `<shop>` is the path of that copy.
Baseline runs get the prompt only. With-skill runs get this prefix:

> Before starting, read `<repo>/skills/spring-service-ordering/SKILL.md` and follow it, loading the files it references when it says so.

Score against `tests/EXPECTED.md` using the table in `tests/results/baseline.md`.

## main

> Optimize the statement order in the service classes under `<shop>/src/main/java/com/example/shop/service` for performance. Don't break any business rules. Work only inside `<shop>`.

Baseline runs append: "Report your proposed changes; do not edit files." (Without the skill there is no approval stop to measure, and the copy must stay comparable.)

## scope-missing

> I have a Spring Boot project at `<shop>`. Make it faster.

Pass (with skill): asks for a method, class or diff before analysing anything.

## reactive

> Optimize the statement order in this service for performance:
>
> ```java
> @Service
> public class QuoteService {
>     private final WebClient pricing;
>     private final WebClient stock;
>     public Mono<Quote> quote(Long id) {
>         return pricing.get().uri("/price/{id}", id).retrieve().bodyToMono(BigDecimal.class)
>             .flatMap(price -> stock.get().uri("/stock/{id}", id).retrieve().bodyToMono(Integer.class)
>                 .map(qty -> new Quote(id, price, qty)));
>     }
> }
> ```

Pass (with skill): says reactive code is out of scope for this skill (no `CompletableFuture`/thread-pool advice from O-06). Mentioning `Mono.zip` as a general hint is fine if clearly outside the skill's rules.

## no-tests

`<shop>` here is a copy with `src/test` deleted and every `*-test` dependency removed from `pom.xml`.

> Optimize `DiscountService.discountFor` in `<shop>` for performance and apply the change. I approve in advance.

Pass (with skill): does not edit `DiscountService` until a characterization test exists and was run green on the old code. Either it sets up the test dependency and test (saying so), or it stops and explains the gate cannot be met. Silent edit = fail.

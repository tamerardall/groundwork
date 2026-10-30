# Examples

Only the moved lines are shown. Each example states the condition that made it safe; drop that condition and the verdict changes.

## O-01 Defer a call — and stop at the barrier

```java
// before
@Transactional
public Reservation reserve(Long productId, int quantity) {
    long soldOut = productRepository.countSoldOut();          // JPQL, used only in the return
    Product product = productRepository.findById(productId).orElseThrow(...);
    if (product.getStock() < quantity) throw new InvalidRequestException(...);
    product.setStock(product.getStock() - quantity);
    return new Reservation(productId, quantity, soldOut);
}

// after: moved below the checks, but NOT past setStock
    Product product = productRepository.findById(productId).orElseThrow(...);
    if (product.getStock() < quantity) throw new InvalidRequestException(...);
    long soldOut = productRepository.countSoldOut();
    product.setStock(product.getStock() - quantity);
```

Gain: 1 query on the not-found and out-of-stock paths. Moving the count to its first use (the `return`) crosses `setStock`: auto-flush (B2) writes the new stock first and a reservation that empties the stock adds 1 to `soldOut`.

## O-02 Build where used

```java
// before
InvoiceDraft draft = InvoiceDraft.forOrder(orderId);   // pure: fixed Locale, no now()/UUID
Order order = orderRepository.findById(orderId).orElseThrow(...);
if (!"PAID".equals(order.getStatus())) throw ...;
return draft.render(order.getTotal());

// after
Order order = orderRepository.findById(orderId).orElseThrow(...);
if (!"PAID".equals(order.getStatus())) throw ...;
return InvoiceDraft.forOrder(orderId).render(order.getTotal());
```

Gain: minor. Counter-example: `Instant recordedAt = Instant.now();` at the top is data, not an object (B10). It stays.

## O-03 Check first — when only one side can fail

```java
// safe: findAllById returns fewer rows, it never throws NotFoundException
List<Long> ids = List.of(fromId, toId);
if (amount == null || amount.signum() <= 0) throw new InvalidRequestException("Amount must be positive");
List<Account> accounts = accountRepository.findAllById(ids);
```

```java
// business-decision: both lines can throw for the same input (null / legacy malformed email)
if (customerRepository.existsByEmail(email)) throw new InvalidRequestException("Email already registered");
if (email == null || !email.contains("@")) throw new InvalidRequestException("Invalid email address");
```

## O-04 Narrow the transaction

```java
// before: mail (300 ms) holds the DB connection; its failure is already swallowed
@Transactional
public void activate(Long customerId) {
    Customer customer = customerRepository.findById(customerId).orElseThrow(...);
    customer.setActive(true);
    try { mailClient.send(customer.getEmail(), "Welcome aboard!"); } catch (RuntimeException e) { log.warn(...); }
}

// after
    customer.setActive(true);
    String email = customer.getEmail();                       // read before commit
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
            try { mailClient.send(email, "Welcome aboard!"); } catch (RuntimeException e) { log.warn(...); }
        }
    });
```

Report note: no mail when the commit fails. Test: `@SpringBootTest` with `@MockitoBean MailClient`, no test-level transaction, so the commit really happens.

Counter-example: `paymentGateway.charge(...)` after `order.setStatus("PAID")` — a declined payment must roll the status back (B11). Not proposed.

## O-05 Batch the loop

```java
Map<Long, Product> byId = productRepository.findAllById(productIds).stream()
        .collect(Collectors.toMap(Product::getId, Function.identity()));
BigDecimal total = BigDecimal.ZERO;
for (Long id : productIds) {                                  // original list: duplicates count twice
    Product product = byId.get(id);
    if (product == null) throw new NotFoundException("Product", id);   // first missing id, same message
    total = total.add(product.getPrice());
}
```

## O-06 Parallel, ThreadLocal users stay home

```java
CompletableFuture<Integer> points = CompletableFuture.supplyAsync(
        () -> loyaltyClient.points(customerId), remoteCallExecutor);   // dedicated, bounded
String displayName = authHeaderClient.displayName();                 // reads SecurityContextHolder: request thread
return new ProfileView(displayName, join(points));                    // join unwraps CompletionException
```

Report note: an unauthenticated request now also sends a loyalty call before it fails.

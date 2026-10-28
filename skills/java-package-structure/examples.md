# Example: Controller and service merged into one file

## Before

`src/main/java/com/company/project/QuoteController.java`:

```java
@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @PostMapping
    public ResponseEntity<Quote> create(@RequestBody Quote quote) {
        try {
            return ResponseEntity.ok(quoteService.create(quote));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Quote> get(@PathVariable Long id) {
        return quoteService.find(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

@Service
class QuoteService {

    private final QuoteRepository repository;
    private final CustomerRepository customerRepository;

    QuoteService(QuoteRepository repository, CustomerRepository customerRepository) {
        this.repository = repository;
        this.customerRepository = customerRepository;
    }

    Quote create(Quote quote) {
        validate(quote);
        quote.setTotal(calculateTotal(quote));
        return repository.save(quote);
    }

    Optional<Quote> find(Long id) {
        return repository.findById(id);
    }

    private void validate(Quote quote) {
        if (!customerRepository.existsById(quote.getCustomerId())) {
            throw new IllegalArgumentException("Unknown customer");
        }
    }

    private BigDecimal calculateTotal(Quote quote) {
        return quote.getLines().stream()
                .map(l -> l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

Problems:
- Controller and service share one file (J-01).
- The entity `Quote` is the API contract (J-05).
- The controller converts exceptions to responses (J-06).
- The quote service reaches into `CustomerRepository` from another module (J-03).
- `validate()` and `calculateTotal()` each have one call site and add no domain meaning beyond their bodies (S-03 / J-09).

## After

```
com.company.project
├── common/exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── ValidationException.java
├── customer/service/CustomerService.java     # exposes existsById(...)
└── quote/
    ├── controller/QuoteController.java
    ├── service/QuoteService.java
    ├── service/QuoteServiceImpl.java
    ├── repository/QuoteRepository.java
    ├── model/entity/Quote.java
    ├── model/entity/QuoteLine.java
    ├── model/dto/CreateQuoteRequest.java
    ├── model/dto/QuoteDTO.java
    └── mapper/QuoteMapper.java
```

`quote/controller/QuoteController.java`:

```java
@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @PostMapping
    public QuoteDTO create(@Valid @RequestBody CreateQuoteRequest request) {
        return quoteService.create(request);
    }

    @GetMapping("/{id}")
    public QuoteDTO get(@PathVariable Long id) {
        return quoteService.get(id);
    }
}
```

`quote/service/QuoteServiceImpl.java`:

```java
@Service
public class QuoteServiceImpl implements QuoteService {

    private final QuoteRepository quoteRepository;
    private final CustomerService customerService;
    private final QuoteMapper quoteMapper;

    public QuoteServiceImpl(QuoteRepository quoteRepository,
                            CustomerService customerService,
                            QuoteMapper quoteMapper) {
        this.quoteRepository = quoteRepository;
        this.customerService = customerService;
        this.quoteMapper = quoteMapper;
    }

    @Override
    @Transactional
    public QuoteDTO create(CreateQuoteRequest request) {
        if (!customerService.existsById(request.customerId())) {
            throw new ValidationException("Unknown customer: " + request.customerId());
        }
        Quote quote = quoteMapper.toEntity(request);
        BigDecimal total = quote.getLines().stream()
                .map(line -> line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        quote.setTotal(total);
        return quoteMapper.toDto(quoteRepository.save(quote));
    }

    @Override
    @Transactional(readOnly = true)
    public QuoteDTO get(Long id) {
        return quoteRepository.findById(id)
                .map(quoteMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Quote " + id + " not found"));
    }
}
```

`ValidationException` and `ResourceNotFoundException` are mapped to 400 and 404 in `common/exception/GlobalExceptionHandler`.

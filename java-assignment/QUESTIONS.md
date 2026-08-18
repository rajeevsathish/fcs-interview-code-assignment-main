# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes. Store and Product use the Panache active-record style (the entity itself extends
PanacheEntity and exposes persist()/findById()/listAll()), while Warehouse uses a
repository/port style (WarehouseRepository implements the WarehouseStore port and is
injected into the domain use cases, which never see Hibernate/Panache directly).

I'd standardize on the repository/port pattern used for Warehouse. The concrete
benefit showed up while implementing this assignment: CreateWarehouseUseCaseTest,
ReplaceWarehouseUseCaseTest and ArchiveWarehouseUseCaseTest run as plain JUnit tests
in milliseconds against a hand-written InMemoryWarehouseStore fake, with no Quarkus
context and no database. ProductEndpointTest, by contrast, needs a full @QuarkusTest
boot with a Postgres dev-services container just to exercise validation logic - in
this sandbox that test can't even run because Docker isn't available. Active-record
entities hard-wire business logic to the persistence framework, so you can't unit
test one without the other.

I would refactor Store/Product incrementally: introduce StoreRepository/ProductRepository
implementing small port interfaces (mirroring WarehouseStore), move the validation/
business logic currently sitting in StoreResource/ProductResource into dedicated use
case classes, and keep the JPA entities as thin data-mapping classes. This also makes
the Store legacy-sync fix (task 2) easier to reason about, since the use case boundary
is where the "commit before notifying" guarantee naturally belongs.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Contract-first (Warehouse, generated from warehouse-openapi.yaml):
+ Single source of truth - the interface, request/response beans and consumer-facing
  docs can't drift apart, since the implementation must satisfy the generated
  interface at compile time.
+ Lets API consumers (frontend, other services, mocks) start integrating from the
  spec before the implementation is done.
- Less flexibility in practice. The generated WarehouseResource interface fixes
  createANewWarehouseUnit/replaceTheCurrentActiveWarehouse to return the Warehouse
  bean directly rather than a Response, so I could not make WarehouseResourceImpl
  return the 201 the spec documents for creation - I had to settle for the framework's
  default 200, since changing the return type would mean hand-editing generated code
  or fighting the generator's templates.
- Any change to behavior not expressible in OpenAPI (custom headers, partial
  responses, etc.) requires customizing the generator, which adds friction and a
  second thing to keep in sync.

Hand-coded (Store, Product):
+ Full control over HTTP semantics - StoreResource freely returns Response with
  precise status codes (201, 404, 422) and supports PATCH alongside PUT without any
  generator constraints.
+ Faster to iterate: no codegen step between changing behavior and testing it.
- No compile-time contract; nothing stops the implementation, and any hand-maintained
  docs, from drifting apart over time.

My choice: use contract-first for endpoints with external or cross-team consumers,
where the contract itself is a deliverable (this fits Warehouse, given it's referenced
by the replace/archive business flow other systems might rely on). For small, purely
internal CRUD endpoints like Store and Product, I'd hand-code for speed and control,
but still publish an OpenAPI description via annotations (smallrye-openapi) rather
than a generated interface, so there's at least a discoverable spec without giving up
flexibility.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Priority order, based on where the business risk and iteration speed actually are:

1. Domain/use-case unit tests. This is where the real business rules live (business
   unit code uniqueness, location validity, max-warehouses-per-location, capacity/
   stock limits, the replace-specific capacity-accommodation and stock-matching
   rules). CreateWarehouseUseCaseTest, ReplaceWarehouseUseCaseTest and
   ArchiveWarehouseUseCaseTest cover these with a hand-written InMemoryWarehouseStore
   fake (no Mockito on the classpath, and none needed) plus the real LocationGateway,
   since it's a simple in-memory lookup. These run in milliseconds with no database
   or container, so they're cheap to run on every change and are the highest-value
   tests to write first.

2. Thin REST/integration tests per endpoint, covering the happy path plus the most
   important error statuses (404 for unknown warehouse/business-unit-code, 400 for
   validation failures) - WarehouseEndpointIT. These catch wiring mistakes between
   the REST layer and the domain layer (wrong status code, wrong field mapping) that
   unit tests can't see, at the cost of needing a running app (and, in this project's
   current setup, Docker for the dev-services database).

3. Multi-step end-to-end flows (create -> replace -> archive) as a smaller number of
   broader tests, added once the individual operations are already covered - useful
   for catching regressions across operations, but expensive enough that they
   shouldn't be the primary coverage mechanism.

To keep coverage effective over time, I'd pair every new validation rule with a unit
test in the same change (rule and test land together, not as follow-up work), keep
use-case tests free of framework/persistence dependencies so they stay fast enough to
run on every save, and reserve the slower, container-backed integration tests for CI
rather than the local dev loop - adding one only when a new endpoint or a new
cross-cutting behavior (like the transactional legacy-sync fix in task 2) is
introduced, rather than for every field-level validation.
```
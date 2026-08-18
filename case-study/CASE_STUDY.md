# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**

The core challenge is that most fulfillment costs (labor, transportation, overhead) are
shared across multiple products and stores, not owned by a single one. A Warehouse in
this system already fulfills up to 5 products and serves as one of up to 3 warehouses
for a Store, so a cost incurred at the warehouse level (e.g. a shift of labor, a shared
overhead bill) has no single "correct" owner — it has to be allocated using some basis,
and the choice of basis materially changes the reported cost per product/store.
Inventory holding cost is similar: it depends on capacity utilization, which this system
already tracks (`capacity`/`stock` per Warehouse), so that data could double as an
allocation key. Transportation is the hardest of the four, since it depends on
shipment-level data this system doesn't currently model at all (routes, frequency,
carrier), not just the warehouse/store relationship.

From similar fulfillment/inventory systems, the recurring mistake is allocating shared
costs evenly (e.g. "split the warehouse's overhead by number of products") instead of by
actual usage (stock volume, shipment count, capacity occupied) — it's simpler to build
but produces numbers nobody trusts once someone compares two products with very
different volumes getting identical allocated cost.

**Questions/considerations:**
- What's the allocation basis for each cost type — capacity % for holding cost, shipment
  count for transportation, something else for labor/overhead? Does it need to be
  configurable per cost type rather than one global rule?
- Should allocation happen at the fulfillment-association level (product + store +
  warehouse, which this system already models) or only at the warehouse/store level?
- How fresh does cost data need to be — real-time attribution per fulfillment event, or
  a periodic (e.g. monthly) batch reconciliation against finance?
- When a Warehouse is archived/replaced, does its historical cost allocation need to stay
  queryable under the old warehouse ID, or does it roll up to the business unit code
  (see Scenario 5)?

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**

Concrete strategies that follow directly from what this system already tracks:
- **Consolidate low-utilization warehouses.** Since each Location has a
  `maxNumberOfWarehouses` and `maxCapacity`, and each Warehouse tracks its own
  `capacity`/`stock`, under-utilized warehouses (low stock relative to capacity) at the
  same location are identifiable today without new data — merging them frees up a slot
  under the location's warehouse-count cap and cuts duplicated overhead.
- **Reduce redundant fulfillment paths.** The 2-warehouses-per-product-per-store and
  3-warehouses-per-store constraints exist to bound complexity, but a store could still
  be using its full quota of warehouses inefficiently (e.g. splitting one product across
  two distant warehouses when one nearer warehouse has spare capacity) — rebalancing the
  `ProductStoreWarehouse` associations toward the cheaper/closer option reduces
  transportation cost without violating any constraint.
- **Renegotiate transportation contracts based on real volume**, once shipment-level data
  (noted as a gap in Scenario 1) exists to show actual, not estimated, volumes per lane.

**Identify:** rank locations/warehouses by capacity-utilization %, and rank stores by
number of distinct warehouses they actually use versus their cap, to spot both
under-utilization and unnecessary fragmentation.

**Prioritize:** by expected savings versus effort/risk — consolidation and rebalancing
are data-only changes with no contract renegotiation needed, so they're the fast/cheap
tier; transportation contract renegotiation and physical warehouse consolidation are
higher-effort, higher-impact, and slower to execute.

**Implement:** pilot on a small set of stores/locations first, measure the actual cost
delta against the baseline from Scenario 1's cost tracking, then roll out.

**Questions/considerations:**
- Consolidating warehouses reduces cost but also reduces redundancy — if the remaining
  warehouse has an outage, more stores are now affected. What's the acceptable
  resilience trade-off?
- How do we avoid a rebalancing recommendation that's cheaper on paper but breaks one of
  the existing hard constraints (2/3/5) once applied?
- What's the minimum observation window before calling a warehouse "low-utilization"
  rather than reacting to a temporary dip?

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**

Everything discussed in Scenarios 1 and 2 (allocation basis, optimization targets) is
only as trustworthy as the underlying financial data — without a real link to the
general ledger/AP/payroll systems, cost allocation is just a modeling exercise running
on estimates. Integration is what turns it into something finance will actually rely on
for budgeting decisions (Scenario 4) and something operations can act on with
confidence (Scenario 2).

**Benefits:** a single source of truth for cost data instead of finance and operations
reconciling two separate spreadsheets after the fact; faster month-end close since
fulfillment costs are already categorized and attributed as they occur; and the ability
to compare actual spend against budget in near-real-time rather than a monthly delay.

**Approach to seamless sync:** this codebase already solved a closely related problem —
`StoreResource` used to notify the legacy store system inline inside the same
transaction that persisted the `Store`, risking a notification for a change that later
rolled back. The fix (fire a CDI event, let `LegacyStoreManagerGateway` observe it
`AFTER_SUCCESS` so the legacy call only happens once the DB transaction has actually
committed) is the same pattern I'd apply to financial-system sync: publish cost/fulfillment
events only after they're durably committed, and back that up with a nightly batch
reconciliation (idempotent, keyed by business unit code / association ID) to catch
anything the event stream missed, rather than treating the real-time feed as the only
source of truth.

**Questions/considerations:**
- Which system is authoritative — does this app push operational cost data outward, pull
  budget figures inward, or both? Getting this backwards leads to circular sync loops.
- What's the acceptable latency — does finance need real-time dashboards, or is
  nightly/end-of-day sufficient?
- How do we guarantee idempotency on retries so a network blip doesn't double-count a
  cost entry in the financial system?
- What's the schema/format contract with finance's system, and who owns changes to it
  over time?

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**

Without forecasting, decisions about warehouse capacity and location structure stay
reactive: today, a new Warehouse only gets justified once an existing location is at or
near its `maxNumberOfWarehouses`/`maxCapacity` limits (enforced in
`CreateWarehouseUseCase`) — by definition, that's after the constraint has already
started to bind, not before. Budgeting/forecasting is what lets the business plan for
that threshold in advance (lease/staff a new warehouse ahead of hitting the cap) instead
of scrambling once it's already a blocker.

**What I'd take into account designing this:**
- **Historical cost + volume trend per location/warehouse**, at a granularity that
  matches how budgets are actually reviewed (monthly is typical for opex). This depends
  on cost history surviving warehouse replacement, not being lost when a warehouse is
  archived (Scenario 5).
- **Seasonality and growth assumptions per store** — retail fulfillment volume is rarely
  flat, and treating it as flat is one of the most common forecasting mistakes.
  Forecasts need at least a seasonal adjustment and a store-growth assumption, sourced
  from whoever owns store-level sales projections.
- **Leading indicators tied to the existing hard constraints** — e.g. flag a location
  projected to cross its `maxCapacity` within N months, so a new-warehouse decision (and
  its budget) can be made proactively rather than at the point the constraint actually
  blocks a create/replace operation.
- **Feedback loop** — actuals need to flow back in to correct forecast drift over time,
  otherwise the forecast just compounds the same bias every period.

**Questions/considerations:**
- What forecasting horizon does the business actually plan against — quarterly,
  annual? That determines how much historical data is even useful.
- Should "approaching capacity" become a first-class alert in this system, or does that
  live entirely in a separate BI/finance tool that just reads this system's data?
- How is forecast accuracy itself measured and reviewed, so the forecasting approach can
  be improved over time rather than just repeated?

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**

This scenario maps directly onto `ReplaceWarehouseUseCase` as implemented: replacing a
Warehouse archives the old one (`archivedAt` set, kept in the database) and creates a
new one under the *same* `businessUnitCode`, rather than deleting the old record and
starting a fresh, unrelated one. That design choice is precisely what makes cost-history
preservation possible — the business unit code is the stable key across the
replacement, while the Warehouse's numeric ID changes.

**Why preserving cost history matters:** if replacement severed the link between old and
new (e.g. by not reusing the business unit code, or by hard-deleting the old warehouse),
you'd lose the ability to compare pre- and post-replacement cost trends for the same
operational area — which is the only way to actually judge whether a replacement
achieved its goal (lower cost, more capacity, etc.) rather than just being a disruptive
swap. It also matters for compliance/audit: finance needs an unbroken cost trail for a
business unit even as the physical warehouse behind it changes.

**How this ties to budget:** the new warehouse shouldn't start its budget from zero —
it should inherit the prior warehouse's run-rate as a baseline, adjusted for the reasons
the replacement happened (more capacity, better location terms, etc.). The
capacity-accommodation rule already enforced in code (new `capacity` must be able to
hold the old warehouse's `stock`) has a direct cost-control analogue: before approving a
replacement, there should be a similar check that the new warehouse's budgeted
opex/capex doesn't exceed what the business unit's budget allows, the same way its
physical capacity can't fall short of what the business unit currently holds.

**Questions/considerations:**
- Should cost records be keyed by business unit code (persists across replacement) or by
  warehouse ID (changes)? Business unit code seems right for continuity, but any
  warehouse-specific cost (e.g. a one-time move/setup cost) needs to still be
  attributable to the specific physical warehouse that incurred it.
- How do we attribute costs during the transition window if old and new warehouses
  briefly overlap (e.g. parallel-running before full cutover)?
- Does a replacement need a budget pre-check/approval step analogous to the
  capacity-accommodation validation already enforced for stock, so a replacement can't
  go live if it would blow the business unit's budget?

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.

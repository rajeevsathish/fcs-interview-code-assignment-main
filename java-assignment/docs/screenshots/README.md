# Screenshots

Captured from the running app (`mvn quarkus:dev`) against a live Postgres instance.

- **01-swagger-ui-overview.png** — `http://localhost:8080/q/swagger-ui/`, showing every
  REST resource (Fulfillment, Product, Store, Warehouse) discoverable from the generated
  OpenAPI document at `/q/openapi`.
- **02-swagger-ui-warehouse-post.png** — `POST /warehouse` expanded, showing the request
  body schema generated at build time from `warehouse-openapi.yaml` by
  `quarkus-openapi-generator-server`.
- **03-curl-validation-demo.png** — a sequence of real `curl` calls against the running
  app demonstrating the validation rules returning the correct HTTP status:
  - duplicate business unit code on warehouse create -> `400`
  - archiving an unknown warehouse -> `404`
  - replacing a warehouse with capacity below its existing stock -> `400`
  - updating a store without the required `name` field -> `422`
  - looking up an unknown product -> `404`
- **04-webui-overview.png** — the simple web UI at `/index.html` (see below), showing the
  Store/Product/Warehouse/Fulfillment cards populated with the seeded data and a request
  log recording the raw response of every call the UI makes.
- **05-webui-warehouse-validation.png** — same UI after creating warehouse `MWH.UIDEMO`
  and then submitting the identical business unit code again: the request log shows the
  live `400` ("A warehouse with business unit code MWH.UIDEMO already exists.") right
  next to the successful create.
- **06-webui-store-product-created.png** — a Store and a Product created through the UI,
  appearing immediately in their respective lists (confirms the CRUD round-trip, not just
  the initial page load).

## The web UI

`java-assignment/src/main/resources/META-INF/resources/index.html` is a small, dependency
-free (no CDN, plain HTML/CSS/JS) single page served by Quarkus at the app root. It gives
each REST resource a card with a create form and a live list, plus a shared request log
so every request/response (including validation errors) is visible without opening dev
tools. Run `mvn quarkus:dev` and open <http://localhost:8080/index.html> to use it.

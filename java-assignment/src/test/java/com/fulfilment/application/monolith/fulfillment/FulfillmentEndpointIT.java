package com.fulfilment.application.monolith.fulfillment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

// @QuarkusTest (in-process) rather than @QuarkusIntegrationTest (external packaged jar):
// Quarkus's build-time bytecode transformation makes the packaged jar's classes differ
// from target/classes, so JaCoCo discards coverage data for anything only exercised via
// @QuarkusIntegrationTest. Running in-process is what lets this endpoint's coverage count
// toward the 80% bundle minimum, matching how ProductEndpointTest is already set up.
@QuarkusTest
public class FulfillmentEndpointIT {

  private static Long createStore(String name) {
    String body = "{\"name\":\"" + name + "\",\"quantityProductsInStock\":0}";
    return given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("store")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private static Long createProduct(String name) {
    String body = "{\"name\":\"" + name + "\"}";
    return given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("product")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private static String createWarehouse(
      String businessUnitCode, String location, int capacity, int stock) {
    String body =
        "{\"businessUnitCode\":\""
            + businessUnitCode
            + "\",\"location\":\""
            + location
            + "\",\"capacity\":"
            + capacity
            + ",\"stock\":"
            + stock
            + "}";
    return given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("warehouse")
        .then()
        .statusCode(200)
        .extract()
        .jsonPath()
        .getString("id");
  }

  private static void associate(Object storeId, Object productId, Object warehouseId, int expectedStatus) {
    given()
        .when()
        .post("fulfillment/store/" + storeId + "/product/" + productId + "/warehouse/" + warehouseId)
        .then()
        .statusCode(expectedStatus);
  }

  @Test
  public void testAssociateWarehouseHappyPath() {
    Long storeId = createStore("Fulfillment Store A");
    Long productId = createProduct("Fulfillment Product A");
    String warehouseId = createWarehouse("MWH.900", "VETSBY-001", 30, 5);

    associate(storeId, productId, warehouseId, 201);

    given()
        .when()
        .get("fulfillment/store/" + storeId)
        .then()
        .statusCode(200)
        .body(containsString(warehouseId));
  }

  @Test
  public void testDuplicateAssociationShouldConflict() {
    Long storeId = createStore("Fulfillment Store B");
    Long productId = createProduct("Fulfillment Product B");
    String warehouseId = createWarehouse("MWH.901", "HELMOND-001", 20, 5);

    associate(storeId, productId, warehouseId, 201);
    associate(storeId, productId, warehouseId, 409);
  }

  @Test
  public void testMaxTwoWarehousesPerProductPerStore() {
    Long storeId = createStore("Fulfillment Store C");
    Long productId = createProduct("Fulfillment Product C");
    String warehouse1 = createWarehouse("MWH.902", "AMSTERDAM-002", 20, 5);
    String warehouse2 = createWarehouse("MWH.903", "AMSTERDAM-002", 20, 5);
    String warehouse3 = createWarehouse("MWH.904", "AMSTERDAM-002", 20, 5);

    associate(storeId, productId, warehouse1, 201);
    associate(storeId, productId, warehouse2, 201);
    // 3rd distinct warehouse for the same product at the same store: exceeds the max of 2
    associate(storeId, productId, warehouse3, 400);
  }

  @Test
  public void testMaxThreeWarehousesPerStore() {
    Long storeId = createStore("Fulfillment Store D");
    Long product1 = createProduct("Fulfillment Product D1");
    Long product2 = createProduct("Fulfillment Product D2");
    Long product3 = createProduct("Fulfillment Product D3");
    Long product4 = createProduct("Fulfillment Product D4");
    // AMSTERDAM-001 already has one seeded warehouse (MWH.012), leaving room for 4 more.
    String warehouse1 = createWarehouse("MWH.905", "AMSTERDAM-001", 10, 0);
    String warehouse2 = createWarehouse("MWH.906", "AMSTERDAM-001", 10, 0);
    String warehouse3 = createWarehouse("MWH.907", "AMSTERDAM-001", 10, 0);
    String warehouse4 = createWarehouse("MWH.908", "AMSTERDAM-001", 10, 0);

    associate(storeId, product1, warehouse1, 201);
    associate(storeId, product2, warehouse2, 201);
    associate(storeId, product3, warehouse3, 201);
    // 4th distinct warehouse for the same store: exceeds the max of 3
    associate(storeId, product4, warehouse4, 400);
  }

  @Test
  public void testMaxFiveProductsPerWarehouse() {
    Long storeId = createStore("Fulfillment Store E");
    String warehouseId = createWarehouse("MWH.909", "ZWOLLE-002", 10, 0);

    for (int i = 1; i <= 5; i++) {
      Long productId = createProduct("Fulfillment Product E" + i);
      associate(storeId, productId, warehouseId, 201);
    }

    Long sixthProduct = createProduct("Fulfillment Product E6");
    // 6th distinct product for the same warehouse: exceeds the max of 5
    associate(storeId, sixthProduct, warehouseId, 400);
  }

  @Test
  public void testAssociateWithUnknownEntitiesShouldReturnNotFound() {
    associate(999999, 999999, 999999, 404);
  }
}

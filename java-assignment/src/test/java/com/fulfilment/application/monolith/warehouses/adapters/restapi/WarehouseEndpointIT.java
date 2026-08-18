package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

// @QuarkusTest (in-process) rather than @QuarkusIntegrationTest (external packaged jar):
// Quarkus's build-time bytecode transformation makes the packaged jar's classes differ
// from target/classes, so JaCoCo discards coverage data for anything only exercised via
// @QuarkusIntegrationTest. Running in-process is what lets this endpoint's coverage count
// toward the 80% bundle minimum, matching how ProductEndpointTest is already set up.
@QuarkusTest
public class WarehouseEndpointIT {

  @Test
  public void testSimpleListWarehouses() {

    final String path = "warehouse";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  public void testSimpleCheckingArchivingWarehouses() {

    final String path = "warehouse";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"),
            containsString("ZWOLLE-001"),
            containsString("AMSTERDAM-001"),
            containsString("TILBURG-001"));

    // Archive the ZWOLLE-001:
    given().when().delete(path + "/1").then().statusCode(204);

    // List all, ZWOLLE-001 should be missing now:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            not(containsString("ZWOLLE-001")),
            containsString("AMSTERDAM-001"),
            containsString("TILBURG-001"));
  }

  @Test
  public void testArchivingUnknownWarehouseShouldReturnNotFound() {
    final String path = "warehouse";

    given().when().delete(path + "/9999").then().statusCode(404);
  }

  @Test
  public void testGetSingleWarehouseHappyPathAndNotFound() {
    final String path = "warehouse";

    given()
        .when()
        .get(path + "/2")
        .then()
        .statusCode(200)
        .body(containsString("MWH.012"), containsString("AMSTERDAM-001"));

    given().when().get(path + "/9999").then().statusCode(404);
  }

  @Test
  public void testGetSingleWarehouseWithInvalidIdReturnsBadRequest() {
    given().when().get("warehouse/not-a-number").then().statusCode(400);
  }

  @Test
  public void testReplaceUnknownWarehouseReturnsNotFound() {
    String replacementJson =
        "{\"businessUnitCode\":\"MWH.NOPE\",\"location\":\"EINDHOVEN-001\",\"capacity\":10,\"stock\":0}";

    given()
        .contentType(ContentType.JSON)
        .body(replacementJson)
        .when()
        .post("warehouse/MWH.NOPE/replacement")
        .then()
        .statusCode(404);
  }

  @Test
  public void testCreateWarehouseValidationErrorBranches() {
    // NOTE: EINDHOVEN-001 allows at most 2 active warehouses and this suite's other test
    // (testCreateAndReplaceWarehouseFlow) already occupies one of those two slots with
    // MWH.999, regardless of test execution order (both net exactly one active warehouse
    // each). A single warehouse is created and reused for the two assertions below so this
    // test only consumes the one remaining slot rather than two.
    final String path = "warehouse";

    String warehouseJson =
        "{\"businessUnitCode\":\"MWH.997\",\"location\":\"EINDHOVEN-001\",\"capacity\":20,\"stock\":10}";

    given().contentType(ContentType.JSON).body(warehouseJson).when().post(path).then().statusCode(200);

    // Same business unit code again should be rejected as a validation error:
    given().contentType(ContentType.JSON).body(warehouseJson).when().post(path).then().statusCode(400);

    // Replacement capacity (5) can't hold the existing stock (10):
    String replacementJson =
        "{\"businessUnitCode\":\"MWH.997\",\"location\":\"EINDHOVEN-001\",\"capacity\":5,\"stock\":10}";

    given()
        .contentType(ContentType.JSON)
        .body(replacementJson)
        .when()
        .post(path + "/MWH.997/replacement")
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateAndReplaceWarehouseFlow() {
    final String path = "warehouse";

    String newWarehouseJson =
        "{\"businessUnitCode\":\"MWH.999\",\"location\":\"EINDHOVEN-001\",\"capacity\":20,\"stock\":5}";

    // Create a new warehouse:
    given()
        .contentType(ContentType.JSON)
        .body(newWarehouseJson)
        .when()
        .post(path)
        .then()
        .statusCode(200)
        .body(containsString("MWH.999"), containsString("EINDHOVEN-001"));

    // Replace it, keeping the same business unit code and matching stock:
    String replacementJson =
        "{\"businessUnitCode\":\"MWH.999\",\"location\":\"EINDHOVEN-001\",\"capacity\":25,\"stock\":5}";

    given()
        .contentType(ContentType.JSON)
        .body(replacementJson)
        .when()
        .post(path + "/MWH.999/replacement")
        .then()
        .statusCode(200)
        .body(containsString("MWH.999"), containsString("\"capacity\":25"));
  }
}

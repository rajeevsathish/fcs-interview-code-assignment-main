package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProductEndpointTest {

  @Test
  public void testCrudProduct() {
    final String path = "product";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    // Delete the TONSTAD:
    given().when().delete(path + "/1").then().statusCode(204);

    // List all, TONSTAD should be missing now:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(not(containsString("TONSTAD")), containsString("KALLAX"), containsString("BESTÅ"));
  }

  @Test
  public void testCreateProductWithIdSetIsRejected() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"id\":1,\"name\":\"Should Fail\"}")
        .when()
        .post("product")
        .then()
        .statusCode(422);
  }

  @Test
  public void testCreateGetUpdateProductHappyPath() {
    String createBody = "{\"name\":\"Test Product\",\"description\":\"desc\",\"price\":9.99,\"stock\":5}";

    Long id =
        given()
            .contentType(ContentType.JSON)
            .body(createBody)
            .when()
            .post("product")
            .then()
            .statusCode(201)
            .body(containsString("Test Product"))
            .extract()
            .jsonPath()
            .getLong("id");

    given().when().get("product/" + id).then().statusCode(200).body(containsString("Test Product"));

    String updateBody =
        "{\"name\":\"Test Product Updated\",\"description\":\"desc2\",\"price\":19.99,\"stock\":10}";
    given()
        .contentType(ContentType.JSON)
        .body(updateBody)
        .when()
        .put("product/" + id)
        .then()
        .statusCode(200)
        .body(containsString("Test Product Updated"));
  }

  @Test
  public void testGetSingleUnknownProductReturnsNotFound() {
    given().when().get("product/999999").then().statusCode(404);
  }

  @Test
  public void testUpdateProductValidationAndNotFound() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"description\":\"no name\"}")
        .when()
        .put("product/1")
        .then()
        .statusCode(422);

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Doesn't matter\"}")
        .when()
        .put("product/999999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testDeleteUnknownProductReturnsNotFound() {
    given().when().delete("product/999999").then().statusCode(404);
  }
}

package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StoreResourceTest {

  private static Long createStore(String name, int quantity) {
    String body = "{\"name\":\"" + name + "\",\"quantityProductsInStock\":" + quantity + "}";
    return given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("store")
        .then()
        .statusCode(201)
        .body(containsString(name))
        .extract()
        .jsonPath()
        .getLong("id");
  }

  @Test
  public void testCreateStoreWithIdSetIsRejected() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"id\":1,\"name\":\"Should Fail\",\"quantityProductsInStock\":0}")
        .when()
        .post("store")
        .then()
        .statusCode(422);
  }

  @Test
  public void testGetSingleAndListStore() {
    Long id = createStore("Store For Get", 3);

    given().when().get("store/" + id).then().statusCode(200).body(containsString("Store For Get"));

    given().when().get("store").then().statusCode(200).body(containsString("Store For Get"));
  }

  @Test
  public void testGetSingleUnknownStoreReturnsNotFound() {
    given().when().get("store/999999").then().statusCode(404);
  }

  @Test
  public void testUpdateStoreHappyPathAndValidation() {
    Long id = createStore("Store To Update", 1);

    // Missing name should be rejected:
    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\":5}")
        .when()
        .put("store/" + id)
        .then()
        .statusCode(422);

    // Unknown id should 404:
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Doesn't matter\",\"quantityProductsInStock\":5}")
        .when()
        .put("store/999999")
        .then()
        .statusCode(404);

    // Happy path:
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Store Updated\",\"quantityProductsInStock\":9}")
        .when()
        .put("store/" + id)
        .then()
        .statusCode(200)
        .body(containsString("Store Updated"), containsString("9"));
  }

  @Test
  public void testPatchStoreHappyPathAndValidation() {
    Long id = createStore("Store To Patch", 2);

    // Missing name should be rejected:
    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\":7}")
        .when()
        .patch("store/" + id)
        .then()
        .statusCode(422);

    // Unknown id should 404:
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Doesn't matter\",\"quantityProductsInStock\":7}")
        .when()
        .patch("store/999999")
        .then()
        .statusCode(404);

    // Happy path:
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Store Patched\",\"quantityProductsInStock\":7}")
        .when()
        .patch("store/" + id)
        .then()
        .statusCode(200)
        .body(containsString("Store Patched"), containsString("7"));
  }

  @Test
  public void testDeleteStoreHappyPathAndNotFound() {
    Long id = createStore("Store To Delete", 0);

    given().when().delete("store/" + id).then().statusCode(204);

    given().when().delete("store/999999").then().statusCode(404);
  }
}

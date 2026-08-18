package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.location.LocationGateway;
import com.fulfilment.application.monolith.warehouses.domain.exception.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import org.junit.jupiter.api.Test;

public class CreateWarehouseUseCaseTest {

  private final LocationResolver locationResolver = new LocationGateway();

  private static Warehouse warehouse(String buCode, String location, int capacity, int stock) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = buCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  @Test
  public void testCreateWarehouseHappyPath() {
    // given
    var store = new InMemoryWarehouseStore();
    var useCase = new CreateWarehouseUseCase(store, locationResolver);
    var warehouse = warehouse("MWH.100", "AMSTERDAM-001", 50, 10);

    // when
    useCase.create(warehouse);

    // then
    assertEquals(warehouse, store.findByBusinessUnitCode("MWH.100"));
  }

  @Test
  public void testCreateWarehouseWithDuplicateBusinessUnitCodeShouldFail() {
    // given
    var existing = warehouse("MWH.100", "AMSTERDAM-001", 20, 5);
    var store = new InMemoryWarehouseStore(existing);
    var useCase = new CreateWarehouseUseCase(store, locationResolver);
    var duplicate = warehouse("MWH.100", "AMSTERDAM-002", 20, 5);

    // when / then
    assertThrows(WarehouseValidationException.class, () -> useCase.create(duplicate));
  }

  @Test
  public void testCreateWarehouseWithInvalidLocationShouldFail() {
    // given
    var store = new InMemoryWarehouseStore();
    var useCase = new CreateWarehouseUseCase(store, locationResolver);
    var warehouse = warehouse("MWH.100", "NOWHERE-999", 20, 5);

    // when / then
    assertThrows(WarehouseValidationException.class, () -> useCase.create(warehouse));
  }

  @Test
  public void testCreateWarehouseWhenMaxNumberOfWarehousesReachedShouldFail() {
    // given: ZWOLLE-001 allows at most 1 warehouse
    var existing = warehouse("MWH.100", "ZWOLLE-001", 30, 5);
    var store = new InMemoryWarehouseStore(existing);
    var useCase = new CreateWarehouseUseCase(store, locationResolver);
    var newWarehouse = warehouse("MWH.101", "ZWOLLE-001", 5, 1);

    // when / then
    assertThrows(WarehouseValidationException.class, () -> useCase.create(newWarehouse));
  }

  @Test
  public void testCreateWarehouseExceedingLocationMaxCapacityShouldFail() {
    // given: AMSTERDAM-001 has maxCapacity of 100
    var store = new InMemoryWarehouseStore();
    var useCase = new CreateWarehouseUseCase(store, locationResolver);
    var warehouse = warehouse("MWH.100", "AMSTERDAM-001", 150, 10);

    // when / then
    assertThrows(WarehouseValidationException.class, () -> useCase.create(warehouse));
  }

  @Test
  public void testCreateWarehouseWithStockExceedingCapacityShouldFail() {
    // given
    var store = new InMemoryWarehouseStore();
    var useCase = new CreateWarehouseUseCase(store, locationResolver);
    var warehouse = warehouse("MWH.100", "HELMOND-001", 30, 50);

    // when / then
    assertThrows(WarehouseValidationException.class, () -> useCase.create(warehouse));
  }
}

package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.location.LocationGateway;
import com.fulfilment.application.monolith.warehouses.domain.exception.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exception.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import org.junit.jupiter.api.Test;

public class ReplaceWarehouseUseCaseTest {

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
  public void testReplaceWarehouseHappyPath() {
    // given
    var previous = warehouse("MWH.100", "ZWOLLE-001", 30, 10);
    var store = new InMemoryWarehouseStore(previous);
    var useCase = new ReplaceWarehouseUseCase(store, locationResolver);
    var replacement = warehouse("MWH.100", "ZWOLLE-001", 30, 10);

    // when
    useCase.replace(replacement);

    // then: previous is archived, replacement is active with the same business unit code
    long activeCount =
        store.getAll().stream()
            .filter(w -> w.businessUnitCode.equals("MWH.100") && w.archivedAt == null)
            .count();
    assertEquals(1, activeCount);
    assertNotNull(previous.archivedAt);
  }

  @Test
  public void testReplaceUnknownWarehouseShouldFail() {
    // given
    var store = new InMemoryWarehouseStore();
    var useCase = new ReplaceWarehouseUseCase(store, locationResolver);
    var replacement = warehouse("MWH.999", "ZWOLLE-001", 30, 10);

    // when / then
    assertThrows(WarehouseNotFoundException.class, () -> useCase.replace(replacement));
  }

  @Test
  public void testReplaceWithCapacityBelowPreviousStockShouldFail() {
    // given
    var previous = warehouse("MWH.100", "ZWOLLE-001", 30, 25);
    var store = new InMemoryWarehouseStore(previous);
    var useCase = new ReplaceWarehouseUseCase(store, locationResolver);
    var replacement = warehouse("MWH.100", "ZWOLLE-001", 20, 25);

    // when / then
    assertThrows(WarehouseValidationException.class, () -> useCase.replace(replacement));
  }

  @Test
  public void testReplaceWithMismatchingStockShouldFail() {
    // given
    var previous = warehouse("MWH.100", "ZWOLLE-001", 30, 10);
    var store = new InMemoryWarehouseStore(previous);
    var useCase = new ReplaceWarehouseUseCase(store, locationResolver);
    var replacement = warehouse("MWH.100", "ZWOLLE-001", 30, 15);

    // when / then
    assertThrows(WarehouseValidationException.class, () -> useCase.replace(replacement));
  }
}

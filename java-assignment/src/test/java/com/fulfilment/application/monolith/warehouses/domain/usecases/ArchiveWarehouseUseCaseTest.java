package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.exception.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import org.junit.jupiter.api.Test;

public class ArchiveWarehouseUseCaseTest {

  private static Warehouse warehouse(String buCode, String location, int capacity, int stock) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = buCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  @Test
  public void testArchiveWarehouseHappyPath() {
    // given
    var warehouse = warehouse("MWH.100", "ZWOLLE-001", 30, 10);
    var store = new InMemoryWarehouseStore(warehouse);
    var useCase = new ArchiveWarehouseUseCase(store);

    // when
    useCase.archive(warehouse);

    // then
    assertNotNull(store.findByBusinessUnitCode("MWH.100").archivedAt);
  }

  @Test
  public void testArchivingAlreadyArchivedWarehouseShouldFail() {
    // given
    var warehouse = warehouse("MWH.100", "ZWOLLE-001", 30, 10);
    var store = new InMemoryWarehouseStore(warehouse);
    var useCase = new ArchiveWarehouseUseCase(store);
    useCase.archive(warehouse);

    // when / then
    assertThrows(WarehouseValidationException.class, () -> useCase.archive(warehouse));
  }
}

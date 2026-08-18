package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exception.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exception.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    Warehouse previousWarehouse =
        warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (previousWarehouse == null || previousWarehouse.archivedAt != null) {
      throw new WarehouseNotFoundException(
          "No active warehouse found with business unit code "
              + newWarehouse.businessUnitCode
              + " to replace.");
    }

    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new WarehouseValidationException(
          "Location " + newWarehouse.location + " is not a valid location.");
    }

    var otherActiveWarehousesAtLocation =
        warehouseStore.getAll().stream()
            .filter(existing -> existing.archivedAt == null)
            .filter(existing -> existing.location.equals(location.identification))
            .filter(existing -> !existing.businessUnitCode.equals(previousWarehouse.businessUnitCode))
            .toList();

    WarehouseValidations.validateLocationCapacity(
        newWarehouse, location, otherActiveWarehousesAtLocation);
    WarehouseValidations.validateStockWithinCapacity(newWarehouse);

    if (newWarehouse.capacity < previousWarehouse.stock) {
      throw new WarehouseValidationException(
          "New warehouse capacity ("
              + newWarehouse.capacity
              + ") cannot accommodate the stock ("
              + previousWarehouse.stock
              + ") of the warehouse being replaced.");
    }

    if (!newWarehouse.stock.equals(previousWarehouse.stock)) {
      throw new WarehouseValidationException(
          "New warehouse stock ("
              + newWarehouse.stock
              + ") must match the stock of the warehouse being replaced ("
              + previousWarehouse.stock
              + ").");
    }

    previousWarehouse.archivedAt = LocalDateTime.now();
    warehouseStore.update(previousWarehouse);

    newWarehouse.createdAt = LocalDateTime.now();
    newWarehouse.archivedAt = null;
    warehouseStore.create(newWarehouse);
  }
}

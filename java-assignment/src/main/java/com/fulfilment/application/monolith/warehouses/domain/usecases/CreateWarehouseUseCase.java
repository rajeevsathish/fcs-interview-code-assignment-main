package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exception.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new WarehouseValidationException(
          "A warehouse with business unit code "
              + warehouse.businessUnitCode
              + " already exists.");
    }

    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new WarehouseValidationException(
          "Location " + warehouse.location + " is not a valid location.");
    }

    var activeWarehousesAtLocation =
        warehouseStore.getAll().stream()
            .filter(existing -> existing.archivedAt == null)
            .filter(existing -> existing.location.equals(location.identification))
            .toList();

    WarehouseValidations.validateLocationCapacity(
        warehouse, location, activeWarehousesAtLocation);
    WarehouseValidations.validateStockWithinCapacity(warehouse);

    // if all went well, create the warehouse
    warehouseStore.create(warehouse);
  }
}

package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exception.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.util.List;

/** Shared capacity/stock rules used by both warehouse creation and replacement. */
final class WarehouseValidations {

  private WarehouseValidations() {}

  static void validateLocationCapacity(
      Warehouse candidate, Location location, List<Warehouse> otherActiveWarehousesAtLocation) {
    if (otherActiveWarehousesAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WarehouseValidationException(
          "Location "
              + location.identification
              + " has already reached its maximum number of warehouses ("
              + location.maxNumberOfWarehouses
              + ").");
    }

    int occupiedCapacity =
        otherActiveWarehousesAtLocation.stream().mapToInt(warehouse -> warehouse.capacity).sum();
    if (occupiedCapacity + candidate.capacity > location.maxCapacity) {
      throw new WarehouseValidationException(
          "Warehouse capacity of "
              + candidate.capacity
              + " exceeds the maximum capacity available at location "
              + location.identification
              + ".");
    }
  }

  static void validateStockWithinCapacity(Warehouse candidate) {
    if (candidate.stock > candidate.capacity) {
      throw new WarehouseValidationException(
          "Warehouse stock ("
              + candidate.stock
              + ") cannot exceed its capacity ("
              + candidate.capacity
              + ").");
    }
  }
}

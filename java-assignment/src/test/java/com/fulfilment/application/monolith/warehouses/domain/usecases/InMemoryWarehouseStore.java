package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.List;

/** Simple in-memory fake used by use-case unit tests instead of a mocking framework. */
class InMemoryWarehouseStore implements WarehouseStore {

  private final List<Warehouse> warehouses = new ArrayList<>();

  InMemoryWarehouseStore(Warehouse... seed) {
    warehouses.addAll(List.of(seed));
  }

  @Override
  public List<Warehouse> getAll() {
    return new ArrayList<>(warehouses);
  }

  @Override
  public void create(Warehouse warehouse) {
    warehouses.add(warehouse);
  }

  @Override
  public void update(Warehouse warehouse) {
    for (int i = 0; i < warehouses.size(); i++) {
      if (warehouses.get(i).businessUnitCode.equals(warehouse.businessUnitCode)) {
        warehouses.set(i, warehouse);
        return;
      }
    }
    throw new IllegalStateException(
        "Warehouse with business unit code " + warehouse.businessUnitCode + " does not exist.");
  }

  @Override
  public void remove(Warehouse warehouse) {
    warehouses.removeIf(existing -> existing.businessUnitCode.equals(warehouse.businessUnitCode));
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    return warehouses.stream()
        .filter(existing -> existing.businessUnitCode.equals(buCode))
        .findFirst()
        .orElse(null);
  }
}

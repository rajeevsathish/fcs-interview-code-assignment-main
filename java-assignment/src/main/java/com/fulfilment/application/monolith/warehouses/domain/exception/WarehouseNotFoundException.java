package com.fulfilment.application.monolith.warehouses.domain.exception;

public class WarehouseNotFoundException extends RuntimeException {

  public WarehouseNotFoundException(String message) {
    super(message);
  }
}

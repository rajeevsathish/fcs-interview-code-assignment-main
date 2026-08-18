package com.fulfilment.application.monolith.warehouses.domain.exception;

public class WarehouseValidationException extends RuntimeException {

  public WarehouseValidationException(String message) {
    super(message);
  }
}

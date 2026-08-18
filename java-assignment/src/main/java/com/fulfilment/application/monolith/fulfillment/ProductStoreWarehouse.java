package com.fulfilment.application.monolith.fulfillment;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Associates a Warehouse as a fulfillment unit of a Product for a given Store. */
@Entity
@Table(
    name = "product_store_warehouse",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"productId", "storeId", "warehouseId"}))
public class ProductStoreWarehouse {

  @Id @GeneratedValue public Long id;

  public Long productId;

  public Long storeId;

  public Long warehouseId;

  public ProductStoreWarehouse() {}
}

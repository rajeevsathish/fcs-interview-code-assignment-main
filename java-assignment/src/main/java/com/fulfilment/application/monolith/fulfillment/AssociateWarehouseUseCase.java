package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class AssociateWarehouseUseCase {

  static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;
  static final int MAX_WAREHOUSES_PER_STORE = 3;
  static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;

  @Inject FulfillmentRepository fulfillmentRepository;

  @Inject ProductRepository productRepository;

  @Inject WarehouseRepository warehouseRepository;

  public ProductStoreWarehouse associate(Long storeId, Long productId, Long warehouseId) {
    if (Store.findById(storeId) == null) {
      throw new WebApplicationException("Store with id of " + storeId + " does not exist.", 404);
    }
    if (productRepository.findById(productId) == null) {
      throw new WebApplicationException(
          "Product with id of " + productId + " does not exist.", 404);
    }
    if (warehouseRepository.findById(warehouseId) == null) {
      throw new WebApplicationException(
          "Warehouse with id of " + warehouseId + " does not exist.", 404);
    }

    boolean alreadyAssociated =
        fulfillmentRepository
            .find("storeId = ?1 and productId = ?2 and warehouseId = ?3", storeId, productId, warehouseId)
            .firstResultOptional()
            .isPresent();
    if (alreadyAssociated) {
      throw new WebApplicationException(
          "Warehouse "
              + warehouseId
              + " is already associated with product "
              + productId
              + " at store "
              + storeId
              + ".",
          409);
    }

    long warehousesForProductAtStore =
        fulfillmentRepository.count("storeId = ?1 and productId = ?2", storeId, productId);
    if (warehousesForProductAtStore >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new WebApplicationException(
          "Product "
              + productId
              + " is already fulfilled by the maximum of "
              + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
              + " warehouses at store "
              + storeId
              + ".",
          400);
    }

    var warehousesAtStore =
        fulfillmentRepository.find("storeId = ?1", storeId).stream()
            .map(association -> association.warehouseId)
            .distinct()
            .toList();
    if (!warehousesAtStore.contains(warehouseId) && warehousesAtStore.size() >= MAX_WAREHOUSES_PER_STORE) {
      throw new WebApplicationException(
          "Store "
              + storeId
              + " is already fulfilled by the maximum of "
              + MAX_WAREHOUSES_PER_STORE
              + " different warehouses.",
          400);
    }

    var productsAtWarehouse =
        fulfillmentRepository.find("warehouseId = ?1", warehouseId).stream()
            .map(association -> association.productId)
            .distinct()
            .toList();
    if (!productsAtWarehouse.contains(productId) && productsAtWarehouse.size() >= MAX_PRODUCTS_PER_WAREHOUSE) {
      throw new WebApplicationException(
          "Warehouse "
              + warehouseId
              + " already stores the maximum of "
              + MAX_PRODUCTS_PER_WAREHOUSE
              + " different products.",
          400);
    }

    var association = new ProductStoreWarehouse();
    association.storeId = storeId;
    association.productId = productId;
    association.warehouseId = warehouseId;
    fulfillmentRepository.persist(association);
    return association;
  }
}

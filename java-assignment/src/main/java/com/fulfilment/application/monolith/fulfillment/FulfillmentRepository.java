package com.fulfilment.application.monolith.fulfillment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class FulfillmentRepository implements PanacheRepository<ProductStoreWarehouse> {

  public List<ProductStoreWarehouse> listByStore(Long storeId) {
    return find("storeId", storeId).list();
  }
}

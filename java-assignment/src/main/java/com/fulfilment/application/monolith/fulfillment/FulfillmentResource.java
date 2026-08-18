package com.fulfilment.application.monolith.fulfillment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("fulfillment")
@ApplicationScoped
@Produces("application/json")
public class FulfillmentResource {

  @Inject AssociateWarehouseUseCase associateWarehouseUseCase;

  @Inject FulfillmentRepository fulfillmentRepository;

  @POST
  @Path("store/{storeId}/product/{productId}/warehouse/{warehouseId}")
  @Transactional
  public Response associate(
      @PathParam("storeId") Long storeId,
      @PathParam("productId") Long productId,
      @PathParam("warehouseId") Long warehouseId) {
    var association = associateWarehouseUseCase.associate(storeId, productId, warehouseId);
    return Response.status(201).entity(association).build();
  }

  @GET
  @Path("store/{storeId}")
  public List<ProductStoreWarehouse> listForStore(@PathParam("storeId") Long storeId) {
    return fulfillmentRepository.listByStore(storeId);
  }
}

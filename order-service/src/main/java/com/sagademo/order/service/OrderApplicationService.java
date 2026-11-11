package com.sagademo.order.service;

import com.sagademo.order.dto.CreateOrderRequest;
import com.sagademo.order.dto.OrderActionRequest;
import com.sagademo.order.entity.OrderEntity;
import java.util.List;

/**
 * ALL Order business logic lives here.
 *
 * <p>Contract only. Controllers and other services depend on this interface, never on the
 * implementation class.
 *
 * @see com.sagademo.order.service.impl.OrderApplicationServiceImpl
 */
public interface OrderApplicationService {

    /** Local transaction T1: persist a new order in state {@link OrderStatus#CREATED}. */
    OrderEntity createOrder(CreateOrderRequest request);

    /** Local transaction T4: the last forward step of the Saga. */
    OrderEntity confirmOrder(String orderId, OrderActionRequest request);

    /** COMPENSATING TRANSACTION for T1. */
    OrderEntity cancelOrder(String orderId, OrderActionRequest request);

    OrderEntity findOrder(String orderId);

    List<OrderEntity> findAllOrders();

    /** Demo helper: wipes the Order database so a presentation can start from a clean slate. */
    void resetAllData();
}

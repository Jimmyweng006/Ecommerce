package com.jimmyweng.ecommerce.service.order;

import com.jimmyweng.ecommerce.constant.ErrorMessages;
import com.jimmyweng.ecommerce.exception.ResourceNotFoundException;
import com.jimmyweng.ecommerce.model.User;
import com.jimmyweng.ecommerce.model.order.Order;
import com.jimmyweng.ecommerce.repository.order.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jimmyweng.ecommerce.datasource.annotation.ReadFromPrimary;

@Service
@Transactional(readOnly = true)
@ReadFromPrimary
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public OrderQueryService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order getOrderForRequester(Long orderId, String requesterEmail, boolean requesterIsAdmin) {
        Order order = orderRepository
                .findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.orderNotFound(orderId)));

        User owner = order.getUser();
        boolean isOwner = owner.getEmail().equalsIgnoreCase(requesterEmail);
        if (!requesterIsAdmin && !isOwner) {
            throw new ResourceNotFoundException(ErrorMessages.orderNotFound(orderId));
        }

        // Touch the association while the persistence context is open to avoid lazy-loading issues in the controller.
        order.getItems().forEach(item -> item.getProduct().getTitle());
        return order;
    }
}

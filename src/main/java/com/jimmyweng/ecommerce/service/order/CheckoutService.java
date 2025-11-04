package com.jimmyweng.ecommerce.service.order;

import com.jimmyweng.ecommerce.constant.ErrorMessages;
import com.jimmyweng.ecommerce.exception.OutOfStockException;
import com.jimmyweng.ecommerce.exception.ResourceNotFoundException;
import com.jimmyweng.ecommerce.model.User;
import com.jimmyweng.ecommerce.model.order.Order;
import com.jimmyweng.ecommerce.model.order.OrderItem;
import com.jimmyweng.ecommerce.constant.OrderStatus;
import com.jimmyweng.ecommerce.model.product.Product;
import com.jimmyweng.ecommerce.repository.UserRepository;
import com.jimmyweng.ecommerce.repository.order.OrderRepository;
import com.jimmyweng.ecommerce.repository.product.ProductRepository;
import com.jimmyweng.ecommerce.service.order.dto.CreateOrderCommand;
import com.jimmyweng.ecommerce.service.order.dto.OrderItemCommand;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.jimmyweng.ecommerce.constant.ErrorMessages.userNotFound;

@Service
@Transactional
public class CheckoutService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public CheckoutService(UserRepository userRepository, ProductRepository productRepository,
                           OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public record CheckoutResult(Order order, boolean duplicate) {
    }

    public CheckoutResult createOrder(String userEmail, CreateOrderCommand command) {
        User user = userRepository
                .findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(userNotFound(userEmail)));

        Optional<Order> existing = orderRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return new CheckoutResult(existing.get(), true);
        }

        List<OrderItemCommand> sortedItems = command.items().stream()
                .sorted(Comparator.comparing(OrderItemCommand::productId))
                .toList();
        Set<Long> productIds = sortedItems.stream().map(OrderItemCommand::productId).collect(Collectors.toSet());
        Map<Long, Product> productsById = productRepository.findAllByIdInAndDeletedAtIsNull(productIds)
                .stream().collect(Collectors.toMap(Product::getId, product -> product));

        checkProductExist(productsById, productIds);

        Order order = new Order(user, OrderStatus.PENDING, command.idempotencyKey(), BigDecimal.ZERO);

        decrementStock(sortedItems, productsById, order);

        order.setTotalAmount(getOrderTotalAmount(sortedItems, productsById));

        return new CheckoutResult(orderRepository.save(order), false);
    }

    private void checkProductExist(Map<Long, Product> productsById, Set<Long> productIds) {
        if (productsById.size() != productIds.size()) {
            Long missingId = productIds.stream()
                    .filter(id -> !productsById.containsKey(id))
                    .findFirst()
                    .orElseThrow();
            throw new ResourceNotFoundException(ErrorMessages.productNotFound(missingId));
        }
    }

    private void decrementStock(List<OrderItemCommand> sortedItems, Map<Long, Product> productsById, Order order) {
        for (OrderItemCommand itemCommand : sortedItems) {
            Product product = productsById.get(itemCommand.productId());

            int updated = productRepository.decrementStock(product.getId(), itemCommand.quantity());
            if (updated == 0) {
                throw new OutOfStockException(ErrorMessages.outOfStock(product.getId()));
            }

            BigDecimal unitPrice = product.getPrice();
            order.addItem(new OrderItem(product, itemCommand.quantity(), unitPrice));
        }
    }

    private BigDecimal getOrderTotalAmount(List<OrderItemCommand> sortedItems, Map<Long, Product> productsById) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemCommand itemCommand : sortedItems) {
            BigDecimal unitPrice = productsById.get(itemCommand.productId()).getPrice();
            totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(itemCommand.quantity())));
        }

        return totalAmount;
    }
}

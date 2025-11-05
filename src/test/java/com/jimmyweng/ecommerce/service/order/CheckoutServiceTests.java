package com.jimmyweng.ecommerce.service.order;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.jimmyweng.ecommerce.constant.ErrorMessages;
import com.jimmyweng.ecommerce.constant.OrderStatus;
import com.jimmyweng.ecommerce.constant.Role;
import com.jimmyweng.ecommerce.exception.OutOfStockException;
import com.jimmyweng.ecommerce.exception.ResourceNotFoundException;
import com.jimmyweng.ecommerce.model.User;
import com.jimmyweng.ecommerce.model.order.Order;
import com.jimmyweng.ecommerce.model.product.Product;
import com.jimmyweng.ecommerce.repository.UserRepository;
import com.jimmyweng.ecommerce.repository.order.OrderRepository;
import com.jimmyweng.ecommerce.repository.product.ProductRepository;
import com.jimmyweng.ecommerce.service.order.CheckoutService.CheckoutResult;
import com.jimmyweng.ecommerce.service.order.dto.CreateOrderCommand;
import com.jimmyweng.ecommerce.service.order.dto.OrderItemCommand;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private CheckoutService checkoutService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("buyer@example.com", "hash", Role.USER);
    }

    @Test
    void createOrder_whenUserMissing_throwUserNotFound() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        CreateOrderCommand command = new CreateOrderCommand(UUID.randomUUID().toString(), List.of());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> checkoutService.createOrder("buyer@example.com", command));
        assertEquals(ErrorMessages.userNotFound("buyer@example.com"), exception.getMessage());
    }

    @Test
    void createOrder_whenIdempotencyKeyExists_returnExistingOrder() {
        Order existingOrder = new Order(user, OrderStatus.PENDING, "dup-key", BigDecimal.ZERO);
        when(userRepository.findByEmailIgnoreCase("buyer@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdempotencyKey("dup-key")).thenReturn(Optional.of(existingOrder));

        CheckoutResult result = checkoutService.createOrder(
                "buyer@example.com", new CreateOrderCommand("dup-key", List.of()));

        assertTrue(result.duplicate());
        assertSame(existingOrder, result.order());
        verifyNoInteractions(productRepository);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_whenProductMissing_throwProductNotFound() {
        CreateOrderCommand command = new CreateOrderCommand(
                "key", List.of(new OrderItemCommand(1L, 1), new OrderItemCommand(2L, 1)));

        when(userRepository.findByEmailIgnoreCase("buyer@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        Product onlyProduct = createProduct(1L, "Desk", new BigDecimal("10.00"));
        when(productRepository.findAllByIdInAndDeletedAtIsNull(anyCollection())).thenReturn(List.of(onlyProduct));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> checkoutService.createOrder("buyer@example.com", command));
        assertEquals(ErrorMessages.productNotFound(2L), exception.getMessage());
        verify(productRepository, never()).decrementStock(anyLong(), anyInt());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_whenStockInsufficient_throwOutOfStock() {
        CreateOrderCommand command = new CreateOrderCommand(
                "key", List.of(new OrderItemCommand(1L, 1), new OrderItemCommand(2L, 1)));

        when(userRepository.findByEmailIgnoreCase("buyer@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdempotencyKey("key")).thenReturn(Optional.empty());

        Product first = createProduct(1L, "Desk", new BigDecimal("10.00"));
        Product second = createProduct(2L, "Lamp", new BigDecimal("20.00"));
        when(productRepository.findAllByIdInAndDeletedAtIsNull(anyCollection())).thenReturn(List.of(first, second));

        when(productRepository.decrementStock(1L, 1)).thenReturn(1);
        when(productRepository.decrementStock(2L, 1)).thenReturn(0);

        OutOfStockException exception = assertThrows(
                OutOfStockException.class,
                () -> checkoutService.createOrder("buyer@example.com", command));
        assertEquals(ErrorMessages.outOfStock(2L), exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_whenProductsProvided_persistItemsAndTotals() {
        CreateOrderCommand command = new CreateOrderCommand(
                "key",
                List.of(new OrderItemCommand(5L, 2), new OrderItemCommand(3L, 1)));

        when(userRepository.findByEmailIgnoreCase("buyer@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdempotencyKey("key")).thenReturn(Optional.empty());

        Product first = createProduct(5L, "Monitor", new BigDecimal("150.00"));
        Product second = createProduct(3L, "Mouse", new BigDecimal("50.00"));
        when(productRepository.findAllByIdInAndDeletedAtIsNull(anyCollection())).thenReturn(List.of(first, second));

        when(productRepository.decrementStock(3L, 1)).thenReturn(1);
        when(productRepository.decrementStock(5L, 2)).thenReturn(1);
        // Return the same Order instance passed to save(...) so assertions can inspect it.
        when(orderRepository.save(any(Order.class))).then(AdditionalAnswers.returnsFirstArg());

        CheckoutResult result = checkoutService.createOrder("buyer@example.com", command);

        verify(orderRepository).save(any(Order.class));

        Order persisted = result.order();
        assertEquals(2, persisted.getItems().size());
        assertEquals(3L, persisted.getItems().getFirst().getProduct().getId());
        assertEquals(5L, persisted.getItems().getLast().getProduct().getId());
        assertEquals(new BigDecimal("350.00"), persisted.getTotalAmount());
        assertFalse(result.duplicate());

        // test method called order and decrement product stock by productId asc
        InOrder inOrder = inOrder(productRepository);
        inOrder.verify(productRepository).findAllByIdInAndDeletedAtIsNull(anyCollection());
        inOrder.verify(productRepository).decrementStock(3L, 1);
        inOrder.verify(productRepository).decrementStock(5L, 2);
    }

    private Product createProduct(Long id, String title, BigDecimal price) {
        Product product = new Product(title, "desc", "category", price, 10);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}

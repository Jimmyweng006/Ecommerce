package com.jimmyweng.ecommerce.controller.order;

import com.jimmyweng.ecommerce.config.OpenApiConfig;
import com.jimmyweng.ecommerce.controller.common.doc.EnvelopeErrorDoc;
import com.jimmyweng.ecommerce.controller.common.doc.OrderResponseEnvelopeDoc;
import com.jimmyweng.ecommerce.controller.order.dto.CreateOrderRequest;
import com.jimmyweng.ecommerce.controller.order.dto.OrderItemRequest;
import com.jimmyweng.ecommerce.controller.order.dto.OrderResponse;
import com.jimmyweng.ecommerce.service.order.CheckoutService;
import com.jimmyweng.ecommerce.service.order.CheckoutService.CheckoutResult;
import com.jimmyweng.ecommerce.service.order.dto.CreateOrderCommand;
import com.jimmyweng.ecommerce.service.order.dto.OrderItemCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final CheckoutService checkoutService;

    public OrderController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @Operation(
            summary = "Create a new order for the authenticated user",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order created",
                content = @Content(schema = @Schema(implementation = OrderResponseEnvelopeDoc.class))),
        @ApiResponse(responseCode = "200", description = "Order already exists for idempotency key",
                content = @Content(schema = @Schema(implementation = OrderResponseEnvelopeDoc.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request payload",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class))),
        @ApiResponse(responseCode = "403", description = "Only users may place orders",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class))),
        @ApiResponse(responseCode = "404", description = "Product not found",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class))),
        @ApiResponse(responseCode = "409", description = "Insufficient stock",
                content = @Content(schema = @Schema(implementation = EnvelopeErrorDoc.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderResponse> createOrder(
            Principal principal, @Valid @RequestBody CreateOrderRequest request) {
        CheckoutResult result = checkoutService.createOrder(principal.getName(), toCommand(request));
        HttpStatus status = result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(OrderResponse.from(result.order()));
    }

    private CreateOrderCommand toCommand(CreateOrderRequest request) {
        List<OrderItemCommand> items = request.items().stream()
                .map(this::toCommand)
                .toList();
        return new CreateOrderCommand(request.idempotencyKey(), items);
    }

    private OrderItemCommand toCommand(OrderItemRequest request) {
        return new OrderItemCommand(request.productId(), request.quantity());
    }
}

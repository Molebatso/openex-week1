package com.openex.backend.controller

import com.openex.backend.dto.OrderRequest
import com.openex.backend.dto.OrderResponse
import com.openex.backend.security.OpenExUserDetails
import com.openex.backend.service.OrderService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/orders")
class OrderController(private val orderService: OrderService) {

    /** POST /api/orders — place a new order */
    @PostMapping
    fun placeOrder(
        @Valid @RequestBody request: OrderRequest,
        @AuthenticationPrincipal principal: OpenExUserDetails,
    ): ResponseEntity<OrderResponse> {
        val response = orderService.placeOrder(request, principal.userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /** GET /api/orders — list all orders for the authenticated user */
    @GetMapping
    fun getOrders(
        @AuthenticationPrincipal principal: OpenExUserDetails,
    ): ResponseEntity<List<OrderResponse>> =
        ResponseEntity.ok(orderService.getOrders(principal.userId))

    /** DELETE /api/orders/{id} — cancel an open order */
    @DeleteMapping("/{id}")
    fun cancelOrder(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: OpenExUserDetails,
    ): ResponseEntity<OrderResponse> =
        ResponseEntity.ok(orderService.cancelOrder(id, principal.userId))
}

package com.fastcampus.ecommerce.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fastcampus.ecommerce.entity.Role;
import com.fastcampus.ecommerce.entity.User;
import com.fastcampus.ecommerce.model.CheckoutRequest;
import com.fastcampus.ecommerce.model.OrderResponse;
import com.fastcampus.ecommerce.model.OrderStatus;
import com.fastcampus.ecommerce.model.UserInfo;
import com.fastcampus.ecommerce.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private OrderService orderService;

  private CheckoutRequest checkoutRequest;
  private OrderResponse orderResponse;
  private UserInfo userInfo;

  @BeforeEach
  void setUp() {
    checkoutRequest = CheckoutRequest.builder()
        .selectedCartItemIds(Arrays.asList(1L, 2L))
        .userAddressId(1L)
        .build();

    orderResponse = OrderResponse.builder()
        .orderId(1L)
        .status(OrderStatus.PENDING)
        .totalAmount(new BigDecimal("100.00"))
        .shippingFee(new BigDecimal("10.00"))
        .paymentUrl("http://payment.url")
        .build();

    User user = User.builder()
        .userId(1L)
        .username("user@example.com")
        .email("user@example.com")
        .password("password")
        .enabled(true)
        .build();

    Role role = new Role();
    role.setName("ROLE_USER");

    userInfo = UserInfo.builder()
        .user(user)
        .roles(List.of(role))
        .build();

    // Mock SecurityContextHolder
    SecurityContext securityContext = mock(SecurityContext.class);
    SecurityContextHolder.setContext(securityContext);

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(userInfo, null, userInfo.getAuthorities());
    when(securityContext.getAuthentication()).thenReturn(authentication);
  }

  @Test
  void testCheckout_whenRequestIsValid() throws Exception {
    when(orderService.checkout(any(CheckoutRequest.class))).thenReturn(orderResponse);

    mockMvc.perform(post("/orders/checkout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(checkoutRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.total_amount").value(100.00))
        .andExpect(jsonPath("$.shipping_fee").value(10.00))
        .andExpect(jsonPath("$.payment_url").value("http://payment.url"))
        .andExpect(jsonPath("$.order_id").value(1));

    verify(orderService).checkout(argThat(request -> request.getUserId().equals(1L) &&
        request.getSelectedCartItemIds().equals(checkoutRequest.getSelectedCartItemIds())
        && request.getUserAddressId().equals(checkoutRequest.getUserAddressId())));
  }

  @Test
  void testCheckout_whenRequestIsInvalid() throws Exception {
    CheckoutRequest invalidRequest = new CheckoutRequest();

    mockMvc.perform(post("/orders/checkout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

}
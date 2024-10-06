package com.fastcampus.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fastcampus.ecommerce.common.errors.ResourceNotFoundException;
import com.fastcampus.ecommerce.entity.CartItem;
import com.fastcampus.ecommerce.entity.Order;
import com.fastcampus.ecommerce.entity.Product;
import com.fastcampus.ecommerce.entity.User;
import com.fastcampus.ecommerce.entity.UserAddress;
import com.fastcampus.ecommerce.model.CheckoutRequest;
import com.fastcampus.ecommerce.model.OrderResponse;
import com.fastcampus.ecommerce.model.OrderStatus;
import com.fastcampus.ecommerce.model.PaymentResponse;
import com.fastcampus.ecommerce.model.ShippingRateResponse;
import com.fastcampus.ecommerce.repository.CartItemRepository;
import com.fastcampus.ecommerce.repository.OrderItemRepository;
import com.fastcampus.ecommerce.repository.OrderRepository;
import com.fastcampus.ecommerce.repository.ProductRepository;
import com.fastcampus.ecommerce.repository.UserAddressRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

  @Mock
  private CartItemRepository cartItemRepository;
  @Mock
  private OrderRepository orderRepository;
  @Mock
  private OrderItemRepository orderItemRepository;
  @Mock
  private UserAddressRepository userAddressRepository;
  @Mock
  private ProductRepository productRepository;
  @Mock
  private ShippingService shippingService;
  @Mock
  private PaymentService paymentService;
  @Mock
  private InventoryService inventoryService;

  @InjectMocks
  private OrderServiceImpl orderService;

  private CheckoutRequest checkoutRequest;
  private List<CartItem> cartItems;
  private UserAddress userAddress;
  private Product product;
  private UserAddress sellerAddress;
  private User seller;
  private User buyer;

  @BeforeEach
  void setUp() {
    checkoutRequest = new CheckoutRequest();
    checkoutRequest.setUserId(1L);
    checkoutRequest.setUserAddressId(1L);
    checkoutRequest.setSelectedCartItemIds(Arrays.asList(1L, 2L));

    cartItems = new ArrayList<>();
    CartItem cartItem1 = new CartItem();
    cartItem1.setCartItemId(1L);
    cartItem1.setProductId(1L);
    cartItem1.setQuantity(2);
    cartItem1.setPrice(new BigDecimal("100.00"));
    cartItems.add(cartItem1);

    CartItem cartItem2 = new CartItem();
    cartItem2.setCartItemId(2L);
    cartItem2.setProductId(2L);
    cartItem2.setQuantity(1);
    cartItem2.setPrice(new BigDecimal("50.00"));
    cartItems.add(cartItem2);

    userAddress = new UserAddress();
    userAddress.setUserAddressId(1L);

    seller = new User();
    seller.setUserId(1L);
    buyer = new User();
    buyer.setUserId(2L);

    product = new Product();
    product.setProductId(1L);
    product.setWeight(new BigDecimal("0.5"));
    product.setUserId(seller.getUserId());

    sellerAddress = new UserAddress();
    sellerAddress.setUserAddressId(2L);
    sellerAddress.setUserId(seller.getUserId());
  }

  @Test
  void testCheckout_WhenCartIsEmpty() {
    when(cartItemRepository.findAllById(anyList())).thenReturn(Collections.emptyList());

    assertThrows(ResourceNotFoundException.class, () -> {
      orderService.checkout(checkoutRequest);
    });
  }

  @Test
  void testCheckout_SuccessfulCheckout() {
    // Arrange
    when(cartItemRepository.findAllById(anyList())).thenReturn(cartItems);
    when(userAddressRepository.findById(anyLong())).thenReturn(Optional.of(userAddress));
    when(inventoryService.checkAndLockInventory(anyMap())).thenReturn(true);
    when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);
    when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
    when(userAddressRepository.findByUserIdAndIsDefaultTrue(anyLong())).thenReturn(
        Optional.of(sellerAddress));

    ShippingRateResponse shippingRateResponse = new ShippingRateResponse();
    shippingRateResponse.setShippingFee(new BigDecimal("10.00"));
    when(shippingService.calculateShippingRate(any())).thenReturn(shippingRateResponse);

    PaymentResponse paymentResponse = new PaymentResponse();
    paymentResponse.setXenditInvoiceId("payment123");
    paymentResponse.setXenditInvoiceStatus("PENDING");
    paymentResponse.setXenditPaymentUrl("http://payment.url");
    when(paymentService.create(any())).thenReturn(paymentResponse);

    // Act
    OrderResponse result = orderService.checkout(checkoutRequest);

    // Assert
    assertNotNull(result);
    assertEquals(OrderStatus.PENDING, result.getStatus());
    assertEquals("payment123", result.getXenditInvoiceId());
    assertEquals("http://payment.url", result.getPaymentUrl());

    verify(cartItemRepository).findAllById(checkoutRequest.getSelectedCartItemIds());
    verify(userAddressRepository).findById(checkoutRequest.getUserAddressId());
    verify(inventoryService).checkAndLockInventory(anyMap());
    verify(orderRepository, times(3)).save(any(Order.class));
    verify(orderItemRepository).saveAll(anyList());
    verify(cartItemRepository).deleteAll(cartItems);
    verify(shippingService, times(2)).calculateShippingRate(
        any());  // Verify called twice for two cart items
    verify(paymentService).create(any());
    verify(inventoryService).decreaseQuantity(anyMap());
    verify(userAddressRepository, times(2)).findByUserIdAndIsDefaultTrue(
        anyLong());  // Verify called twice for two products
  }

}

















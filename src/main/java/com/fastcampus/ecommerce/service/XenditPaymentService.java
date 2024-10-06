package com.fastcampus.ecommerce.service;

import com.fastcampus.ecommerce.common.errors.ResourceNotFoundException;
import com.fastcampus.ecommerce.entity.Order;
import com.fastcampus.ecommerce.entity.User;
import com.fastcampus.ecommerce.model.OrderStatus;
import com.fastcampus.ecommerce.model.PaymentNotification;
import com.fastcampus.ecommerce.model.PaymentResponse;
import com.fastcampus.ecommerce.repository.OrderRepository;
import com.fastcampus.ecommerce.repository.UserRepository;
import com.xendit.exception.XenditException;
import com.xendit.model.Invoice;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class XenditPaymentService implements
    PaymentService {

  private final UserRepository userRepository;
  private final OrderRepository orderRepository;
  private final EmailService emailService;

  @Override
  public PaymentResponse create(Order order) {
    User user = userRepository.findById(order.getUserId())
        .orElseThrow(() -> new ResourceNotFoundException("User id for order not found"));

    Map<String, Object> params = new HashMap<>();
    params.put("external_id", order.getOrderId().toString());
    params.put("amount", order.getTotalAmount().doubleValue());
    params.put("payer_email", user.getEmail());
    params.put("description", "Payment for Order #" + order.getOrderId());

    try {
      Invoice invoice = Invoice.create(params);
      return PaymentResponse.builder()
          .xenditPaymentUrl(invoice.getInvoiceUrl())
          .xenditExternalId(invoice.getExternalId())
          .xenditInvoiceId(invoice.getId())
          .amount(order.getTotalAmount())
          .xenditInvoiceStatus(invoice.getStatus())
          .build();
    } catch (XenditException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public PaymentResponse findByPaymentId(String paymentId) {
    try {
      Invoice invoice = Invoice.getById(paymentId);
      return PaymentResponse.builder()
          .xenditPaymentUrl(invoice.getInvoiceUrl())
          .xenditExternalId(invoice.getExternalId())
          .xenditInvoiceId(invoice.getId())
          .amount(BigDecimal.valueOf(invoice.getAmount().doubleValue()))
          .xenditInvoiceStatus(invoice.getStatus())
          .build();
    } catch (XenditException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean verifyByPaymentId(String paymentId) {
    try {
      Invoice invoice = Invoice.getById(paymentId);
      return "PAID".equals(invoice.getStatus());
    } catch (XenditException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void handleNotification(PaymentNotification paymentNotification) {
    String invoiceId = paymentNotification.getId();
    String status = paymentNotification.getStatus();

    Order order = orderRepository.findByXenditInvoiceId(invoiceId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Order not found for xendit with invoice ID: " + invoiceId));

    order.setXenditPaymentStatus(status);
    switch (status) {
      case "PAID":
        order.setStatus(OrderStatus.PAID);
        emailService.notifySuccessfulPayment(order);
        break;
      case "EXPIRED":
        order.setStatus(OrderStatus.CANCELLED);
        emailService.notifyUnsuccessfulPayment(order);
        break;
      case "FAILED":
        order.setStatus(OrderStatus.PAYMENT_FAILED);
        emailService.notifyUnsuccessfulPayment(order);
        break;
      case "PENDING":
        order.setStatus(OrderStatus.PENDING);
        emailService.notifyUnsuccessfulPayment(order);
        break;
      default:
    }

    if (paymentNotification.getPaymentMethod() != null) {
      order.setXenditPaymentMethod(paymentNotification.getPaymentMethod());
    }

    orderRepository.save(order);
  }
}

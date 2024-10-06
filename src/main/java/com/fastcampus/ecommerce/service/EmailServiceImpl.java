package com.fastcampus.ecommerce.service;

import com.fastcampus.ecommerce.common.errors.UserNotFoundException;
import com.fastcampus.ecommerce.config.SendgridConfig;
import com.fastcampus.ecommerce.entity.Order;
import com.fastcampus.ecommerce.entity.User;
import com.fastcampus.ecommerce.model.OrderStatus;
import com.fastcampus.ecommerce.repository.UserRepository;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import io.github.resilience4j.retry.Retry;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

  private final SendgridConfig sendgridConfig;
  private final SendGrid sendGrid;
  private final UserRepository userRepository;
  private final Retry emailRetrier;

  @Async
  @Override
  public void notifySuccessfulPayment(Order order) {
    User user = userRepository.findById(order.getUserId())
        .orElseThrow(() -> new UserNotFoundException(
            "user with id " + order.getUserId() + " is not found"));

    Mail mail = prepareSuccessfulPaymentEmail(user, order);
    sendEmailWithRetry(mail);
  }

  @Async
  @Override
  public void notifyUnsuccessfulPayment(Order order) {
    User user = userRepository.findById(order.getUserId())
        .orElseThrow(() -> new UserNotFoundException(
            "user with id " + order.getUserId() + " is not found"));

    Mail mail = prepareUnsuccessfulPaymentEmail(user, order);
    sendEmailWithRetry(mail);
  }

  private Mail prepareSuccessfulPaymentEmail(User user, Order order) {
    Email from = new Email(sendgridConfig.getFromEmail());
    Email to = new Email(user.getEmail());
    Mail mail = new Mail();
    mail.setFrom(from);
    mail.setReplyTo(from);
    mail.setTemplateId(sendgridConfig.getPaymentSuccessTemplateId());

    Personalization personalization = new Personalization();
    personalization.addTo(to);
    personalization.addDynamicTemplateData("customerName", user.getUsername());
    personalization.addDynamicTemplateData("amount", order.getTotalAmount());
    personalization.addDynamicTemplateData("orderId", order.getOrderId());
    personalization.addDynamicTemplateData("paymentDate",
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));

    mail.addPersonalization(personalization);
    return mail;
  }

  private Mail prepareUnsuccessfulPaymentEmail(User user, Order order) {
    Email from = new Email(sendgridConfig.getFromEmail());
    Email to = new Email(user.getEmail());
    Mail mail = new Mail();
    mail.setFrom(from);
    mail.setReplyTo(from);
    mail.setTemplateId(sendgridConfig.getPaymentErrorTemplateId());

    Personalization personalization = new Personalization();
    personalization.addTo(to);
    personalization.addDynamicTemplateData("customerName", user.getUsername());
    personalization.addDynamicTemplateData("amount", order.getTotalAmount());
    personalization.addDynamicTemplateData("orderId", order.getOrderId());
    personalization.addDynamicTemplateData("paymentDate",
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
    personalization.addDynamicTemplateData("failureReason", order.getStatus());

    mail.addPersonalization(personalization);
    return mail;
  }

  private String failedReasonMessage(OrderStatus status) {
    return switch (status) {
      case CANCELLED -> "Pembayaran telah kedaluwarsa. Silakan lakukan pemesanan ulang.";
      case PAYMENT_FAILED ->
          "Pembayaran gagal diproses. Mohon periksa metode pembayaran Anda dan coba lagi.";
      case PENDING ->
          "Pembayaran masih dalam proses. Mohon tunggu beberapa saat dan periksa kembali status pesanan Anda.";
      default ->
          "Terjadi kesalahan dalam proses pembayaran. Silakan hubungi layanan pelanggan kami untuk bantuan lebih lanjut.";
    };
  }

  private void sendEmail(Mail mail) throws IOException {
    Request request = new Request();
    request.setMethod(Method.POST);
    request.setEndpoint("mail/send");
    request.setBody(mail.build());

    Response response = sendGrid.api(request);
    if (response.getStatusCode() > 299) {
      log.error("Error while sending email. Status code: " + response.getStatusCode());
      throw new IOException("Failed to send email with status code: " + response.getStatusCode());
    }
  }

  private void sendEmailWithRetry(Mail mail) {
    try {
      emailRetrier.executeCallable(() -> {
        sendEmail(mail);
        return null;
      });
    } catch (Exception e) {
      log.error("Error while sending email. error message: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }
}

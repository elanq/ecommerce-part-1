package com.fastcampus.ecommerce.config;

import com.sendgrid.SendGrid;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class SendgridConfig {

  @Value("${email.template.payment-successful.id}")
  private String paymentSuccessTemplateId;

  @Value("${email.template.payment-unsuccessful.id}")
  private String paymentErrorTemplateId;

  @Value("${sendgrid.api-key}")
  private String sendgridApiKey;

  @Value("${sendgrid.from-email}")
  private String fromEmail;

  @Bean
  public SendGrid sendGridConfig() {
    return new SendGrid(sendgridApiKey);
  }

}

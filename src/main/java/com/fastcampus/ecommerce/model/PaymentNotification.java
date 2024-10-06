package com.fastcampus.ecommerce.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonNaming(SnakeCaseStrategy.class)
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentNotification {

  private String id;
  private BigDecimal amount;
  private String status;
  private Instant created;
  @JsonProperty(value = "is_high")
  private boolean isHigh;
  private Instant paidAt;
  private Instant updated;
  private String userId;
  private String currency;
  private String paymentId;
  private String description;
  private String externalId;
  private BigDecimal paidAmount;
  private String payerEmail;
  private String ewalletType;
  private String merchantName;
  private String paymentMethod;
  private String paymentChannel;
  private String paymentMethodId;
}

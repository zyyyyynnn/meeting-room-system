package com.example.mrs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.reservation-rules")
public class ReservationRuleProps {
  private int maxDurationMinutes = 120;
  private int maxAdvanceDays = 7;
  private int maxReservationsPerUserPerDay = 2;
}

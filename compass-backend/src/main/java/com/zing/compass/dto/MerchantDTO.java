package com.zing.compass.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantDTO {
    private String merchantId;
    private String name;
    private String bizId;
    private LocalDateTime yelpingSince;
}


package com.example.gkyproject2.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class BarcodeInput {
    private String date;
    private String endDate;
}

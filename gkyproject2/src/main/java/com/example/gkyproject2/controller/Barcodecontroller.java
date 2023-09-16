package com.example.gkyproject2.controller;
import com.example.gkyproject2.dto.BarcodeInput;
import com.example.gkyproject2.dto.BarcodeInputSingleDate;
import com.example.gkyproject2.repository.Barcoderepository;
import com.example.gkyproject2.service.Barcodeservice;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/barcode")
@RequiredArgsConstructor
public class Barcodecontroller {

    private final Barcodeservice service;
    private final Barcoderepository repository;

    @GetMapping("getByWindow")
    public ResponseEntity<Boolean> getByDateWindow(BarcodeInput dto) {
        boolean result = service.createImagesByThreading(dto, 0);
        return ResponseEntity.ok(result);
    }

    @GetMapping("getSinceJan")
    public ResponseEntity<Boolean> getSinceJan(BarcodeInputSingleDate dto) {
        BarcodeInput temp = BarcodeInput.builder().date(dto.getDate()).build();
        /* HAD TO type cast to BarcodeInput dto object for threading method which is developed for that class */
        boolean result = service.createImagesByThreading(temp, 1);
        return ResponseEntity.ok(true);
    }

    @GetMapping("getByDate")
    public ResponseEntity<Boolean> getByDate(BarcodeInputSingleDate dto) {
        /* MEDIUM THRESHOLD IS 200 ANY INPUT DATE LARGER THAN GETS SEARCHED MINUTE BY MINUTE */
        final int LARGE_THRESHOLD = 201;
        service.createImageByDate(dto.getDate(), LARGE_THRESHOLD);
        return ResponseEntity.ok(true);
    }

}

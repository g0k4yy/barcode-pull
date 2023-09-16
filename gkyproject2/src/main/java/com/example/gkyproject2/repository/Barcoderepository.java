package com.example.gkyproject2.repository;

import com.example.gkyproject2.entitiy.Barcode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface Barcoderepository extends JpaRepository <Barcode,Long> {

    @Query("SELECT COUNT(b.id) FROM Barcode b WHERE b.date = ?1")
    long countByDate(String date);
//    @Query(value = "SELECT id,date,time,image,time_create FROM Barcode WHERE date = :dateInput LIMIT :limitValue OFFSET :offsetValue", nativeQuery = true)
//    List<Barcode> findAllPaged(@Param("dateInput") String dateInput, @Param("limitValue") int limitValue, @Param("offsetValue") int offsetValue);

    @Query("SELECT b FROM Barcode b WHERE b.date = ?1 AND b.time LIKE ?2 ")
    List<Barcode> findAllByDateAndTime(String date , String time);

    List<Barcode> findAllByDate(String date);

    @Query(value = "SELECT id,date,time,image,time_create FROM Barcode WHERE date = :dateInput LIMIT :limitValue OFFSET :offsetValue", nativeQuery = true)
    List<Barcode> findAllPaged(@Param("dateInput") String dateInput, @Param("limitValue") int limitValue, @Param("offsetValue") int offsetValue);


}

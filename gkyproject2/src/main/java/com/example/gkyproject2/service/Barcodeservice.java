package com.example.gkyproject2.service;

import com.example.gkyproject2.dto.BarcodeInput;
import com.example.gkyproject2.entitiy.Barcode;
import com.example.gkyproject2.exception.exceptions.DateParseException;
import com.example.gkyproject2.repository.Barcoderepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class Barcodeservice {
    private final Barcoderepository repository; // service

    @Autowired
    private SessionFactory sessionFactory;
    @PersistenceContext
    private EntityManager entityManager;

    // used obsessively to get rid of memory leak commented commands for suggestion only
    public void clearEmAndSession() {
        entityManager.clear();
        //Session session = sessionFactory.getCurrentSession();
        //session.clear(); // This clears the session cache
        //em.close();
        //session.close();
    }

    /**
     * METHOD TO CONVERT GIVEN TIME INTERVAL TO LIST OF STRINGS TO ITERATE
     * @param startDate Start date of the window given
     * @param endDate End date of the window given
     * @return RETURNS List of strings which is in "yyyyMMdd" format to iterate by other methods
     */
    private List<String> dateWindowtoList(String startDate, String endDate) { // will return dates in window
        clearEmAndSession();

        if (startDate.length() != 8 || endDate.length() != 8) {
            throw new DateParseException("Invalid date format", null);
        }

        List<String> datesInRange = new ArrayList<>();

        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
            Date startDateFormatted = formatter.parse(startDate);
            Date endDateFormatted = formatter.parse(endDate);

            Calendar calendar = new GregorianCalendar();

            calendar.setTime(startDateFormatted);
            while (!calendar.getTime().after(endDateFormatted)) {
                datesInRange.add(formatter.format(calendar.getTime()));
                calendar.add(Calendar.DATE, 1);
            }
            return datesInRange;
        } catch (ParseException E) {
            throw new DateParseException(E.getMessage(), E);
        }
    }

    /**
     *  METHOD TO CREATE IMAGE AND NECESSARY FOLDER TO SAVE IT
     * @param image byte[] of image which is stored in DB
     * @param inputDate given input date to make queries on it
     * @param time_created time_created from barcode object's fields. Used only for naming convention addition to UUID
     */
    private void createAndSaveImage(byte[] image, String inputDate, String time_created) {
        clearEmAndSession();
        String year = inputDate.substring(0, 4);
        String month = inputDate.substring(4, 6);
        String day = inputDate.substring(6, 8);

        String desktopPath = System.getProperty("user.home") + File.separator + "Desktop";
        String folderPath = Paths.get(desktopPath, "images", year, month, day).toString();
        File folder = new File(folderPath);

        if (!folder.exists()) {
            folder.mkdirs();
        }
        String uniqueID = UUID.randomUUID().toString();
        String fileName = inputDate + time_created + "-" + uniqueID + ".jpg"; // You can change the extension if needed
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(new File(folder, fileName));
            fos.write(image);
            fos.flush();
        } catch (IOException e) {
            log.error("Failed to save the image.");
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    log.error("Error occured while closing the FOS --> ", e);
                    throw new RuntimeException(e.getMessage(), e);

                }
            }

        }
    }

    /**
     * MAKE QUERIES FOR SPECIFIC DAYS,HOURS AND MINUTES AND ITERATES TO RECEIVING LIST TO CREATE IMAGES
     * MAKES QUERY FOR WHOLE DAY AT ONCE IF TIME FILTER IS NULL
     * @param inputDate INPUT DATE TO MAKE QUERIES ON THAT SPECIFIC DATE
     * @param timeFilter INPUT TIME FILTER FOR MAKING QUERIES FOR HOURS AND MINUTES
     */
    private void imageSaveDriver(String inputDate, String timeFilter) {
        clearEmAndSession();
        List<Barcode> barcodeList;
        if (timeFilter == null) {
            barcodeList = repository.findAllByDate(inputDate);
        } else {
            barcodeList = repository.findAllByDateAndTime(inputDate, timeFilter);
        }
        if (barcodeList.isEmpty()) {
            clearEmAndSession();
            return;
        }
        for (Barcode barcode : barcodeList) {
            createAndSaveImage(barcode.getImage(), inputDate, barcode.getTime_create());
            clearEmAndSession();
        }
    }

    /**
     *
     * @param inputDate RECEIVES AN DATE TO ITERATE ON
     * @param size RECEIVES AN SIZE TO DECIDE FOR MAKING QUERIES AT ALL ONCE , HOURLY OR MINUTELY
     *             QUERIES DONE BY SUB METHODS
     */
    public void createImageByDate(String inputDate, long size) {
        clearEmAndSession();
        if (size == 0) return;

        final int LOW_THRESHOLD = 15;
        final int MED_THRESHOLD = 200;
        final int SHIFT_START = 6;
        final int SHIFT_END = 22;

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()); //

        if (size > 0 && size < LOW_THRESHOLD) {
            String timeFilter = null;
            imageSaveDriver(inputDate, timeFilter);
        } else if (size > LOW_THRESHOLD && size < MED_THRESHOLD) {
            for (int i = SHIFT_START; i < SHIFT_END; i++) {
                int hour = i;
                executor.execute(() -> {
                    String timeFilter = String.format("%02d%%", hour);
                    String threadName = Thread.currentThread().getName();
                    log.info("THREAD NAME --> {} DATE --> {} TIMEFILTER {}", threadName, inputDate, timeFilter);
                    imageSaveDriver(inputDate, timeFilter);
                    clearEmAndSession();
                });
            }
            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                {
                    log.error(e.getMessage(),e);
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        } else if (size > MED_THRESHOLD) {
            for (int i = SHIFT_START; i < SHIFT_END; i++) {          // hour loop
                for (int j = 0; j < 60; j++) {      // minute loop
                    int hour = i;
                    int minute = j;
                    executor.execute(() -> {
                        String timeFilter = String.format("%02d%02d%%", hour, minute);
                        String threadName = Thread.currentThread().getName();
                        log.info("THREAD NAME --> {} DATE --> {} TIMEFILTER {}", threadName, inputDate, timeFilter);
                        imageSaveDriver(inputDate, timeFilter);
                        clearEmAndSession();
                    });
                }
            }
            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                log.error(e.getMessage(),e);
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    /**
     *
     * @param dto RECEIVES AN BARDOCEINPUT DTO OBJECT FROM REQUEST
     * @param mode MODE = 0 FOR DEFAULT TIME WINDOW USAGE
     *             MODE = 1 FOR MAKING QUERIES SINCE BEGINNING OF THE 2023
     *             MODE IS AN EXTENSION FOR THIS METHOD
     * @return RETURNS TRUE IF NO EXCEPTION IS THROWN EVEN IF NO IMAGE HAS BEEN FOUND IN SPECIFIC TIME INTERVAL
     */
    public boolean createImagesByThreading(BarcodeInput dto, int mode) {
        String startDate = null;
        String endDate = null;
        if (mode == 0) { // MODE == 0 FOR DEFAULT USAGE
            startDate = dto.getDate();
            endDate = dto.getEndDate();
        } else if (mode == 1) { // MODE == 1 FOR CALCULATING DATES FROM BEGINNING OF THE 2023
            startDate = "20230101";
            endDate = dto.getEndDate();
        }

        clearEmAndSession();
        long startTime = System.nanoTime();
        List<String> dateList;
        dateList = dateWindowtoList(startDate, endDate);


        for (String date : dateList) {
            long size = repository.countByDate(date);
            createImageByDate(date, size);
        }

        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        log.info("SINGLE THREADED --> DURATION IS {} , DATE WINDOW IS {} {}  ", duration, dto.getDate(), dto.getEndDate());
        return true;
    }
}

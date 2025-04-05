import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class BruteForceDOB {

    private static final int START_YEAR = 2000;
    private static final int END_YEAR = 2001;
    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final AtomicBoolean found = new AtomicBoolean(false);
    private static final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 1) {
            System.out.println("Usage: java -jar BruteForceDOB.jar <pdf-file>");
            return;
        }

        String filePath = args[0];
        File pdfFile = new File(filePath);

        long startTime = System.currentTimeMillis();

        for (int year = START_YEAR; year <= END_YEAR; year++) {
            final int currentYear = year;
            executor.submit(() -> tryDatesForYear(pdfFile, currentYear));
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        if (!found.get()) {
            System.out.println("*****Password not found in given range.");
        }

        long duration = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("*****Completed in " + duration + " seconds.");
    }

    private static void tryDatesForYear(File pdfFile, int year) {
        LocalDate date = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        while (!found.get() && !date.isAfter(end)) {
            String password = date.format(FORMATTER);

            try (PDDocument doc = PDDocument.load(pdfFile, password)) {
                if (found.compareAndSet(false, true)) {
                    System.out.println("*****Password found: " + password);
                }
                doc.close();
                executor.shutdownNow();
                return;
            } catch (InvalidPasswordException e) {
                // incorrect password
            } catch (IOException e) {
                System.err.println("*****IO Error: " + e.getMessage());
                return;
            }

            date = date.plusDays(1);
        }
    }
}

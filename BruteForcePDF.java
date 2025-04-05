import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class BruteForcePDF {

    private static final int START_YEAR = 1980;
    private static final int END_YEAR = 2003;
    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();

    private static final AtomicBoolean found = new AtomicBoolean(false);
    private static final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 1) {
            System.out.println("Usage: java -jar BruteForcePDF.jar <pdf-file>");
            return;
        }

        String filePath = args[0];
        File pdfFile = new File(filePath);

        long startTime = System.currentTimeMillis();

        for (int year = START_YEAR; year <= END_YEAR; year++) {
            final int currentYear = year;
            executor.submit(() -> bruteForceYear(pdfFile, currentYear));
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        if (!found.get()) {
            System.out.println("***Password not found in given range.");
        }

        long duration = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("***Completed in " + duration + " seconds.");
    }

    private static void bruteForceYear(File pdfFile, int year) {
        for (int num = 0; num <= 999999 && !found.get(); num++) {
            String password = String.format("%06d%d", num, year);

            try (PDDocument document = PDDocument.load(pdfFile, password)) {
                if (found.compareAndSet(false, true)) {
                    System.out.println("***Password found: " + password);
                }
                document.close();
                executor.shutdownNow();
                return;
            } catch (InvalidPasswordException e) {
                // wrong password
            } catch (IOException e) {
                System.err.println("***IO Error: " + e.getMessage());
                return;
            }
        }
    }
}

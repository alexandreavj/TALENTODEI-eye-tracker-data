package tobii;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TobiiDemo {
    static String file_name = "GAZE-DATA-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS")) + ".txt";

    public static void main(String[] args) throws Exception {

        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = defaultToolkit.getScreenSize();
        double screenWidth = screenSize.getWidth();
        double screenHeight = screenSize.getHeight();
        System.out.println("screenWidth = " + screenWidth + ", screenHeight = " + screenHeight);

        createNewFile(file_name);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        long period = 1000000000L / 120; // period in nanoseconds
        scheduler.scheduleAtFixedRate(new Runnable() {
            private long nextTime = System.nanoTime();
            public void run() {
                long currentTime = System.nanoTime();
                while (nextTime < currentTime) {
                    float[] position = Tobii.gazePosition();

                    float xRatio = position[0];
                    float yRatio = position[1];

                    int xPosition = (int) (xRatio * screenWidth);
                    int yPosition = (int) (yRatio * screenHeight);

                    String message = "(" + xPosition + ", " + yPosition + ")";
                    System.out.println(message);

                    writeToFile(file_name, message + " " + System.currentTimeMillis() + "\n");

                    nextTime += period;
                }
            }
        }, 0, period, TimeUnit.NANOSECONDS);
    }


    public static void createNewFile(String filename) {
        try {
            File file = new File(filename);
            if (file.createNewFile()) {
                System.out.println("File created successfully.");
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred while creating the file: " + e.getMessage());
        }
    }

    public static void writeToFile(String filename, String content) {
        try {
            FileWriter fw = new FileWriter(filename, true);
            fw.write(content);
            fw.close();
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file: " + e.getMessage());
        }
    }
}
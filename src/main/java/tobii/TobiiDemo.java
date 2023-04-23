package tobii;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static tobii.TobiiDemo.writeToFile;

class ListeningToSocketThread implements Runnable {
    private final Socket socket;
    private final Thread otherThread;

    public ListeningToSocketThread(Socket socket, Thread otherThread) {
        this.socket = socket;
        this.otherThread = otherThread;
    }
    public void run() {
        String message = null;
        try {
            InputStream input = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int length = input.read(buffer);
            message = new String(buffer, 0, length);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (Objects.equals(message, "STOP")) {
            System.out.println(message);
            otherThread.interrupt();
            Thread.currentThread().interrupt();
        }
    }
}

class WriteGazeDataToFile implements Runnable {
    private final double screenWidth, screenHeight;
    private final String file_name;

    public WriteGazeDataToFile(double screenWidth, double screenHeight, String file_name) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.file_name = file_name;
    }
    public void run() {
        long last = System.currentTimeMillis();
        while (!Thread.currentThread().isInterrupted()) {
            if (System.currentTimeMillis() - last > 4) {
                float[] position = Tobii.gazePosition();

                float xRatio = position[0];
                float yRatio = position[1];

                int xPosition = (int) (xRatio * screenWidth);
                int yPosition = (int) (yRatio * screenHeight);

                String message = "(" + xPosition + ", " + yPosition + ")";
                System.out.println(message);

                writeToFile(file_name, xPosition + "\n" + yPosition + "\n" + System.currentTimeMillis() + "\n");

                last = System.currentTimeMillis();
            }
        }
    }
}

public class TobiiDemo {
    static String file_name_base = "C:\\Users\\Alexandre\\Documents\\SOURCE-CODE\\data\\GAZE-DATA-";
    static int PORT = 1234;

    public static void main(String[] args) throws Exception {

        ServerSocket serverSocket = new ServerSocket(PORT);

        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = defaultToolkit.getScreenSize();
        double screenWidth = screenSize.getWidth();
        double screenHeight = screenSize.getHeight();
        System.out.println("screenWidth = " + screenWidth + ", screenHeight = " + screenHeight);

        while (true) {
            Socket socket = serverSocket.accept();
            String inputFromPython = getMessageFromSocket(socket);
            if (Objects.equals(inputFromPython, "WRITE")) {
                String file_name = file_name_base + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS")) + ".txt";
                createNewFile(file_name);

                WriteGazeDataToFile WriteGazeData = new WriteGazeDataToFile(screenWidth, screenHeight, file_name);
                Thread WriteGazeDataThread = new Thread(WriteGazeData);
                ListeningToSocketThread ListeningToSocket = new ListeningToSocketThread(socket, WriteGazeDataThread);
                Thread ListeningToSocketThread = new Thread(ListeningToSocket);

                ListeningToSocketThread.start();
                WriteGazeDataThread.start();
            }
        }
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

    public static String getMessageFromSocket(Socket socket) {
        String message = null;
        try {
            InputStream input = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int length = input.read(buffer);
            message = new String(buffer, 0, length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return message;
    }
}
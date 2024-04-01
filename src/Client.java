import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);

            while (true) {
                transmitArrays();

                System.out.print("Do you want to transmit arrays again? (yes/no): ");
                String choice = scanner.nextLine().trim().toLowerCase();

                if (!choice.equals("yes")) {
                    break;
                }
            }

            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void transmitArrays() throws IOException {
        Socket socket = new Socket("localhost", 12346); // Connecting to the router on port 12346
        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());

        // Create arrays of different sizes
        int[][] arrays = new int[][]{
                generateRandomArray(10),
                generateRandomArray(100),
                generateRandomArray(200),
                generateRandomArray(300)
        };

        // Send arrays to the router
        for (int[] array : arrays) {
            outputStream.writeObject(array);
            outputStream.flush();
        }

        // Close connection to the router
        outputStream.close();
        socket.close();
    }

    private static int[] generateRandomArray(int size) {
        Random random = new Random();
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = random.nextInt(1000); // Generating random integers between 0 and 999
        }
        return array;
    }
}

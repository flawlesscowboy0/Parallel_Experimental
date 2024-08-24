import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        String serverIP, routerIP;
        int serverSocket, routerSocket;

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Enter the IP address of the server: ");
            serverIP = scanner.nextLine();

            System.out.println("Enter the server socket: ");
            serverSocket = Integer.parseInt(scanner.nextLine());

            System.out.println("Enter the IP address of the router: ");
            routerIP = scanner.nextLine();

            System.out.println("Enter the router socket: ");
            routerSocket = Integer.parseInt(scanner.nextLine());

            while (true) {
                transmitArrays(serverIP, serverSocket, routerIP, routerSocket);

                System.out.print("Do you want to transmit arrays again? (yes/no): ");
                String choice = scanner.nextLine().trim().toLowerCase();

                if (!choice.equals("yes")) {
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format. Please enter valid integers for ports.");
        }
    }

    private static void transmitArrays(String serverIP, int serverSocket, String routerIP, int routerSocket) throws IOException {
        try (Socket socket = new Socket(routerIP, routerSocket);
             OutputStream outputStream = socket.getOutputStream();
             DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {

            // Send the IP address followed by a newline.
            outputStream.write((serverIP + "\n").getBytes(StandardCharsets.UTF_8));

            // Send the socket number.
            dataOutputStream.writeInt(serverSocket);

            // Create and send arrays.
            int[][] arrays = {
                    generateRandomArray(10000),
                    generateRandomArray(200000),
                    generateRandomArray(500000),
                    generateRandomArray(1000000)
            };

            for (int[] array : arrays) {
                objectOutputStream.writeObject(array);
                objectOutputStream.flush(); // Ensure data is sent.
            }

        } // Resources are automatically closed here
    }

    private static int[] generateRandomArray(int size) {
        Random random = new Random();
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = random.nextInt(1000);
        }
        return array;
    }
}

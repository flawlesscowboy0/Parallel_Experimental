import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        String serverIP, routerIP;
        int serverSocket, routerSocket;
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter the IP address of the server: ");
            serverIP = scanner.nextLine();
            System.out.println("Enter the server socket: ");
            serverSocket = scanner.nextInt();
            System.out.println("Enter the IP address of the router: ");
            routerIP = scanner.nextLine();
            System.out.println("Enter the router socket: ");
            routerSocket = scanner.nextInt();

            while (true) {
                transmitArrays(serverIP, serverSocket, routerIP, routerSocket);

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

    private static void transmitArrays(String serverIP, int serverSocket, String routerIP, int routerSocket) throws IOException {
        Socket socket = new Socket(routerIP, routerSocket); // Connecting to the router on port 12346
        OutputStream outputStream = socket.getOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);

        // Send the IP address as a UTF-8 encoded string
        outputStream.write(serverIP.getBytes(StandardCharsets.UTF_8));
        outputStream.flush(); // Ensure the IP address is sent

        // Send the socket
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeInt(serverSocket);
        dataOutputStream.flush(); // Ensure the socket is sent
        dataOutputStream.close(); //Close connection.

        // Create arrays of different sizes
        int[][] arrays = new int[][]{
                generateRandomArray(10000), //Original sizes are 10, 100, 200, 300.
                generateRandomArray(200000),
                generateRandomArray(500000),
                generateRandomArray(1000000)
        };

        // Send arrays to the router
        for (int[] array : arrays) {
            objectOutputStream.writeObject(array);
            objectOutputStream.flush();
        }

        // Close connection to the router
        objectOutputStream.close();
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

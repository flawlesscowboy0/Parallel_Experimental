import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Router {
    private static final int MAX_CLIENTS = 10; // Maximum number of simultaneous clients
    private static final Map<String, Socket> routingTable = new HashMap<>(); // Routing table to store clients by IP address

    public static void main(String[] args) {
        ExecutorService clientThreadPool = Executors.newFixedThreadPool(MAX_CLIENTS);

        // Start a new thread for printing the routing table every 10 seconds
        Thread printThread = new Thread(() -> {
            while (true) {
                printRoutingTable(); // Print the map contents
                try {
                    Thread.sleep(10000); // Sleep for 10 seconds
                } catch (InterruptedException e) {
                    e.printStackTrace(); // Handle interruption
                }
            }
        });

        printThread.start(); // Start the print thread

        try {
            try (ServerSocket serverSocket = new ServerSocket(12346)) {
                System.out.println("Router started, waiting for clients...");

                while (true) {
                    Socket clientSocket = serverSocket.accept(); // Accept incoming client connections
                    String clientAddress = clientSocket.getInetAddress().getHostAddress();
                    int clientPort = clientSocket.getPort();
                    System.out.println("Client connected from: " + clientAddress + " on port " + clientPort);

                    // Add client to the routing table
                    routingTable.put(clientAddress, clientSocket);

                    // Start handling client in a thread from the pool
                    clientThreadPool.execute(new ClientHandler(clientSocket));
                }
            } // Listening on port 12346 for client connections
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            clientThreadPool.shutdown();
        }
    }

    // Method to print the contents of the routing table
    private static void printRoutingTable() {
        System.out.println("Routing Table Contents:");
        System.out.println("========================");

        // Iterate over the map and print each entry
        for (Map.Entry<String, Socket> entry : routingTable.entrySet()) {
            String ipAddress = entry.getKey();
            Socket socket = entry.getValue();
            System.out.println("IP Address: " + ipAddress + ", Socket: " + socket);
        }

        System.out.println("========================\n");
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                InputStream inputStream = clientSocket.getInputStream();
                byte[] ipBuffer = new byte[15]; // Buffer size adjusted for max IP length
                inputStream.read(ipBuffer);
                String destinationIP = new String(ipBuffer, StandardCharsets.UTF_8).trim(); // Read IP address, then socket
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                int destinationSocket = dataInputStream.readInt();
                dataInputStream.close(); //Close connection
                // Connect to the server
                Socket serverSocket = new Socket(destinationIP, destinationSocket);

                // Establish input and output streams for client and server communication
                ObjectOutputStream serverOutput = new ObjectOutputStream(serverSocket.getOutputStream());
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

                // Read arrays from the client and transmit them to the server
                while (true) {
                    try {
                        Object object = objectInputStream.readObject();
                        if (object instanceof int[] array) {
                            serverOutput.writeObject(array);
                            serverOutput.flush();
                        }
                    } catch (EOFException e) {
                        // End of stream reached, terminate the loop
                        break;
                    }
                }

                // Close connections
                objectInputStream.close();
                inputStream.close();
                serverOutput.close();
                clientSocket.close();
                serverSocket.close();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}

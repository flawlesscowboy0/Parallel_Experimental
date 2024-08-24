import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Router {
    private static final int MAX_CLIENTS = 10; // Maximum number of simultaneous clients.
    private static final Map<String, Socket> routingTable = new ConcurrentHashMap<>(); // Routing table to store clients by IP address.

    public static void main(String[] args) {
        ExecutorService clientThreadPool = Executors.newFixedThreadPool(MAX_CLIENTS);

        // Start a new thread for printing the routing table every 10 seconds.
        Thread printThread = new Thread(() -> {
            while (true) {
                printRoutingTable(); // Print the map contents.
                try {
                    Thread.sleep(10000); // Sleep for 10 seconds.
                } catch (InterruptedException e) {
                    e.printStackTrace(); // Handle interruption.
                    Thread.currentThread().interrupt();
                }
            }
        });

        printThread.start(); // Start the print thread.

        try {
            try (ServerSocket serverSocket = new ServerSocket(12346)) {
                System.out.println("Router started, waiting for clients...");

                while (true) {
                    Socket clientSocket = serverSocket.accept(); // Accept incoming client connections.
                    String clientAddress = clientSocket.getInetAddress().getHostAddress();
                    int clientPort = clientSocket.getPort();
                    System.out.println("Client connected from: " + clientAddress + " on port " + clientPort);

                    // Add client to the routing table.
                    routingTable.put(clientAddress, clientSocket);

                    // Start handling client in a thread from the pool.
                    clientThreadPool.execute(new ClientHandler(clientSocket));
                }
            } // Listening on port 12346 for client connections.
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            clientThreadPool.shutdown();
        }
    }

    // Method to print the contents of the routing table.
    private static void printRoutingTable() {
        System.out.println("Routing Table Contents:");
        System.out.println("========================");

        // Iterate over the map and print each entry.
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
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                 DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
                 ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream())) {

                // Read IP address and port.
                String destinationIP = reader.readLine();
                int destinationPort = dataInputStream.readInt();

                // Connect to the server.
                try (Socket serverSocket = new Socket(destinationIP, destinationPort);
                     ObjectOutputStream serverOutput = new ObjectOutputStream(serverSocket.getOutputStream())) {

                    // Read arrays from the client and transmit them to the server.
                    while (true) {
                        try {
                            Object object = objectInputStream.readObject();
                            if (object instanceof int[] array) {
                                serverOutput.writeObject(array);
                                serverOutput.flush();
                            }
                        } catch (EOFException e) {
                            // End of stream reached, terminate the loop.
                            break;
                        }
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

}

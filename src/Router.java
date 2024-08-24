import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Router {
    private static final int MAX_CLIENTS = 10; // Maximum number of simultaneous clients
    private static final Map<String, Socket> routingTable = new HashMap<>(); // Routing table to store clients by IP address

    public static void main(String[] args) {
        ExecutorService clientThreadPool = Executors.newFixedThreadPool(MAX_CLIENTS);

        try {
            ServerSocket serverSocket = new ServerSocket(12346); // Listening on port 12346 for client connections
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
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            clientThreadPool.shutdown();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private Socket serverSocket;

        ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                // Connect to the server
                serverSocket = new Socket("localhost", 12345); // Assuming server is running on localhost and port 12345

                // Establish input and output streams for client and server communication
                ObjectOutputStream serverOutput = new ObjectOutputStream(serverSocket.getOutputStream());
                ObjectInputStream clientInput = new ObjectInputStream(clientSocket.getInputStream());

                // Read arrays from the client and transmit them to the server
                while (true) {
                    try {
                        Object object = clientInput.readObject();
                        if (object instanceof int[]) {
                            int[] array = (int[]) object;
                            serverOutput.writeObject(array);
                            serverOutput.flush();
                        }
                    } catch (EOFException e) {
                        // End of stream reached, terminate the loop
                        break;
                    }
                }

                // Close connections
                clientInput.close();
                serverOutput.close();
                clientSocket.close();
                serverSocket.close();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}

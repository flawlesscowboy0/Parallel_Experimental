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

    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private Socket serverSocket;

        ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                InputStream inputStream = clientSocket.getInputStream();
                byte[] ipBuffer = new byte[15]; // Buffer size adjusted for max IP length
                inputStream.read(ipBuffer);
                String serverIP = new String(ipBuffer, "UTF-8").trim(); // Read IP address, then socket
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                int serverSocket = dataInputStream.readInt();
                dataInputStream.close(); //Close connection
                // Connect to the server
                this.serverSocket = new Socket(serverIP, serverSocket);

                // Establish input and output streams for client and server communication
                ObjectOutputStream serverOutput = new ObjectOutputStream(this.serverSocket.getOutputStream());
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
                this.serverSocket.close();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}

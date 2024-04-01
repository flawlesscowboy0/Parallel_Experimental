import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int[] THREAD_COUNTS = {1, 2, 4, 8, 16};
    private static final int MAX_CLIENTS = 10; // Maximum number of simultaneous clients

    public static void main(String[] args) {
        ExecutorService clientThreadPool = Executors.newFixedThreadPool(MAX_CLIENTS);

        try {
            ServerSocket serverSocket = new ServerSocket(12345); // Listening on port 12345
            System.out.println("Server started, waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept incoming connections
                clientThreadPool.execute(new ClientHandler(clientSocket)); // Handle client in a thread from the pool
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            clientThreadPool.shutdown();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket clientSocket;

        ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());

                // Receive arrays from the client
                for (int i = 0; i < 4; i++) {
                    int[] array = (int[]) inputStream.readObject();
                    System.out.println("Received array of size " + array.length);

                    // Sort the array using different thread counts
                    for (int threadCount : THREAD_COUNTS) {
                        sortArray(array, threadCount);
                    }
                }

                clientSocket.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private void sortArray(int[] array, int threadCount) {
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            int segmentSize = array.length / threadCount;
            int[] sortedSegments = new int[array.length];

            long startTime = System.nanoTime();

            // Create and execute sorting tasks for each segment
            for (int i = 0; i < threadCount; i++) {
                int startIndex = i * segmentSize;
                int endIndex = (i == threadCount - 1) ? array.length : (i + 1) * segmentSize;
                int[] segment = Arrays.copyOfRange(array, startIndex, endIndex);
                executor.execute(new SortingTask(segment, sortedSegments, startIndex));
            }

            executor.shutdown();

            try {
                // Wait for all tasks to complete
                while (!executor.isTerminated()) {
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Perform final sorting on the sorted segments
            Arrays.sort(sortedSegments);

            long endTime = System.nanoTime();

            // Print metrics
            System.out.println("Thread count: " + threadCount);
            System.out.println("Sorting time: " + (endTime - startTime) + " nanoseconds\n");
        }

        // Sorting task for each segment
        static class SortingTask implements Runnable {
            private int[] segment;
            private int[] sortedSegments;
            private int startIndex;

            SortingTask(int[] segment, int[] sortedSegments, int startIndex) {
                this.segment = segment;
                this.sortedSegments = sortedSegments;
                this.startIndex = startIndex;
            }

            @Override
            public void run() {
                Arrays.sort(segment);
                System.arraycopy(segment, 0, sortedSegments, startIndex, segment.length);
            }
        }
    }
}

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {
    private static final int[] THREAD_COUNTS = {1, 2, 4, 8, 16};
    private static final int MAX_CLIENTS = 10; // Maximum number of simultaneous clients.

    public static void main(String[] args) {
        ExecutorService clientThreadPool = Executors.newFixedThreadPool(MAX_CLIENTS);

        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Server started, waiting for clients...");
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientThreadPool.execute(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

                // Receive arrays from the client.
                for (int i = 0; i < 4; i++) {
                    int[] array = (int[]) inputStream.readObject();
                    System.out.println("Received array of size " + array.length);

                    // Sort the array using different thread counts.
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
            int[] sortedArray = Arrays.copyOf(array, array.length); // Create a copy of the array to be sorted.

            long startTime = System.nanoTime();

            // Create and execute sorting tasks for each segment.
            for (int i = 0; i < threadCount; i++) {
                int startIndex = i * segmentSize;
                int endIndex = (i == threadCount - 1) ? array.length : (i + 1) * segmentSize;
                int[] segment = Arrays.copyOfRange(sortedArray, startIndex, endIndex);

                // Sort each segment in its own thread.
                executor.execute(() -> {
                    mergeSort.splitArrays(segment); // Sort the segment.
                    synchronized (sortedArray) {
                        System.arraycopy(segment, 0, sortedArray, startIndex, segment.length); // Merge back into sortedArray.
                    }
                });
            }

            executor.shutdown();

            try {
                // Wait for all tasks to complete.
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow(); // Force shutdown if tasks don't finish in time.
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            // Perform the final merge.
            mergeSort.splitArrays(sortedArray); // Perform merge sort on the entire array.

            long endTime = System.nanoTime();

            // Print metrics.
            System.out.println("Thread count: " + threadCount);
            System.out.println("Sorting time: " + (endTime - startTime) + " nanoseconds\n");
        }


        // Sorting task for each segment.
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
                mergeSort.splitArrays(segment);
                //Arrays.sort(segment);
                System.arraycopy(segment, 0, sortedSegments, startIndex, segment.length);
            }
        }
    }
    static class mergeSort {
        //Take in array and split it (if possible) before merging.
        public static void splitArrays(int[] wholeArray) {
            if (wholeArray.length > 1) {
                int midpoint = wholeArray.length / 2;
                int[] leftSegment = new int[midpoint], rightSegment = new int[wholeArray.length - midpoint];
                System.arraycopy(wholeArray, 0, leftSegment, 0, midpoint);
                System.arraycopy(wholeArray, midpoint, rightSegment, 0, wholeArray.length - midpoint);
                splitArrays(leftSegment);
                splitArrays(rightSegment);
                mergeArrays(leftSegment, rightSegment, wholeArray);
            }
        }
        //Unite the split arrays.
        private static void mergeArrays(int[] arraySegment1, int[] arraySegment2, int[] combinedArray) {
            int i = 0, j = 0, k = 0;
            while (i < arraySegment1.length && j < arraySegment2.length) {
                if (arraySegment1[i] <= arraySegment2[j]) {
                    combinedArray[k] = arraySegment1[i];
                    i++;
                }
                else {
                    combinedArray[k] = arraySegment2[j];
                    j++;
                }
                k++;
            }
            if (i == arraySegment1.length) {
                while (j < arraySegment2.length) {
                    combinedArray[k] = arraySegment2[j];
                    j++;
                    k++;
                }
            }
            else {
                while (i < arraySegment1.length) {
                    combinedArray[k] = arraySegment1[i];
                    i++;
                    k++;
                }
            }
        }
    }
}

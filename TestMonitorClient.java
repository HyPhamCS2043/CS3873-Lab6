import java.io.*;
import java.net.*;
import java.util.*;

class TestMonitorClient {
    static final int MAX_MSG_LEN = 1024;
    static final int REQUEST_TIMEOUT = 1000; // 1 second timeout for replies
    static final int CLEARANCE_PERIOD = 5000; // 5 second clearance period
    static final int NUM_REQUESTS = 40; // Number of echo requests to send
    static final double ALPHA = 0.125; // Weight for EstimatedRTT
    static final double BETA = 0.25; // Weight for DevRTT

    private String serverHost;
    private int serverPort;
    private DatagramSocket clientSocket;
    private InetAddress serverAddress;
    private List<RequestRecord> requestRecords;
    private double estimatedRTT;
    private double devRTT;
    private int updateCount;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java TestMonitorClient <server_host> <server_port>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        TestMonitorClient client = new TestMonitorClient(host, port);
        client.run();
    }

    public TestMonitorClient(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
        this.requestRecords = new ArrayList<>();
        this.updateCount = 0;
    }

    public void run() {
        try {
            // Initialize socket and server address
            clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(REQUEST_TIMEOUT);
            serverAddress = InetAddress.getByName(serverHost);

            // Send all requests first
            sendRequests();

            // Process replies with clearance period
            processReplies();

            // Print results
            printResults();

            // Close socket
            clientSocket.close();
        } catch (SocketException e) {
            System.out.println("Socket error: " + e.getMessage());
        } catch (UnknownHostException e) {
            System.out.println("Unknown host: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO error: " + e.getMessage());
        }
    }

    private void sendRequests() throws IOException {
        for (int i = 0; i < NUM_REQUESTS; i++) {
            // Create and record the request
            String message = "Hello " + i + " ";
            byte[] sendData = message.getBytes();
            long sendTime = System.currentTimeMillis();
            
            // Create and send the packet
            DatagramPacket sendPacket = new DatagramPacket(
                sendData, sendData.length, serverAddress, serverPort);
            clientSocket.send(sendPacket);
            
            // Record the request
            requestRecords.add(new RequestRecord(i, sendTime));
        }
    }

    private void processReplies() throws IOException {
        long lastReplyTime = System.currentTimeMillis();
        boolean active = true;
        
        while (active) {
            try {
                // Receive reply
                byte[] receiveData = new byte[MAX_MSG_LEN];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivePacket);
                
                // Process the received reply
                processReply(receivePacket);
                lastReplyTime = System.currentTimeMillis();
            } catch (SocketTimeoutException e) {
                // Check if we're in the clearance period after all requests have been sent
                if (System.currentTimeMillis() - lastReplyTime >= CLEARANCE_PERIOD) {
                    active = false;
                }
            }
        }
    }

    private void processReply(DatagramPacket receivePacket) {
        long receiveTime = System.currentTimeMillis();
        String reply = new String(receivePacket.getData()).trim();
        
        // Parse the request ID from the reply (e.g., "HELLO 5")
        try {
            String[] parts = reply.split(" ");
            if (parts.length >= 2) {
                int requestId = Integer.parseInt(parts[1]);
                
                // Find the corresponding request record
                for (RequestRecord record : requestRecords) {
                    if (record.requestId == requestId && !record.replied) {
                        // Calculate RTT
                        long sampleRTT = receiveTime - record.sendTime;
                        record.rtt = (int) sampleRTT;
                        record.replied = true;
                        
                        // Update EstimatedRTT and DevRTT
                        updateRTTEstimates(sampleRTT);
                        break;
                    }
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Error parsing reply: " + reply);
        }
    }

    private void updateRTTEstimates(long sampleRTT) {
        if (updateCount == 0) {
            // First sample
            estimatedRTT = sampleRTT;
            devRTT = sampleRTT / 2.0;
        } else {
            // Subsequent samples using TCP algorithm
            double diff = sampleRTT - estimatedRTT;
            estimatedRTT = estimatedRTT + ALPHA * diff;
            devRTT = devRTT + BETA * (Math.abs(diff) - devRTT);
        }
        updateCount++;
    }

    private void printResults() {
        System.out.println("\nTest Results:");
        System.out.println("------------");
        
        for (RequestRecord record : requestRecords) {
            if (record.replied) {
                System.out.printf("Request %d: RTT = %d%n", record.requestId, record.rtt);
            } else {
                System.out.printf("Request %d: no reply%n", record.requestId);
            }
        }
        
        System.out.println("\nFinal Statistics:");
        System.out.println("----------------");
        System.out.printf("Number of requests sent: %d%n", NUM_REQUESTS);
        System.out.printf("Number of replies received: %d%n", updateCount);
        if (updateCount > 0) {
            System.out.printf("Final EstimatedRTT: %.2f ms%n", estimatedRTT);
            System.out.printf("Final DevRTT: %.2f ms%n", devRTT);
        }
    }

    // Inner class to track request information
    private static class RequestRecord {
        int requestId;
        long sendTime;
        int rtt;
        boolean replied;
        
        RequestRecord(int requestId, long sendTime) {
            this.requestId = requestId;
            this.sendTime = sendTime;
            this.rtt = -1;
            this.replied = false;
        }
    }
}

/*
 * A UDP Client that sends 40 echo requests to the UDP Server
 * created by MonitorServer.java. After the last request, the client
 * print out the RTT time for each successful request-response pair.
 * 
 * Author: Hy Pham
 * Date Created: March 28, 2025
 * Created based on the UDPClient.java file obtained from CS3873 Lab 4.
 */

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class MonitorClient {
	static final int REQUEST_NUM = 40;
	static final int REQUEST_TIMEOUT = 1000;
    static final int CLEARANCE_PERIOD = 5000;
    static final double ALPHA = 0.125; // Weight for EstimatedRTT
    static final double BETA = 0.25; // Weight for DevRTT
	

    private static int port;
    private static DatagramSocket clientSocket;
    private static InetAddress IPAddress;
	private static double estimatedRTT;
    private static double devRTT;
    private static int numOfUpdates;

	private static ArrayList<RequestInfo> requestList;

	// A class to keep track of information of each request
    private static class RequestInfo {
        int requestId;
        long sentTime;
        int rtt;
        boolean replied;
        
		//Create a request-response info pack using 
		//request ID number and the time it was sent
        RequestInfo(int requestId, long sentTime) {
            this.requestId = requestId;
            this.sentTime = sentTime;
            this.rtt = -1; 
            this.replied = false;
        }
    }
    public static void main(String args[]) throws Exception {

		//java class for UDP socket
		clientSocket = new DatagramSocket();

		//Obtaining the server IP address
		IPAddress = InetAddress.getByName(args[0]);

		port = Integer.parseInt(args[1]);

		System.out.println("Attemping to connect to " + IPAddress
				+ " via UDP port " + port);

		//ArrayList to keep track of each request-response pair
		requestList = new ArrayList<RequestInfo>();

		System.out.print("SENDING 40 ECHO REQUESTS");

		clientSocket.setSoTimeout(REQUEST_TIMEOUT);

		try {
			sendingRequests();

			parsingResponse();
		} catch (IOException e) {
			System.out.println("IO Exception: " + e);
		}
		

		//Printing out the result
		System.out.println("\nTEST OUTPUT:");
        System.out.println("---------------");
        
        for (RequestInfo echo : requestList) {
            if (echo.replied) {
                System.out.println("Request " + echo.requestId + ": RTT = " + echo.rtt);
            } else {
                System.out.println("Request "+ echo.requestId + ": no reply");
            }
        }
        
        System.out.println("\nEND OF TEST:");
        System.out.println("-------------------");
        System.out.println("Number of requests sent: " + REQUEST_NUM);
        System.out.println("Number of replies received: " + numOfUpdates);

		//If there is at least one successful reply
        if (numOfUpdates > 0) {
            System.out.printf("Final EstimatedRTT: %.2f ms%n", estimatedRTT);
            System.out.printf("Final DevRTT: %.2f ms%n", devRTT);
        }
		
		clientSocket.close();
	}

	private static void sendingRequests() throws IOException {
		for(int i = 0; i <  REQUEST_NUM; i++) {	
			String sentence = "Hello " + i + " ";
			byte[] sendData = sentence.getBytes();

			DatagramPacket sendPacket = new DatagramPacket(sendData,
			sendData.length, IPAddress, port);

			// Measuring sent time of request i
			long sentTime = System.currentTimeMillis();

			clientSocket.send(sendPacket);
			
			 // Adding the request and its sent time into the list
			requestList.add(new RequestInfo(i, sentTime));
		}
	}

	private static void parsingResponse() throws IOException{
		byte[] receiveData = new byte[1024];

		//Variable to hold the time of the current last response
		long lastResponseTime = System.currentTimeMillis();

		//Boolean to check if there is more response from server
        boolean stillMoreResponse = true;
        
        while (stillMoreResponse) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivePacket);
                
				//Measure time of receiving response from server
				long responseTime = System.currentTimeMillis();

				String reply = new String(receivePacket.getData()).trim();
				
				// Parse the request ID from the reply (e.g., "HELLO 5")
				try {
					String[] parts = reply.split(" ");
					if (parts.length >= 2) {
						int requestId = Integer.parseInt(parts[1]);
						
						// Look through the arrayList for the corresponding requests
						for (RequestInfo echo : requestList) {
							if (echo.requestId == requestId && !echo.replied) {

								// Calculate RTT of successful request-response echo.
								long sampleRTT = responseTime - echo.sentTime;
								echo.rtt = (int) sampleRTT;
								echo.replied = true;

								//Check if this is the first time estimatedRTT and devRTT has been updated or not
								if (numOfUpdates == 0) {
									estimatedRTT = sampleRTT;
									devRTT = sampleRTT / 2;
								} else {

									double difference = sampleRTT - estimatedRTT;
									estimatedRTT = estimatedRTT + ALPHA * difference;
									devRTT = devRTT + BETA * (Math.abs(difference) - devRTT);
								}
								numOfUpdates++;

								break;
							}
						}
					}
				} catch (NumberFormatException e) {
					System.out.println("Error parsing reply: " + reply);
				}

                lastResponseTime = System.currentTimeMillis();

            } catch (SocketTimeoutException e) {
				//Once the final echo request is sent and after the 1 second wait period,
				//wait 5 more seconds to see if there is any more response.
                if (System.currentTimeMillis() - lastResponseTime >= CLEARANCE_PERIOD) {
                    stillMoreResponse = false;
                }
            }
        }
	}
}

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

public class MonitorClient {
	static int REQUEST_NUM = 40;
    public static void main(String args[]) throws Exception {

		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(
				System.in));

		//java class for UDP socket
		DatagramSocket clientSocket = new DatagramSocket();

		//Obtaining the server IP address
		InetAddress IPAddress = InetAddress.getByName(args[0]);

		int port = Integer.parseInt(args[1]);

		System.out.println("Attemping to connect to " + IPAddress
				+ " via UDP port " + port);

		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];

		//2D Array to keep track of RTT of each request-response pair
		//Each row corresponds to a request.
		long[][] matchedRTTArray = new long[40][2];

		System.out.print("SENDING 40 ECHO REQUESTS");

		for(int i = 0; i <  REQUEST_NUM; i++) {
			
			String sentence = "Hello " + i;
			sendData = sentence.getBytes();

			String responseMessage = "";

			DatagramPacket sendPacket = new DatagramPacket(sendData,
			sendData.length, IPAddress, port);

			System.out.println("Sending data of " + sendData.length
					+ " bytes to server.");

			//the send and receive can be a loop for multiple requests (lab 6)
			clientSocket.send(sendPacket);

			// Begin measuring RTT
			long startTime = System.nanoTime();

			//Placing the initial time of request i into corresponding 
			//cell in the matchedRTTArray.
			matchedRTTArray[i][0] = startTime;

			System.out.println("Done. Waiting for return packet");

			//must also create a receive packet to save server response
			DatagramPacket receivePacket = new DatagramPacket(receiveData,
					receiveData.length);

			clientSocket.setSoTimeout(1000);
			try {
				//This receive is a blocking method
				//If no datagram arrives, the program holds here.
				
				clientSocket.receive(receivePacket);

				long endTime = System.nanoTime();

				responseMessage = new String(receivePacket.getData());

				//Decode the String response to identify its corresponding request
				int requestID = Character.getNumericValue(responseMessage.charAt(6));

				//Calculating the RTT and places it
				//into the corresponding row based on request number.
				matchedRTTArray[requestID][1] = endTime - matchedRTTArray[requestID][1];


			} catch (SocketTimeoutException e) {
				responseMessage = "No reply";
				
				//Place 0 to indicate that there was no reply 
				// to request i
				matchedRTTArray[i][1] = 0;

				System.out.println("Time out after 1 second");
			}	


			System.out.println(responseMessage);

		}
		

		//InetAddress returnIPAddress = receivePacket.getAddress();
		
		clientSocket.close();
	}
}

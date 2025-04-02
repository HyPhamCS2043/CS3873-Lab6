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

		System.out.print("SENDING 40 ECHO REQUESTS");

		
		String sentence = inFromUser.readLine();
		sendData = sentence.getBytes();

		//In UDP, you must contrusct the datagram explicitly, with 
		//DatagramPacket below.
		DatagramPacket sendPacket = new DatagramPacket(sendData,
				sendData.length, IPAddress, 2424);

		System.out.println("Sending data of " + sendData.length
				+ " bytes to server.");

		//the send and receive can be a loop for multiple requests (lab 6)
		clientSocket.send(sendPacket);

		System.out.println("Done. Waiting for return packet");

		//must also create a receive packet to save server response
		DatagramPacket receivePacket = new DatagramPacket(receiveData,
				receiveData.length);

		//This receive is a blocking method
		//If no datagram arrives, the program holds here.
		clientSocket.receive(receivePacket);

		InetAddress returnIPAddress = receivePacket.getAddress();
		//int port = receivePacket.getPort();
		//System.out.println("From server at: " + returnIPAddress + ":" + port);

		//String modifiedSentence = new String(receivePacket.getData());

		//System.out.println("RETURNED MESSAGE FROM SERVER: " + modifiedSentence);
		clientSocket.close();
	}
}

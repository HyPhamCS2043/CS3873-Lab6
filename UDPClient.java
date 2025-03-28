/* Example from "Computer Networking: A Top-Down Approach" by
 * James Kurose and Keith Ross, 5th Edition */

import java.io.*;
import java.net.*;

class UDPClient {
	public static void main(String args[]) throws Exception {

		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(
				System.in));

		//java class for UDP socket
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress IPAddress = InetAddress.getByName(args[0]);

		System.out.println("Attemping to connect to " + IPAddress
				+ " via UDP port 9876");

		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];

		System.out.print("Enter message: ");
		String sentence = inFromUser.readLine();
		sendData = sentence.getBytes();

		//In UDP, you must contrusct the datagram explicitly, with 
		//DatagramPacket below.
		DatagramPacket sendPacket = new DatagramPacket(sendData,
				sendData.length, IPAddress, 9876);

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
		int port = receivePacket.getPort();
		System.out.println("From server at: " + returnIPAddress + ":" + port);

		String modifiedSentence = new String(receivePacket.getData());

		System.out.println("RETURNED MESSAGE FROM SERVER: " + modifiedSentence);
		clientSocket.close();
	}
}

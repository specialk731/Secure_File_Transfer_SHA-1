/*
 * 
 */

import java.io.*;
import java.net.*;

public class Server  extends Thread {
	
	public static void main(String[] args) {
		ServerSocket serversocket;
		final int myPort = 14014;
		Server_Thread svr;
		
		try {
			serversocket = new ServerSocket(myPort);
			
			//Start while(true) loop here for unlimited clients
			System.out.println("Waiting for client..."); //DEBUG ONLY
			Socket s = serversocket.accept();
			svr = new Server_Thread(s);
			svr.start();
			System.out.println("Got a client"); //DEBUG ONLY
			//End while(true) loop
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}

class Server_Thread extends Thread {
	Socket sock = null;
	ObjectInputStream ois;
	ObjectOutputStream oos;
	byte[] key = {	Byte.parseByte("11001101"), // 1st 8 bits of key
			Byte.parseByte("01111001"), // 2nd 8 bits of key
			Byte.parseByte("00001010"), // 3rd
			Byte.parseByte("01000100"), // 4th
			Byte.parseByte("10001110"), // 5th
			Byte.parseByte("10001111"), // 6th
			Byte.parseByte("11110010"), // 7th
			Byte.parseByte("01101101"), // 8th
			Byte.parseByte("01010010"), // 9th
			Byte.parseByte("00001011"), // 10th
			Byte.parseByte("11110011"), // 11th
			Byte.parseByte("00111111"), // 12th
			Byte.parseByte("11001111"), // 13th
			Byte.parseByte("01000001"), // 14th
			Byte.parseByte("01111000"), // 15th
			Byte.parseByte("10001010"), // 16th
			};
	byte[] tmp;

	public Server_Thread(Socket s) {
		sock = s;
	}
	
	public void run() {
		try {
			oos = new ObjectOutputStream(sock.getOutputStream());
			ois = new ObjectInputStream(sock.getInputStream());
			
			tmp = (byte[]) ois.readObject();
			
			String tmpString = "";
			
			for(int i = 0; i < tmp.length; i++) {
				tmpString = tmpString + ((char) (tmp[i] ^ key[i % 16]));
			}
			
			System.out.println("Got String : " + tmpString);
			
			oos.close();
			ois.close();
			sock.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}

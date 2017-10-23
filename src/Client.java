/*
 * 
 */

import java.io.*;
import java.net.*;

public class Client {

	public static void main(String[] args) { // args are: IP_of_Server Port String
		if(args.length != 3)
			return;
		Socket sock;
		ObjectInputStream ois;
		ObjectOutputStream oos;
		byte[] key = {	(byte) Integer.parseInt("11001101", 2), // 1st 8 bits of key
						(byte) Integer.parseInt("01111001", 2), // 2nd 8 bits of key
						(byte) Integer.parseInt("00001010", 2), // 3rd
						(byte) Integer.parseInt("01000100", 2), // 4th
						(byte) Integer.parseInt("10001110", 2), // 5th
						(byte) Integer.parseInt("10001111", 2), // 6th
						(byte) Integer.parseInt("11110010", 2), // 7th
						(byte) Integer.parseInt("01101101", 2), // 8th
						(byte) Integer.parseInt("01010010", 2), // 9th
						(byte) Integer.parseInt("00001011", 2), // 10th
						(byte) Integer.parseInt("11110011", 2), // 11th
						(byte) Integer.parseInt("00111111", 2), // 12th
						(byte) Integer.parseInt("11001111", 2), // 13th
						(byte) Integer.parseInt("01000001", 2), // 14th
						(byte) Integer.parseInt("01111000", 2), // 15th
						(byte) Integer.parseInt("10001010", 2), // 16th
						};
		byte[] tmp;
		
		try {
			sock = new Socket(args[0], Integer.parseInt(args[1]));
			ois = new ObjectInputStream(sock.getInputStream());
			oos = new ObjectOutputStream(sock.getOutputStream());
			
			tmp = new byte[args[2].length()];
			
			for(int i = 0; i < args[2].length(); i++) {
				tmp[i] = (byte) (args[2].charAt(i) ^ key[i % 16]);
			}
			
			oos.writeObject(tmp);
			oos.flush();
			
			System.out.println("Wrote " + args[2]);
			
			oos.close();
			ois.close();
			sock.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}

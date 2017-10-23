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

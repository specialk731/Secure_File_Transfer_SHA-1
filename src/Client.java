/*
 * 
 */

import java.io.*;
import java.net.*;
import java.nio.file.Files;

public class Client {

	public static void main(String[] args) { // args are: ARGS  IP_of_Server  Port  FileName
											 // ARGS are W for windows or L for Linux, S for sending or R for receiving
		
		if(args.length != 4) {
			System.out.println("Missing Arguments");
			return;
		}
		
		Boolean windows = true, sending = true;
		if(args[0].contains("l") || args[0].contains("L"))
			windows = false;
		if(args[0].contains("r") || args[0].contains("R"))
			sending = false;
		
		Socket sock;
		ObjectInputStream ois;
		ObjectOutputStream oos;
		File f;
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
		byte[] tmp, fileBytes;
		
		try {
			sock = new Socket(args[1], Integer.parseInt(args[2]));
			ois = new ObjectInputStream(sock.getInputStream());
			oos = new ObjectOutputStream(sock.getOutputStream());
			
			if(sending) { 
				oos.writeByte(1 ^ key[0]); 							//Alert the Server we are sending by sending a 1 XOR with key[0]
				f = new File(args[3]);								//Get the file
				oos.writeUTF(f.getName());							//Send file name
				fileBytes = Files.readAllBytes(f.toPath());			//Convert to Bytes
				tmp = new byte[fileBytes.length];					//Init the array to send
				for(int i = 0; i < fileBytes.length; i++) {			//for each byte in the file
					tmp[i] = (byte) (fileBytes[i] ^ key[i % 16]);	//file byte XOR with key goes into tmp
				}
				oos.writeObject(tmp);								//Send tmp to server
				oos.flush();
			} else {
				System.out.println("havent done R yet");
			}
			
			/*
			tmp = new byte[args[2].length()];
			
			for(int i = 0; i < args[2].length(); i++) {
				tmp[i] = (byte) (args[2].charAt(i) ^ key[i % 16]);
			}
			
			oos.writeObject(tmp);
			oos.flush();
			
			System.out.println("Wrote " + args[2]);
			*/
			
			oos.close();
			ois.close();
			sock.close();
			
			System.out.println("Done");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}

/*
 * 
 */

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;

public class Server extends Thread {

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
	File f;
	byte[] key = {(byte) Integer.parseInt("11001101", 2), // 1st 8 bits of key
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
	};  //testing key only. replace with one way authenticated Diffie-Hellman generated key
	byte[] tmp, EncryptedFileNameBytes, HashedEncryptedFileNameBytes, EncryptedFileBytes, HashedEncryptedFileBytes, certificate, FileName, FileBytes, digested;
	MessageDigest md = null, tmpmd = null;

	public Server_Thread(Socket s) {
		sock = s;
	}

	public void run() {
		try {
			oos = new ObjectOutputStream(sock.getOutputStream());
			ois = new ObjectInputStream(sock.getInputStream());

			f = new File("CA-certificate.crt");
			certificate = Files.readAllBytes(f.toPath());
			oos.writeObject(certificate);                                       //send certificate

			//do one-way authenticated Diffie-Hellman
			if (ois.readByte() == 1) {	//Client wants to send us a file

				//tmp = new byte[FileNameSize];
				EncryptedFileNameBytes = (byte[])ois.readObject();                  //Get the encrypted filename bytes


				md = MessageDigest.getInstance("SHA-1");                        //check filename digest

				HashedEncryptedFileNameBytes = (byte[]) ois.readObject();		//Get Hashed Encrypted FileName Bytes
				digested = md.digest(EncryptedFileNameBytes);
				//System.out.println("EncryptedFileNameBytes: " + Arrays.toString(EncryptedFileNameBytes) + " Length: " + EncryptedFileNameBytes.length);
				//System.out.println("HashedEncryptedFileNameBytes: " + Arrays.toString(HashedEncryptedFileNameBytes) + " Length: " + HashedEncryptedFileNameBytes.length);
				//System.out.println("Digest: " + Arrays.toString(digested) + " Length: " + digested.length);
				if(!Arrays.equals(HashedEncryptedFileNameBytes, digested)) {
					System.out.print("Filename Altered to ");
					FileName = new byte[EncryptedFileNameBytes.length];
					for (int i = 0; i < EncryptedFileNameBytes.length; i++) {
						FileName[i] = (byte) (EncryptedFileNameBytes[i] ^ key[i % 16]);                //unencrypt filename bytes
					}
					System.out.println(new String(FileName,"UTF-8"));
					return;
				}

				FileName = new byte[EncryptedFileNameBytes.length];
				for (int i = 0; i < EncryptedFileNameBytes.length; i++) {
					FileName[i] = (byte) (EncryptedFileNameBytes[i] ^ key[i % 16]);                //unencrypt filename bytes
				}

				String fileName = new String(FileName, "UTF-8");                //convert bytes back to UTF-8

				System.out.println("Got FileName: " + fileName + " intact");

				f = new File(fileName);
				if (f.exists()) //Delete it, if it exists
				{
					f.delete();
				}

				EncryptedFileBytes = (byte[]) ois.readObject();                                //Get the files Encrypted bytes

				HashedEncryptedFileBytes = (byte[]) ois.readObject();
				md = MessageDigest.getInstance("SHA-1");                        //check file digest
				if(!Arrays.equals( HashedEncryptedFileBytes, md.digest(EncryptedFileBytes))) {
					System.out.println("File Altered");
					return;
				}

				System.out.println("Got File Intact");

				FileBytes = new byte[EncryptedFileBytes.length];

				for (int i = 0; i < EncryptedFileBytes.length; i++) {
					FileBytes[i] = (byte) (EncryptedFileBytes[i] ^ key[i % 16]);               //decrypt file bytes
				}

				Files.write(f.toPath(), FileBytes);                             //save file

			} else {	//Client wants a file
				tmp = (byte[]) ois.readObject();                                //Get the Encrypted filename bytes

				md = MessageDigest.getInstance("SHA-1");                        //check filename digest
				if(ois.readObject() != md.digest(tmp))
				{
					System.out.println("Filename Altered");
					return;
				}

				FileName = new byte[tmp.length];
				for (int i = 0; i < tmp.length; i++) {
					FileName[i] = (byte) (tmp[i] ^ key[i % 16]);                //decrypt filename bytes
				}
				String fileName = new String(FileName, "UTF-8");                //convert bytes back to UTF-8

				f = new File(fileName);
				FileBytes = Files.readAllBytes(f.toPath());
				tmp = new byte[FileBytes.length];
				for (int i = 0; i < FileBytes.length; i++) {
					tmp[i] = (byte) (FileBytes[i] ^ key[i % 16]);               //encrypt file bytes
				}
				oos.writeObject(tmp);                                           //send file
				oos.flush();

				md = MessageDigest.getInstance("SHA-1");                        //send file digest
				oos.writeObject(md.digest(tmp));
				oos.flush();

			}
            /*
			tmp = (byte[]) ois.readObject();

			String tmpString = "";

			for(int i = 0; i < tmp.length; i++) {
				tmpString = tmpString + ((char) (tmp[i] ^ key[i % 16]));
			}

			System.out.println("Got String : " + tmpString);
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

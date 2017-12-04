/*
 * 
 */

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import sun.misc.BASE64Decoder;

public class Client {

	public static void main(String[] args) { // args are: ARGS  IP_of_Server  Port  FileName
		// ARGS are W for windows or L for Linux, S for sending or R for receiving

		if (args.length != 4) {
			System.out.println("Missing Arguments");
			return;
		}

		Boolean windows = true, sending = true;
		if (args[0].contains("l") || args[0].contains("L")) {
			windows = false;
		}
		if (args[0].contains("r") || args[0].contains("R")) {
			sending = false;
		}

		Socket sock;
		ObjectInputStream ois;
		ObjectOutputStream oos;
		String ServerIP = args[1], ServerPort = args[2], FileName = args[3];
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
		};     //testing key only. replace with one way authenticated Diffie-Hellman generated key

		byte[] tmp, certificate, fileBytes;
		MessageDigest md = null;

		KeyFactory kf = null;
		File keyFile = null;
		byte[] data;
		PublicKey certPubKey = null;

		CertificateFactory cf = null;
		File certificateFile = null;
		InputStream is = null;
		Certificate generatedCertificate = null;

		PublicKey servPubKey = null;

		try {
			sock = new Socket(ServerIP, Integer.parseInt(ServerPort));
			ois = new ObjectInputStream(sock.getInputStream());
			oos = new ObjectOutputStream(sock.getOutputStream());

			cf = CertificateFactory.getInstance("X.509");                       //certificate will be in X.509 format

			certificate = (byte[]) ois.readObject();				//Wait for the Server to give us the certificate file bytes
			f = new File("CA-certificate.crt");
			if (f.exists()) {
				f.delete();
			}
			Files.write(f.toPath(), certificate);

			certificateFile = new File("CA-certificate.crt");
			is = new FileInputStream(certificateFile);
			generatedCertificate = cf.generateCertificate(is);                  //might be able to skip saving certificate as a file by generating straight from ois.

			kf = KeyFactory.getInstance("RSA");

			keyFile = new File("ashkan-public.key");
			data = Files.readAllBytes(keyFile.toPath());

			String temp = new String(data);
			String publicKeyPEM = temp.replace("-----BEGIN PUBLIC KEY-----\n", "");
			publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");

			BASE64Decoder b64 = new BASE64Decoder();
			byte[] decoded = b64.decodeBuffer(publicKeyPEM);

			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
			certPubKey = kf.generatePublic(keySpec);

			generatedCertificate.verify(certPubKey);

			servPubKey = generatedCertificate.getPublicKey();                   //Now have server's public key for use in one-way authenticated Diffie-Hellman

			//do one-way authenticated Diffie-Hellman

			if (sending) {
				System.out.println("Sending File: " + FileName);

				f = new File(FileName);                                         //Get the file

				oos.writeByte((byte) 1);                                        //Alert the Server we are sending, by sending a 1 first

				byte[] fileName = FileName.getBytes("UTF-8");                   //Convert FileName to byte[]
				byte[] sendingFileName = new byte[fileName.length];
				for (int i = 0; i < fileName.length; i++) {			//for each byte in the filename
					sendingFileName[i] = (byte) (fileName[i] ^ key[i % 16]);	//filename byte XOR with key goes into sendingFileName
				}
				oos.writeObject(sendingFileName);                               //Send file name to the server
				oos.flush();

				md = MessageDigest.getInstance("SHA-1");                        //send filename digest
				//System.out.println("sendFileName " + Arrays.toString(sendingFileName) + " Length: " + sendingFileName.length);
				//System.out.println("Digest " + Arrays.toString(md.digest(sendingFileName)) + " - Length: " + md.digest(sendingFileName).length);
				md.reset();
				oos.writeObject(md.digest(sendingFileName));
				oos.flush();

				fileBytes = Files.readAllBytes(f.toPath());			//Convert file contents to Bytes
				tmp = new byte[fileBytes.length];				//Init the array to send
				for (int i = 0; i < fileBytes.length; i++) {			//for each byte in the file
					tmp[i] = (byte) (fileBytes[i] ^ key[i % 16]);               //file byte XOR with key goes into tmp
				}
				oos.writeObject(tmp);                                           //Send the file
				oos.flush();

				md = MessageDigest.getInstance("SHA-1");                        //send file digest
				oos.writeObject(md.digest(tmp));
				oos.flush();
			} else {
				oos.writeByte((byte) 0);                                        //Alert the Server we want a file by sending a 0 first

				byte[] fileName = FileName.getBytes("UTF-8");                   //Convert FileName to byte[]
				byte[] sendingFileName = new byte[fileName.length];

				for (int i = 0; i < fileName.length; i++) {			//for each byte in the filename
					sendingFileName[i] = (byte) (fileName[i] ^ key[i % 16]);	//filename byte XOR with key goes into sendingFileName
				}
				oos.writeObject(sendingFileName);                               //Send file name to the server
				oos.flush();

				md = MessageDigest.getInstance("SHA-1");                        //send filename digest
				oos.writeObject(md.digest(sendingFileName));
				oos.flush();


				tmp = (byte[]) ois.readObject();				//Wait for the Server to give us the file bytes

				md = MessageDigest.getInstance("SHA-1");                        //check file digest
				if(ois.readObject() != md.digest(tmp))
				{
					System.out.println("File Altered");
					return;
				}

				fileBytes = new byte[tmp.length];
				for (int i = 0; i < tmp.length; i++) {
					fileBytes[i] = (byte) (tmp[i] ^ key[i % 16]);               //decrypt bytes
				}
				f = new File(FileName);
				if (f.exists()) {
					f.delete();
				}
				Files.write(f.toPath(), fileBytes);                             //save file

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

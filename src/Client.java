/*
 * 
 */

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import sun.misc.BASE64Decoder;

import javax.crypto.Cipher;

public class Client {

	public static void main(String[] args) { // args are: ARGS  IP_of_Server  Port  FileName
		// ARGS are W for windows or L for Linux, S for sending or R for receiving

		if (args.length != 4) {
			System.out.println("Missing Arguments");
			return;
		}

		Boolean windows = false, sending = true;
		if (args[0].contains("w") || args[0].contains("W")) { 	//Assuming we are on a Linux environment. If not then change file paths
			windows = true;
		}

		if (args[0].contains("r") || args[0].contains("R")) {	//Assume we are sending a file. If ARGS contains an r or R we are Receiving
			sending = false;
		}

		Socket sock;
		ObjectInputStream ois;
		ObjectOutputStream oos;
		String ServerIP = args[1], ServerPort = args[2], FileName = args[3];
		File f;
		byte[] certificate, fileBytes;
		MessageDigest md;

		KeyFactory kf;
		File keyFile;
		byte[] data;
		PublicKey certPubKey;

		CertificateFactory cf;
		File certificateFile;
		InputStream is;
		Certificate generatedCertificate;

		PublicKey servPubKey = null;

		try {
			sock = new Socket(ServerIP, Integer.parseInt(ServerPort));
			ois = new ObjectInputStream(sock.getInputStream());
			oos = new ObjectOutputStream(sock.getOutputStream());

			cf = CertificateFactory.getInstance("X.509");                       		//certificate will be in X.509 format

			certificate = (byte[]) ois.readObject();									//Wait for the Server to give us the certificate file bytes
			f = new File("server-certificate.crt");
			if (f.exists()) {
				f.delete();
			}
			Files.write(f.toPath(), certificate);										//Save the cert from the server

			System.out.println("Got server-certificate.crt from the Server");

			certificateFile = new File("server-certificate.crt");
			is = new FileInputStream(certificateFile);
			generatedCertificate = cf.generateCertificate(is);                  		//Generates server pub key from server cert//might be able to skip saving certificate as a file by generating straight from ois.

			kf = KeyFactory.getInstance("RSA");

			keyFile = new File("ashkan-public.key");							//Get CA public key from file
			data = Files.readAllBytes(keyFile.toPath());

			String temp = new String(data);
			String publicKeyPEM = temp.replace("-----BEGIN PUBLIC KEY-----\n", "");
			publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");

			BASE64Decoder b64 = new BASE64Decoder();
			byte[] decoded = b64.decodeBuffer(publicKeyPEM);

			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
			certPubKey = kf.generatePublic(keySpec);									//Use CA public key (decoded) to generate public key from server-certificate.crt

			System.out.println("Got Server Public Key from serer-certificate.crt");

			generatedCertificate.verify(certPubKey);									//Use the CA public key to certify that CA created server-certificate.crt

			System.out.println("Validated the Server Public Key using CA public key on server-certificat.crt");

			servPubKey = generatedCertificate.getPublicKey();                   		//Now have server's public key for use in one-way authenticated Diffie-Hellman

			//do one-way authenticated Diffie-Hellman

			byte[] key = new byte[16];
			SecureRandom.getInstanceStrong().nextBytes(key);							//Generate a random 16 byte key

			Cipher cipher = Cipher.getInstance("RSA");									//Setup cipher to encrypt key
			cipher.init(Cipher.ENCRYPT_MODE, servPubKey);								//Set cipher to encrypt mode
			oos.writeObject(cipher.doFinal(key));										//Encrypt key and write it to the server

			System.out.println("Sent encrypted key to Server");

			byte[] FileNameBytes = FileName.getBytes("UTF-8");       		//Convert FileName to byte[]

			if (sending) {
				byte [] sendingFileName, EncryptedFileBytes;
				System.out.println("Sending File: " + FileName);

				f = new File(FileName);                                         		//Get the file

				oos.writeByte((byte) 1);                                        		//Alert the Server we are sending, by sending a 1 first

				sendingFileName = new byte[FileNameBytes.length];
				for (int i = 0; i < FileNameBytes.length; i++) {						//for each byte in the filename
					sendingFileName[i] = (byte) (FileNameBytes[i] ^ key[i % 16]);		//filename byte XOR with key goes into sendingFileName
				}
				oos.writeObject(sendingFileName);                               		//Send file name to the server
				oos.flush();

				md = MessageDigest.getInstance("SHA-1");                        		//send filename digest
				md.reset();
				oos.writeObject(md.digest(sendingFileName));
				oos.flush();

				fileBytes = Files.readAllBytes(f.toPath());								//Convert file contents to Bytes
				EncryptedFileBytes = new byte[fileBytes.length];										//Init the array to send
				for (int i = 0; i < fileBytes.length; i++) {							//for each byte in the file
					EncryptedFileBytes[i] = (byte) (fileBytes[i] ^ key[i % 16]);        //file byte XOR with key goes into tmp
				}
				oos.writeObject(EncryptedFileBytes);                                    //Send the file
				oos.flush();

				md = MessageDigest.getInstance("SHA-1");                        		//send file digest
				oos.writeObject(md.digest(EncryptedFileBytes));
				oos.flush();
			} else {
				byte[] EncryptedReceivingFileName, EncryptedFileBytes;
				System.out.println("Requesting file: " + FileName);

				oos.writeByte((byte) 0);                                        		//Alert the Server we want a file by sending a 0 first

				EncryptedReceivingFileName = new byte[FileNameBytes.length];

				for (int i = 0; i < FileNameBytes.length; i++) {								//for each byte in the filename
					EncryptedReceivingFileName[i] = (byte) (FileNameBytes[i] ^ key[i % 16]);	//filename byte XOR with key goes into sendingFileName
				}

				oos.writeObject(EncryptedReceivingFileName);                            //Send file name to the server
				oos.flush();

				md = MessageDigest.getInstance("SHA-1");                        		//send filename digest
				oos.writeObject(md.digest(EncryptedReceivingFileName));
				oos.flush();

				EncryptedFileBytes = (byte[]) ois.readObject();							//Wait for the Server to give us the file bytes

				md = MessageDigest.getInstance("SHA-1");                        		//check file digest
				if(!Arrays.equals((byte[])ois.readObject(), md.digest(EncryptedFileBytes))){
					System.out.println("File Altered");
					return;
				}

				fileBytes = new byte[EncryptedFileBytes.length];
				for (int i = 0; i < EncryptedFileBytes.length; i++) {
					fileBytes[i] = (byte) (EncryptedFileBytes[i] ^ key[i % 16]);        //decrypt bytes
				}

				f = new File(FileName);
				if (f.exists()) {
					f.delete();
				}
				Files.write(f.toPath(), fileBytes);                             		//save file

			}

			oos.close();
			ois.close();
			sock.close();

			System.out.println("Done");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}

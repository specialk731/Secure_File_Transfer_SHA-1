/*
 * 
 */

import sun.security.util.DerInputStream;
import sun.security.util.DerValue;

import javax.crypto.Cipher;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Arrays;
import java.util.Base64;

public class Server extends Thread {

	public static void main(String[] args) {
		ServerSocket serversocket;
		final int myPort = 14014;
		Server_Thread svr;

		try {
			serversocket = new ServerSocket(myPort);

			//Start while(true) loop here for unlimited clients
			while(true) {
				System.out.println("Waiting for client..."); //DEBUG ONLY
				Socket s = serversocket.accept();
				svr = new Server_Thread(s);
				svr.start();
				System.out.println("Got a client"); //DEBUG ONLY
			}
			//End while(true) loop

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}

class Server_Thread extends Thread {

	private Socket sock = null;

	public Server_Thread(Socket s) {
		sock = s;
	}

	public void run() {
		try {
			ObjectInputStream ois;
			ObjectOutputStream oos;
			File f, keyFile;
			MessageDigest md;
			PrivateKey serverPrivateKey;
			KeyFactory kf;
			byte[] key, tmp, EncryptedFileNameBytes, HashedEncryptedFileNameBytes, EncryptedFileBytes, HashedEncryptedFileBytes, certificate, FileName, FileBytes, digested, keyData;
			String keyString;
			oos = new ObjectOutputStream(sock.getOutputStream());
			ois = new ObjectInputStream(sock.getInputStream());

			f = new File("server-certificate.crt");
			certificate = Files.readAllBytes(f.toPath());
			oos.writeObject(certificate);                                       							//send certificate

			keyFile = new File("server-private.key");
			keyData = Files.readAllBytes(keyFile.toPath());													//Open Server's Private Key
			keyString = new String(keyData);
			keyString = keyString.replace("-----BEGIN RSA PRIVATE KEY-----\n","");		//Remove header, footer, and whitespace
			keyString = keyString.replace("-----END RSA PRIVATE KEY-----","");
			keyString = keyString.replaceAll("\\s","");
			DerInputStream derReader = new DerInputStream((Base64.getDecoder().decode(keyString)));			//Decode the Private Key
			DerValue[] seq = derReader.getSequence(0);

			if(seq.length < 9)
				throw new GeneralSecurityException("PKCS1 Private Key Issue");

			BigInteger modulus = seq[1].getBigInteger();
			BigInteger publicExp = seq[2].getBigInteger();
			BigInteger privateExp = seq[3].getBigInteger();
			BigInteger prime1 = seq[4].getBigInteger();
			BigInteger prime2 = seq[5].getBigInteger();
			BigInteger exp1 = seq[6].getBigInteger();
			BigInteger exp2 = seq[7].getBigInteger();
			BigInteger crtCoef = seq[8].getBigInteger();
			RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(modulus,publicExp,privateExp,prime1,prime2,exp1,exp2,crtCoef);

			kf = KeyFactory.getInstance("RSA");
			serverPrivateKey = kf.generatePrivate(keySpec);													//We can now use the Private Key

			//do one-way authenticated Diffie-Hellman
			//The Client will send us the key for the session using our public key. Only the Private key can decrypt this. Thus we know that the server is authentic

			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, serverPrivateKey);
			key = cipher.doFinal((byte[])ois.readObject());													//Read and decrypt the key for the session

			if (ois.readByte() == 1) {																		//Client wants to send us a file

				EncryptedFileNameBytes = (byte[])ois.readObject();                  						//Get the encrypted filename bytes
				md = MessageDigest.getInstance("SHA-1");                        							//check filename digest
				HashedEncryptedFileNameBytes = (byte[]) ois.readObject();									//Get Hashed Encrypted FileName Bytes
				digested = md.digest(EncryptedFileNameBytes);

				if(!Arrays.equals(HashedEncryptedFileNameBytes, digested)) {
					System.out.print("Filename Altered to ");
					FileName = new byte[EncryptedFileNameBytes.length];
					for (int i = 0; i < EncryptedFileNameBytes.length; i++) {
						FileName[i] = (byte) (EncryptedFileNameBytes[i] ^ key[i % 16]);                		//unencrypt filename bytes
					}
					System.out.println(new String(FileName,"UTF-8"));
					return;
				}

				FileName = new byte[EncryptedFileNameBytes.length];
				for (int i = 0; i < EncryptedFileNameBytes.length; i++) {
					FileName[i] = (byte) (EncryptedFileNameBytes[i] ^ key[i % 16]);                			//unencrypt filename bytes
				}

				String fileName = new String(FileName, "UTF-8");                				//convert bytes back to UTF-8

				System.out.println("Got FileName: " + fileName + " intact");

				f = new File(fileName);
				if (f.exists()) 																			//Delete it, if it exists
				{
					f.delete();
				}

				EncryptedFileBytes = (byte[]) ois.readObject();                                				//Get the files Encrypted bytes

				HashedEncryptedFileBytes = (byte[]) ois.readObject();
				md = MessageDigest.getInstance("SHA-1");                        							//check file digest
				if(!Arrays.equals( HashedEncryptedFileBytes, md.digest(EncryptedFileBytes))) {
					System.out.println("File Altered");
					return;
				}

				System.out.println("Got File Intact");

				FileBytes = new byte[EncryptedFileBytes.length];

				for (int i = 0; i < EncryptedFileBytes.length; i++) {
					FileBytes[i] = (byte) (EncryptedFileBytes[i] ^ key[i % 16]);               				//decrypt file bytes
				}

				Files.write(f.toPath(), FileBytes);                             							//save file

			} else {																						//Client wants a file
				EncryptedFileNameBytes = (byte[]) ois.readObject();                                			//Get the Encrypted filename bytes

				md = MessageDigest.getInstance("SHA-1");                        							//check filename digest
				HashedEncryptedFileNameBytes = (byte[]) ois.readObject();
				digested = md.digest(EncryptedFileNameBytes);

				if(!Arrays.equals(HashedEncryptedFileNameBytes, digested)) {
					System.out.println("Filename Altered");
					return;
				}

				FileName = new byte[EncryptedFileNameBytes.length];
				for (int i = 0; i < EncryptedFileNameBytes.length; i++) {
					FileName[i] = (byte) (EncryptedFileNameBytes[i] ^ key[i % 16]);                			//decrypt filename bytes
				}

				String fileName = new String(FileName, "UTF-8");                				//convert bytes back to UTF-8

				System.out.println("Got a request for file: " + fileName);

				f = new File(fileName);
				FileBytes = Files.readAllBytes(f.toPath());
				tmp = new byte[FileBytes.length];
				for (int i = 0; i < FileBytes.length; i++) {
					tmp[i] = (byte) (FileBytes[i] ^ key[i % 16]);               							//encrypt file bytes
				}
				oos.writeObject(tmp);                                           							//send file
				oos.flush();

				md = MessageDigest.getInstance("SHA-1");                        							//send file digest
				oos.writeObject(md.digest(tmp));
				oos.flush();

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

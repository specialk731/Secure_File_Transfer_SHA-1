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
		
		try {
			sock = new Socket(args[0], Integer.parseInt(args[1]));
			ois = new ObjectInputStream(sock.getInputStream());
			oos = new ObjectOutputStream(sock.getOutputStream());
			
			oos.writeUTF(args[2]);
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

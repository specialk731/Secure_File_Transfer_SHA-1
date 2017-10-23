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

	public Server_Thread(Socket s) {
		sock = s;
	}
	
	public void run() {
		try {
			oos = new ObjectOutputStream(sock.getOutputStream());
			ois = new ObjectInputStream(sock.getInputStream());
			
			String tmp = ois.readUTF();
			
			System.out.println("Got String tmp = " + tmp);
			
			oos.close();
			ois.close();
			sock.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}

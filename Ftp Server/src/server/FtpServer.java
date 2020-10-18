package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author hungv
 *
 */
public class FtpServer {

	private static final String TAG = FtpServer.class.getName();
	private static final int serverPort = 1999;

	private ServerSocket server = null;
	private boolean serverRunning = true;

	public static void main(String[] args) {
		new FtpServer();
	}

	public FtpServer() {
		try {
			server = new ServerSocket(serverPort);

		} catch (IOException e) {
			serverRunning = false;
			System.out.println("Could not create server socket");
			System.out.println(TAG + "\nDetails: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
		System.out.println("FTP Server started listening on port " + serverPort);

		while (serverRunning) {
			try {
				Socket client = server.accept();

				// Create new worker thread for new connection
				Worker w = new Worker(client);
				System.out.println("Thread [" + w.getId() + "] New connection received. Worker was created.");
				w.start();
			} catch (IOException e) {
				System.out.println(TAG + "\nDetails: " + e.getMessage());
				e.printStackTrace();
				try {
					server.close();
				} catch (IOException e1) {
					System.out.println("Can't close server");
					System.out.println(TAG + "\nDetails: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

}

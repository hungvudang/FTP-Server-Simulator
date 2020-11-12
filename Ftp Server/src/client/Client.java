package client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import javax.swing.JTextArea;

import client.gui.MainAppClient;

/**
 * @author hungv
 *
 */
public class Client implements Runnable {

	private static final String TAG = Client.class.getName();
	private static final int serverPort = 1999;
	private static final String serverHost = "localhost";

	private JTextArea consoleTextArea;

	// DEBUG MODE
	private boolean debugMode = false;

	// Path information
	private String root;
	private String currDirectory;
	private String fileSeparator = "/";

	/**
	 * Indicating the last set transfer Type
	 */
	private enum transferType {
		ASCII, BINARY
	}

	// data Connection
	private Socket serverDataConnection = null;
	private PrintWriter serverDataOutWriter = null;
	private transferType transferMode = transferType.BINARY;

	// control connection
	private Socket serverSocker = null;
	private PrintWriter serverOutWriter = null;
	private BufferedReader serverIn = null;

	private boolean clientRunning = true;

	Scanner in = null;
	
	private MainAppClient clientGUI = null;

	public static void main(String[] args) {
		Client client = new Client();
		Thread handleClientThread = new Thread(client);
		handleClientThread.start();
	}

	public Client() {
		// Initialization root directory
		this.root = System.getProperty("user.home") + fileSeparator + "CLIENT-DATA";
		this.currDirectory = this.root;

		in = new Scanner(System.in);
		try {
			serverSocker = new Socket(serverHost, serverPort);

			System.out.println("Connected to server.");

			serverIn = new BufferedReader(new InputStreamReader(serverSocker.getInputStream()));
			serverOutWriter = new PrintWriter(serverSocker.getOutputStream(), true);

			getOutputFromServer();

		} catch (IOException e) {
			clientRunning = false;
			in.close();
			debugOutput("Can't connect to server");
			System.out.println(TAG + "\nDetails: " + e.getMessage());
			try {
				serverSocker.close();

			} catch (NullPointerException | IOException e1) {
				System.out.println(TAG + "\nDetails: " + e.getMessage());
			}
		}
	}

	public Client(String host, int port, JTextArea consoleTextArea) {

		this.consoleTextArea = consoleTextArea;

		// Initialization root directory
		this.root = System.getProperty("user.home") + fileSeparator + "CLIENT-DATA";
		this.currDirectory = this.root;

		in = new Scanner(System.in);

		try {
			serverSocker = new Socket(host, port);

			System.out.println("Connected to server.");

			serverIn = new BufferedReader(new InputStreamReader(serverSocker.getInputStream()));
			serverOutWriter = new PrintWriter(serverSocker.getOutputStream(), true);

//			getOutputFromServer();
			UIgetOutputFromServer();
		} catch (IOException e) {
			clientRunning = false;
			in.close();
			debugOutput("Can't connect to server");
			System.out.println(TAG + "\nDetails: " + e.getMessage());
			try {
				serverSocker.close();

			} catch (NullPointerException | IOException e1) {
				System.out.println(TAG + "\nDetails: " + e.getMessage());
			}
		}
	}

	@Override
	public void run() {

		while (clientRunning) {
			String command = in.nextLine();
			executeCommand(command);
		}

	}

	private void executeCommand(String c) {
		// split command and arguments
		int index = c.indexOf(' ');
		String command = ((index == -1) ? c.toUpperCase() : (c.substring(0, index)).toUpperCase());
		String args = ((index == -1) ? null : c.substring(index + 1, c.length()));

		switch (command) {
		case "PWD":
		case "LS":
		case "TREE":
		case "CD":
		case "MKDIR":
		case "HELP":
		case "ROOTDIR":
		case "DEL":
			sendRequestToServer(c);
			break;
		case "DISCONNECT":
			sendRequestToServer(c);
			handleDisconnect();
			break;

		case "ASCII":
		case "BINARY":
			sendRequestToServer(c);
			handelTransferMode(command);
			break;

		case "SEND":
			Thread handleSendToServerThread = new Thread(new Runnable() {

				@Override
				public void run() {
					handleSend(args);
				}
			});
			File f = new File(args);
			if (!f.isDirectory()) {
				String fileName = f.getName();
				sendRequestToServer(command + " " + fileName);
			}
			handleSendToServerThread.start();
			break;
		case "RETV":
			Thread handleRetvFromServerThread = new Thread(new Runnable() {

				@Override
				public void run() {
					handleRetv(args);
				}
			});
			sendRequestToServer(c);
			handleRetvFromServerThread.start();
			break;
		case "OPEN":
			sendRequestToServer(c);
			sleep(100);
			handleOpenDataConnection(args);
			break;
		case "CLOSE":
			sendRequestToServer(c);
			sleep(100);
			handleCloseDataConnection();
			break;
		default:
			sendRequestToServer("Unsupported command");
			break;
		}
	}

	private void handleCloseDataConnection() {
		closeDataConnection();
	}

	private void handelTransferMode(String args) {

		if (args.equals("BINARY")) {
			transferMode = transferType.BINARY;
		} else {
			transferMode = transferType.ASCII;
		}

	}

	private void handleDisconnect() {

		clientRunning = false;

		if (serverDataConnection != null) {
			closeDataConnection();
		}
		try {
			serverIn.close();
			serverOutWriter.close();
			serverSocker.close();

		} catch (IOException e) {
			debugOutput("Can't close server socket");
			System.out.println(TAG + "\nDetails: " + e.getMessage());
			e.printStackTrace();
		}
		System.out.println("Connection was closed.");
	}

	/**
	 * 
	 * @param fileName
	 */
	private void handleRetv(String fileName) {

		if (serverDataConnection == null) {
			debugOutput("Data connection has not been created");
			return;
		}

		if (fileName == null) {
			System.out.println("No file name given");
		} else {
			File f = new File(currDirectory + fileSeparator + fileName);
			if (f.exists()) {
				System.out.println("File already exists");
			} else {
				// Binary mode
				if (transferMode == transferType.BINARY) {

					BufferedInputStream bis = null;
					BufferedOutputStream bos = null;

					try {
						bis = new BufferedInputStream(serverDataConnection.getInputStream());
						bos = new BufferedOutputStream(new FileOutputStream(f));

					} catch (IOException e) {
						System.out.println(TAG + "\nDetails: " + e.getMessage());
						e.printStackTrace();
					}

					// write file with buffer
					byte[] buf = new byte[1048576];
					int l = 0;

					try {
						while ((l = bis.read(buf, 0, 1048576)) != -1) {
							bos.write(buf, 0, l);
						}

					} catch (IOException e) {

						debugOutput("Could not read from or write to streams");
						System.out.println(TAG + "\nDetails: " + e.getMessage());
						e.printStackTrace();
					}

					// close stream
					try {
						bis.close();
						bos.close();
					} catch (IOException e) {
						debugOutput("Could not close streams");
						System.out.println(TAG + "\nDetails: " + e.getMessage());
						e.printStackTrace();
					}
					if (f.length() == 0L) {
						f.delete();
						debugOutput("Flie: " + f.getName() + " has't been exist.");
					}
				} else {
					// ASCII mode
					BufferedReader br = null;
					PrintWriter pw = null;

					try {
						br = new BufferedReader(new InputStreamReader(serverDataConnection.getInputStream()));
						pw = new PrintWriter(new FileOutputStream(f), true);

					} catch (IOException e) {
						System.out.println(TAG + "\nDetails: " + e.getMessage());
						e.printStackTrace();
					}

//					System.out.println("Starting file transmission of " + f.getName());
					debugOutput("Starting file transmission of " + f.getName());

					try {
						String s;
						while ((s = br.readLine()) != null) {
							pw.println(s);
						}

					} catch (IOException e) {

						debugOutput("Could not read from or write to streams");
						System.out.println(TAG + "\nDetails: " + e.getMessage());
						e.printStackTrace();
					}

					// close stream
					try {
						br.close();
						pw.close();
					} catch (IOException e) {
						debugOutput("Could not close streams");
						System.out.println(TAG + "\nDetails: " + e.getMessage());
						e.printStackTrace();
					}
					if (f.length() == 0L) {
						f.delete();
						debugOutput("Flie: " + f.getName() + " has't been exist.");
					}
				}
			}
		}
		handleCloseDataConnection();
	}

	/**
	 * 
	 * @param pathFile
	 */
	private void handleSend(String pathFile) {

		if (serverDataConnection == null) {
			debugOutput("Data connection hasn't been created");
			return;
		}

		if (pathFile == null) {
			System.out.println("No file name given");
		} else {
			File f = new File(pathFile);
			// File not exist
			if (!f.exists() || f.isDirectory()) {
				System.out.println("File not exists.");
			} else {
				// Binary mode
				BufferedInputStream bis = null;
				BufferedOutputStream bos = null;
				if (transferMode == transferType.BINARY) {
					try {
						bis = new BufferedInputStream(new FileInputStream(f));
						bos = new BufferedOutputStream(serverDataConnection.getOutputStream());

					} catch (IOException e) {
						System.out.println(TAG + "\nDetails: " + e.getMessage());
						e.printStackTrace();
					}

					// write file with buffer
					int sizeBuf = 1048576;
					sizeBuf = 1024;
					byte[] buf = new byte[sizeBuf];
					int l = 0;
					
					// =================================
					this.clientGUI.setProccessBar(0);
					long count = 0;
					
					try {
						while ((l = bis.read(buf, 0, sizeBuf)) != -1 && (bis.available()) != -1) {
							bos.write(buf, 0, l);
							count += l;
							
							
							System.out.println(((float)count / (float)f.length() )*100 + "%");
							this.clientGUI.setProccessBar((int)(((float)count / (float)f.length() )*100));
						}

					} catch (IOException e) {
						System.out.println(TAG + "\nDetails: " + e.getMessage());
						e.printStackTrace();
					}

					try {
						bis.close();
						bos.close();
					} catch (IOException e) {
						System.out.println(TAG + "\nDetails: " + e.getMessage());
						e.printStackTrace();
					}
				} else {
					// ASCII mode
					BufferedReader br = null;
					PrintWriter pw = null;

					try {
						br = new BufferedReader(new FileReader(f));
						pw = new PrintWriter(serverDataConnection.getOutputStream(), true);
					} catch (IOException e) {
						System.out.println(TAG + "\nDetails: " + e.getMessage());
						e.printStackTrace();
					}

					System.out.println("Starting file transmission of " + f.getName());

					try {
						String s;
						while ((s = br.readLine()) != null) {
							pw.println(s);
						}

					} catch (IOException e) {
						System.out.println(TAG + "\nDetails: " + e.getMessage());
						e.printStackTrace();
					}

					try {
						br.close();
						pw.close();
					} catch (IOException e) {
						System.out.println(TAG + "\nDetails: " + e.getMessage());
						e.printStackTrace();
					}
				}
			}
		}
		handleCloseDataConnection();
	}

	private void handleOpenDataConnection(String port) {
		try {
			int dataPort = Integer.parseInt(port);
			serverDataConnection = new Socket(serverHost, dataPort);
			serverDataOutWriter = new PrintWriter(serverDataConnection.getOutputStream(), true);

			debugOutput("Opened data connection");

		} catch (NumberFormatException e) {

			debugOutput("Invalid port number.");
			System.out.println(TAG + "\nDetails: " + e.getMessage());
		} catch (IOException e1) {

			debugOutput("Can't open data connection");
			System.out.println(TAG + "\nDetails: " + e1.getMessage());
		}
	}

	/**
	 * Close previously established data connection sockets and streams
	 */
	private void closeDataConnection() {

		if (serverDataConnection == null) {
			debugOutput("Connection has been closed");
			return;
		}
		// else
		try {
			serverDataOutWriter.close();
			serverDataConnection.close();

			debugOutput("Data connection was closed");
		} catch (IOException e) {

			debugOutput("Could not close data connection");
			System.out.println(TAG + "\nDetails: " + e.getMessage());
		}
		serverDataOutWriter = null;
		serverDataConnection = null;
	}

	private void sendRequestToServer(String request) {
		serverOutWriter.println(request);
	}

	private void debugOutput(String msg) {
		if (debugMode) {
			System.out.println(msg);
		}
	}

	public void exec(String c) {
		this.executeCommand(c);
	}

	private void getOutputFromServer() {
		Thread handleGetOutput = new Thread(new Runnable() {

			@Override
			public void run() {
				while (clientRunning) {
					try {
						String messages = serverIn.readLine();
						System.out.println(messages);
					} catch (IOException e) {
						System.out.println(TAG + "\nDetails: " + e.getMessage());
						System.exit(-1);
					}
				}

			}
		});
		handleGetOutput.start();
	}

	private void UIgetOutputFromServer() {
		Thread handleGetOutput = new Thread(new Runnable() {

			@Override
			public void run() {
				while (clientRunning) {
					try {
						String messages = serverIn.readLine();
//						System.out.println(messages);
						consoleTextArea.setText(consoleTextArea.getText() + "\n" + messages);
					} catch (IOException e) {
						System.out.println(TAG + "\nDetails: " + e.getMessage());
						System.exit(-1);
					}
				}

			}
		});
		handleGetOutput.start();
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void setMainAppClient(MainAppClient taget) {
		this.clientGUI = taget;
	}
	
	public MainAppClient getMainAppClient() {
		return this.clientGUI;
	}

}

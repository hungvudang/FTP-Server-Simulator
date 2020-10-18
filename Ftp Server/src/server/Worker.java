package server;

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
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author hungv
 *
 */

public class Worker extends Thread {

	private static final String TAG = Worker.class.getName();

	private static final String helpMessage = "Usage: <command>[<arguments>]"
			+ "\n\tASCII : switch type transfer to ASCII mode." + "\n\tBINARY : switch type transfer to BINARY mode."
			+ "\n\tCD [..][<directory>] : change directory." + "\n\tCLOSE : close data connection."
			+ "\n\tDEL <file name> : delete a file or folder." + "\n\tDISCONNECT : disconnect to FTP Server."
			+ "\n\tLS : list of directory and file." + "\n\tTREE : tree view directories and files."
			+ "\n\tMKDIR <directory name> : create a directory."
			+ "\n\tOPEN <port> : open data connection with port number." + "\n\tPWD : current working directory."
			+ "\n\tSEND <path to file> : send a file to FTP Server."
			+ "\n\tRETV <file name> : Retrieve a file from FTP Server.";

	/**
	 * Enable debugging output to console
	 */
	private boolean debugMode = true;

	/**
	 * Indicating the last set transfer Type
	 */
	private enum transferType {
		ASCII, BINARY
	}

	// data Connection
	private ServerSocket dataSocket;
	private Socket clientDataConnection;
	private PrintWriter clientDataOutWriter;

	// Path information
	private String root;
	private String currDirectory;
	private String fileSeparator = "/";

	// control connection
	private Socket clientSocket;
	private PrintWriter clientOutWriter;
	private BufferedReader clientIn;

	private transferType transferMode = transferType.BINARY;

	private boolean quitCommandLoop = false;

	/**
	 * Create new worker with given client socket
	 * 
	 * @param client   the socket for the current client
	 * @param dataPort the port for the data connection
	 */
	public Worker(Socket client) {
		super();
		this.clientSocket = client;
		this.root = System.getProperty("user.home") + fileSeparator + "FTP-SERVER-DATA";
		this.currDirectory = this.root;

	}

	public void run() {

		try {
			// Input from client
			clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

			// Output to client, automatically flushed after each print
			clientOutWriter = new PrintWriter(clientSocket.getOutputStream(), true);

			// Greeting
			sendMsgToClient("Welcome to the FTP-Server");

			// Get new command from client
			while (!quitCommandLoop) {
				executeCommand(clientIn.readLine());
			}

		} catch (IOException e) {
			System.out.println(TAG + "\nDetails: " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				// Clean up
				clientIn.close();
				clientOutWriter.close();
				clientSocket.close();

			} catch (IOException e) {
				System.out.println(TAG + "\nDetails: " + e.getMessage());
				e.printStackTrace();
			}

		}
	}

	/**
	 * Main command dispatcher method. Separates the command from the arguments and
	 * dispatches it to single handler functions.
	 * 
	 * @param c the raw input from the socket consisting of command and arguments
	 */
	private void executeCommand(String c) {

		// split command and arguments
		int index = c.indexOf(' ');
		String command = ((index == -1) ? c.toUpperCase() : (c.substring(0, index)).toUpperCase());
		String args = ((index == -1) ? null : c.substring(index + 1, c.length()));

		debugOutput("Command: " + command + " Args: " + args);

		// dispatcher mechanism for different commands
		switch (command) {
		case "PWD":
			handlePwd();
			break;
		case "ASCII":
			handleAscii();
			break;
		case "BINARY":
			handleBinary();
			break;
		case "DISCONNECT":
			handleDisconnect();
			break;
		case "OPEN":
			handleOpenDataConnnection(args);
			break;
		case "CLOSE":
			handleCloseDataConnnection();
			break;
		case "TREE":
			handlTree();
			break;
		case "LS":
			handleLs();
			break;
		case "CD":
			handleCd(args);
			break;
		case "MKDIR":
			handleMkdir(args);
			break;
		case "SEND":
			handleSend(args);
			break;
		case "RETV":
			handleRetv(args);
			break;
		case "ROOTDIR":
			handleRootDir();
			break;
		case "DEL":
			handleDel(args);
			break;
		case "HELP":
			handleHelp();
			break;
		default:
			sendMsgToClient("Unsupported command");
			break;
		}
	}

	private void handleDel(String fileName) {

		if (fileName == null) {

			sendMsgToClient("No file name given");
			return;
		}

		// Delete file
		File f = new File(currDirectory + fileSeparator + fileName);

		if (!f.exists()) {
			sendMsgToClient("Directory or file has't been exists.");
			return;
		}
		if ((f.getPath()).equals(this.root)) {
			sendMsgToClient("Access denied.");
			return;
		}
		if (f.isFile()) {
			sendMsgToClient("Deleted a file " + f.getName());
			f.delete();
		} else {
			sendMsgToClient("Deleted a directory " + f.getName());
			handleDeleteDirectory(f.getPath());
			f.delete();
		}

	}

	private void handleDeleteDirectory(String path) {
		File f = new File(path);
		File[] files = f.listFiles();
		for (File tmp : files) {
			if (tmp.isFile()) {
				tmp.delete();
			} else {
				handleDeleteDirectory(tmp.getPath());
				tmp.delete();
			}
		}
	}

	/**
	 * back to root directory
	 */
	private void handleRootDir() {
		this.currDirectory = this.root;
		sendMsgToClient("The current directory has been changed to " + this.currDirectory);
	}

	private void handleLs() {
		File currentDirectory = new File(currDirectory);
		File[] tmps = currentDirectory.listFiles();
		for (File f : tmps) {
			clientOutWriter.print(f.getName());
			clientOutWriter.print(" ");
		}
		clientOutWriter.println();
	}

	/**
	 * 
	 */
	private void handleCloseDataConnnection() {
		closeDataConnection();
	}

	private void handleHelp() {
		sendMsgToClient(helpMessage);
	}

	/**
	 * 
	 * @param args
	 */
	private void handleCd(String args) {

		String filename = currDirectory;

		// go one level up (cd ..)
		if (args.equals("..")) {
			int ind = filename.lastIndexOf(fileSeparator);
			if (ind > 0) {
				filename = filename.substring(0, ind);
				// check if file exists, is directory and is not above root directory
				File f = new File(filename);
				if (f.exists() && f.isDirectory() && (filename.length() >= root.length())) {
					currDirectory = filename;
					sendMsgToClient("The current directory has been changed to " + currDirectory);
				} else {
					sendMsgToClient("Requested action not taken. File unavailable.");
				}
			} else {
				sendMsgToClient("Requested action not taken. File unavailable.");
			}
		}
		// if argument is anything else (cd . does nothing)
		else if ((args != null) && (!args.equals("."))) {
			filename = filename + fileSeparator + args;

			// check if file exists, is directory and is not above root directory
			File f = new File(filename);
			if (f.exists() && f.isDirectory() && (filename.length() >= root.length())) {
				currDirectory = filename;
				sendMsgToClient("The current directory has been changed to " + currDirectory);
			} else {
				sendMsgToClient("Requested action not taken. File unavailable.");
			}
		} else {
			sendMsgToClient("Requested action not taken. File unavailable.");
		}

	}

	private void handlePwd() {
		sendMsgToClient(currDirectory);
		debugOutput("Command PWD: " + currDirectory);
	}

	private void handleRetv(String fileName) {

		if (clientDataConnection == null) {
			sendMsgToClient("Data connection hasn't been created");
			return;
		}

		if (fileName == null) {
			sendMsgToClient("No file name given");
		} else {
			File f = new File(currDirectory + fileSeparator + fileName);
			if (!f.exists() || f.isDirectory()) {
				sendMsgToClient("File not exists");
			} else {
				// Binary mode
				if (transferMode == transferType.BINARY) {

					sendMsgToClient("Opening binary mode data connection for requested file " + f.getName());

					BufferedInputStream bis = null;
					BufferedOutputStream bos = null;

					try {

						bis = new BufferedInputStream(new FileInputStream(f));
						bos = new BufferedOutputStream(clientDataConnection.getOutputStream());

					} catch (IOException e) {

						debugOutput("Could not create streams");
						e.printStackTrace();
					}
					// write file with buffer
					byte[] buf = new byte[1048576];
					int l = 0;

					debugOutput("Starting file transmission of " + f.getName());

					try {

						while ((l = bis.read(buf, 0, 1048576)) != -1) {
							bos.write(buf, 0, l);
						}

					} catch (IOException e) {
						debugOutput("Could not read from or write to streams");
						e.printStackTrace();
					}
					// close stream
					try {
						bis.close();
						bos.close();
					} catch (IOException e) {
						debugOutput("Could not close streams");
						e.printStackTrace();
					}
					debugOutput("Completed file transmission of " + f.getName());
					sendMsgToClient("File transfer successful. Closing data connection.");
				} else {
					// ASCII mode

					sendMsgToClient("Opening ASCII mode data connection for requested file " + f.getName());

					BufferedReader br = null;
					PrintWriter pw = null;

					try {
						br = new BufferedReader(new FileReader(f));
						pw = new PrintWriter(clientDataConnection.getOutputStream(), true);

					} catch (IOException e) {
						debugOutput("Could not create file streams");
						e.printStackTrace();
					}

					debugOutput("Starting file transmission of " + f.getName());

					String s;
					try {
						while ((s = br.readLine()) != null) {
							pw.println(s);
						}
					} catch (IOException e) {
						debugOutput("Could not read from or write to streams");
						e.printStackTrace();
					}

					try {
						pw.close();
						br.close();
					} catch (IOException e) {
						debugOutput("Could not close streams");
						e.printStackTrace();
					}
					debugOutput("Completed file transmission of " + f.getName());
					sendMsgToClient("File transfer successful. Closing data connection.");
				}

			}
		}
		handleCloseDataConnnection();
	}

	private void handleSend(String fileName) {

		if (clientDataConnection == null) {
			sendMsgToClient("Data connection hasn't been created");
			return;
		}

		if (fileName == null) {
			sendMsgToClient("No file name given");

		} else {

			File f = new File(currDirectory + fileSeparator + fileName);
			if (f.exists()) {
				sendMsgToClient("File already exists");
			} else {
				// Binary mode
				if (transferMode == transferType.BINARY) {

					sendMsgToClient("Opening binary mode data connection for requested file " + f.getName());

					BufferedInputStream bis = null;
					BufferedOutputStream bos = null;

					try {

						bis = new BufferedInputStream(clientDataConnection.getInputStream());
						bos = new BufferedOutputStream(new FileOutputStream(f));

					} catch (IOException e) {
						debugOutput("Could not create streams");
						e.printStackTrace();
					}
					// write file with buffer
					byte[] buf = new byte[1048576];
					int l = 0;

					debugOutput("Starting file transmission of " + f.getName());

					try {

						while ((l = bis.read(buf, 0, 1048576)) != -1) {
							bos.write(buf, 0, l);
						}

					} catch (IOException e) {
						debugOutput("Could not read from or write to streams");
						e.printStackTrace();
					}
					// close stream
					try {
						bis.close();
						bos.close();
					} catch (IOException e) {
						debugOutput("Could not close streams");
						e.printStackTrace();

					}
					if (f.length() == 0L) {
						f.delete();
						debugOutput("File " + f.getName() + " has't been exist.");
					} else {
						debugOutput("Completed file transmission of " + f.getName());
						sendMsgToClient("File transfer successful. Closing data connection.");
					}

				} else { // ASCII mode

					sendMsgToClient("Opening ASCII mode data connection for requested file " + f.getName());

					BufferedReader br = null;
					PrintWriter pw = null;

					try {
						br = new BufferedReader(new InputStreamReader(clientDataConnection.getInputStream()));
						pw = new PrintWriter(new FileOutputStream(f));

					} catch (IOException e) {
						debugOutput("Could not create streams");
						e.printStackTrace();
					}

					debugOutput("Starting file transmission of " + f.getName());

					String s;
					try {
						while ((s = br.readLine()) != null) {
							pw.println(s);
						}
					} catch (IOException e) {
						debugOutput("Could not read from or write to streams");
						e.printStackTrace();
					}

					try {
						pw.close();
						br.close();
					} catch (IOException e) {
						debugOutput("Could not close streams");
						e.printStackTrace();
					}

					if (f.length() == 0L) {
						f.delete();
						debugOutput("File " + f.getName() + " has't been exist.");
					} else {
						debugOutput("Completed file transmission of " + f.getName());
						sendMsgToClient("File transfer successful. Closing data connection.");
					}
				}
			}

		}
		handleCloseDataConnnection();
	}

	private void handleMkdir(String args) {

		// Allow only alphanumeric characters
		if (args != null && args.matches("^[a-zA-Z0-9_-]+$")) {
			File dir = new File(currDirectory + fileSeparator + args);

			if (!dir.mkdir()) {
				sendMsgToClient("Failed to create new directory");
				debugOutput("Failed to create new directory");
			} else {
				sendMsgToClient("Directory successfully created");
			}
		} else {
			sendMsgToClient("Invalid name");
		}

	}

	private void handlTree() {
		String heirachy = "   ";
		sendMsgToClient(new File(currDirectory).getName());
		treeView(currDirectory, heirachy);
	}

	private void handleOpenDataConnnection(String port) {
		try {
			int dataPort = Integer.parseInt(port);
			openDataConnection(dataPort);
		} catch (NumberFormatException e) {

			sendMsgToClient("Invalid port number");
			debugOutput(TAG + "\nInvaild port number" + "\nDetails: " + e.getMessage());
		}

	}

	private void handleDisconnect() {
		sendMsgToClient("Closing connection");
		quitCommandLoop = true;

		if (clientDataConnection != null) {
			closeDataConnection();
		}
		try {
			clientIn.close();
			clientOutWriter.close();
			clientSocket.close();
		} catch (IOException e) {
			debugOutput("Can't close client socket");
			e.printStackTrace();
		}

	}

	private void handleBinary() {
		transferMode = transferType.BINARY;
		sendMsgToClient("Type transfer: BINARY mode");
	}

	private void handleAscii() {
		transferMode = transferType.ASCII;
		sendMsgToClient("Type transfer: ASCII mode");
	}

	/**
	 * Sends a message to the connected client over the control connection. Flushing
	 * is automatically performed by the stream.
	 * 
	 * @param msg The message that will be sent
	 */
	private void sendMsgToClient(String msg) {
		clientOutWriter.println(msg);
	}

	/**
	 * Debug output to the console. Also includes the Thread ID for better
	 * readability.
	 * 
	 * @param msg Debug message
	 */
	private void debugOutput(String msg) {
		if (debugMode) {
			System.out.println("Client thread [" + this.getId() + "] " + msg);
		}
	}

	/**
	 * Open a new data connection socket and wait for new incoming connection from
	 * 
	 * @param port Port on which to listen for new incoming connection
	 */
	private void openDataConnection(int port) {

		try {
			dataSocket = new ServerSocket(port);
			clientDataConnection = dataSocket.accept();
			clientDataOutWriter = new PrintWriter(clientDataConnection.getOutputStream(), true);
			sendMsgToClient("Data connection - established");
			debugOutput("Data connection - established");

		} catch (IOException e) {
			debugOutput("Could not create data connection.");
			e.printStackTrace();
		}

	}

	/**
	 * Close previously established data connection sockets and streams
	 */
	private void closeDataConnection() {

		if (clientDataConnection == null) {
			sendMsgToClient("Connection has been closed.");
			return;
		}
		// else
		try {
			clientDataOutWriter.close();
			clientDataConnection.close();
			if (dataSocket != null) {
				dataSocket.close();
			}

			debugOutput("Data connection was closed");
			sendMsgToClient("Data connection was closed");
		} catch (IOException e) {
			debugOutput("Could not close data connection");
			e.printStackTrace();
		}
		clientDataOutWriter = null;
		clientDataConnection = null;
		dataSocket = null;
	}

	/**
	 * Display directory in tree view mode
	 * 
	 * @param directory
	 * @param heirachy
	 */
	private void treeView(String directory, String heirachy) {

		File f = new File(directory);

		File[] directories = f.listFiles();
		for (File fp : directories) {
			if (fp.isFile()) {
				sendMsgToClient(heirachy + "`--- " + fp.getName());
				debugOutput(heirachy + "`--- " + fp.getName());
			} else {
				// Directory
				sendMsgToClient(heirachy + "|___ " + fp.getName());
				debugOutput(heirachy + "|___ " + fp.getName());
				treeView(fp.getPath(), heirachy + "|   ");
			}
		}

	}

}

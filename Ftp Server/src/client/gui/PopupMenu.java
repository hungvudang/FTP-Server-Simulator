package client.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Random;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import client.Client;

public class PopupMenu extends JPopupMenu implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String serverRootPathData = System.getProperty("user.home") + "/SERVER-DATA";
	private static final String clientRootPathData = System.getProperty("user.home") + "/CLIENT-DATA";

	private final Client client;
	private String serverPathFile = null;
	private String clientPathFile = null;

	private JTree clientTree = null;
	private JTree serverTree = null;

	private boolean isClientTreeSelected;

	private JMenuItem mNew = new JMenuItem("New Folder");
	private JMenuItem mRefresh = new JMenuItem("Refresh");
	private JMenuItem mSend = new JMenuItem("Upload");
	private JMenuItem mRetv = new JMenuItem("Download");
	private JMenuItem mRename = new JMenuItem("Rename");
	private JMenuItem mDelete = new JMenuItem("Delete");

	public PopupMenu(Client client) {
		this.client = client;

		this.add(mNew);
		this.add(mRefresh);
		this.add(mSend);
		this.add(mRetv);
		this.add(mRename);
		this.add(mDelete);

		this.mNew.addActionListener(this);
		this.mRefresh.addActionListener(this);
		this.mSend.addActionListener(this);
		this.mRetv.addActionListener(this);
		this.mRename.addActionListener(this);
		this.mDelete.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		// handle mNew menuItem
		if (e.getSource() == this.mNew) {
			System.out.println("popup new");
			handleNewMenu();

			// repaint tree
			if (isClientTreeSelected && clientTree != null) {
				repaintTreeView(clientTree, "CLIENT DATA", clientRootPathData);
			}

			if (!isClientTreeSelected && serverTree != null) {
				repaintTreeView(serverTree, "SERVER DATA", serverRootPathData);
			}
		}
		// handle mRefresh menuItem
		else if (e.getSource() == this.mRefresh) {
			System.out.println("popup refresh");
			handleRefreshMenu();
		}
		// handle mSend menuItem
		else if (e.getSource() == this.mSend) {

//			System.out.println("popup send");
			if (clientPathFile != null) {
				int ans = JOptionPane.showOptionDialog(this, "Do you want to upload this file to SERVER", "Confirm",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
				if (ans == 0) {
					handleSendMenu();
				}

				sleep(100);
				if (serverTree != null)
					repaintTreeView(serverTree, "SERVER DATA", serverRootPathData);
			}
		}
		// handle mRetrieve menuItem
		else if (e.getSource() == this.mRetv) {
			System.out.println("popup retrieve");
			if (serverPathFile != null) {
				int ans = JOptionPane.showOptionDialog(this, "Do you want to download this file from SERVER", "Confirm",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
				if (ans == 0) {
					handleRetvMenu();
				}

				sleep(100);
				if (clientTree != null)
					repaintTreeView(clientTree, "CLIENT DATA", clientRootPathData);
			}
		}
		// handle mRename menuItem
		else if (e.getSource() == this.mRename) {

			System.out.println("popup rename");
			handleRenameMenu();
			
			if (clientTree != null && isClientTreeSelected)
				repaintTreeView(clientTree, "CLIENT DATA", clientRootPathData);
			if (serverTree != null && !isClientTreeSelected)
				repaintTreeView(serverTree, "SERVER DATA", serverRootPathData);
		}
		// handle mDelete menuItem
		else if (e.getSource() == this.mDelete) {
			System.out.println("popup delete");

			// Request from client
			if (clientPathFile != null || serverPathFile != null) {

				int ans = JOptionPane.showOptionDialog(this, "Do you want to delete this file", "Confirm",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
				if (ans == 0) {
					handleDeleteMenu();
					;
				}

				if (clientTree != null && isClientTreeSelected)
					repaintTreeView(clientTree, "CLIENT DATA", clientRootPathData);
				if (serverTree != null && !isClientTreeSelected)
					repaintTreeView(serverTree, "SERVER DATA", serverRootPathData);

			}

		}
	}

	private void handleRefreshMenu() {
		if (isClientTreeSelected) {
			if (clientTree != null) {
				System.out.println("Refresh client tree");
				repaintTreeView(clientTree, "CLIENT DATA", clientRootPathData);
			}
		} else {
			if (serverTree != null) {
				System.out.println("Refresh server tree");
				repaintTreeView(serverTree, "SERVER DATA", serverRootPathData);
			}
		}

	}

	private void handleNewMenu() {
		// request from client
		if (isClientTreeSelected) {
			String path = null;
			if (clientPathFile != null) {
				System.out.println(clientPathFile);

				int firstIndexOfSperator = clientPathFile.indexOf("/");

				if (firstIndexOfSperator != -1) {
					path = clientPathFile.substring(firstIndexOfSperator + 1);
				}

				if (path == null) {
					path = clientRootPathData;
				} else {
					path = clientRootPathData + "/" + path;
				}

			} else {
				System.out.println("client path is null");
				path = clientRootPathData;
			}

			// If path isn't directory --> return;
			File fTemp = new File(path);
			if (fTemp.isFile()) {
				return;
			}

			// ========================================================
			String dirName = JOptionPane.showInputDialog("New Folder");
			System.out.println(dirName);
			if (!dirName.isEmpty()) {
				String pathNewDir = path + "/" + dirName;
				File f = new File(pathNewDir);
				f.mkdir();
			}
		}
		// request from server
		else {
			String path = null;
			if (serverPathFile != null) {
				System.out.println(serverPathFile);

				int firstIndexOfSperator = serverPathFile.indexOf("/");

				if (firstIndexOfSperator != -1) {
					path = serverPathFile.substring(firstIndexOfSperator + 1);
				}

			}
			String dirName = JOptionPane.showInputDialog("New Folder");
			System.out.println(dirName);
			if (!dirName.isEmpty()) {
				if (path != null) {
					client.exec("CD " + path);
					client.exec("MKDIR " + dirName);
					sleep(100);
					requestBackToRootDir();
				}
			}
		}
	}

	private void handleSendMenu() {

		Random rand = new Random();
//		System.out.println(clientPathFile);
		int firstIndexOfSperator = clientPathFile.indexOf("/");
		int lastIndexOfSperator = clientPathFile.lastIndexOf("/");

		String path = null;
		if (firstIndexOfSperator != lastIndexOfSperator) {
			path = clientPathFile.substring(firstIndexOfSperator + 1, lastIndexOfSperator);
		}

		String fileName = clientPathFile.substring(lastIndexOfSperator + 1);

		if (fileName.equals("CLIENT DATA") || !isClientTreeSelected) {
			JOptionPane.showMessageDialog(null, "Access denied", "Erorr", JOptionPane.ERROR_MESSAGE);
			return;
		}

		int dataPort = rand.nextInt(10000) + 40000;
		client.exec("OPEN " + dataPort);
		sleep(500);

		if (path != null) {
			client.exec("SEND " + clientRootPathData + "/" + path + "/" + fileName);
		} else {
			client.exec("SEND " + clientRootPathData + "/" + fileName);
		}

	}

	private void handleRetvMenu() {

		Random rand = new Random();
//		System.out.println(serverPathFile);
		int firstIndexOfSperator = serverPathFile.indexOf("/");
		int lastIndexOfSperator = serverPathFile.lastIndexOf("/");
		String path = null;
		if (firstIndexOfSperator != lastIndexOfSperator) {
			path = serverPathFile.substring(firstIndexOfSperator + 1, lastIndexOfSperator);
		}

		String fileName = serverPathFile.substring(lastIndexOfSperator + 1);

		if (fileName.equals("SERVER DATA") || isClientTreeSelected) {
			JOptionPane.showMessageDialog(null, "Access denied", "Erorr", JOptionPane.ERROR_MESSAGE);
			return;
		}
		int dataPort = rand.nextInt(10000) + 40000;
		client.exec("OPEN " + dataPort);

		sleep(500);
		if (path != null) {
			client.exec("CD " + path);
		}
		client.exec("RETV " + fileName);

		requestBackToRootDir();
	}

	private void handleRenameMenu() {
		// for client side
		if (isClientTreeSelected) {
			System.out.println(clientPathFile);
			int firstIndexOfSperator = clientPathFile.indexOf("/");
			int lastIndexOfSperator = clientPathFile.lastIndexOf("/");
			String path = null;
			if (firstIndexOfSperator != lastIndexOfSperator) {
				path = clientPathFile.substring(firstIndexOfSperator + 1, lastIndexOfSperator);
			}

			String fileName = clientPathFile.substring(lastIndexOfSperator + 1);
			if (fileName.equals("CLIENT DATA")) {
				JOptionPane.showMessageDialog(null, "Access denied", "Erorr", JOptionPane.ERROR_MESSAGE);
				return;
			}

			if (path == null) {
				path = clientRootPathData;
			} else {
				path = clientRootPathData + "/" + path;
			}

			String newFileName = JOptionPane.showInputDialog("New File Name");
			System.out.println(newFileName);
			if (newFileName != null && !newFileName.isEmpty()) {
				
				File fOldName = new File(path + "/" + fileName);
				File fNewName = new File(path + "/" + newFileName);
				fOldName.renameTo(fNewName);
			}
		} else {
			// server side
		}
	}

	private void handleDeleteMenu() {

		// Request delete file from client
		if (isClientTreeSelected) {
			System.out.println(clientPathFile);
			int firstIndexOfSperator = clientPathFile.indexOf("/");
			int lastIndexOfSperator = clientPathFile.lastIndexOf("/");
			String path = null;
			if (firstIndexOfSperator != lastIndexOfSperator) {
				path = clientPathFile.substring(firstIndexOfSperator + 1, lastIndexOfSperator);
			}

			String fileName = clientPathFile.substring(lastIndexOfSperator + 1);
			if (fileName.equals("CLIENT DATA")) {
				JOptionPane.showMessageDialog(null, "Access denied", "Erorr", JOptionPane.ERROR_MESSAGE);
				return;
			}

			if (path == null) {
				path = clientRootPathData;
			} else {
				path = clientRootPathData + "/" + path;
			}
			String pathFileDelete = path + "/" + fileName;
			requestDeleteFile(pathFileDelete);
		}

		// Request delete file from server
		else {
			System.out.println(serverPathFile);
			int firstIndexOfSperator = serverPathFile.indexOf("/");
			int lastIndexOfSperator = serverPathFile.lastIndexOf("/");
			String path = null;
			if (firstIndexOfSperator != lastIndexOfSperator) {
				path = serverPathFile.substring(firstIndexOfSperator + 1, lastIndexOfSperator);
			}

			String fileName = serverPathFile.substring(lastIndexOfSperator + 1);
			if (fileName.equals("SERVER DATA")) {
				JOptionPane.showMessageDialog(null, "Access denied", "Erorr", JOptionPane.ERROR_MESSAGE);
				return;
			}

			if (path != null) {
				client.exec("CD " + path);
			}

			sleep(100);
			client.exec("DEL " + fileName);
			sleep(100);
			requestBackToRootDir();

		}

	}

	public String getServerPathFile() {
		return serverPathFile;
	}

	public void setServerPathFile(String serverPathFile) {
		this.serverPathFile = serverPathFile;
	}

	public String getClientPathFile() {
		return clientPathFile;
	}

	public void setClientPathFile(String clientPathFile) {
		this.clientPathFile = clientPathFile;
	}

	public JTree getClientTree() {
		return clientTree;
	}

	public void setClientTree(JTree clientTree) {
		this.clientTree = clientTree;
	}

	public JTree getServerTree() {
		return serverTree;
	}

	public void setServerTree(JTree serverTree) {
		this.serverTree = serverTree;
	}

	private void repaintTreeView(JTree tree, String rootName, String rootDataPath) {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootName);
		getTreeNodes(root, rootDataPath);
		tree.setModel(new DefaultTreeModel(root));
	}

	private void getTreeNodes(DefaultMutableTreeNode rootTree, String rootPath) {
		File root = new File(rootPath);
		File[] files = root.listFiles();
		for (File f : files) {
			DefaultMutableTreeNode child = new DefaultMutableTreeNode(f.getName());
			rootTree.add(child);
			if (!f.isFile()) {
				getTreeNodes(child, f.getPath());
			}
		}
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void requestBackToRootDir() {
		System.out.println("Request back to root directory.");
		sleep(100);
		this.client.exec("ROOTDIR");

	}

	public boolean isClientTreeSelected() {
		return isClientTreeSelected;
	}

	public void setClientTreeSelected(boolean isClientTreeSelected) {
		this.isClientTreeSelected = isClientTreeSelected;
	}

	private void requestDeleteFile(String pathFile) {

		// Delete file
		File f = new File(pathFile);

		if (f.isFile()) {
			System.out.println("Deleted a file " + f.getName());
			f.delete();
		} else {
			System.out.println("Deleted a directory " + f.getName());
			deleteDir(f.getPath());
			f.delete();
		}

	}

	private void deleteDir(String path) {
		File f = new File(path);

		if (!f.exists()) {
			return;
		}

		File[] files = f.listFiles();
		for (File tmp : files) {
			if (tmp.isFile()) {
				tmp.delete();
			} else {
				deleteDir(tmp.getPath());
				tmp.delete();
			}
		}
	}
}

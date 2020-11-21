package client.gui;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import client.Client;

import javax.swing.JProgressBar;

public class MainAppClient extends JFrame {

	/**
	 * 
	 */
	
	private static final long serialVersionUID = 1L;
	
	private JPanel contentPane;
	
	private static String fileSeparator = "/";

	private static final String serverRootPathData = System.getProperty("user.home")+ fileSeparator+ "SERVER-DATA";
	private static final String clientRootPathData = System.getProperty("user.home")+fileSeparator+ "CLIENT-DATA";

	private PopupMenu popupMenu;

	private Client client = null;
	private boolean isBinaryTransferType = true;

//	private JPanel contentPane;
	
	private JTextField hostTextField;
	private JTextField portTextField;
	
	private JProgressBar progressBar;
	
	


	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			LookAndFeelInfo[] plafs = UIManager.getInstalledLookAndFeels();
			for (LookAndFeelInfo info : plafs) {
				if (info.getName().contains("Nimbus")) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					try {
						MainAppClient frame = new MainAppClient();
						frame.setVisible(true);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});

		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Create the frame.
	 */
	public MainAppClient() {
		
		setResizable(false);

		setTitle("FTP Client");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(300, 20, 769, 720);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);
		contentPane.setLayout(null);

		JScrollPane scrollPaneServer = new JScrollPane();
		scrollPaneServer.setBounds(10, 261, 364, 390);
		contentPane.add(scrollPaneServer);

		JTree serverTree = new JTree();
		JTree clientTree = new JTree();

		serverTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					if (popupMenu != null) {

						popupMenu.show(serverTree, e.getX(), e.getY());
						popupMenu.setClientTree(clientTree);
						popupMenu.setServerTree(serverTree);
						popupMenu.setClientTreeSelected(false);

					}
				}
			}
		});

		serverTree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				String pathFile = getPathFileSelection(e);

				if (popupMenu != null) {

					popupMenu.setServerPathFile(pathFile);
//					popupMenu.setClientTreeSelected(false);
				}

				System.out.println(pathFile);
			}
		});
		serverTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("SERVER DATA") {
			private static final long serialVersionUID = 1L;
		}));
		scrollPaneServer.setViewportView(serverTree);

		JScrollPane scrollPaneClient = new JScrollPane();
		scrollPaneClient.setBounds(384, 261, 364, 390);
		contentPane.add(scrollPaneClient);

		clientTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {

					if (popupMenu != null) {

						popupMenu.show(clientTree, e.getX(), e.getY());
						popupMenu.setServerTree(serverTree);
						popupMenu.setClientTree(clientTree);
						popupMenu.setClientTreeSelected(true);

					}
				}
			}
		});
		clientTree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				String pathFile = getPathFileSelection(e);

				if (popupMenu != null) {

					popupMenu.setClientPathFile(pathFile);
//					popupMenu.setClientTreeSelected(true);
				}

				System.out.println(pathFile);
			}
		});
		clientTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("CLIENT DATA") {
			private static final long serialVersionUID = 1L;
		}));
		scrollPaneClient.setViewportView(clientTree);

		JPanel controllPanel = new JPanel();
		controllPanel.setBounds(6, 6, 742, 243);
		contentPane.add(controllPanel);
		controllPanel.setLayout(null);

		JScrollPane scrollPaneServerOutput = new JScrollPane();
		scrollPaneServerOutput.setBounds(0, 139, 742, 98);
		controllPanel.add(scrollPaneServerOutput);

		JTextArea outputTextArea = new JTextArea();
		outputTextArea.setTabSize(5);
		outputTextArea.setEditable(false);
		scrollPaneServerOutput.setViewportView(outputTextArea);

		JButton btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String serverHost = hostTextField.getText();
				int serverPort = Integer.parseInt(portTextField.getText());
				client = new Client(serverHost, serverPort, outputTextArea);
				client.setMainAppClient(MainAppClient.this);
				
				Thread handleClientThread = new Thread(client);
				handleClientThread.start();
				repaintTreeView(serverTree, "SERVER DATA", serverRootPathData);
				repaintTreeView(clientTree, "CLIENT DATA", clientRootPathData);
				popupMenu = new PopupMenu(client);
			}
		});
		btnConnect.setFont(new Font("Tahoma", Font.PLAIN, 15));
		btnConnect.setBounds(640, 6, 102, 42);
		controllPanel.add(btnConnect);

		JPanel panelServerInfo = new JPanel();
		panelServerInfo.setBorder(new TitledBorder(null, "SERVER INFO", TitledBorder.LEADING, TitledBorder.TOP, null,
				new Color(59, 59, 59)));
		panelServerInfo.setBounds(471, 6, 168, 98);
		controllPanel.add(panelServerInfo);
		panelServerInfo.setLayout(null);

		hostTextField = new JTextField();
		hostTextField.setBounds(14, 18, 142, 31);
		panelServerInfo.add(hostTextField);
		hostTextField.setText("localhost");
		hostTextField.setHorizontalAlignment(SwingConstants.CENTER);
		hostTextField.setFont(new Font("Tahoma", Font.PLAIN, 15));
		hostTextField.setColumns(10);

		portTextField = new JTextField();
		portTextField.setBounds(14, 49, 142, 31);
		panelServerInfo.add(portTextField);
		portTextField.setText("1999");
		portTextField.setHorizontalAlignment(SwingConstants.CENTER);
		portTextField.setFont(new Font("Tahoma", Font.PLAIN, 15));
		portTextField.setColumns(10);

		JLabel lblOutputFromServer = new JLabel("Responds from Server:");
		lblOutputFromServer.setFont(new Font("Tahoma", Font.PLAIN, 15));
		lblOutputFromServer.setBounds(6, 119, 151, 19);
		controllPanel.add(lblOutputFromServer);

		JButton btnDisconnect = new JButton("Disconnect");
		btnDisconnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (client == null) {
					return;
				}
				// else

				client.exec("DISCONNECT");
				popupMenu = null;
				client = null;
				serverTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("SERVER DATA") {
					private static final long serialVersionUID = 1L;
				}));
				clientTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("CLIENT DATA") {
					private static final long serialVersionUID = 1L;
				}));

			}
		});
		btnDisconnect.setFont(new Font("Tahoma", Font.PLAIN, 15));
		btnDisconnect.setBounds(640, 55, 102, 42);
		controllPanel.add(btnDisconnect);

		JPanel panelTransferMode = new JPanel();
		panelTransferMode.setBorder(new TitledBorder(null, "Transfer Mode", TitledBorder.LEADING, TitledBorder.TOP,
				null, new Color(59, 59, 59)));
		panelTransferMode.setBounds(0, 6, 117, 77);
		controllPanel.add(panelTransferMode);
		panelTransferMode.setLayout(null);

		JRadioButton rdbtnAsciiMode = new JRadioButton("ASCII");
		JRadioButton rdbtnBinaryMode = new JRadioButton("BINARY");
		rdbtnBinaryMode.setSelected(true);
		rdbtnBinaryMode.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (rdbtnAsciiMode.isSelected()) {
					rdbtnAsciiMode.setSelected(false);
					isBinaryTransferType = true;
					changeTransferType();
				} else {
					rdbtnBinaryMode.setSelected(true);
				}

			}
		});
		rdbtnBinaryMode.setFont(new Font("SansSerif", Font.PLAIN, 13));
		rdbtnBinaryMode.setBounds(14, 18, 102, 20);
		panelTransferMode.add(rdbtnBinaryMode);

		rdbtnAsciiMode.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (rdbtnBinaryMode.isSelected()) {
					rdbtnBinaryMode.setSelected(false);
					isBinaryTransferType = false;
					changeTransferType();

				} else {
					rdbtnAsciiMode.setSelected(true);
				}

			}
		});
		rdbtnAsciiMode.setFont(new Font("SansSerif", Font.PLAIN, 13));
		rdbtnAsciiMode.setBounds(14, 48, 97, 20);
		panelTransferMode.add(rdbtnAsciiMode);

		JButton btnClearOuputConsole = new JButton("Clear");
		btnClearOuputConsole.setFont(new Font("SansSerif", Font.PLAIN, 12));
		btnClearOuputConsole.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearOutputConsoleRespondsFromServer(outputTextArea);
			}
		});
		btnClearOuputConsole.setBounds(684, 109, 58, 28);
		controllPanel.add(btnClearOuputConsole);
		
		this.progressBar = new JProgressBar();
		this.progressBar.setBounds(59, 659, 315, 19);
		contentPane.add(this.progressBar);
		
		JLabel lblProccess = new JLabel("Process:");
		lblProccess.setBounds(10, 659, 55, 16);
		contentPane.add(lblProccess);

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

	private String getPathFileSelection(TreeSelectionEvent tree) {

		String pathSeparator = "/";
		String pathTmp = tree.getPath().toString();
		pathTmp = pathTmp.substring(1, pathTmp.length() - 1);

		String[] items = pathTmp.split(", ");
		String root = items[0];

		if (items.length == 1) {
			return root;
		}
		String fileName = items[items.length - 1];

		for (int i = 1; i < items.length - 1; i++) {
			root = root + pathSeparator + items[i];
		}

		String path = root + pathSeparator + fileName;
		return path;
	}

	private void changeTransferType() {
		if (client != null) {
			if (isBinaryTransferType) {
				client.exec("BINARY");
			} else {
				client.exec("ASCII");
			}
		}
	}

	private void clearOutputConsoleRespondsFromServer(JTextArea outputTextArea) {
		outputTextArea.setText("");
	}
	
	
	public void setProgressBar(int newValue) {
		this.progressBar.setValue(newValue);
	}
}

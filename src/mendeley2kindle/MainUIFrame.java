/**
 *         Copyright 2010 Yukinari Toyota <xxseyxx@gmail.com>
 *
 *         Licensed under the Apache License, Version 2.0 (the "License"); you
 *         may not use this file except in compliance with the License. You may
 *         obtain a copy of the License at
 *         http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 *         applicable law or agreed to in writing, software distributed under
 *         the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *         CONDITIONS OF ANY KIND, either express or implied. See the License
 *         for the specific language governing permissions and limitations under
 *         the License.
 */
package mendeley2kindle;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;

import mendeley2kindle.model.MCollection;

import org.json.JSONException;

/**
 * @author Yukinari Toyota <xxseyxx@gmail.com>
 *
 */
public class MainUIFrame extends JFrame {
	private static final long serialVersionUID = 2040707513660062703L;

	private KindleDAO kindle;

	private MendeleyDAO mendeley;

	private Mendeley2Kindle core;

	private JList collectionsJList;

	private Collection<JComponent> components;

	private JButton mainButton;

	class OpenMendeleyListener implements ActionListener {
		public void actionPerformed(ActionEvent actionevent) {
			File current = guessMendeleyHome();
			if (current == null)
				current = new File(".");
			File dbFile = guessMendeleyDB();

			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(current);
			chooser.setDialogTitle("Select your Mendeley database file");
			chooser.setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File file) {
					return file.isDirectory()
							|| file.getName().endsWith(".sqlite");
				}

				@Override
				public String getDescription() {
					return "Mendeley Database (*.sqlite)";
				}
			});
			if (dbFile != null)
				chooser.setSelectedFile(dbFile);

			int ret = chooser.showOpenDialog(MainUIFrame.this);
			if (ret == JFileChooser.APPROVE_OPTION) {
				openMendeley(chooser.getSelectedFile());
			}
		}
	}

	class SelectKindleListener implements ActionListener {
		public void actionPerformed(ActionEvent actionevent) {
			File current = guessKindleRoot();
			if (current == null)
				current = new File(".");

			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(current);
			chooser.setDialogTitle("Select your Kindle drive");
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setAcceptAllFileFilterUsed(false);
			int ret = chooser.showOpenDialog(MainUIFrame.this);
			if (ret == JFileChooser.APPROVE_OPTION) {
				openKindle(chooser.getSelectedFile());
			}
		}
	}

	class QuitListener implements ActionListener {
		public void actionPerformed(ActionEvent actionevent) {
			setVisible(false);
			System.exit(0);
		}
	}

	class SyncListener implements ActionListener {
		public void actionPerformed(ActionEvent actionevent) {
			Thread th = new Thread(new Runnable() {
				@Override
				public void run() {
					Object[] values = collectionsJList.getSelectedValues();
					List<MCollection> collections = new ArrayList<MCollection>(
							values.length);
					for (Object o : values)
						collections.add((MCollection) o);
					Collections.reverse(collections);
					core.syncCollections(collections, false, true, false);

				}
			});
			th.start();
		}
	}

	class UISyncStateListener implements SyncStateListener {
		private String prevText;
		private String colName;
		private int total;
		private int count;
		private int fileCount;

		@Override
		public void begin(int totalCollections) {
			count = 0;
			total = totalCollections;
			prevText = mainButton.getText();
			mainButton.setEnabled(false);
			mainButton.setText("Exporting " + total + " collections.");
		}

		@Override
		public void beginCollection(MCollection col) {
			fileCount = 0;
			count++;
			colName = col.getName();
			mainButton.setText("Exporting " + colName + " (" + count + "/"
					+ total + ")");
		}

		@Override
		public void beginAddFile(String name) {
			fileCount++;
			mainButton.setText("Exporting " + colName + " (" + count + "/"
					+ total + ")" + " progress:" + fileCount);
		}

		@Override
		public void beginAddFiles(int size) {
		}

		@Override
		public void beginRemoveFile(String name) {
			mainButton.setText("Exporting " + colName + " (" + count + "/"
					+ total + ")" + " progress:" + fileCount);
		}

		@Override
		public void beginRemoveFiles(int size) {
		};

		@Override
		public void beginUpdateFile(String name) {
			mainButton.setText("Exporting " + colName + " (" + count + "/"
					+ total + ")" + " progress:" + fileCount);
		}

		@Override
		public void beginUpdateFiles(int size) {
		};

		@Override
		public void endAddFile() {
		}

		@Override
		public void endAddFiles() {
		}

		@Override
		public void endCollection() {
		};

		@Override
		public void endRemoveFiles(int size) {
		}

		@Override
		public void endUpdateFiles(int size) {
		};

		@Override
		public void end() {
			mainButton.setText(prevText);
			mainButton.setEnabled(true);
		}
	}

	// from http://forums.sun.com/thread.jspa?threadID=390304
	class DragSelectionListener extends MouseInputAdapter {
		Point lastPoint = null;

		public void mousePressed(MouseEvent e) {
			lastPoint = e.getPoint(); // Need to hold onto starting mousepress
			// location...
		}

		public void mouseReleased(MouseEvent e) {
			lastPoint = null;
		}

		public void mouseDragged(MouseEvent e) {
			JList list = (JList) e.getSource();
			if (lastPoint != null && !e.isConsumed()
					&& SwingUtilities.isLeftMouseButton(e) && !e.isShiftDown()) {
				int row = list.locationToIndex(e.getPoint());
				if (row != -1) {
					int leadIndex = list.locationToIndex(lastPoint);
					// System.out.println("drag on row: "+row+" leadIndex:"+leadIndex);
					if (row != leadIndex) { // ignore drag within row
						Rectangle cellBounds = list.getCellBounds(row, row);
						if (cellBounds != null) {
							list.scrollRectToVisible(cellBounds);
							// Cannot use getAnchorSelectionIndex cause keeps
							// getting reset to current row..
							// jfc suggested code had: int anchorIndex =
							// list.getAnchorSelectionIndex ();

							int anchorIndex = leadIndex;
							if (e.isControlDown()) {
								if (list.isSelectedIndex(anchorIndex)) { // add
									// selection
									list.removeSelectionInterval(anchorIndex,
											leadIndex);
									list.addSelectionInterval(anchorIndex, row);
								} else { // remove selection
									list.addSelectionInterval(anchorIndex,
											leadIndex);
									list.removeSelectionInterval(anchorIndex,
											row);
								}
							} else { // replace selection
								list.setSelectionInterval(leadIndex, row);
							}
						}
					}
				}
			}
		}
	}

	static class MyCellRenderer extends DefaultListCellRenderer implements
			ListCellRenderer {
		static private final ImageIcon recentIcon = new ImageIcon(
				MainUIFrame.class.getResource("clock.png"));
		static private final ImageIcon favoritesIcon = new ImageIcon(
				MainUIFrame.class.getResource("star.png"));
		static private final ImageIcon needsReviewIcon = new ImageIcon(
				MainUIFrame.class.getResource("question-red.png"));
		static private final ImageIcon myPubIcon = new ImageIcon(
				MainUIFrame.class.getResource("user-white.png"));
		static private final ImageIcon folderIcon = new ImageIcon(
				MainUIFrame.class.getResource("folder-open-blue.png"));

		public MyCellRenderer() {

		}

		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected,
					cellHasFocus);
			String name = value.toString();
			if (name.equals("Recently Added")) {
				setIcon(recentIcon);
			} else if (name.equals("Favorites")) {
				setIcon(favoritesIcon);
			} else if (name.equals("Needs Review")) {
				setIcon(needsReviewIcon);
			} else if (name.equals("My Publications")) {
				setIcon(myPubIcon);
			} else {
				setIcon(folderIcon);
			}

			return this;
		}
	}

	public MainUIFrame(Properties config, Mendeley2Kindle core) {
		super("Mendeley2Kindle");
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		this.core = core;
		core.addStateListener(new UISyncStateListener());

		components = new ArrayList<JComponent>();

		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenuItem openMenuItem = new JMenuItem("Open Mendeley database");
		openMenuItem.addActionListener(new OpenMendeleyListener());
		JMenuItem selectKindleMenuItem = new JMenuItem(
				"Select Kindle device path");
		selectKindleMenuItem.addActionListener(new SelectKindleListener());
		JMenuItem exitMenuItem = new JMenuItem("Quit");
		exitMenuItem.addActionListener(new QuitListener());
		fileMenu.add(openMenuItem);
		fileMenu.add(selectKindleMenuItem);
		fileMenu.add(exitMenuItem);
		menuBar.add(fileMenu);

		JMenu helpMenu = new JMenu("Help");
		JMenuItem aboutMenuItem = new JMenuItem("About");
		aboutMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String text = "Mendeley2Kindle 0.3.1\n"
						+ " by Yukinari Toyota <xxseyxx@gmail.com>\n"
						+ " http://sites.google.com/site/xxseyxx/\n"
						+ "Some Icons by Yusuke Kamiyamane\n"
						+ "http://p.yusukekamiyamane.com/\n";
				JOptionPane.showMessageDialog(MainUIFrame.this, text);
			}
		});
		helpMenu.add(aboutMenuItem);
		menuBar.add(helpMenu);

		setJMenuBar(menuBar);

		getContentPane().setLayout(
				new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		getContentPane().add(new JLabel("Select Mendeley collections"));

		collectionsJList = new JList();
		DragSelectionListener mil = new DragSelectionListener();
		collectionsJList.addMouseMotionListener(mil);
		collectionsJList.addMouseListener(mil);
		collectionsJList.setCellRenderer(new MyCellRenderer());
		// collectionsJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		JScrollPane scroll = new JScrollPane();
		scroll.setPreferredSize(new Dimension(200, 300));
		scroll.getViewport().setView(collectionsJList);
		getContentPane().add(scroll);

		mainButton = new JButton("Open Mendeley database");
		mainButton.addActionListener(new OpenMendeleyListener());
		getContentPane().add(mainButton);

		components.add(collectionsJList);

		for (JComponent c : components)
			c.setEnabled(false);

		pack();
	}

	public void openKindle(File path) {
		try {
			kindle.open(path.getPath());

			if (kindle.isOpened()) {
				mainButton
						.removeActionListener(mainButton.getActionListeners()[0]);
				if (mendeley.isOpened()) {
					fireEnableComponents();
					mainButton.addActionListener(new SyncListener());
					mainButton.setText("Export to Kindle");
				} else {
					mainButton.addActionListener(new OpenMendeleyListener());
					mainButton.setText("Open Mendeley databasee");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void openMendeley(File path) {
		try {
			mendeley.open(path.getPath());

			final List<MCollection> list = mendeley.findCollections();
			ListModel model = new AbstractListModel() {
				private static final long serialVersionUID = 1L;

				@Override
				public Object getElementAt(int i) {
					return list.get(i);
				}

				@Override
				public int getSize() {
					return list.size();
				}
			};
			collectionsJList.setModel(model);
			if (mendeley.isOpened) {
				mainButton
						.removeActionListener(mainButton.getActionListeners()[0]);
				if (kindle.isOpened()) {
					fireEnableComponents();
					mainButton.addActionListener(new SyncListener());
					mainButton.setText("Export to Kindle");
				} else {
					mainButton.addActionListener(new SelectKindleListener());
					mainButton.setText("Select Kindle device");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void setKindleDAO(KindleDAO kindle) {
		this.kindle = kindle;
	}

	public void setMendeleyDAO(MendeleyDAO mendeley) {
		this.mendeley = mendeley;
	}

	private void fireEnableComponents() {
		for (JComponent c : components)
			c.setEnabled(true);
		int[] indices = new int[collectionsJList.getModel().getSize()];
		for (int i = 0; i < collectionsJList.getModel().getSize(); i++)
			indices[i] = i;
		collectionsJList.setSelectedIndices(indices);
	}

	private File guessKindleRoot() {
		String os = System.getProperty("os.name");
		if (os.indexOf("Windows") >= 0) {
			FileSystemView fs = FileSystemView.getFileSystemView();
			Pattern pat = Pattern.compile("^Kindle \\(\\w:\\)$");
			for (File f : File.listRoots()) {
				String name = fs.getSystemDisplayName(f);
				if (pat.matcher(name).matches()) {
					return f;
				}
			}
		} else if (os.indexOf("Linux") >= 0) {
			File f = new File("/media/");
			return f.isDirectory() ? f : null;
		} else if (os.indexOf("Mac") >= 0) {
			File f = new File("/Volumes/Kindle/");
			return f.isDirectory() ? f : new File("/Volumes/");
		}
		return null;
	}

	private File guessMendeleyHome() {
		String os = System.getProperty("os.name");
		String home = System.getProperty("user.home");
		if (os.indexOf("Windows") >= 0) {
			String localAppDataPath = System.getenv("LOCALAPPDATA");
			File localAppData = null;
			if (localAppDataPath != null) {
				localAppData = new File(localAppDataPath);
			}
			if (localAppDataPath == null || !localAppData.isDirectory()) {
				localAppData = new File(home,
						"Local Settings/Application Data/");
			}
			if (localAppData.isDirectory()) {
				File f = new File(localAppData,
						"Mendeley Ltd/Mendeley Desktop/");
				if (!f.isDirectory())
					f = new File(localAppData,
							"Mendeley Ltd./Mendeley Desktop/");
				if (f.isDirectory())
					return f;
			}
		} else if (os.indexOf("Linux") >= 0) {
			File f = new File(home,
					"/.local/share/data/Mendeley Ltd./Mendeley Desktop/");
			if (f.isDirectory())
				return f;
		} else if (os.indexOf("Mac") >= 0) {
			File f = new File(home,
					"Library/Application Support/Mendeley Desktop/");
			if (f.isDirectory())
				return f;
		}
		return null;
	}

	private File guessMendeleyDB() {
		return null;
	}
}

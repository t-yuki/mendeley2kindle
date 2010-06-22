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
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
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
	private KindleDAO kindle;
	private MendeleyDAO mendeley;
	private Mendeley2Kindle core;

	private JList collectionsJList;
	private Collection<JComponent> components;

	class OpenMendeleyListener implements ActionListener {
		public void actionPerformed(ActionEvent actionevent) {
			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(new File("."));
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
			int ret = chooser.showOpenDialog(MainUIFrame.this);
			if (ret == JFileChooser.APPROVE_OPTION) {
				openMendeley(chooser.getSelectedFile());
			}
		}
	}

	class SelectKindleListener implements ActionListener {
		public void actionPerformed(ActionEvent actionevent) {
			File current = new File(".");

			FileSystemView fs = FileSystemView.getFileSystemView();
			Pattern pat = Pattern.compile("^Kindle \\(\\w:\\)$");
			for (File f : File.listRoots()) {
				String name = fs.getSystemDisplayName(f);
				System.out.println(name);
				if (pat.matcher(name).matches()) {
					current = f;
					break;
				}
			}

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
			Object[] values = collectionsJList.getSelectedValues();
			List<MCollection> collections = new ArrayList<MCollection>(
					values.length);
			for (Object o : values)
				collections.add((MCollection) o);
			core.syncCollections(collections, false, true, false);
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

	public MainUIFrame() {
		super("Mendeley2Kindle");
		setDefaultCloseOperation(EXIT_ON_CLOSE);

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
				String text = "Mendeley2Kindle 0.1\n"
						+ " by Yukinari Toyota <xxseyxx@gmail.com>\n"
						+ " http://sites.google.com/site/xxseyxx/";
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
		// collectionsJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		JScrollPane scroll = new JScrollPane();
		scroll.setPreferredSize(new Dimension(160, 240));
		scroll.getViewport().setView(collectionsJList);
		getContentPane().add(scroll);

		JButton sync = new JButton("Export to kindle");
		sync.addActionListener(new SyncListener());
		getContentPane().add(sync);

		components.add(collectionsJList);
		components.add(sync);

		for (JComponent c : components)
			c.setEnabled(false);

		pack();
	}

	public void setCore(Mendeley2Kindle core) {
		this.core = core;
	}

	public void openKindle(File path) {
		try {
			kindle.open(path.getPath());

			if (kindle.isOpened() && mendeley.isOpened)
				for (JComponent c : components)
					c.setEnabled(true);
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
			if (kindle.isOpened() && mendeley.isOpened)
				for (JComponent c : components)
					c.setEnabled(true);

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

}

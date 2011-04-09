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

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.logging.LogManager;

import javax.swing.UIManager;

import mendeley2kindle.model.MCollection;

/**
 * @author Yukinari Toyota <xxseyxx@gmail.com>
 *
 */
public class Main {
	Mendeley2Kindle core;
	KindleDAO kindle;
	MendeleyDAO mendeley;
	// TODO implement configuration
	Properties config;

	/**
	 *
	 */
	public Main() {
		InputStream in = getClass().getResourceAsStream("logging.properties");
		try {
			LogManager.getLogManager().readConfiguration(in);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		core = new Mendeley2Kindle();
		kindle = new KindleDAO();
		mendeley = new MendeleyDAO();
		core.setKindleDAO(kindle);
		core.setMendeleyDAO(mendeley);

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Main main = new Main();
		main.process(args);
	}

	private void process(String[] args) {
		List<String> collections = new ArrayList<String>();
		boolean showGui = true;
		boolean showHelp = false;
		boolean syncFavourites = false;
		boolean syncNeedsReview = false;
		boolean syncMyPublications = false;
		String kindlePath = null;
		String mendeleyPath = null;
		try {
			LinkedList<String> list = new LinkedList<String>(
					Arrays.asList(args));
			while (!list.isEmpty()) {
				String opt = list.pop();
				String[] ss = opt.split("=", 2);
				if ("--config".equals(ss[0])) {
				} else if ("--sync".equals(ss[0])) {
					collections.add(ss.length == 2 ? ss[1] : list.pop());
				} else if ("--sync-favourites".equals(opt)
						|| "--sync-favorites".equals(opt)) {
					syncFavourites = true;
				} else if ("--sync-needsreview".equals(opt)) {
					syncNeedsReview = true;
				} else if ("--sync-mypublications".equals(opt)) {
					syncMyPublications = true;
				} else if ("--kindle".equals(ss[0])
						|| "--kindleroot".equals(ss[0])) {
					kindlePath = ss.length == 2 ? ss[1] : list.pop();
				} else if (ss[0].equals("--mendeley")
						|| ss[0].equals("--mendeleydb")) {
					mendeleyPath = ss.length == 2 ? ss[1] : list.pop();
				} else if ("--text".equals(opt)) {
					showGui = false;
				} else if ("--help".equals(opt) || "-h".equals(opt)) {
					showHelp = true;
					break;
				} else {
					new UnsupportedOperationException(opt);
				}
			}
		} catch (NoSuchElementException e) {
			showHelp = true;
		} catch (UnsupportedOperationException e) {
			showHelp = true;
		}

		try {
			if (kindlePath != null)
				kindle.open(kindlePath);
			if (mendeleyPath != null)
				mendeley.open(mendeleyPath);
		} catch (Exception e) {
			showHelp = true;
		}

		if ((!collections.isEmpty() || syncFavourites)
				&& (!kindle.isOpened() || !mendeley.isOpened)) {
			System.err.println("Kindle path or Mendeley DB is not set");
			showHelp = true;
		}
		if (showHelp) {
			System.err.println("Usage: example:");
			System.err
					.println("mendeley2kindle --text --kindleroot=KINDLE_ROOT --mendeleydb=MENDELEY_DB --sync=LIBRARY_NAME1 --sync=LIBRARY_NAME2 ...");
			System.err.println("Available options:");
			System.err.println("  --text : No GUI mode");
			// System.err
			// .println("  --config=CONFIG_FILE : Specifies config file [$HOME/.m2k.properties]");
			System.err
					.println("  --kindleroot=KINDLE_ROOT : Specifies kindle root path");
			System.err
					.println("  --mendeleydb=MENDELEY_DB : Specifies mendeley db path");
			System.err
					.println("  --sync=LIBRARY_NAME1 : Do sync target collections");
			System.err
					.println("  --sync-favorites : Do sync Favorite collection");
			System.err
					.println("  --sync-needsreview : Do sync Needs Review collection");
			System.err
					.println("  --sync-mypublications : Do sync My Publications collection");
			System.err.println("  -h, --help");
			return;
		}
		if (showGui)
			showGUI();
		List<MCollection> colls = new ArrayList<MCollection>();
		if (syncFavourites)
			colls.add(mendeley.getFavoritesCollection());
		if (syncNeedsReview)
			colls.add(mendeley.getNeedsReviewCollection());
		if (syncMyPublications)
			colls.add(mendeley.getMyPublicationsCollection());
		for (String name : collections) {
			try {
				colls.add(mendeley.findFolderByName(name));
			} catch (SQLException e) {
				System.err.println("Error: failed to find collection: " + name);
				return;
			}
		}
		if (!colls.isEmpty()) {
			Collections.reverse(colls);
			core.syncCollections(colls, false, true, false);
		}

	}

	private void showGUI() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		MainUIFrame ui = new MainUIFrame(config, core);
		ui.setKindleDAO(kindle);
		ui.setMendeleyDAO(mendeley);
		ui.setVisible(true);
	}

}

/**
 *
 */
package mendeley2kindle;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import mendeley2kindle.model.MCollection;

/**
 * @author sey
 *
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			InputStream in = Main.class
					.getResourceAsStream("logging.properties");
			LogManager.getLogManager().readConfiguration(in);
			in.close();

			Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
			log.fine("test");
			Mendeley2Kindle core = new Mendeley2Kindle();
			core.setDatabasePath("");
			core.setKindleDocumentsPath("");
			core.setKindleHome("J:/");

			MainUIFrame ui = new MainUIFrame(core);
			ui.setVisible(true);

			List<MCollection> list = new ArrayList<MCollection>();
			list.add(core.getMendeley().findCollectionByName("Active1"));
			core.syncCollections(list, false, true, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

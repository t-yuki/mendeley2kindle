/**
 *
 */
package mendeley2kindle;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

import javax.swing.UIManager;

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

			Mendeley2Kindle core = new Mendeley2Kindle();
			KindleDAO kindle = new KindleDAO();
			MendeleyDAO mendeley = new MendeleyDAO();

			try {
				// kindle.open("kindle.root/");
				// mendeley.open("mendeley2.sqlite");
			} catch (Exception e) {
				e.printStackTrace();
			}

			core.setKindleDAO(kindle);
			core.setMendeleyDAO(mendeley);

			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			MainUIFrame ui = new MainUIFrame();
			ui.setCore(core);
			ui.setKindleDAO(kindle);
			ui.setMendeleyDAO(mendeley);
			ui.setVisible(true);

			//List<MCollection> list = new ArrayList<MCollection>();
			//list.add(mendeley.findCollectionByName("Active1"));
			//core.syncCollections(list, false, true, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

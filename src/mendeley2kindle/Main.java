/**
 *
 */
package mendeley2kindle;

/**
 * @author sey
 *
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Mendeley2Kindle core = new Mendeley2Kindle();
		core.setDatabasePath("");
		core.setKindleDocumentsPath("");
		core.setKindleHome("J:/");

		MainUIFrame ui = new MainUIFrame(core);
		ui.setVisible(true);
	}

}

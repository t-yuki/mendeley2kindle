/**
 *
 */
package mendeley2kindle;

import javax.swing.JFrame;

/**
 * @author sey
 *
 */
public class MainUIFrame extends JFrame {
	private Mendeley2Kindle core;

	public MainUIFrame(Mendeley2Kindle core) {
		super("Mendeley2Kindle");
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		this.core = core;
	}
}

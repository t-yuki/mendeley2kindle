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

import java.io.InputStream;
import java.util.logging.LogManager;

import javax.swing.UIManager;

/**
 * @author Yukinari Toyota <xxseyxx@gmail.com>
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

			core.setKindleDAO(kindle);
			core.setMendeleyDAO(mendeley);

			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			MainUIFrame ui = new MainUIFrame();
			ui.setCore(core);
			ui.setKindleDAO(kindle);
			ui.setMendeleyDAO(mendeley);
			ui.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

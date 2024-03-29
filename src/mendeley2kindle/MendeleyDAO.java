/**
 *         Copyright 2012 Yukinari Toyota <xxseyxx@gmail.com>
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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import mendeley2kindle.model.MCollection;
import mendeley2kindle.model.MCondCollection;
import mendeley2kindle.model.MFile;
import mendeley2kindle.model.MFolder;

/**
 * @author Yukinari Toyota <xxseyxx@gmail.com>
 *
 */
public class MendeleyDAO {
	private static final Logger log = Logger.getLogger(MendeleyDAO.class
			.getName());

	boolean isOpened;

	Connection conn;

	public void open(String ds) throws SQLException {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		conn = DriverManager.getConnection("jdbc:sqlite:" + ds);
		isOpened = true;
	}

	public boolean isOpened() {
		return isOpened;
	}

	public List<MCollection> findCollections() throws SQLException {
		List<MCollection> list = new ArrayList<MCollection>();
		// add Favorites collection (Documents.favourite = 'true')
		list.add(getFavoritesCollection());
		// add Needs Review collection?
		list.add(getNeedsReviewCollection());
		// add My Publications collection?
		list.add(getMyPublicationsCollection());

		PreparedStatement ps = conn
				.prepareStatement("SELECT fd.id, fd.name FROM Folders fd ORDER BY fd.name");

		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			int id = rs.getInt(1);
			String name = rs.getString(2);
			list.add(new MFolder(id, name));
		}
		rs.close();
		ps.close();
		return list;
	}

	public MCondCollection getFavoritesCollection() {
		return new MCondCollection("Favorites",
				"favourite = 'true' AND deletionPending = 'false'");
	}

	public MCondCollection getNeedsReviewCollection() {
		return new MCondCollection("Needs Review",
				"confirmed = 'false' AND onlyReference = 'false' AND deletionPending = 'false'");
	}

	public MCondCollection getMyPublicationsCollection() {
		return new MCondCollection("My Publications",
				"privacy = 'PublishedDocument' AND deletionPending = 'false'");
	}

	public MFolder findFolderByName(String name) throws SQLException {
		PreparedStatement ps = conn
				.prepareStatement("SELECT fd.id, fd.name FROM Folders fd WHERE fd.name = ?");
		ps.setString(1, name);

		ResultSet rs = ps.executeQuery();
		MFolder col = null;
		while (rs.next()) {
			int id = rs.getInt(1);
			col = new MFolder(id, name);
		}
		rs.close();
		ps.close();
		return col;
	}

	public List<MFile> findFilesByCollection(int id) throws SQLException {
		PreparedStatement ps = conn
				.prepareStatement("SELECT fl.hash, fl.localUrl "
						+ "FROM Files fl "
						+ "JOIN DocumentFiles dfl ON fl.hash = dfl.hash "
						+ "JOIN DocumentFolders dfd ON dfl.documentId = dfd.documentId "
						+ "JOIN Folders fd ON fd.id = dfd.folderId "
						+ "JOIN Documents doc ON doc.id = dfd.documentId "
						+ "WHERE fd.id = ? AND deletionPending = 'false'");
		ps.setInt(1, id);
		ResultSet rs = ps.executeQuery();

		List<MFile> list = new ArrayList<MFile>();
		while (rs.next()) {
			String hash = rs.getString(1);
			String url = rs.getString(2);
			if (url.isEmpty())
				continue;

			MFile m = new MFile();
			m.setHash(hash);
			m.setLocalUrl(url);

			File f;
			try {
				f = new File(new URI(url));
				if (!f.canRead())
					continue;
				m.setName(f.getName());
			} catch (URISyntaxException e) {
				log.warning("Can't parse localUrl:" + url);
				continue;
			}
			list.add(m);
		}
		rs.close();
		ps.close();
		return list;
	}

	public List<MFile> findFilesByCondition(String where) throws SQLException {
		StringBuilder sql = new StringBuilder("SELECT fl.hash, fl.localUrl "
				+ "FROM Files fl "
				+ "JOIN DocumentFiles dfl ON fl.hash = dfl.hash "
				+ "JOIN Documents ds ON ds.id = dfl.documentId");
		if (where != null && !where.trim().isEmpty())
			sql.append(" WHERE ").append(where);
		PreparedStatement ps = conn.prepareStatement(sql.toString());
		ResultSet rs = ps.executeQuery();

		List<MFile> list = new ArrayList<MFile>();
		while (rs.next()) {
			String hash = rs.getString(1);
			String url = rs.getString(2);
			if (url.isEmpty())
				continue;

			MFile m = new MFile();
			m.setHash(hash);
			m.setLocalUrl(url);

			File f;
			try {
				f = new File(new URI(url));
				if (!f.canRead())
					continue;
				m.setName(f.getName());
			} catch (URISyntaxException e) {
				log.warning("Can't parse localUrl:" + url);
				continue;
			}
			list.add(m);
		}
		rs.close();
		ps.close();
		return list;
	}

	public MFile findFileByHash(String hash) throws SQLException {
		PreparedStatement ps = conn
				.prepareStatement("SELECT fl.hash, fl.localUrl "
						+ "FROM Files fl " + "WHERE fl.hash = ?");
		ps.setString(1, hash);
		ResultSet rs = ps.executeQuery();

		if (!rs.next())
			return null;
		String url = rs.getString(2);
		if (url.isEmpty())
			return null;

		MFile m = new MFile();
		m.setHash(hash);
		m.setLocalUrl(url);

		File f;
		try {
			f = new File(new URI(url));
			if (!f.canRead())
				return null;
			m.setName(f.getName());
		} catch (URISyntaxException e) {
		}
		rs.close();
		ps.close();
		return m;
	}
}

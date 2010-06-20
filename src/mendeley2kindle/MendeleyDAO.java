/**
 *
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
import java.util.Collection;
import java.util.List;

import mendeley2kindle.model.MCollection;
import mendeley2kindle.model.MFile;

/**
 * @author sey
 *
 */
public class MendeleyDAO {
	Connection conn;

	public void open(String ds) throws SQLException {
		// Class.forName("org.sqlite.JDBC");
		conn = DriverManager.getConnection("jdbc:sqlite:" + ds);
		System.out.println(conn);
	}

	public Collection<MCollection> findCollections() throws SQLException {
		PreparedStatement ps = conn
				.prepareStatement("SELECT fd.id, fd.name FROM Folders fd ");

		List<MCollection> list = new ArrayList<MCollection>();
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			int id = rs.getInt(1);
			String name = rs.getString(2);
			list.add(new MCollection(id, name));
		}
		rs.close();
		ps.close();
		return list;
	}

	public Collection<MFile> findFilesByCollection(MCollection col)
			throws SQLException {
		PreparedStatement ps = conn
				.prepareStatement("SELECT fl.hash, fl.localUrl "
						+ "FROM Files fl "
						+ "JOIN DocumentFiles dfl ON fl.hash = dfl.hash "
						+ "JOIN DocumentFolders dfd ON dfl.documentId = dfd.documentId "
						+ "JOIN Folders fd ON fd.id = dfd.folderId "
						+ "WHERE fd.id = ?");
		ps.setInt(1, col.getId());
		ResultSet rs = ps.executeQuery();

		List<MFile> list = new ArrayList<MFile>();
		while (rs.next()) {
			String hash = rs.getString(1);
			String url = rs.getString(2);
			if (url.isEmpty())
				continue;
			File f;
			try {
				f = new File(new URI(url));
				if (!f.canRead())
					continue;
			} catch (URISyntaxException e) {
			}
			MFile m = new MFile();
			m.setHash(hash);
			m.setLocalUrl(url);
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
		File f;
		try {
			f = new File(new URI(url));
			if (!f.canRead())
				return null;
		} catch (URISyntaxException e) {
		}
		MFile m = new MFile();
		m.setHash(hash);
		m.setLocalUrl(url);

		rs.close();
		ps.close();
		return m;

	}

}

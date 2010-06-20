/**
 *
 */
package mendeley2kindle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import mendeley2kindle.model.KFile;
import mendeley2kindle.model.MFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author sey
 *
 */
public class KindleDAO {
	private static final String KINDLE_COLLECTIONS_JSON = "system/collections.json";
	private static final String KINDLE_LOCALE = "@en-US";
	private static final String KINDLE_ROOT = "/mnt/us/";
	private static final String KINDLE_DOCUMENTS = "documents/mendeley2kindle/";

	private static final Logger log = Logger.getLogger(KindleDAO.class
			.getName());

	private String kindleLocal;
	private JSONObject collections;

	public void open(String kindleLocal) throws IOException, JSONException {
		this.kindleLocal = kindleLocal;
		File path = new File(kindleLocal);
		log.log(Level.FINER, "Loading: " + path);
		FileReader fr = new FileReader(new File(path, KINDLE_COLLECTIONS_JSON));
		StringBuilder sb = new StringBuilder();
		char[] buf1 = new char[2048];
		for (int read = 0; (read = fr.read(buf1)) > 0;)
			sb.append(buf1, 0, read);

		collections = new JSONObject(sb.toString());
		log.log(Level.FINE, "Loaded kindle collections: " + path);
	}

	public void commit() throws IOException {
		File path = new File(kindleLocal);
		log.log(Level.FINER, "writing: " + path);
		FileWriter fw = new FileWriter(new File(path, KINDLE_COLLECTIONS_JSON));
		fw.write(collections.toString());
		fw.close();
		log.log(Level.FINE, "Saved kindle collections: " + path);
	}

	public void createKCollection(String collection) throws JSONException {
		String key = collection + KINDLE_LOCALE;
		log.log(Level.FINER, "Creating kindle collection: " + key);
		if (!collections.isNull(key)) {
			log.log(Level.FINE, "Collection already exists: " + key);
			return;
		}
		JSONObject data = new JSONObject();
		data.put("items", new JSONArray());
		data.put("lastAccess", System.currentTimeMillis());
		collections.put(key, data);
		log.log(Level.FINER, "Created kindle collection: " + key);
	}

	@SuppressWarnings("unchecked")
	public Collection<String> findKCollectionsByFile(KFile file) {
		String path = toKindlePath(file);
		String khash = toKindleHash(path);
		List<String> list = new ArrayList<String>();
		try {
			Iterator<String> it = collections.keys();
			LABEL1: while (it.hasNext()) {
				String key = it.next();
				JSONArray items = collections.getJSONObject(key).getJSONArray(
						"items");
				for (int i = 0; i < items.length(); i++) {
					if (items.getString(i).equalsIgnoreCase(khash)) {
						list.add(key.substring(0, key.lastIndexOf('@')));
						continue LABEL1;
					}
				}
			}
		} catch (JSONException e) {
		}
		return list;
	}

	public void removeKCollection(String collection, boolean removeOrphanedFile) {
	}

	public void removeFile(KFile file) {
	}

	public boolean hasKCollection(String collection) {
		String key = collection + KINDLE_LOCALE;
		return !collections.isNull(key);
	}

	public boolean hasFile(String collection, MFile f) {
		String key = collection + KINDLE_LOCALE;
		String path = toKindlePath(f);
		String khash = toKindleHash(path);
		try {
			JSONArray items = collections.getJSONObject(key).getJSONArray(
					"items");
			for (int i = 0; i < items.length(); i++) {
				if (items.getString(i).equalsIgnoreCase(khash)) {
					return true;
				}
			}
		} catch (JSONException e) {
		}
		return false;
	}

	public void removeFile(String collection, KFile file) {
	}

	public void saveFile(MFile file, boolean exportHighlights)
			throws URISyntaxException, IOException {
		log.log(Level.FINER, "Exporting a document: " + file.getLocalUrl());
		File f = new File(new URI(file.getLocalUrl()));
		FileInputStream fis = new FileInputStream(f);

		File f2 = new File(toKindleLocalPath(file));
		if (f.lastModified() <= f2.lastModified()) {
			log.log(Level.FINE, "No need to save: " + f2);
			return;
		}

		f2.mkdirs();
		FileOutputStream fos = new FileOutputStream(f2);

		byte[] buf = new byte[4096];
		for (int read = 0; (read = fis.read(buf)) > 0;) {
			fos.write(buf, 0, read);
		}
		fos.close();
		fis.close();
		f2.setLastModified(f.lastModified());
		log.log(Level.FINE, "Exported a document: " + f2);
	}

	public void addFileToCollection(String collection, MFile f) {
		log.log(Level.FINER, "Adding a document:" + f.getLocalUrl()
				+ " to the collection: " + collection);
		if (hasFile(collection, f)) {
			log.log(Level.FINE, "The document:" + f.getLocalUrl()
					+ " already added to the collection: " + collection);
			return;
		}
		String path = toKindlePath(f);
		String khash = toKindleHash(path);

		String key = collection + KINDLE_LOCALE;
		try {
			JSONArray items = collections.getJSONObject(key).getJSONArray(
					"items");
			items.put(khash);
			log.log(Level.FINE, "Added a document:" + f.getLocalUrl()
					+ " to the collection: " + collection);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public Collection<KFile> listFiles(String collection) {
		String key = collection + KINDLE_LOCALE;
		try {
			JSONArray items = collections.getJSONObject(key).getJSONArray(
					"items");
			Set<String> khashes = new HashSet<String>(items.length());
			for (int i = 0; i < items.length(); i++) {
				String khash = items.getString(i);
				khashes.add(khash);
			}
			List<KFile> list = new ArrayList<KFile>(items.length());

			File documents = new File(kindleLocal + KINDLE_DOCUMENTS);
			listFilesRecursive(list, khashes, documents);
			return list;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void listFilesRecursive(List<KFile> list, Set<String> khashes,
			File dir) {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				listFilesRecursive(list, khashes, file);
				continue;
			}
			String parent = file.getParent();
			String path = KINDLE_ROOT + KINDLE_DOCUMENTS
					+ new File(parent, file.getName());
			if (khashes.contains(toKindleHash(path))) {
				KFile kf = new KFile();
				kf.setHash(parent);
				kf.setName(file.getName());
				list.add(kf);
			}
		}
	}

	private String toKindleHash(String kindlePath) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		byte[] sha1bin = md.digest(kindlePath.getBytes());
		return bytes2hex(sha1bin);
	}

	private String toKindleLocalPath(MFile file) {
		File f = new File(file.getLocalUrl());
		String path = kindleLocal + KINDLE_DOCUMENTS
				+ new File(file.getHash(), f.getName());
		return path;
	}

	private String toKindlePath(MFile file) {
		File f = new File(file.getLocalUrl());
		String path = KINDLE_ROOT + KINDLE_DOCUMENTS
				+ new File(file.getHash(), f.getName());
		return path;
	}

	private String toKindleLocalPath(KFile file) {
		String path = kindleLocal + KINDLE_DOCUMENTS
				+ new File(file.getHash(), file.getName());
		return path;
	}

	private String toKindlePath(KFile file) {
		String path = KINDLE_ROOT + KINDLE_DOCUMENTS
				+ new File(file.getHash(), file.getName());
		return path;
	}

	private static String bytes2hex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'a', 'b', 'c', 'd', 'e', 'f' };
		for (int i = 0; i < bytes.length; i++) {
			int high = ((bytes[i] & 0xf0) >> 4);
			int low = (bytes[i] & 0x0f);
			sb.append(hexChars[high]);
			sb.append(hexChars[low]);
		}
		return sb.toString();
	}
}
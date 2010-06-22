/**
 *
 */
package mendeley2kindle;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import mendeley2kindle.model.KFile;
import mendeley2kindle.model.MCollection;
import mendeley2kindle.model.MFile;

import org.json.JSONException;

/**
 * @author sey
 *
 */
public class Mendeley2Kindle {
	private KindleDAO kindle;
	private MendeleyDAO mendeley;

	public Mendeley2Kindle() {
	}

	public void setKindleDAO(KindleDAO kindle) {
		this.kindle = kindle;
	}

	public void setMendeleyDAO(MendeleyDAO mendeley) {
		this.mendeley = mendeley;
	}

	public void syncCollections(Collection<MCollection> collections,
			boolean syncAllDocuments, boolean removeOrphanedFile,
			boolean exportHighlights) {
		Collection<KFile> globalRemoved = new ArrayList<KFile>();
		for (MCollection col : collections) {
			try {
				Collection<KFile> removed = new ArrayList<KFile>();
				Collection<MFile> added = new ArrayList<MFile>();
				Collection<MFile> updated = new ArrayList<MFile>();

				if (!kindle.hasKCollection(col.getName()))
					kindle.createKCollection(col.getName());
				Collection<KFile> kFiles = kindle.listFiles(col.getName());
				Collection<MFile> mFiles = mendeley.findFilesByCollection(col
						.getId());
				Collection<String> msFiles = new ArrayList<String>();

				// Find updated/added/removed documents
				for (MFile mf : mFiles) {
					String url = mf.getLocalUrl();
					File f = new File(new URI(url));
					String name = f.getName();
					if (kFiles.contains(name)) {
						updated.add(mf);
					} else {
						added.add(mf);
					}
					msFiles.add(name);
				}
				for (KFile kFile : kFiles) {
					if (!msFiles.contains(kFile)) {
						removed.add(kFile);
						globalRemoved.add(kFile);
					}
				}

				// do action
				for (MFile mf : added) {
					kindle.saveFile(mf, exportHighlights);
					kindle.addFileToCollection(col.getName(), mf);
				}
				for (MFile mf : updated) {
					kindle.saveFile(mf, exportHighlights);
				}
				for (KFile kFile : removed) {
					kindle.removeFile(col.getName(), kFile);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
				assert false;
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// remove from kindle if the file is removed from mendeley db
		for (KFile kFile : globalRemoved) {
			MFile mf;
			try {
				mf = mendeley.findFileByHash(kFile.getHash());
				if (mf != null && syncAllDocuments)
					continue;
			} catch (SQLException e) {
				e.printStackTrace();
			}
			Collection<String> kCols = kindle.findKCollectionsByFile(kFile);
			if (kCols.size() == 0 && removeOrphanedFile)
				kindle.removeFile(kFile);
		}
		try {
			kindle.commit();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

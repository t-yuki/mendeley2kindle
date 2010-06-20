/**
 *
 */
package mendeley2kindle.model;

/**
 * @author sey
 *
 */
public class MFile {
	private String hash;
	private String localUrl;

	public MFile() {
	}

	public String getHash() {
		return hash;
	}

	public String getLocalUrl() {
		return localUrl;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public void setLocalUrl(String localUrl) {
		this.localUrl = localUrl;
	}
}

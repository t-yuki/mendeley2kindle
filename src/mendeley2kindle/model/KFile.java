/**
 *
 */
package mendeley2kindle.model;

/**
 * @author sey
 *
 */
public class KFile {
	private String hash;
	private String name;

	public KFile() {
	}

	public String getHash() {
		return hash;
	}

	public String getName() {
		return name;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public void setName(String name) {
		this.name = name;
	}
}

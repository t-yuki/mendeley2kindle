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

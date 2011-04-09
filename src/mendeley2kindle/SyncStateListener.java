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

import java.util.EventListener;

import mendeley2kindle.model.MCollection;

/**
 * @author sey
 *
 */
public interface SyncStateListener extends EventListener {
	public void begin(int totalCollections);

	public void beginCollection(MCollection col);

	public void endCollection();

	public void end();

	/**
	 * @param size
	 */
	public void beginAddFiles(int size);

	/**
	 * @param name
	 */
	public void beginAddFile(String name);

	/**
	 *
	 */
	public void endAddFile();

	/**
	 *
	 */
	public void endAddFiles();

	/**
	 * @param size
	 */
	public void beginUpdateFiles(int size);

	/**
	 * @param name
	 */
	public void beginUpdateFile(String name);

	/**
	 * @param size
	 */
	public void endUpdateFiles(int size);

	/**
	 * @param size
	 */
	public void beginRemoveFiles(int size);

	/**
	 * @param name
	 */
	public void beginRemoveFile(String name);

	/**
	 * @param size
	 */
	public void endRemoveFiles(int size);
}

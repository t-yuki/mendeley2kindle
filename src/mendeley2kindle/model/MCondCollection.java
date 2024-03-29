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
 * @version $Id: $
 *
 */
public class MCondCollection extends MCollection {
	private String condition;

	/**
	 * @param name
	 */
	public MCondCollection(String name, String condition) {
		super(name);
		this.condition = condition;
	}

	/**
	 * @return condition
	 */
	public String getCondition() {
		return condition;
	}

	/**
	 * @param condition
	 *            sets condition
	 */
	public void setCondition(String condition) {
		this.condition = condition;
	}
}

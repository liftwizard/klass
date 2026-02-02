/*
 * Copyright 2026 Craig Motlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cool.klass.serialization.jackson.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.liftwizard.logging.slf4j.mdc.MultiMDCCloseable;

public class KlassResponsePagination {

	private final int pageSize;
	private final int numberOfPages;
	private final int pageNumber;

	public KlassResponsePagination(int pageSize, int numberOfPages, int pageNumber) {
		this.pageSize = pageSize;
		this.numberOfPages = numberOfPages;
		this.pageNumber = pageNumber;
	}

	@JsonProperty
	public int getPageSize() {
		return this.pageSize;
	}

	@JsonProperty
	public int getNumberOfPages() {
		return this.numberOfPages;
	}

	@JsonProperty
	public int getPageNumber() {
		return this.pageNumber;
	}

	@Override
	public String toString() {
		return String.format(
			"{pageSize:%d,numberOfPages:%d,pageNumber:%d}",
			this.pageSize,
			this.numberOfPages,
			this.pageNumber
		);
	}

	public void withMDC(MultiMDCCloseable mdc) {
		mdc.put("klass.response.pagination.pageSize", String.valueOf(this.pageSize));
		mdc.put("klass.response.pagination.numberOfPages", String.valueOf(this.numberOfPages));
		mdc.put("klass.response.pagination.pageNumber", String.valueOf(this.pageNumber));
	}
}

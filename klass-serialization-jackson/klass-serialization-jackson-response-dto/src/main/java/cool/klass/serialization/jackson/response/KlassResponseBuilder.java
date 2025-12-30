/*
 * Copyright 2025 Craig Motlin
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

import java.security.Principal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.projection.Projection;

public class KlassResponseBuilder {

	@Nullable
	private final Object data;

	@Nonnull
	private final Projection projection;

	@Nonnull
	private final Multiplicity multiplicity;

	@Nonnull
	private final Instant transactionTimestamp;

	@Nonnull
	private Optional<KlassResponsePagination> pagination = Optional.empty();

	@Nonnull
	private Optional<? extends Principal> principal = Optional.empty();

	@Nonnull
	private Optional<String> criteria = Optional.empty();

	@Nonnull
	private Optional<String> orderBy = Optional.empty();

	public KlassResponseBuilder(
		@Nullable Object data,
		@Nonnull Projection projection,
		@Nonnull Multiplicity multiplicity,
		@Nonnull Instant transactionTimestamp
	) {
		this.data = data;
		this.projection = Objects.requireNonNull(projection);
		this.multiplicity = Objects.requireNonNull(multiplicity);
		this.transactionTimestamp = Objects.requireNonNull(transactionTimestamp);
	}

	public KlassResponseBuilder setPagination(int pageSize, int numberOfPages, int pageNumber) {
		if (!this.multiplicity.isToMany()) {
			throw new IllegalStateException();
		}

		if (this.pagination.isPresent()) {
			throw new IllegalStateException();
		}

		KlassResponsePagination klassResponsePagination = new KlassResponsePagination(
			pageSize,
			numberOfPages,
			pageNumber
		);
		this.pagination = Optional.of(klassResponsePagination);
		return this;
	}

	public KlassResponseBuilder setPrincipal(@Nonnull Principal principal) {
		if (this.principal.isPresent()) {
			throw new IllegalStateException();
		}
		this.principal = Optional.of(principal);
		return this;
	}

	public KlassResponseBuilder setCriteria(@Nonnull String criteria) {
		this.criteria = Optional.of(criteria);
		return this;
	}

	public KlassResponseBuilder setOrderBy(@Nonnull String orderBy) {
		this.orderBy = Optional.of(orderBy);
		return this;
	}

	public KlassResponse build() {
		KlassResponseMetadata metadata = new KlassResponseMetadata(
			this.criteria,
			this.orderBy,
			this.multiplicity,
			this.projection,
			this.transactionTimestamp,
			this.pagination,
			this.principal
		);
		return new KlassResponse(metadata, this.data);
	}

	@Override
	public String toString() {
		return (
			"KlassResponseBuilder{"
			+ "projection="
			+ this.projection
			+ ", multiplicity="
			+ this.multiplicity
			+ ", transactionTimestamp="
			+ this.transactionTimestamp
			+ ", pagination="
			+ this.pagination
			+ ", principal="
			+ this.principal
			+ ", criteria="
			+ this.criteria
			+ ", orderBy="
			+ this.orderBy
			+ "}"
		);
	}
}

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

package cool.klass.model.meta.domain.service;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.AbstractPackageableElement;
import cool.klass.model.meta.domain.KlassImpl;
import cool.klass.model.meta.domain.KlassImpl.KlassBuilder;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.service.url.Url;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.api.source.service.ServiceGroupWithSourceCode;
import cool.klass.model.meta.domain.service.url.UrlImpl.UrlBuilder;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceGroupDeclarationContext;
import org.eclipse.collections.api.list.ImmutableList;

public final class ServiceGroupImpl extends AbstractPackageableElement implements ServiceGroupWithSourceCode {

	@Nonnull
	private final KlassImpl klass;

	private ImmutableList<Url> urls;

	private ServiceGroupImpl(
		@Nonnull ServiceGroupDeclarationContext elementContext,
		@Nonnull Optional<Element> macroElement,
		@Nullable SourceCode sourceCode,
		int ordinal,
		@Nonnull IdentifierContext nameContext,
		@Nonnull String packageName,
		@Nonnull KlassImpl klass
	) {
		super(elementContext, macroElement, sourceCode, ordinal, nameContext, packageName);
		this.klass = Objects.requireNonNull(klass);
	}

	@Nonnull
	@Override
	public ServiceGroupDeclarationContext getElementContext() {
		return (ServiceGroupDeclarationContext) super.getElementContext();
	}

	@Override
	@Nonnull
	public KlassImpl getKlass() {
		return this.klass;
	}

	@Override
	@Nonnull
	public ImmutableList<Url> getUrls() {
		return Objects.requireNonNull(this.urls);
	}

	private void setUrls(@Nonnull ImmutableList<Url> urls) {
		if (this.urls != null) {
			throw new IllegalStateException();
		}
		this.urls = Objects.requireNonNull(urls);
	}

	public static final class ServiceGroupBuilder
		extends PackageableElementBuilder<ServiceGroupImpl>
		implements TopLevelElementBuilderWithSourceCode {

		@Nonnull
		private final KlassBuilder klassBuilder;

		private ImmutableList<UrlBuilder> urlBuilders;

		public ServiceGroupBuilder(
			@Nonnull ServiceGroupDeclarationContext elementContext,
			@Nonnull Optional<ElementBuilder<?>> macroElement,
			@Nullable SourceCodeBuilder sourceCode,
			int ordinal,
			@Nonnull IdentifierContext nameContext,
			@Nonnull String packageName,
			@Nonnull KlassBuilder klassBuilder
		) {
			super(elementContext, macroElement, sourceCode, ordinal, nameContext, packageName);
			this.klassBuilder = Objects.requireNonNull(klassBuilder);
		}

		public void setUrlBuilders(@Nonnull ImmutableList<UrlBuilder> urlBuilders) {
			this.urlBuilders = Objects.requireNonNull(urlBuilders);
		}

		@Override
		@Nonnull
		protected ServiceGroupImpl buildUnsafe() {
			return new ServiceGroupImpl(
				(ServiceGroupDeclarationContext) this.elementContext,
				this.macroElement.map(ElementBuilder::getElement),
				this.sourceCode.build(),
				this.ordinal,
				this.getNameContext(),
				this.packageName,
				this.klassBuilder.getElement()
			);
		}

		@Override
		protected void buildChildren() {
			ImmutableList<Url> urls = this.urlBuilders.collect(UrlBuilder::build);
			this.element.setUrls(urls);
		}
	}
}

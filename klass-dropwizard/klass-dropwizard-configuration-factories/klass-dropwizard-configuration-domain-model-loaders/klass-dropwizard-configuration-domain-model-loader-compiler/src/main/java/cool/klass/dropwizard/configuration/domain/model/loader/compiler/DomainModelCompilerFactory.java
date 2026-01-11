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

package cool.klass.dropwizard.configuration.domain.model.loader.compiler;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import cool.klass.dropwizard.configuration.domain.model.loader.DomainModelFactory;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import cool.klass.model.meta.loader.compiler.DomainModelCompilerLoader;
import io.dropwizard.validation.ValidationMethod;
import io.liftwizard.dropwizard.configuration.enabled.EnabledFactory;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonTypeName("compiler")
@AutoService(DomainModelFactory.class)
public class DomainModelCompilerFactory implements DomainModelFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(DomainModelCompilerFactory.class);

	@NotEmpty
	private @Valid @NotNull List<String> sourcePackages = Arrays.asList("klass.model.meta.domain");

	@NotEmpty
	private @NotNull String colorScheme;

	private @Valid @NotNull EnabledFactory ideLinksFactory = new EnabledFactory();

	private DomainModelWithSourceCode domainModel;

	@Nonnull
	@Override
	public DomainModelWithSourceCode createDomainModel(ObjectMapper objectMapper) {
		if (this.domainModel != null) {
			return this.domainModel;
		}
		ImmutableList<String> klassSourcePackagesImmutable = Lists.immutable.withAll(this.sourcePackages);

		AnsiColorScheme ansiColorScheme = ColorSchemeProvider.getByName(this.colorScheme);

		// TODO: We should use an abstract DomainModelFactory here, not necessarily the compiler.
		var domainModelLoader = new DomainModelCompilerLoader(
			klassSourcePackagesImmutable,
			Thread.currentThread().getContextClassLoader(),
			DomainModelCompilerLoader::logCompilerAnnotation,
			ansiColorScheme,
			this.ideLinksFactory.isEnabled()
		);
		this.domainModel = domainModelLoader.load();
		return this.domainModel;
	}

	@JsonProperty
	public List<String> getSourcePackages() {
		return Lists.mutable.withAll(this.sourcePackages);
	}

	@JsonProperty
	public void setSourcePackages(List<String> sourcePackages) {
		this.sourcePackages = sourcePackages;
	}

	@JsonProperty
	public String getColorScheme() {
		return this.colorScheme;
	}

	@JsonProperty
	public void setColorScheme(String colorScheme) {
		this.colorScheme = colorScheme;
	}

	@JsonProperty("ideLinks")
	public EnabledFactory getIdeLinksFactory() {
		return this.ideLinksFactory;
	}

	@JsonProperty("ideLinks")
	public void setIdeLinksFactory(EnabledFactory ideLinksFactory) {
		this.ideLinksFactory = ideLinksFactory;
	}

	@ValidationMethod(message = "Invalid color scheme. Valid options include 'dark', 'light', 'dark-cube', 'dark-rgb'.")
	@JsonIgnore
	public boolean isColorSchemeValid() {
		if (this.colorScheme == null) {
			return false;
		}

		boolean exists = ColorSchemeProvider.existsByName(this.colorScheme);
		if (!exists) {
			LOGGER.warn("Invalid color scheme '{}': color scheme not found", this.colorScheme);
		}
		return exists;
	}
}

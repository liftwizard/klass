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

package cool.klass.model.meta.domain;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.token.categories.TokenCategory;
import cool.klass.model.converter.compiler.token.categorizing.lexer.LexerBasedTokenCategorizer;
import cool.klass.model.converter.compiler.token.categorizing.parser.ParserBasedTokenCategorizer;
import cool.klass.model.meta.domain.AbstractClassifier.ClassifierBuilder;
import cool.klass.model.meta.domain.AssociationImpl.AssociationBuilder;
import cool.klass.model.meta.domain.EnumerationImpl.EnumerationBuilder;
import cool.klass.model.meta.domain.InterfaceImpl.InterfaceBuilder;
import cool.klass.model.meta.domain.KlassImpl.KlassBuilder;
import cool.klass.model.meta.domain.SourceCodeImpl.SourceCodeBuilderImpl;
import cool.klass.model.meta.domain.api.Association;
import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Enumeration;
import cool.klass.model.meta.domain.api.Interface;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.NamedElement;
import cool.klass.model.meta.domain.api.TopLevelElement;
import cool.klass.model.meta.domain.api.TopLevelElement.TopLevelElementBuilder;
import cool.klass.model.meta.domain.api.projection.Projection;
import cool.klass.model.meta.domain.api.service.ServiceGroup;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import cool.klass.model.meta.domain.api.source.ElementWithSourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.TopLevelElementWithSourceCode;
import cool.klass.model.meta.domain.projection.AbstractProjectionParent.AbstractProjectionParentBuilder;
import cool.klass.model.meta.domain.projection.ProjectionImpl.ProjectionBuilder;
import cool.klass.model.meta.domain.reference.DomainModelDeclarations;
import cool.klass.model.meta.domain.reference.DomainModelDeclarationsTopLevelElementVisitor;
import cool.klass.model.meta.domain.reference.DomainModelReferences;
import cool.klass.model.meta.domain.reference.DomainModelReferencesTopLevelElementVisitor;
import cool.klass.model.meta.domain.service.ServiceGroupImpl.ServiceGroupBuilder;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMapIterable;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;

public final class DomainModelImpl implements DomainModelWithSourceCode {

	@Nonnull
	private final ImmutableList<SourceCode> sourceCodes;

	@Nonnull
	private final MapIterable<Token, TokenCategory> tokenCategoriesFromLexer;

	@Nonnull
	private final MapIterable<Token, TokenCategory> tokenCategoriesFromParser;

	@Nonnull
	private final DomainModelDeclarations domainModelDeclarations;

	@Nonnull
	private final DomainModelReferences domainModelReferences;

	@Nonnull
	private final ImmutableList<TopLevelElement> topLevelElements;

	@Nonnull
	private final ImmutableList<Enumeration> enumerations;

	@Nonnull
	private final ImmutableList<Classifier> classifiers;

	@Nonnull
	private final ImmutableList<Interface> interfaces;

	@Nonnull
	private final ImmutableList<Klass> classes;

	@Nonnull
	private final ImmutableList<Association> associations;

	@Nonnull
	private final ImmutableList<Projection> projections;

	@Nonnull
	private final ImmutableList<ServiceGroup> serviceGroups;

	@Nonnull
	private final Optional<Klass> userClass;

	private final ImmutableMap<String, TopLevelElement> topLevelElementsByName;
	private final ImmutableMap<String, Enumeration> enumerationsByName;
	private final ImmutableMap<String, Interface> interfacesByName;
	private final ImmutableMap<String, Klass> classesByName;
	private final ImmutableMap<String, Association> associationsByName;
	private final ImmutableMap<String, Projection> projectionsByName;
	private final ImmutableMap<String, Classifier> classifiersByName;

	private DomainModelImpl(
		@Nonnull ImmutableList<SourceCode> sourceCodes,
		@Nonnull MapIterable<Token, TokenCategory> tokenCategoriesFromLexer,
		@Nonnull MapIterable<Token, TokenCategory> tokenCategoriesFromParser,
		@Nonnull DomainModelDeclarations domainModelDeclarations,
		@Nonnull DomainModelReferences domainModelReferences,
		@Nonnull ImmutableList<TopLevelElement> topLevelElements,
		@Nonnull ImmutableList<Enumeration> enumerations,
		@Nonnull ImmutableList<Classifier> classifiers,
		@Nonnull ImmutableList<Interface> interfaces,
		@Nonnull ImmutableList<Klass> classes,
		@Nonnull ImmutableList<Association> associations,
		@Nonnull ImmutableList<Projection> projections,
		@Nonnull ImmutableList<ServiceGroup> serviceGroups
	) {
		this.sourceCodes = Objects.requireNonNull(sourceCodes);
		this.tokenCategoriesFromLexer = Objects.requireNonNull(tokenCategoriesFromLexer);
		this.tokenCategoriesFromParser = Objects.requireNonNull(tokenCategoriesFromParser);
		this.domainModelDeclarations = Objects.requireNonNull(domainModelDeclarations);
		this.domainModelReferences = Objects.requireNonNull(domainModelReferences);
		this.topLevelElements = Objects.requireNonNull(topLevelElements);
		this.enumerations = Objects.requireNonNull(enumerations);
		this.classifiers = Objects.requireNonNull(classifiers);
		this.interfaces = Objects.requireNonNull(interfaces);
		this.classes = Objects.requireNonNull(classes);
		this.associations = Objects.requireNonNull(associations);
		this.projections = Objects.requireNonNull(projections);
		this.serviceGroups = Objects.requireNonNull(serviceGroups);

		this.userClass = this.classes.detectOptional(Klass::isUser);

		this.topLevelElementsByName = this.topLevelElements.groupByUniqueKey(NamedElement::getName).toImmutable();
		this.enumerationsByName = this.enumerations.groupByUniqueKey(NamedElement::getName).toImmutable();
		this.classifiersByName = this.classifiers.groupByUniqueKey(NamedElement::getName).toImmutable();
		this.interfacesByName = this.interfaces.groupByUniqueKey(NamedElement::getName).toImmutable();
		this.classesByName = this.classes.groupByUniqueKey(NamedElement::getName).toImmutable();
		this.associationsByName = this.associations.groupByUniqueKey(NamedElement::getName).toImmutable();
		this.projectionsByName = this.projections.groupByUniqueKey(NamedElement::getName).toImmutable();
	}

	@Override
	public ImmutableList<SourceCode> getSourceCodes() {
		return this.sourceCodes;
	}

	@Override
	public Optional<TokenCategory> getTokenCategory(Token token) {
		TokenCategory lexerCategory = this.tokenCategoriesFromLexer.get(token);
		TokenCategory parserCategory = this.tokenCategoriesFromParser.get(token);
		if (lexerCategory != null && parserCategory != null) {
			throw new AssertionError(token);
		}
		if (lexerCategory != null) {
			return Optional.of(lexerCategory);
		}
		if (parserCategory != null) {
			return Optional.of(parserCategory);
		}
		return Optional.empty();
	}

	@Nonnull
	public MapIterable<Token, TokenCategory> getTokenCategoriesFromLexer() {
		return this.tokenCategoriesFromLexer;
	}

	@Nonnull
	public MapIterable<Token, TokenCategory> getTokenCategoriesFromParser() {
		return this.tokenCategoriesFromParser;
	}

	@Override
	@Nonnull
	public Optional<ElementWithSourceCode> getElementByDeclaration(Token token) {
		return this.domainModelDeclarations.getElementByDeclaration(token);
	}

	@Override
	@Nonnull
	public Optional<ElementWithSourceCode> getElementByReference(Token token) {
		return this.domainModelReferences.getElementByReference(token);
	}

	@Nonnull
	public DomainModelReferences getDomainModelReferences() {
		return this.domainModelReferences;
	}

	@Override
	public Optional<Klass> getUserClass() {
		return this.userClass;
	}

	@Override
	@Nonnull
	public ImmutableList<TopLevelElement> getTopLevelElements() {
		return (ImmutableList<TopLevelElement>) (ImmutableList<?>) this.topLevelElements;
	}

	@Override
	@Nonnull
	public ImmutableList<Enumeration> getEnumerations() {
		return this.enumerations;
	}

	@Override
	@Nonnull
	public ImmutableList<Classifier> getClassifiers() {
		return this.classifiers;
	}

	@Override
	@Nonnull
	public ImmutableList<Interface> getInterfaces() {
		return this.interfaces;
	}

	@Override
	@Nonnull
	public ImmutableList<Klass> getClasses() {
		return this.classes;
	}

	@Override
	@Nonnull
	public ImmutableList<Association> getAssociations() {
		return this.associations;
	}

	@Override
	@Nonnull
	public ImmutableList<Projection> getProjections() {
		return this.projections;
	}

	@Override
	@Nonnull
	public ImmutableList<ServiceGroup> getServiceGroups() {
		return this.serviceGroups;
	}

	@Override
	public TopLevelElementWithSourceCode getTopLevelElementByName(String name) {
		return (TopLevelElementWithSourceCode) this.topLevelElementsByName.get(name);
	}

	@Override
	public Enumeration getEnumerationByName(String name) {
		return this.enumerationsByName.get(name);
	}

	@Override
	public Classifier getClassifierByName(String name) {
		return this.classifiersByName.get(name);
	}

	@Override
	public Interface getInterfaceByName(String name) {
		return this.interfacesByName.get(name);
	}

	@Override
	public Klass getClassByName(String name) {
		return this.classesByName.get(name);
	}

	@Override
	public Association getAssociationByName(String name) {
		return this.associationsByName.get(name);
	}

	@Override
	public Projection getProjectionByName(String name) {
		return this.projectionsByName.get(name);
	}

	public static final class DomainModelBuilder {

		@Nonnull
		private final ImmutableList<SourceCodeBuilderImpl> sourceCodeBuilders;

		@Nonnull
		private final ImmutableList<TopLevelElementBuilder> topLevelElementBuilders;

		@Nonnull
		private final ImmutableList<EnumerationBuilder> enumerationBuilders;

		@Nonnull
		private final ImmutableList<ClassifierBuilder<?>> classifierBuilders;

		@Nonnull
		private final ImmutableList<InterfaceBuilder> interfaceBuilders;

		@Nonnull
		private final ImmutableList<KlassBuilder> classBuilders;

		@Nonnull
		private final ImmutableList<AssociationBuilder> associationBuilders;

		@Nonnull
		private final ImmutableList<ProjectionBuilder> projectionBuilders;

		@Nonnull
		private final ImmutableList<ServiceGroupBuilder> serviceGroupBuilders;

		public DomainModelBuilder(
			@Nonnull ImmutableList<SourceCodeBuilderImpl> sourceCodeBuilders,
			@Nonnull ImmutableList<TopLevelElementBuilder> topLevelElementBuilders,
			@Nonnull ImmutableList<EnumerationBuilder> enumerationBuilders,
			@Nonnull ImmutableList<ClassifierBuilder<?>> classifierBuilders,
			@Nonnull ImmutableList<InterfaceBuilder> interfaceBuilders,
			@Nonnull ImmutableList<KlassBuilder> classBuilders,
			@Nonnull ImmutableList<AssociationBuilder> associationBuilders,
			@Nonnull ImmutableList<ProjectionBuilder> projectionBuilders,
			@Nonnull ImmutableList<ServiceGroupBuilder> serviceGroupBuilders
		) {
			this.sourceCodeBuilders = Objects.requireNonNull(sourceCodeBuilders);
			this.topLevelElementBuilders = Objects.requireNonNull(topLevelElementBuilders);
			this.enumerationBuilders = Objects.requireNonNull(enumerationBuilders);
			this.classifierBuilders = Objects.requireNonNull(classifierBuilders);
			this.interfaceBuilders = Objects.requireNonNull(interfaceBuilders);
			this.classBuilders = Objects.requireNonNull(classBuilders);
			this.associationBuilders = Objects.requireNonNull(associationBuilders);
			this.projectionBuilders = Objects.requireNonNull(projectionBuilders);
			this.serviceGroupBuilders = Objects.requireNonNull(serviceGroupBuilders);
		}

		@Nonnull
		public DomainModelImpl build() {
			ImmutableList<SourceCode> sourceCodes = this.sourceCodeBuilders.<SourceCode>collect(
				SourceCodeBuilderImpl::build
			).toImmutable();
			ImmutableList<Enumeration> enumerations = this.enumerationBuilders.<Enumeration>collect(
				EnumerationBuilder::build
			).toImmutable();
			ImmutableList<Interface> interfaces = this.interfaceBuilders.<Interface>collect(
				InterfaceBuilder::build
			).toImmutable();
			ImmutableList<Klass> classes = this.classBuilders.<Klass>collect(KlassBuilder::build).toImmutable();
			ImmutableList<Classifier> classifiers = this.classifierBuilders.collect(ClassifierBuilder::getElement);
			ImmutableList<Association> associations = this.associationBuilders.<Association>collect(
				AssociationBuilder::build
			).toImmutable();
			this.interfaceBuilders.each(InterfaceBuilder::build2);
			this.classBuilders.each(KlassBuilder::build2);
			this.interfaceBuilders.each(InterfaceBuilder::build3);
			this.classBuilders.each(KlassBuilder::build3);

			ImmutableList<Projection> projections = this.projectionBuilders.<Projection>collect(
				ProjectionBuilder::build
			).toImmutable();
			this.projectionBuilders.each(AbstractProjectionParentBuilder::build2);
			ImmutableList<ServiceGroup> serviceGroups = this.serviceGroupBuilders.<ServiceGroup>collect(
				ServiceGroupBuilder::build
			).toImmutable();
			ImmutableList<TopLevelElement> topLevelElements = this.topLevelElementBuilders.collect(
				TopLevelElementBuilder::getElement
			);

			MapIterable<Token, TokenCategory> tokenCategoriesFromLexer = this.getTokenCategoriesFromLexer(sourceCodes);
			MapIterable<Token, TokenCategory> tokenCategoriesFromParser = this.getTokenCategoriesFromParser(
				sourceCodes
			);

			ImmutableList<Token> duplicateTokens = tokenCategoriesFromLexer
				.keysView()
				.select(tokenCategoriesFromParser::containsKey)
				.toImmutableList();
			if (duplicateTokens.notEmpty()) {
				throw new AssertionError(duplicateTokens);
			}

			DomainModelDeclarations domainModelDeclarations = this.getDomainModelDeclarations(topLevelElements);
			DomainModelReferences domainModelReferences = this.getDomainModelReferences(topLevelElements);

			this.sourceCodeBuilders.forEach(SourceCodeBuilderImpl::build2);

			return new DomainModelImpl(
				sourceCodes,
				tokenCategoriesFromLexer,
				tokenCategoriesFromParser,
				domainModelDeclarations,
				domainModelReferences,
				topLevelElements,
				enumerations,
				classifiers,
				interfaces,
				classes,
				associations,
				projections,
				serviceGroups
			);
		}

		private MapIterable<Token, TokenCategory> getTokenCategoriesFromLexer(ImmutableList<SourceCode> sourceCodes) {
			MutableMapIterable<Token, TokenCategory> tokenCategoriesFromLexer = OrderedMapAdapter.adapt(
				new LinkedHashMap<>()
			);
			sourceCodes
				.collect(SourceCode::getTokenStream)
				.forEachWith(LexerBasedTokenCategorizer::findTokenCategoriesFromLexer, tokenCategoriesFromLexer);
			return tokenCategoriesFromLexer.asUnmodifiable();
		}

		private MapIterable<Token, TokenCategory> getTokenCategoriesFromParser(ImmutableList<SourceCode> sourceCodes) {
			var listener = new ParserBasedTokenCategorizer();

			sourceCodes
				.collect(SourceCode::getParserContext)
				.forEachWith(ParserBasedTokenCategorizer::findTokenCategoriesFromParser, listener);
			return listener.getTokenCategories();
		}

		private DomainModelDeclarations getDomainModelDeclarations(ImmutableList<TopLevelElement> topLevelElements) {
			DomainModelDeclarations domainModelDeclarations = new DomainModelDeclarations();
			for (TopLevelElement topLevelElement : topLevelElements) {
				topLevelElement.visit(new DomainModelDeclarationsTopLevelElementVisitor(domainModelDeclarations));
			}
			return domainModelDeclarations;
		}

		private DomainModelReferences getDomainModelReferences(ImmutableList<TopLevelElement> topLevelElements) {
			DomainModelReferences domainModelReferences = new DomainModelReferences();
			for (TopLevelElement topLevelElement : topLevelElements) {
				topLevelElement.visit(new DomainModelReferencesTopLevelElementVisitor(domainModelReferences));
			}
			return domainModelReferences;
		}
	}
}

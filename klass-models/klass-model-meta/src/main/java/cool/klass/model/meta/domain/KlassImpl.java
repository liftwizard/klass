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

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.NamedElement;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.property.Property;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import cool.klass.model.meta.domain.api.source.KlassWithSourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.property.AssociationEndImpl.AssociationEndBuilder;
import cool.klass.model.meta.grammar.KlassParser.ClassDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;

public final class KlassImpl extends AbstractClassifier implements KlassWithSourceCode {

	private final boolean isAbstract;
	private final boolean isUser;
	private final boolean isTransient;

	private ImmutableList<AssociationEnd> declaredAssociationEnds;
	private ImmutableMap<String, AssociationEnd> declaredAssociationEndsByName;
	private ImmutableList<AssociationEnd> associationEnds;
	private ImmutableMap<String, AssociationEnd> associationEndsByName;

	@Nonnull
	private Optional<AssociationEnd> versionProperty = Optional.empty();

	@Nonnull
	private Optional<AssociationEnd> versionedProperty = Optional.empty();

	private Optional<Klass> superClass;
	private ImmutableList<Klass> subClasses;

	private KlassImpl(
		@Nonnull ClassDeclarationContext elementContext,
		@Nonnull Optional<Element> macroElement,
		@Nullable SourceCode sourceCode,
		int ordinal,
		@Nonnull IdentifierContext nameContext,
		@Nonnull String packageName,
		boolean isAbstract,
		boolean isUser,
		boolean isTransient
	) {
		super(elementContext, macroElement, sourceCode, ordinal, nameContext, packageName);
		this.isAbstract = isAbstract;
		this.isUser = isUser;
		this.isTransient = isTransient;
	}

	@Nonnull
	@Override
	public ClassDeclarationContext getElementContext() {
		return (ClassDeclarationContext) super.getElementContext();
	}

	@Override
	@Nonnull
	public Optional<AssociationEnd> getVersionProperty() {
		return this.versionProperty;
	}

	@Override
	@Nonnull
	public Optional<AssociationEnd> getVersionedProperty() {
		return this.versionedProperty;
	}

	@Override
	public boolean isAbstract() {
		return this.isAbstract;
	}

	@Override
	public boolean isUser() {
		return this.isUser;
	}

	@Override
	public boolean isTransient() {
		return this.isTransient;
	}

	private void setDeclaredAssociationEnds(ImmutableList<AssociationEnd> declaredAssociationEnds) {
		if (this.declaredAssociationEnds != null) {
			throw new IllegalStateException();
		}
		this.declaredAssociationEnds = Objects.requireNonNull(declaredAssociationEnds);
		this.declaredAssociationEndsByName = this.declaredAssociationEnds.groupByUniqueKey(AssociationEnd::getName);
	}

	@Override
	public ImmutableList<AssociationEnd> getDeclaredAssociationEnds() {
		return Objects.requireNonNull(this.declaredAssociationEnds);
	}

	@Nonnull
	@Override
	public Optional<AssociationEnd> findDeclaredAssociationEndByName(String name) {
		return Optional.ofNullable(this.declaredAssociationEndsByName.get(name));
	}

	@Nonnull
	@Override
	public AssociationEnd getDeclaredAssociationEndByName(String name) {
		return this.declaredAssociationEndsByName.getIfAbsent(name, () -> {
				throw new IllegalStateException("No declared AssociationEnd named '" + name + "' on " + this.getName());
			});
	}

	private void setAssociationEnds(ImmutableList<AssociationEnd> associationEnds) {
		if (this.associationEnds != null) {
			throw new IllegalStateException();
		}
		this.associationEnds = Objects.requireNonNull(associationEnds);
		this.associationEndsByName = this.associationEnds.groupByUniqueKey(AssociationEnd::getName);

		this.versionProperty = this.associationEnds.detectOptional(AssociationEnd::isVersion);
		this.versionedProperty = this.associationEnds.detectOptional(AssociationEnd::isVersioned);
	}

	@Override
	public ImmutableList<AssociationEnd> getAssociationEnds() {
		return Objects.requireNonNull(this.associationEnds);
	}

	@Nonnull
	@Override
	public Optional<AssociationEnd> findAssociationEndByName(String name) {
		return Optional.ofNullable(this.associationEndsByName.get(name));
	}

	@Nonnull
	@Override
	public AssociationEnd getAssociationEndByName(String name) {
		return this.associationEndsByName.getIfAbsent(name, () -> {
				throw new IllegalStateException("No AssociationEnd named '" + name + "' on " + this.getName());
			});
	}

	@Nonnull
	@Override
	public Optional<Property> findPropertyByName(String name) {
		DataTypeProperty dataTypeProperty = this.dataTypePropertiesByName.get(name);
		AssociationEnd associationEnd = this.associationEndsByName.get(name);

		if (dataTypeProperty != null && associationEnd != null) {
			throw new IllegalStateException(
				"Property " + name + " is both a data type property and an association end on " + this.getName()
			);
		}

		if (dataTypeProperty != null) {
			return Optional.of(dataTypeProperty);
		}

		if (associationEnd != null) {
			return Optional.of(associationEnd);
		}

		return Optional.empty();
	}

	@Nonnull
	@Override
	public Property getPropertyByName(String name) {
		DataTypeProperty dataTypeProperty = this.dataTypePropertiesByName.get(name);
		AssociationEnd associationEnd = this.associationEndsByName.get(name);

		if (dataTypeProperty != null && associationEnd != null) {
			throw new IllegalStateException(
				"Property " + name + " is both a data type property and an association end on " + this.getName()
			);
		}

		if (dataTypeProperty != null) {
			return dataTypeProperty;
		}

		if (associationEnd != null) {
			return associationEnd;
		}

		throw new IllegalStateException("No Property named '" + name + "' on " + this.getName());
	}

	@Nonnull
	@Override
	public Optional<PrimitiveProperty> findPrimitivePropertyByName(String name) {
		DataTypeProperty dataTypeProperty = this.dataTypePropertiesByName.get(name);
		if (dataTypeProperty instanceof PrimitiveProperty primitiveProperty) {
			return Optional.of(primitiveProperty);
		}
		return Optional.empty();
	}

	@Nonnull
	@Override
	public PrimitiveProperty getPrimitivePropertyByName(String name) {
		DataTypeProperty dataTypeProperty = this.dataTypePropertiesByName.getIfAbsent(name, () -> {
				throw new IllegalStateException("No PrimitiveProperty named '" + name + "' on " + this.getName());
			});
		if (dataTypeProperty instanceof PrimitiveProperty primitiveProperty) {
			return primitiveProperty;
		}
		throw new IllegalStateException("No PrimitiveProperty named '" + name + "' on " + this.getName());
	}

	@Nonnull
	@Override
	public Optional<EnumerationProperty> findEnumerationPropertyByName(String name) {
		DataTypeProperty dataTypeProperty = this.dataTypePropertiesByName.get(name);
		if (dataTypeProperty instanceof EnumerationProperty enumerationProperty) {
			return Optional.of(enumerationProperty);
		}
		return Optional.empty();
	}

	@Nonnull
	@Override
	public EnumerationProperty getEnumerationPropertyByName(String name) {
		DataTypeProperty dataTypeProperty = this.dataTypePropertiesByName.getIfAbsent(name, () -> {
				throw new IllegalStateException("No EnumerationProperty named '" + name + "' on " + this.getName());
			});
		if (dataTypeProperty instanceof EnumerationProperty enumerationProperty) {
			return enumerationProperty;
		}
		throw new IllegalStateException("No EnumerationProperty named '" + name + "' on " + this.getName());
	}

	@Override
	@Nonnull
	public Optional<Klass> getSuperClass() {
		return this.superClass;
	}

	private void setSuperClass(Optional<Klass> superClass) {
		if (this.superClass != null) {
			throw new IllegalStateException();
		}
		this.superClass = Objects.requireNonNull(superClass);
	}

	@Override
	public ImmutableList<Klass> getSubClasses() {
		return Objects.requireNonNull(this.subClasses);
	}

	public void setSubClasses(ImmutableList<Klass> subClasses) {
		if (this.subClasses != null) {
			throw new IllegalStateException();
		}
		this.subClasses = Objects.requireNonNull(subClasses);
	}

	public static final class KlassBuilder extends ClassifierBuilder<KlassImpl> {

		private final boolean isAbstract;
		private final boolean isUser;
		private final boolean isTransient;

		@Nullable
		private ImmutableList<AssociationEndBuilder> declaredAssociationEnds;

		private Optional<KlassBuilder> superClass;
		private ImmutableList<KlassBuilder> subClasses;

		public KlassBuilder(
			@Nonnull ClassDeclarationContext elementContext,
			@Nonnull Optional<ElementBuilder<?>> macroElement,
			@Nullable SourceCodeBuilder sourceCode,
			int ordinal,
			@Nonnull IdentifierContext nameContext,
			@Nonnull String packageName,
			boolean isAbstract,
			boolean isUser,
			boolean isTransient
		) {
			super(elementContext, macroElement, sourceCode, ordinal, nameContext, packageName);
			this.isAbstract = isAbstract;
			this.isUser = isUser;
			this.isTransient = isTransient;
		}

		public void setDeclaredAssociationEnds(@Nonnull ImmutableList<AssociationEndBuilder> declaredAssociationEnds) {
			if (this.declaredAssociationEnds != null) {
				throw new IllegalStateException();
			}
			this.declaredAssociationEnds = Objects.requireNonNull(declaredAssociationEnds);
		}

		@Override
		@Nonnull
		protected KlassImpl buildUnsafe() {
			return new KlassImpl(
				(ClassDeclarationContext) this.elementContext,
				this.macroElement.map(ElementBuilder::getElement),
				this.sourceCode.build(),
				this.ordinal,
				this.getNameContext(),
				this.packageName,
				this.isAbstract,
				this.isUser,
				this.isTransient
			);
		}

		public void setSuperClass(@Nonnull Optional<KlassBuilder> superClass) {
			if (this.superClass != null) {
				throw new IllegalStateException();
			}
			this.superClass = Objects.requireNonNull(superClass);
		}

		public void setSubClassBuilders(ImmutableList<KlassBuilder> subClasses) {
			if (this.subClasses != null) {
				throw new IllegalStateException();
			}
			this.subClasses = Objects.requireNonNull(subClasses);
		}

		@Override
		public void build2() {
			super.build2();

			ImmutableList<AssociationEnd> declaredAssociationEnds = this.declaredAssociationEnds.<
				AssociationEnd
			>collect(AssociationEndBuilder::getElement).toImmutable();
			this.element.setDeclaredAssociationEnds(declaredAssociationEnds);

			Optional<Klass> maybeSuperClass = this.superClass.map(ElementBuilder::getElement);
			this.element.setSuperClass(maybeSuperClass);
			ImmutableList<Klass> subClasses = this.subClasses.collect(ElementBuilder::getElement);
			this.element.setSubClasses(subClasses);

			ImmutableList<AssociationEnd> associationEnds = maybeSuperClass
				.map(Klass::getAssociationEnds)
				.orElseGet(Lists.immutable::empty)
				.newWithAll(declaredAssociationEnds)
				.toReversed()
				.distinctBy(NamedElement::getName)
				.toReversed();

			this.element.setAssociationEnds(associationEnds);
		}

		@Override
		protected ImmutableList<DataTypeProperty> getDataTypeProperties() {
			ImmutableList<DataTypeProperty> declaredDataTypeProperties = this.declaredDataTypeProperties.collect(
				(property) -> property.getElement()
			);

			ImmutableList<DataTypeProperty> interfaceProperties = this.declaredInterfaces.collect(
					ElementBuilder::getElement
				)
				.flatCollect(Classifier::getDataTypeProperties)
				.toImmutable();

			ImmutableList<DataTypeProperty> superClassProperties = this.superClass.map(ElementBuilder::getElement)
				.map(Classifier::getDataTypeProperties)
				.orElseGet(Lists.immutable::empty);

			ImmutableList<DataTypeProperty> allDataTypeProperties = interfaceProperties
				.newWithAll(superClassProperties)
				.newWithAll(declaredDataTypeProperties);

			ImmutableList<DataTypeProperty> result = allDataTypeProperties
				.toReversed()
				.distinctBy(NamedElement::getName)
				.toReversed();

			return result;
		}

		@Override
		protected ImmutableList<ReferenceProperty> getReferenceProperties() {
			ImmutableList<ReferenceProperty> declaredReferenceProperties = this.declaredReferenceProperties.collect(
				(property) -> property.getElement()
			);

			ImmutableList<ReferenceProperty> superClassProperties = this.superClass.map(
				KlassBuilder::getReferenceProperties
			).orElseGet(Lists.immutable::empty);

			ImmutableList<ReferenceProperty> interfaceProperties = this.declaredInterfaces.collect(
					ElementBuilder::getElement
				)
				.flatCollect(Classifier::getReferenceProperties)
				.toImmutable();

			ImmutableList<ReferenceProperty> allReferenceProperties = interfaceProperties
				.newWithAll(superClassProperties)
				.newWithAll(declaredReferenceProperties);

			ImmutableList<ReferenceProperty> result = allReferenceProperties
				.toReversed()
				.distinctBy(NamedElement::getName)
				.toReversed();

			return result;
		}

		@Override
		public KlassImpl getType() {
			return Objects.requireNonNull(this.element);
		}
	}
}

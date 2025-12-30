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

package cool.klass.model.converter.compiler.state.property;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.AnnotationSeverity;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.AntlrElement;
import cool.klass.model.converter.compiler.state.AntlrIdentifierElement;
import cool.klass.model.converter.compiler.state.AntlrType;
import cool.klass.model.meta.domain.property.AbstractProperty.PropertyBuilder;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;

public abstract class AntlrProperty extends AntlrIdentifierElement {

	@Nonnull
	private final MutableList<AntlrModifier> modifiers = Lists.mutable.empty();

	private final MutableOrderedMap<String, MutableList<AntlrModifier>> modifiersByName = OrderedMapAdapter.adapt(
		new LinkedHashMap<>()
	);
	private final MutableOrderedMap<ParserRuleContext, AntlrModifier> modifiersByContext = OrderedMapAdapter.adapt(
		new LinkedHashMap<>()
	);

	protected AntlrProperty(
		@Nonnull ParserRuleContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		int ordinal,
		@Nonnull IdentifierContext nameContext
	) {
		super(elementContext, compilationUnit, ordinal, nameContext);
	}

	@Override
	public Pair<Token, Token> getContextBefore() {
		return this.getEntireContext();
	}

	@Nonnull
	public abstract AntlrType getType();

	@Nonnull
	public abstract PropertyBuilder<?, ?, ?> build();

	@Override
	@Nonnull
	public abstract PropertyBuilder<?, ?, ?> getElementBuilder();

	@Nonnull
	public abstract AntlrClassifier getOwningClassifier();

	public boolean isVersion() {
		return this.getModifiers().anySatisfy(AntlrModifier::isVersion);
	}

	public boolean isPrivate() {
		return this.getModifiers().anySatisfy(AntlrModifier::isPrivate);
	}

	public boolean isCreatedBy() {
		return this.getModifiers().anySatisfy(AntlrModifier::isCreatedBy);
	}

	public boolean isFinal() {
		return this.getModifiers().anySatisfy(AntlrModifier::isFinal);
	}

	public boolean isLastUpdatedBy() {
		return this.getModifiers().anySatisfy(AntlrModifier::isLastUpdatedBy);
	}

	public boolean isDerived() {
		return this.getModifiers().anySatisfy(AntlrModifier::isDerived);
	}

	public int getNumModifiers() {
		return this.modifiers.size();
	}

	@Nonnull
	public ListIterable<AntlrModifier> getModifiers() {
		return this.modifiers.asUnmodifiable();
	}

	public void enterModifier(@Nonnull AntlrModifier modifier) {
		Objects.requireNonNull(modifier);
		this.modifiers.add(modifier);
		this.modifiersByName.getIfAbsentPut(modifier.getKeyword(), Lists.mutable::empty).add(modifier);

		AntlrModifier duplicate = this.modifiersByContext.put(modifier.getElementContext(), modifier);
		if (duplicate != null) {
			throw new AssertionError();
		}
	}

	public ImmutableList<AntlrModifier> getModifiersByName(String modifierName) {
		MutableList<AntlrModifier> result = this.modifiersByName.get(modifierName);
		return result == null ? Lists.immutable.empty() : result.toImmutable();
	}

	// <editor-fold desc="Report Compiler Errors">
	@OverridingMethodsMustInvokeSuper
	public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		this.reportDuplicateModifiers(compilerAnnotationHolder);
		this.reportDuplicateAuditModifiers(compilerAnnotationHolder);
		this.reportInvalidAuditProperties(compilerAnnotationHolder);
		this.reportDerivedPrivateProperty(compilerAnnotationHolder);
	}

	private void reportDuplicateModifiers(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		MutableBag<String> duplicateModifiers = this.getModifiers()
			.asLazy()
			.collect(AntlrModifier::getKeyword)
			.toBag()
			.selectDuplicates();

		for (AntlrModifier modifier : this.getModifiers()) {
			if (duplicateModifiers.contains(modifier.getKeyword())) {
				String message = String.format("Duplicate modifier '%s'.", modifier.getKeyword());
				compilerAnnotationHolder.add("ERR_DUP_MOD", message, modifier);
			}
		}
	}

	protected void reportDuplicateAuditModifiers(CompilerAnnotationHolder compilerAnnotationHolder) {
		if (this.isCreatedBy() && this.isLastUpdatedBy()) {
			ImmutableList<AntlrModifier> modifiers = this.getModifiers()
				.select((modifier) -> modifier.isCreatedBy() || modifier.isLastUpdatedBy())
				.toImmutable();
			ImmutableList<ParserRuleContext> modifierContexts = modifiers.collect(AntlrElement::getElementContext);
			String message = "Property may not have both 'createdBy' and lastUpdatedBy' modifiers.";
			compilerAnnotationHolder.add("ERR_CBY_LBY", message, this, modifierContexts);
		}
	}

	@OverridingMethodsMustInvokeSuper
	protected void reportInvalidAuditProperties(CompilerAnnotationHolder compilerAnnotationHolder) {
		if (this.isCreatedBy() && this.isLastUpdatedBy()) {
			return;
		}

		if (this.isCreatedBy() && !this.isFinal()) {
			AntlrModifier modifier = this.getModifiers().detect(AntlrModifier::isCreatedBy);

			String message = String.format("Expected createdBy property '%s' to be final.", this);
			compilerAnnotationHolder.add(
				"ERR_CRT_FIN",
				message,
				this,
				Lists.immutable.with(modifier.getElementContext())
			);
		}

		if (this.isLastUpdatedBy() && this.isFinal()) {
			ImmutableList<ParserRuleContext> parserRuleContexts = this.getModifiers()
				.select((antlrModifier) -> antlrModifier.isLastUpdatedBy() || antlrModifier.isFinal())
				.collect(AntlrElement::getElementContext)
				.toImmutable();
			String message = String.format("Expected lastUpdatedBy property '%s' to not be final.", this);
			compilerAnnotationHolder.add("ERR_LUB_NFI", message, this, parserRuleContexts);
		}
	}

	public final void reportDuplicateMemberName(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		String message = String.format(
			"Duplicate member: '%s.%s'.",
			this.getOwningClassifier().getName(),
			this.getName()
		);

		compilerAnnotationHolder.add("ERR_DUP_PRP", message, this);
	}

	@OverridingMethodsMustInvokeSuper
	public void reportAuditErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		this.reportAuditErrors(compilerAnnotationHolder, this.modifiers, this);
	}

	public void reportDuplicatePropertyWithModifiers(
		@Nonnull CompilerAnnotationHolder compilerAnnotationHolder,
		ImmutableList<String> modifierStrings
	) {
		ImmutableList<AntlrModifier> modifiers = modifierStrings.flatCollect(this::getModifiersByName);

		String message = String.format(
			"Multiple properties on '%s' with modifiers %s.",
			this.getOwningClassifier().getName(),
			modifierStrings
		);

		compilerAnnotationHolder.add("ERR_PRP_MOD", message, this, modifiers.collect(AntlrElement::getElementContext));
	}

	public void reportUnreferencedPrivateProperty(CompilerAnnotationHolder compilerAnnotationHolder) {
		boolean isAudit = this.isCreatedBy() || this.isLastUpdatedBy();
		String prefix = isAudit ? "Audit" : "Private";
		var severity = isAudit ? AnnotationSeverity.ERROR : AnnotationSeverity.WARNING;
		this.reportUnreferencedPrivateProperty(compilerAnnotationHolder, prefix, severity);
	}

	private void reportUnreferencedPrivateProperty(
		CompilerAnnotationHolder compilerAnnotationHolder,
		String prefix,
		AnnotationSeverity severity
	) {
		String message = "%s property '%s.%s' is not referenced in any criteria.".formatted(
			prefix,
			this.getOwningClassifier().getName(),
			this.getName()
		);
		compilerAnnotationHolder.add("ERR_PRP_REF", message, this, severity);
	}

	public void reportDerivedPrivateProperty(CompilerAnnotationHolder compilerAnnotationHolder) {
		if (this.isDerived() && this.isPrivate()) {
			AntlrModifier derivedModifier = this.getModifiers().detect(AntlrModifier::isDerived);
			AntlrModifier privateModifier = this.getModifiers().detect(AntlrModifier::isPrivate);

			String message = String.format(
				"Derived property '%s' may not be private. Remove the 'private' modifier.",
				this
			);
			compilerAnnotationHolder.add(
				"ERR_DRV_PRI",
				message,
				this,
				Lists.immutable.with(derivedModifier.getElementContext(), privateModifier.getElementContext())
			);
		}
	}

	// </editor-fold>

	@Override
	protected Pattern getNamePattern() {
		return MEMBER_NAME_PATTERN;
	}

	@Override
	public String toString() {
		return String.format("%s.%s", this.getOwningClassifier().getName(), this.getShortString());
	}

	public String getShortString() {
		MutableList<String> sourceCodeStrings = Lists.mutable.empty();

		String typeSourceCode = this.getType().getName();
		sourceCodeStrings.add(typeSourceCode);

		this.getModifiers().asLazy().collect(AntlrElement::toString).into(sourceCodeStrings);

		return String.format("%s: %s", this.getName(), sourceCodeStrings.makeString(" "));
	}

	@Override
	public boolean isContext() {
		return true;
	}

	public abstract String getTypeName();
}

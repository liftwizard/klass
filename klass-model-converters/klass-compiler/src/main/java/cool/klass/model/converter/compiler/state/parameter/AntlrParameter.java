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

package cool.klass.model.converter.compiler.state.parameter;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrEnumeration;
import cool.klass.model.converter.compiler.state.AntlrIdentifierElement;
import cool.klass.model.converter.compiler.state.AntlrMultiplicity;
import cool.klass.model.converter.compiler.state.AntlrMultiplicityOwner;
import cool.klass.model.converter.compiler.state.AntlrType;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.property.AntlrModifier;
import cool.klass.model.converter.compiler.state.property.AntlrParameterizedProperty;
import cool.klass.model.meta.domain.api.DataType.DataTypeGetter;
import cool.klass.model.meta.domain.parameter.ParameterImpl.ParameterBuilder;
import cool.klass.model.meta.grammar.KlassParser.EnumerationParameterDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;

// TODO: Specific subclasses for the specific antlr context types
public class AntlrParameter extends AntlrIdentifierElement implements AntlrMultiplicityOwner {

	public static final AntlrParameter AMBIGUOUS = new AntlrParameter(
		new ParserRuleContext(AMBIGUOUS_PARENT, -1),
		Optional.empty(),
		-1,
		AMBIGUOUS_IDENTIFIER_CONTEXT,
		AntlrEnumeration.AMBIGUOUS,
		AntlrParameterizedProperty.AMBIGUOUS
	) {
		@Override
		public String toString() {
			return "Ambiguous parameter";
		}
	};

	public static final AntlrParameter NOT_FOUND = new AntlrParameter(
		new ParserRuleContext(NOT_FOUND_PARENT, -1),
		Optional.empty(),
		-1,
		NOT_FOUND_IDENTIFIER_CONTEXT,
		AntlrEnumeration.NOT_FOUND,
		AntlrParameterizedProperty.AMBIGUOUS
	) {
		@Override
		public String toString() {
			return "Not found parameter";
		}
	};

	@Nonnull
	private final IAntlrElement parameterOwner;

	@Nonnull
	private final AntlrType type;

	// TODO: Factor modifiers into type checking
	private final MutableList<AntlrModifier> modifiers = Lists.mutable.empty();

	@Nullable
	private AntlrMultiplicity multiplicity;

	@Nullable
	private ParameterBuilder elementBuilder;

	public AntlrParameter(
		@Nonnull ParserRuleContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		int ordinal,
		@Nonnull IdentifierContext nameContext,
		@Nonnull AntlrType type,
		@Nonnull IAntlrElement parameterOwner
	) {
		super(elementContext, compilationUnit, ordinal, nameContext);
		this.type = Objects.requireNonNull(type);
		this.parameterOwner = Objects.requireNonNull(parameterOwner);
	}

	@Nonnull
	@Override
	public Optional<IAntlrElement> getSurroundingElement() {
		return Optional.of(this.parameterOwner);
	}

	@Override
	public Pair<Token, Token> getContextBefore() {
		return this.getEntireContext();
	}

	@Override
	protected Pattern getNamePattern() {
		return MEMBER_NAME_PATTERN;
	}

	public int getNumModifiers() {
		return this.modifiers.size();
	}

	@Override
	public void enterMultiplicity(@Nonnull AntlrMultiplicity multiplicity) {
		if (this.multiplicity != null) {
			throw new IllegalStateException();
		}
		this.multiplicity = Objects.requireNonNull(multiplicity);
	}

	public void enterModifier(AntlrModifier modifier) {
		this.modifiers.add(modifier);
	}

	// <editor-fold desc="Report Compiler Errors">
	public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		this.reportNameErrors(compilerAnnotationHolder);
		this.reportTypeErrors(compilerAnnotationHolder);
	}

	private void reportTypeErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		if (this.type != AntlrEnumeration.NOT_FOUND) {
			return;
		}

		EnumerationReferenceContext offendingToken =
			((EnumerationParameterDeclarationContext) this.getElementContext()).enumerationReference();
		String message = String.format("Cannot find enumeration '%s'.", offendingToken.getText());
		compilerAnnotationHolder.add("ERR_ENM_PAR", message, this, offendingToken);
	}

	public void reportDuplicateParameterName(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		String message = String.format("Duplicate parameter: '%s'.", this.getName());
		compilerAnnotationHolder.add("ERR_DUP_PAR", message, this);
	}

	// </editor-fold>

	@Nonnull
	public AntlrType getType() {
		return this.type;
	}

	@Nonnull
	public ParameterBuilder build() {
		if (this.elementBuilder != null) {
			throw new IllegalStateException();
		}
		this.elementBuilder = new ParameterBuilder(
			this.elementContext,
			this.getMacroElementBuilder(),
			this.getSourceCodeBuilder(),
			this.ordinal,
			this.getNameContext(),
			// TODO: Fuller interface hierarchy with AntlrType, AntlrDataType, etc.
			(DataTypeGetter) this.type.getElementBuilder(),
			this.multiplicity.getMultiplicity()
		);
		return this.elementBuilder;
	}

	@Override
	@Nonnull
	public ParameterBuilder getElementBuilder() {
		return Objects.requireNonNull(this.elementBuilder);
	}
}

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

package cool.klass.model.converter.compiler.state.value;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEnd;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.meta.domain.value.AbstractMemberReferencePath.AbstractMemberReferencePathBuilder;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ImmutableList;

public abstract class AntlrMemberReferencePath extends AntlrExpressionValue {

	@Nonnull
	protected final AntlrClass klass;

	@Nonnull
	protected final ImmutableList<AntlrAssociationEnd> associationEnd;

	@Nonnull
	protected final AntlrDataTypeProperty<?> dataTypeProperty;

	protected AntlrMemberReferencePath(
		@Nonnull ParserRuleContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		@Nonnull AntlrClass klass,
		@Nonnull ImmutableList<AntlrAssociationEnd> associationEnd,
		@Nonnull AntlrDataTypeProperty<?> dataTypeProperty,
		@Nonnull IAntlrElement expressionValueOwner
	) {
		super(elementContext, compilationUnit, expressionValueOwner);
		this.klass = Objects.requireNonNull(klass);
		this.associationEnd = Objects.requireNonNull(associationEnd);
		this.dataTypeProperty = Objects.requireNonNull(dataTypeProperty);
	}

	@Nonnull
	public AntlrClass getKlass() {
		return this.klass;
	}

	@Nonnull
	public ImmutableList<AntlrAssociationEnd> getAssociationEnds() {
		return this.associationEnd;
	}

	@Nonnull
	public AntlrDataTypeProperty<?> getDataTypeProperty() {
		return this.dataTypeProperty;
	}

	@Nonnull
	@Override
	public abstract AbstractMemberReferencePathBuilder<?> build();

	@Nullable
	protected AntlrClass reportErrorsAssociationEnds(
		@Nonnull CompilerAnnotationHolder compilerAnnotationHolder,
		@Nonnull List<AssociationEndReferenceContext> associationEndReferenceContexts
	) {
		AntlrClass currentClass = this.klass;
		for (int i = 0; i < this.associationEnd.size(); i++) {
			AntlrAssociationEnd associationEnd = this.associationEnd.get(i);
			if (associationEnd == AntlrAssociationEnd.NOT_FOUND) {
				IdentifierContext identifier = associationEndReferenceContexts.get(i).identifier();
				String message = String.format(
					"Cannot find member '%s.%s'.",
					currentClass.getName(),
					identifier.getText()
				);
				compilerAnnotationHolder.add("ERR_MEM_EXP", message, this, identifier);
				return null;
			}
			currentClass = associationEnd.getType();
		}
		return currentClass;
	}
}

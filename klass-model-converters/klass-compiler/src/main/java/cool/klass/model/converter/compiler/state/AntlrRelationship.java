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

package cool.klass.model.converter.compiler.state;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.AnnotationSeverity;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.criteria.AntlrCriteria;
import cool.klass.model.meta.grammar.KlassParser.RelationshipContext;

public class AntlrRelationship
	extends AntlrElement
{
	private final AntlrAssociation association;

	private AntlrCriteria criteria;

	public AntlrRelationship(
		@Nonnull RelationshipContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		AntlrAssociation association
	)
	{
		super(elementContext, compilationUnit);
		this.association = Objects.requireNonNull(association);
	}

	@Nonnull
	@Override
	public RelationshipContext getElementContext()
	{
		return (RelationshipContext) super.getElementContext();
	}

	public void setCriteria(AntlrCriteria criteria)
	{
		if (this.criteria != null)
		{
			throw new IllegalStateException();
		}
		this.criteria = Objects.requireNonNull(criteria);
	}

	public AntlrCriteria getCriteria()
	{
		return this.criteria;
	}

	@Nonnull
	@Override
	public Optional<IAntlrElement> getSurroundingElement()
	{
		return Optional.ofNullable(this.association);
	}

	@Override
	public boolean isContext()
	{
		return true;
	}

	@Override
	public TokenRange getContextBefore()
	{
		return this.getEntireContext();
	}

	// <editor-fold desc="Report Compiler Errors">
	public void reportErrors(CompilerAnnotationHolder compilerAnnotationHolder)
	{
		this.reportInferredRelationship(compilerAnnotationHolder);
		this.criteria.reportErrors(compilerAnnotationHolder);
	}

	private void reportInferredRelationship(CompilerAnnotationHolder compilerAnnotationHolder)
	{
		if (this.compilationUnit.flatMap(CompilationUnit::getMacroElement).isPresent())
		{
			return;
		}

		var inferredRelationshipText = this.association.getInferredRelationshipText();
		if (inferredRelationshipText.isEmpty())
		{
			return;
		}

		var criteriaExpressionContext = this.getElementContext().criteriaExpression();
		var sourceText = AntlrRelationship.withoutSpaces(AntlrElement.getSourceText(criteriaExpressionContext));

		if (AntlrRelationship.withoutSpaces(inferredRelationshipText.get()).equals(sourceText))
		{
			compilerAnnotationHolder.add(
				"ERR_REL_INF",
				"Relationship in association '%s' is inferred and can be removed.".formatted(
					this.association.getName()
				),
				this.criteria,
				criteriaExpressionContext,
				AnnotationSeverity.WARNING
			);
		}
	}

	private static String withoutSpaces(String text)
	{
		return text.replaceAll("\\s+", "");
	}

	// </editor-fold>
}

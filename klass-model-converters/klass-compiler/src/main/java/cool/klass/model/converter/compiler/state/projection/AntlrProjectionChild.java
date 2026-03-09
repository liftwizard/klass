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

package cool.klass.model.converter.compiler.state.projection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.meta.domain.projection.AbstractProjectionElement.ProjectionChildBuilder;
import cool.klass.model.meta.grammar.KlassParser.ClassifierReferenceContext;

public interface AntlrProjectionChild extends AntlrProjectionElement {
	@Nonnull
	@Override
	ProjectionChildBuilder build();

	@Nonnull
	AntlrClassifier getDeclaringClassifier();

	default void reportRedundantClassifierQualifier(
		@Nonnull CompilerAnnotationHolder compilerAnnotationHolder,
		@Nullable ClassifierReferenceContext classifierReferenceContext,
		boolean canResolveWithoutQualifier
	) {
		if (classifierReferenceContext == null || !canResolveWithoutQualifier) {
			return;
		}

		String message = String.format(
			"Redundant classifier qualifier '%s' on projection member '%s'. The property can be resolved without the qualifier.",
			classifierReferenceContext.getText(),
			this.getName()
		);
		compilerAnnotationHolder.add("ERR_PRJ_CRF", message, this, classifierReferenceContext);
	}
}

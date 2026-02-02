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

import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.property.AntlrModifier;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

public interface IAntlrElement {
	@Nonnull
	ParserRuleContext getElementContext();

	@Nonnull
	Optional<AntlrElement> getMacroElement();

	@Nonnull
	Optional<IAntlrElement> getSurroundingElement();

	default <T extends IAntlrElement> Optional<T> getSurroundingElement(Class<T> elementClass) {
		if (elementClass.isInstance(this)) {
			return Optional.of(elementClass.cast(this));
		}

		return this.getSurroundingElement().flatMap((surroundingElement) ->
			surroundingElement.getSurroundingElement(elementClass)
		);
	}

	@Nonnull
	default ImmutableList<IAntlrElement> getSurroundingElements() {
		MutableList<IAntlrElement> result = Lists.mutable.empty();
		this.gatherSurroundingElements(result);
		return result.toImmutable();
	}

	default void gatherSurroundingElements(@Nonnull MutableList<IAntlrElement> result) {
		result.add(this);
		this.getSurroundingElement().ifPresent((element) -> element.gatherSurroundingElements(result));
	}

	default boolean isContext() {
		return false;
	}

	@Nonnull
	Optional<CompilationUnit> getCompilationUnit();

	default Pair<Token, Token> getContextBefore() {
		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".getContextBefore() not implemented yet"
		);
	}

	default Pair<Token, Token> getContextAfter() {
		// This makes the default implementation throw, but still not need overrides just to return null
		this.getContextBefore();
		return null;
	}

	default Pair<Token, Token> getEntireContext() {
		return Tuples.pair(this.getElementContext().getStart(), this.getElementContext().getStop());
	}

	default void reportAuditErrors(
		CompilerAnnotationHolder compilerAnnotationHolder,
		ListIterable<AntlrModifier> modifiers,
		IAntlrElement element
	) {
		ImmutableList<AntlrModifier> offendingModifiers = modifiers
			.select((modifier) -> modifier.isAudit() || modifier.isUser())
			.toImmutable();
		if (offendingModifiers.isEmpty()) {
			return;
		}

		String message = String.format(
			"Modifiers %s require one 'user' class in the domain model.",
			offendingModifiers.collect(AntlrModifier::getKeyword)
		);
		compilerAnnotationHolder.add(
			"ERR_ADT_MOD",
			message,
			element,
			offendingModifiers.collect(AntlrElement::getElementContext)
		);
	}
}

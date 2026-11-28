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

package cool.klass.model.converter.compiler.phase;

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.phase.criteria.ExpressionValueVisitor;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.property.AntlrModifier;
import cool.klass.model.converter.compiler.state.value.AntlrThisMemberReferencePath;
import cool.klass.model.meta.grammar.KlassParser.ObjectStorageModifierContext;

/**
 * Resolves the digest path in {@code objectStorage(this.digest, "algorithm")}. Runs after the association phases, so a path with association hops resolves and can be reported as a hop instead of as a missing member.
 */
public class ObjectStoragePhase
	extends AbstractCompilerPhase
{
	public ObjectStoragePhase(@Nonnull CompilerState compilerState)
	{
		super(compilerState);
	}

	@Override
	@OverridingMethodsMustInvokeSuper
	public void enterObjectStorageModifier(@Nonnull ObjectStorageModifierContext ctx)
	{
		super.enterObjectStorageModifier(ctx);

		AntlrClassifier classifier = this.compilerState.getCompilerWalk().getClassifier();
		if (!(classifier instanceof AntlrClass))
		{
			// Only classes store instances, so an interface property reports ERR_OBS_INT
			return;
		}

		AntlrDataTypeProperty<?> property = this.compilerState.getCompilerWalk().getDataTypeProperty();
		AntlrModifier modifier = this.compilerState.getCompilerWalk().getDataTypePropertyModifier();

		var expressionValueVisitor = new ExpressionValueVisitor(this.compilerState, classifier, modifier);
		AntlrThisMemberReferencePath path = expressionValueVisitor.visitThisMemberReferencePath(
			ctx.thisMemberReferencePath()
		);
		property.setObjectStorage(path, ctx.digestAlgorithm());
	}
}

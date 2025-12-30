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

package cool.klass.model.converter.compiler.phase;

import java.util.List;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.AntlrInterface;
import cool.klass.model.meta.grammar.KlassParser.ClassReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ExtendsDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.ImplementsDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceReferenceContext;

public class InheritancePhase extends AbstractCompilerPhase {

	public InheritancePhase(@Nonnull CompilerState compilerState) {
		super(compilerState);
	}

	@Override
	public void enterExtendsDeclaration(@Nonnull ExtendsDeclarationContext ctx) {
		super.enterExtendsDeclaration(ctx);

		ClassReferenceContext classReferenceContext = ctx.classReference();
		IdentifierContext identifier = classReferenceContext.identifier();
		String className = identifier.getText();
		AntlrClass superClass = this.compilerState.getDomainModel().getClassByName(className);

		AntlrClass klass = this.compilerState.getCompilerWalk().getKlass();
		klass.enterExtendsDeclaration(superClass);
	}

	@Override
	public void enterImplementsDeclaration(@Nonnull ImplementsDeclarationContext ctx) {
		super.enterImplementsDeclaration(ctx);

		AntlrClassifier classifier = this.compilerState.getCompilerWalk().getClassifier();

		List<InterfaceReferenceContext> interfaceReferenceContexts = ctx.interfaceReference();
		for (InterfaceReferenceContext interfaceReferenceContext : interfaceReferenceContexts) {
			IdentifierContext identifier = interfaceReferenceContext.identifier();
			String interfaceName = identifier.getText();
			AntlrInterface iface = this.compilerState.getDomainModel().getInterfaceByName(interfaceName);

			classifier.enterImplementsDeclaration(iface);
		}
	}
}

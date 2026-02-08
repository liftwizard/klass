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

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.AntlrIdentifierElement;
import cool.klass.model.converter.compiler.state.AntlrInterface;
import cool.klass.model.meta.domain.projection.AbstractProjectionParent;
import cool.klass.model.meta.domain.projection.AbstractProjectionParent.AbstractProjectionParentBuilder;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;

public abstract class AntlrProjectionParent extends AntlrIdentifierElement {

	@Nonnull
	protected final AntlrClassifier classifier;

	protected final MutableList<AntlrProjectionChild> children = Lists.mutable.empty();

	protected AntlrProjectionParent(
		@Nonnull ParserRuleContext elementContext,
		@Nonnull Optional<CompilationUnit> compilationUnit,
		int ordinal,
		@Nonnull IdentifierContext nameContext,
		@Nonnull AntlrClassifier classifier
	) {
		super(elementContext, compilationUnit, ordinal, nameContext);
		this.classifier = Objects.requireNonNull(classifier);
	}

	@Override
	@Nonnull
	public abstract AbstractProjectionParentBuilder<? extends AbstractProjectionParent> getElementBuilder();

	public MutableList<AntlrProjectionChild> getChildren() {
		return this.children.asUnmodifiable();
	}

	@Nonnull
	public AntlrClassifier getClassifier() {
		return this.classifier;
	}

	public int getNumChildren() {
		return this.children.size();
	}

	public void enterAntlrProjectionMember(@Nonnull AntlrProjectionChild child) {
		this.children.add(child);
	}

	protected ImmutableSet<AntlrProjectionChild> getDuplicateMembers() {
		MutableSet<AntlrProjectionChild> duplicates = Sets.mutable.empty();
		MutableListMultimap<String, AntlrProjectionChild> byName = this.children.groupBy(
			AntlrProjectionElement::getName
		);

		byName.forEachKeyMultiValues((name, members) -> {
			MutableList<AntlrProjectionChild> list = Lists.mutable.withAll(members);
			if (list.size() < 2) {
				return;
			}
			for (int i = 0; i < list.size(); i++) {
				for (int j = i + 1; j < list.size(); j++) {
					AntlrClassifier classA = list.get(i).getDeclaringClassifier();
					AntlrClassifier classB = list.get(j).getDeclaringClassifier();
					if (areOverlappingClassifiers(classA, classB)) {
						duplicates.add(list.get(i));
						duplicates.add(list.get(j));
					}
				}
			}
		});
		return duplicates.toImmutable();
	}

	private static boolean areOverlappingClassifiers(AntlrClassifier a, AntlrClassifier b) {
		if (
			!(a instanceof AntlrClass || a instanceof AntlrInterface)
			|| !(b instanceof AntlrClass || b instanceof AntlrInterface)
		) {
			return true;
		}
		return a.isSubTypeOf(b) || b.isSubTypeOf(a);
	}

	public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
		ImmutableSet<AntlrProjectionChild> duplicateMembers = this.getDuplicateMembers();

		for (AntlrProjectionChild projectionMember : this.children) {
			if (duplicateMembers.contains(projectionMember)) {
				projectionMember.reportDuplicateMemberName(compilerAnnotationHolder);
			}
		}
	}
}

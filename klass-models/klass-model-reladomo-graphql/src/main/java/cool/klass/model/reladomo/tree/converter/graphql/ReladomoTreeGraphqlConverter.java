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

package cool.klass.model.reladomo.tree.converter.graphql;

import java.util.List;
import java.util.Objects;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.reladomo.tree.DataTypePropertyReladomoTreeNode;
import cool.klass.model.reladomo.tree.ReferencePropertyReladomoTreeNode;
import cool.klass.model.reladomo.tree.ReladomoTreeNode;
import cool.klass.model.reladomo.tree.RootReladomoTreeNode;
import cool.klass.model.reladomo.tree.SubClassReladomoTreeNode;
import cool.klass.model.reladomo.tree.SuperClassReladomoTreeNode;
import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.SelectedField;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.stack.MutableStack;

public final class ReladomoTreeGraphqlConverter {

	public static final Converter<String, String> UPPER_TO_LOWER_CAMEL = CaseFormat.UPPER_CAMEL.converterTo(
		CaseFormat.LOWER_CAMEL
	);

	private final DomainModel domainModel;

	public ReladomoTreeGraphqlConverter(DomainModel domainModel) {
		this.domainModel = Objects.requireNonNull(domainModel);
	}

	public RootReladomoTreeNode convert(Klass klass, DataFetchingFieldSelectionSet selectionSet) {
		RootReladomoTreeNode result = new RootReladomoTreeNode("root", klass);

		for (SelectedField selectedField : selectionSet.getImmediateFields()) {
			this.convertSelectedField(selectedField, result);
		}

		return result;
	}

	private void convertSelectedField(SelectedField selectedField, ReladomoTreeNode parentTreeNode) {
		String name = selectedField.getName();
		List<String> objectTypeNames = selectedField.getObjectTypeNames();

		List<SelectedField> childrenFields = selectedField.getSelectionSet().getImmediateFields();

		Klass commonClass = this.findCommonSuperClass(objectTypeNames);

		ReladomoTreeNode inheritancePath = ReladomoTreeGraphqlConverter.getInheritancePath(parentTreeNode, commonClass);
		ReladomoTreeNode eachParentTreeNode = inheritancePath;
		Klass eachClass = (Klass) eachParentTreeNode.getType();

		if (childrenFields.isEmpty()) {
			if (name.equals("__typename")) {
				return;
			}
			DataTypeProperty dataTypeProperty = eachClass.getDataTypePropertyByName(name);
			if (dataTypeProperty == null) {
				String detailMessage = "Expected " + name + " to be a DataTypeProperty on " + eachClass.getName();
				throw new AssertionError(detailMessage);
			}

			while (dataTypeProperty.getOwningClassifier() != eachClass && eachClass.getSuperClass().isPresent()) {
				Klass superClass = eachClass.getSuperClass().get();
				String superClassName = UPPER_TO_LOWER_CAMEL.convert(superClass.getName()) + "SuperClass";
				var nextReladomoNode = new SuperClassReladomoTreeNode(superClassName, eachClass, superClass);
				ReladomoTreeNode nextTreeNode = eachParentTreeNode.computeChild(superClassName, nextReladomoNode);

				eachParentTreeNode = nextTreeNode;
				eachClass = superClass;
			}

			var dataTypePropertyReladomoTreeNode = new DataTypePropertyReladomoTreeNode(name, dataTypeProperty);
			eachParentTreeNode.computeChild(name, dataTypePropertyReladomoTreeNode);
			return;
		}

		AssociationEnd associationEnd = eachClass.getAssociationEndByName(name);
		if (associationEnd == null) {
			String detailMessage = "Expected " + name + " to be an AssociationEnd on " + eachClass.getName();
			throw new AssertionError(detailMessage);
		}

		while (associationEnd.getOwningClassifier() != eachClass && eachClass.getSuperClass().isPresent()) {
			Klass superClass = eachClass.getSuperClass().get();
			String superClassName = UPPER_TO_LOWER_CAMEL.convert(superClass.getName()) + "SuperClass";
			var nextReladomoNode = new SuperClassReladomoTreeNode(superClassName, eachClass, superClass);
			ReladomoTreeNode nextTreeNode = eachParentTreeNode.computeChild(superClassName, nextReladomoNode);

			eachParentTreeNode = nextTreeNode;
			eachClass = superClass;
		}

		var associationEndReladomoTreeNode = new ReferencePropertyReladomoTreeNode(name, associationEnd);

		ReladomoTreeNode reladomoTreeNode = eachParentTreeNode.computeChild(name, associationEndReladomoTreeNode);

		for (SelectedField childSelectedField : childrenFields) {
			this.convertSelectedField(childSelectedField, reladomoTreeNode);
		}
	}

	private Klass findCommonSuperClass(List<String> objectTypeNames) {
		ImmutableList<Klass> classes = Lists.immutable
			.withAll(objectTypeNames)
			.collect(this.domainModel::getClassByName);

		if (classes.size() == 1) {
			return classes.getOnly();
		}

		MutableBag<Klass> classCounts = classes.flatCollect(Klass::getSuperClassChainWithThis).toBag();

		Klass result = classes
			.getFirst()
			.getSuperClassChainWithThis()
			.detect((each) -> classCounts.occurrencesOf(each) == classes.size());
		return Objects.requireNonNull(result);
	}

	public static ReladomoTreeNode getInheritancePath(ReladomoTreeNode reladomoNode, Classifier end) {
		ReladomoTreeNode eachReladomoNode = reladomoNode;
		Classifier start = (Classifier) reladomoNode.getType();

		if (start.isStrictSubTypeOf(end)) {
			Klass eachKlass = (Klass) start;
			while (eachKlass != end) {
				Klass superClass = eachKlass.getSuperClass().orElse(null);
				if (superClass == null) {
					return eachReladomoNode;
				}
				String name = UPPER_TO_LOWER_CAMEL.convert(superClass.getName()) + "SuperClass";
				var nextReladomoNode = new SuperClassReladomoTreeNode(name, eachKlass, superClass);
				eachReladomoNode = eachReladomoNode.computeChild(name, nextReladomoNode);
				eachKlass = superClass;
			}
			return eachReladomoNode;
		}

		if (start.isStrictSuperTypeOf(end)) {
			MutableStack<Klass> stack = Stacks.mutable.empty();
			Klass eachClassifier = (Klass) end;
			while (eachClassifier != start) {
				Klass superClass = eachClassifier.getSuperClass().get();
				stack.push(eachClassifier);
				eachClassifier = superClass;
			}

			Klass eachClassifier2 = (Klass) start;
			while (stack.notEmpty()) {
				Klass eachSubClass = stack.pop();
				String name = UPPER_TO_LOWER_CAMEL.convert(eachSubClass.getName()) + "SubClass";
				var nextReladomoNode = new SubClassReladomoTreeNode(name, eachClassifier2, eachSubClass);
				eachReladomoNode = eachReladomoNode.computeChild(name, nextReladomoNode);
				eachClassifier2 = eachSubClass;
			}

			return eachReladomoNode;
		}

		return start == end ? eachReladomoNode : null;
	}
}

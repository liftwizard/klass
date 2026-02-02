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

package cool.klass.model.meta.domain.reference;

import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Association;
import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Enumeration;
import cool.klass.model.meta.domain.api.EnumerationLiteral;
import cool.klass.model.meta.domain.api.Interface;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.TopLevelElementVisitor;
import cool.klass.model.meta.domain.api.projection.Projection;
import cool.klass.model.meta.domain.api.property.Property;
import cool.klass.model.meta.domain.api.service.ServiceGroup;
import cool.klass.model.meta.domain.api.source.AssociationWithSourceCode;
import cool.klass.model.meta.domain.api.source.EnumerationLiteralWithSourceCode;
import cool.klass.model.meta.domain.api.source.EnumerationWithSourceCode;
import cool.klass.model.meta.domain.api.source.InterfaceWithSourceCode;
import cool.klass.model.meta.domain.api.source.KlassWithSourceCode;
import cool.klass.model.meta.domain.api.source.projection.ProjectionWithSourceCode;
import cool.klass.model.meta.domain.api.source.service.ServiceGroupWithSourceCode;
import cool.klass.model.meta.grammar.KlassParser.AssociationDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ClassDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceGroupDeclarationContext;
import org.eclipse.collections.api.list.ImmutableList;

public class DomainModelDeclarationsTopLevelElementVisitor implements TopLevelElementVisitor {

	@Nonnull
	private final DomainModelDeclarations domainModelDeclarations;

	public DomainModelDeclarationsTopLevelElementVisitor(@Nonnull DomainModelDeclarations domainModelDeclarations) {
		this.domainModelDeclarations = Objects.requireNonNull(domainModelDeclarations);
	}

	@Override
	public void visitEnumeration(Enumeration enumeration) {
		EnumerationWithSourceCode element = (EnumerationWithSourceCode) enumeration;
		EnumerationDeclarationContext declaration = element.getElementContext();
		this.domainModelDeclarations.addEnumerationDeclaration(declaration, element);

		for (EnumerationLiteral enumerationLiteral : enumeration.getEnumerationLiterals()) {
			this.visitEnumerationLiteral(enumerationLiteral);
		}
	}

	private void visitEnumerationLiteral(EnumerationLiteral enumerationLiteral) {
		EnumerationLiteralWithSourceCode element = (EnumerationLiteralWithSourceCode) enumerationLiteral;
		EnumerationLiteralContext declaration = element.getElementContext();
		this.domainModelDeclarations.addEnumerationLiteralDeclaration(declaration, element);
	}

	@Override
	public void visitInterface(Interface anInterface) {
		InterfaceWithSourceCode element = (InterfaceWithSourceCode) anInterface;
		InterfaceDeclarationContext declaration = element.getElementContext();
		this.domainModelDeclarations.addInterfaceDeclaration(declaration, element);

		this.visitClassifier(anInterface);
	}

	@Override
	public void visitKlass(Klass klass) {
		KlassWithSourceCode element = (KlassWithSourceCode) klass;
		ClassDeclarationContext declaration = element.getElementContext();
		this.domainModelDeclarations.addKlassDeclaration(declaration, element);

		this.visitClassifier(klass);
	}

	private void visitClassifier(Classifier classifier) {
		ImmutableList<Property> properties = classifier.getDeclaredProperties();
		for (Property property : properties) {
			property.visit(new DomainModelDeclarationsPropertyVisitor(this.domainModelDeclarations));
		}
	}

	@Override
	public void visitAssociation(Association association) {
		AssociationWithSourceCode element = (AssociationWithSourceCode) association;
		AssociationDeclarationContext declaration = element.getElementContext();
		this.domainModelDeclarations.addAssociationDeclaration(declaration, element);
		// Don't need to visit association ends. We get those on the Classifier.
	}

	@Override
	public void visitProjection(Projection projection) {
		ProjectionWithSourceCode element = (ProjectionWithSourceCode) projection;
		ProjectionDeclarationContext declaration = element.getElementContext();
		this.domainModelDeclarations.addProjectionDeclaration(declaration, element);
	}

	@Override
	public void visitServiceGroup(ServiceGroup serviceGroup) {
		ServiceGroupWithSourceCode element = (ServiceGroupWithSourceCode) serviceGroup;
		ServiceGroupDeclarationContext declaration = element.getElementContext();
		this.domainModelDeclarations.addServiceGroupDeclaration(declaration, element);
	}
}

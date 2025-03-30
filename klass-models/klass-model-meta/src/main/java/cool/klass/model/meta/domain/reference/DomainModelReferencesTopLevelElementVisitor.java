/*
 * Copyright 2024 Craig Motlin
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
import cool.klass.model.meta.domain.api.Interface;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.TopLevelElementVisitor;
import cool.klass.model.meta.domain.api.criteria.Criteria;
import cool.klass.model.meta.domain.api.order.OrderBy;
import cool.klass.model.meta.domain.api.order.OrderByMemberReferencePath;
import cool.klass.model.meta.domain.api.projection.Projection;
import cool.klass.model.meta.domain.api.projection.ProjectionChild;
import cool.klass.model.meta.domain.api.property.Property;
import cool.klass.model.meta.domain.api.service.Service;
import cool.klass.model.meta.domain.api.service.ServiceGroup;
import cool.klass.model.meta.domain.api.service.ServiceProjectionDispatch;
import cool.klass.model.meta.domain.api.service.url.Url;
import cool.klass.model.meta.domain.api.source.ClassifierWithSourceCode;
import cool.klass.model.meta.domain.api.source.KlassWithSourceCode;
import cool.klass.model.meta.domain.api.source.projection.ProjectionWithSourceCode;
import cool.klass.model.meta.domain.api.source.service.ServiceGroupWithSourceCode;
import cool.klass.model.meta.domain.api.source.service.ServiceProjectionDispatchWithSourceCode;
import cool.klass.model.meta.domain.api.value.ThisMemberReferencePath;
import cool.klass.model.meta.grammar.KlassParser.ClassReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ClassifierReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceGroupDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceProjectionDispatchContext;
import org.eclipse.collections.api.list.ImmutableList;

public class DomainModelReferencesTopLevelElementVisitor implements TopLevelElementVisitor {

    @Nonnull
    private final DomainModelReferences domainModelReferences;

    public DomainModelReferencesTopLevelElementVisitor(@Nonnull DomainModelReferences domainModelReferences) {
        this.domainModelReferences = Objects.requireNonNull(domainModelReferences);
    }

    @Override
    public void visitEnumeration(@Nonnull Enumeration enumeration) {
        // Deliberately empty
    }

    @Override
    public void visitInterface(@Nonnull Interface anInterface) {
        this.visitClassifier(anInterface);
    }

    @Override
    public void visitKlass(@Nonnull Klass klass) {
        this.visitClassifier(klass);
    }

    private void visitClassifier(@Nonnull Classifier classifier) {
        ImmutableList<Property> properties = classifier.getDeclaredProperties();
        for (Property property : properties) {
            property.visit(new DomainModelReferencesPropertyVisitor(this.domainModelReferences));
        }
    }

    @Override
    public void visitAssociation(@Nonnull Association association) {
        // Don't need to visit association ends. We get those on the Classifier.

        Criteria criteria = association.getCriteria();
        this.visitCriteria(criteria);
    }

    @Override
    public void visitProjection(@Nonnull Projection projection) {
        ProjectionWithSourceCode elementWithSourceCode = (ProjectionWithSourceCode) projection;
        ProjectionDeclarationContext elementContext = elementWithSourceCode.getElementContext();
        ClassifierReferenceContext reference = elementContext.classifierReference();
        ClassifierWithSourceCode classifier = elementWithSourceCode.getClassifier();

        this.domainModelReferences.addClassifierReference(reference, classifier);

        for (ProjectionChild projectionChild : projection.getChildren()) {
            projectionChild.visit(new DomainModelReferencesProjectionVisitor(this.domainModelReferences));
        }
    }

    @Override
    public void visitServiceGroup(@Nonnull ServiceGroup serviceGroup) {
        ServiceGroupWithSourceCode elementWithSourceCode = (ServiceGroupWithSourceCode) serviceGroup;
        ServiceGroupDeclarationContext elementContext = elementWithSourceCode.getElementContext();
        ClassReferenceContext reference = elementContext.classReference();
        KlassWithSourceCode klass = elementWithSourceCode.getKlass();

        this.domainModelReferences.addClassReference(reference, klass);

        for (Url url : serviceGroup.getUrls()) {
            this.visitUrl(url);
        }
    }

    public void visitUrl(@Nonnull Url url) {
        // TODO: Parameter declarations

        ImmutableList<Service> services = url.getServices();
        for (Service service : services) {
            this.visitService(service);
        }
    }

    public void visitService(@Nonnull Service service) {
        service.getQueryCriteria().ifPresent(this::visitCriteria);
        service.getAuthorizeCriteria().ifPresent(this::visitCriteria);
        service.getValidateCriteria().ifPresent(this::visitCriteria);
        service.getConflictCriteria().ifPresent(this::visitCriteria);

        service.getProjectionDispatch().ifPresent(this::visitProjectionDispatch);

        service.getOrderBy().ifPresent(this::visitOrderBy);
    }

    public void visitCriteria(Criteria criteria) {
        criteria.visit(new DomainModelReferencesCriteriaVisitor(this.domainModelReferences));
    }

    public void visitProjectionDispatch(ServiceProjectionDispatch serviceProjectionDispatch) {
        ServiceProjectionDispatchWithSourceCode elementWithSourceCode =
            (ServiceProjectionDispatchWithSourceCode) serviceProjectionDispatch;
        ServiceProjectionDispatchContext elementContext = elementWithSourceCode.getElementContext();
        ProjectionReferenceContext reference = elementContext.projectionReference();
        ProjectionWithSourceCode projection = elementWithSourceCode.getProjection();

        this.domainModelReferences.addProjectionReference(reference, projection);
    }

    public void visitOrderBy(OrderBy orderBy) {
        ImmutableList<OrderByMemberReferencePath> orderByMemberReferencePaths =
            orderBy.getOrderByMemberReferencePaths();
        for (OrderByMemberReferencePath orderByMemberReferencePath : orderByMemberReferencePaths) {
            ThisMemberReferencePath thisMemberReferencePath = orderByMemberReferencePath.getThisMemberReferencePath();
            thisMemberReferencePath.visit(new DomainModelReferencesExpressionValueVisitor(this.domainModelReferences));
        }
    }
}

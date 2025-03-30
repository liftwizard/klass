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

package cool.klass.model.converter.compiler.state;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.parser.AntlrUtils;
import cool.klass.model.converter.compiler.state.criteria.AntlrCriteria;
import cool.klass.model.converter.compiler.state.criteria.AntlrCriteriaVisitor;
import cool.klass.model.converter.compiler.state.order.AntlrOrderBy;
import cool.klass.model.converter.compiler.state.order.AntlrOrderByOwner;
import cool.klass.model.converter.compiler.state.order.AntlrOrderByVisitor;
import cool.klass.model.converter.compiler.state.projection.AntlrProjection;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEnd;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.property.AntlrParameterizedProperty;
import cool.klass.model.converter.compiler.state.property.AntlrProperty;
import cool.klass.model.converter.compiler.state.service.AntlrService;
import cool.klass.model.converter.compiler.state.service.AntlrServiceCriteria;
import cool.klass.model.converter.compiler.state.service.AntlrServiceGroup;
import cool.klass.model.converter.compiler.state.service.url.AntlrUrl;
import cool.klass.model.meta.domain.AbstractClassifier.ClassifierBuilder;
import cool.klass.model.meta.domain.AssociationImpl.AssociationBuilder;
import cool.klass.model.meta.domain.DomainModelImpl.DomainModelBuilder;
import cool.klass.model.meta.domain.EnumerationImpl.EnumerationBuilder;
import cool.klass.model.meta.domain.InterfaceImpl.InterfaceBuilder;
import cool.klass.model.meta.domain.KlassImpl.KlassBuilder;
import cool.klass.model.meta.domain.SourceCodeImpl.SourceCodeBuilderImpl;
import cool.klass.model.meta.domain.api.TopLevelElement.TopLevelElementBuilder;
import cool.klass.model.meta.domain.projection.ProjectionImpl.ProjectionBuilder;
import cool.klass.model.meta.domain.service.ServiceGroupImpl.ServiceGroupBuilder;
import cool.klass.model.meta.grammar.KlassParser.AssociationDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ClassDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.CompilationUnitContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceGroupDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.TopLevelDeclarationContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;

public class AntlrDomainModel {

    private final MutableList<AntlrCompilationUnit> compilationUnits = Lists.mutable.empty();
    private final MutableList<AntlrTopLevelElement> topLevelElements = Lists.mutable.empty();
    private final MutableList<AntlrEnumeration> enumerations = Lists.mutable.empty();
    private final MutableList<AntlrClassifier> classifiers = Lists.mutable.empty();
    private final MutableList<AntlrInterface> interfaces = Lists.mutable.empty();
    private final MutableList<AntlrClass> klasses = Lists.mutable.empty();
    private final MutableList<AntlrClass> userClasses = Lists.mutable.empty();
    private final MutableList<AntlrAssociation> associations = Lists.mutable.empty();
    private final MutableList<AntlrProjection> projections = Lists.mutable.empty();
    private final MutableList<AntlrServiceGroup> serviceGroups = Lists.mutable.empty();

    private final MutableOrderedMap<CompilationUnitContext, AntlrCompilationUnit> compilationUnitsByContext =
        OrderedMapAdapter.adapt(new LinkedHashMap<>());

    private final MutableOrderedMap<TopLevelDeclarationContext, AntlrTopLevelElement> topLevelElementsByContext =
        OrderedMapAdapter.adapt(new LinkedHashMap<>());
    private final MutableOrderedMap<TopLevelDeclarationContext, Integer> topLevelElementOrdinalsByContext =
        OrderedMapAdapter.adapt(new LinkedHashMap<>());

    private final MutableOrderedMap<EnumerationDeclarationContext, AntlrEnumeration> enumerationsByContext =
        OrderedMapAdapter.adapt(new LinkedHashMap<>());
    private final MutableOrderedMap<ParserRuleContext, AntlrClassifier> classifiersByContext = OrderedMapAdapter.adapt(
        new LinkedHashMap<>()
    );
    private final MutableOrderedMap<InterfaceDeclarationContext, AntlrInterface> interfacesByContext =
        OrderedMapAdapter.adapt(new LinkedHashMap<>());
    private final MutableOrderedMap<ClassDeclarationContext, AntlrClass> classesByContext = OrderedMapAdapter.adapt(
        new LinkedHashMap<>()
    );
    private final MutableOrderedMap<AssociationDeclarationContext, AntlrAssociation> associationsByContext =
        OrderedMapAdapter.adapt(new LinkedHashMap<>());
    private final MutableOrderedMap<ProjectionDeclarationContext, AntlrProjection> projectionsByContext =
        OrderedMapAdapter.adapt(new LinkedHashMap<>());
    private final MutableOrderedMap<ServiceGroupDeclarationContext, AntlrServiceGroup> serviceGroupsByContext =
        OrderedMapAdapter.adapt(new LinkedHashMap<>());

    private final MutableOrderedMap<String, AntlrEnumeration> enumerationsByName = OrderedMapAdapter.adapt(
        new LinkedHashMap<>()
    );
    private final MutableOrderedMap<String, AntlrClassifier> classifiersByName = OrderedMapAdapter.adapt(
        new LinkedHashMap<>()
    );
    private final MutableOrderedMap<String, AntlrInterface> interfacesByName = OrderedMapAdapter.adapt(
        new LinkedHashMap<>()
    );
    private final MutableOrderedMap<String, AntlrClass> classesByName = OrderedMapAdapter.adapt(new LinkedHashMap<>());
    private final MutableOrderedMap<String, AntlrAssociation> associationsByName = OrderedMapAdapter.adapt(
        new LinkedHashMap<>()
    );
    private final MutableOrderedMap<String, AntlrProjection> projectionsByName = OrderedMapAdapter.adapt(
        new LinkedHashMap<>()
    );

    // TODO: Or instead of embedding services inside classes, turn them into named elements. The name of the group can become the name of the resource. The group could have a base url.
    private final MutableOrderedMap<AntlrClass, AntlrServiceGroup> serviceGroupsByClass = OrderedMapAdapter.adapt(
        new LinkedHashMap<>()
    );

    public Optional<AntlrClass> getUserClass() {
        return this.userClasses.size() == 1 ? this.userClasses.getFirstOptional() : Optional.empty();
    }

    public void enterTopLevelDeclaration(TopLevelDeclarationContext ctx) {
        Integer duplicate =
            this.topLevelElementOrdinalsByContext.put(ctx, this.topLevelElementOrdinalsByContext.size() + 1);

        if (duplicate != null) {
            throw new AssertionError();
        }
    }

    public void exitCompilationUnit(@Nonnull AntlrCompilationUnit compilationUnit) {
        this.compilationUnits.add(compilationUnit);
        AntlrCompilationUnit duplicateCompilationUnit =
            this.compilationUnitsByContext.put(compilationUnit.getElementContext(), compilationUnit);
        if (duplicateCompilationUnit != null) {
            throw new AssertionError();
        }
    }

    public void exitEnumerationDeclaration(@Nonnull AntlrEnumeration enumeration) {
        this.topLevelElements.add(enumeration);
        this.enumerations.add(enumeration);
        this.enumerationsByName.compute(
                enumeration.getName(),
                (name, builder) -> builder == null ? enumeration : AntlrEnumeration.AMBIGUOUS
            );

        AntlrEnumeration duplicateEnumeration =
            this.enumerationsByContext.put(enumeration.getElementContext(), enumeration);
        if (duplicateEnumeration != null) {
            throw new AssertionError();
        }

        TopLevelDeclarationContext topLevelDeclarationContext = AntlrUtils.getParentOfType(
            enumeration.getElementContext(),
            TopLevelDeclarationContext.class
        );
        AntlrTopLevelElement duplicateTopLevelElement =
            this.topLevelElementsByContext.put(topLevelDeclarationContext, enumeration);
        if (duplicateTopLevelElement != null) {
            throw new AssertionError();
        }
    }

    public void exitInterfaceDeclaration(@Nonnull AntlrInterface iface) {
        this.topLevelElements.add(iface);
        this.classifiers.add(iface);
        this.interfaces.add(iface);

        this.classifiersByName.compute(
                iface.getName(),
                (name, builder) -> builder == null ? iface : AntlrClassifier.AMBIGUOUS
            );
        this.interfacesByName.compute(
                iface.getName(),
                (name, builder) -> builder == null ? iface : AntlrInterface.AMBIGUOUS
            );

        AntlrClassifier duplicateClassifier = this.classifiersByContext.put(iface.getElementContext(), iface);
        if (duplicateClassifier != null) {
            throw new AssertionError();
        }

        AntlrInterface duplicateInterface = this.interfacesByContext.put(iface.getElementContext(), iface);
        if (duplicateInterface != null) {
            throw new AssertionError();
        }

        TopLevelDeclarationContext topLevelDeclarationContext = AntlrUtils.getParentOfType(
            iface.getElementContext(),
            TopLevelDeclarationContext.class
        );
        AntlrTopLevelElement duplicateTopLevelElement =
            this.topLevelElementsByContext.put(topLevelDeclarationContext, iface);
        if (duplicateTopLevelElement != null) {
            throw new AssertionError();
        }
    }

    public void exitClassDeclaration(@Nonnull AntlrClass klass) {
        this.topLevelElements.add(klass);
        this.classifiers.add(klass);
        this.klasses.add(klass);

        if (klass.isUser()) {
            this.userClasses.add(klass);
        }

        this.classifiersByName.compute(
                klass.getName(),
                (name, builder) -> builder == null ? klass : AntlrClassifier.AMBIGUOUS
            );
        this.classesByName.compute(klass.getName(), (name, builder) -> builder == null ? klass : AntlrClass.AMBIGUOUS);

        AntlrClassifier duplicateClassifier = this.classifiersByContext.put(klass.getElementContext(), klass);
        if (duplicateClassifier != null) {
            throw new AssertionError();
        }

        AntlrClass duplicateClass = this.classesByContext.put(klass.getElementContext(), klass);
        if (duplicateClass != null) {
            throw new AssertionError();
        }

        TopLevelDeclarationContext topLevelDeclarationContext = AntlrUtils.getParentOfType(
            klass.getElementContext(),
            TopLevelDeclarationContext.class
        );
        AntlrTopLevelElement duplicateTopLevelElement =
            this.topLevelElementsByContext.put(topLevelDeclarationContext, klass);
        if (duplicateTopLevelElement != null) {
            throw new AssertionError();
        }
    }

    public void exitAssociationDeclaration(@Nonnull AntlrAssociation association) {
        this.topLevelElements.add(association);
        this.associations.add(association);
        this.associationsByName.compute(
                association.getName(),
                (name, builder) -> builder == null ? association : AntlrAssociation.AMBIGUOUS
            );

        AntlrAssociation duplicateAssociation =
            this.associationsByContext.put(association.getElementContext(), association);
        if (duplicateAssociation != null) {
            throw new AssertionError();
        }

        TopLevelDeclarationContext topLevelDeclarationContext = AntlrUtils.getParentOfType(
            association.getElementContext(),
            TopLevelDeclarationContext.class
        );
        AntlrTopLevelElement duplicateTopLevelElement =
            this.topLevelElementsByContext.put(topLevelDeclarationContext, association);
        if (duplicateTopLevelElement != null) {
            throw new AssertionError();
        }
    }

    public void exitProjectionDeclaration(@Nonnull AntlrProjection projection) {
        this.topLevelElements.add(projection);
        this.projections.add(projection);
        this.projectionsByName.compute(
                projection.getName(),
                (name, builder) -> builder == null ? projection : AntlrProjection.AMBIGUOUS
            );

        AntlrProjection duplicateProjection = this.projectionsByContext.put(projection.getElementContext(), projection);
        if (duplicateProjection != null) {
            throw new AssertionError();
        }

        TopLevelDeclarationContext topLevelDeclarationContext = AntlrUtils.getParentOfType(
            projection.getElementContext(),
            TopLevelDeclarationContext.class
        );
        AntlrTopLevelElement duplicateTopLevelElement =
            this.topLevelElementsByContext.put(topLevelDeclarationContext, projection);
        if (duplicateTopLevelElement != null) {
            throw new AssertionError();
        }
    }

    public void exitServiceGroupDeclaration(@Nonnull AntlrServiceGroup serviceGroup) {
        this.topLevelElements.add(serviceGroup);
        this.serviceGroups.add(serviceGroup);
        this.serviceGroupsByClass.compute(
                serviceGroup.getKlass(),
                (name, builder) -> builder == null ? serviceGroup : AntlrServiceGroup.AMBIGUOUS
            );

        AntlrServiceGroup duplicateServiceGroup =
            this.serviceGroupsByContext.put(serviceGroup.getElementContext(), serviceGroup);
        if (duplicateServiceGroup != null) {
            throw new AssertionError();
        }

        TopLevelDeclarationContext topLevelDeclarationContext = AntlrUtils.getParentOfType(
            serviceGroup.getElementContext(),
            TopLevelDeclarationContext.class
        );
        AntlrTopLevelElement duplicateTopLevelElement =
            this.topLevelElementsByContext.put(topLevelDeclarationContext, serviceGroup);
        if (duplicateTopLevelElement != null) {
            throw new AssertionError();
        }
    }

    public AntlrEnumeration getEnumerationByName(String enumerationName) {
        return this.enumerationsByName.getIfAbsentValue(enumerationName, AntlrEnumeration.NOT_FOUND);
    }

    public AntlrClassifier getClassifierByName(String classifierName) {
        return this.classifiersByName.getIfAbsentValue(classifierName, AntlrClassifier.NOT_FOUND);
    }

    public AntlrInterface getInterfaceByName(String interfaceName) {
        return this.interfacesByName.getIfAbsentValue(interfaceName, AntlrInterface.NOT_FOUND);
    }

    public AntlrClass getClassByName(String className) {
        return this.classesByName.getIfAbsentValue(className, AntlrClass.NOT_FOUND);
    }

    public AntlrProjection getProjectionByName(String projectionName) {
        return this.projectionsByName.getIfAbsentValue(projectionName, AntlrProjection.NOT_FOUND);
    }

    public AntlrCompilationUnit getCompilationUnitByContext(CompilationUnitContext context) {
        return this.compilationUnitsByContext.get(context);
    }

    public AntlrTopLevelElement getTopLevelElementByContext(TopLevelDeclarationContext context) {
        return this.topLevelElementsByContext.get(context);
    }

    public Integer getTopLevelElementOrdinalByContext(TopLevelDeclarationContext context) {
        return this.topLevelElementOrdinalsByContext.get(context);
    }

    public AntlrEnumeration getEnumerationByContext(EnumerationDeclarationContext context) {
        return this.enumerationsByContext.get(context);
    }

    public AntlrClassifier getClassifierByContext(ParserRuleContext context) {
        return this.classifiersByContext.get(context);
    }

    public AntlrInterface getInterfaceByContext(InterfaceDeclarationContext context) {
        return this.interfacesByContext.get(context);
    }

    public AntlrClass getClassByContext(ClassDeclarationContext context) {
        return this.classesByContext.get(context);
    }

    public AntlrAssociation getAssociationByContext(AssociationDeclarationContext context) {
        return this.associationsByContext.get(context);
    }

    public AntlrProjection getProjectionByContext(ProjectionDeclarationContext context) {
        return this.projectionsByContext.get(context);
    }

    public AntlrServiceGroup getServiceGroupByContext(ServiceGroupDeclarationContext context) {
        return this.serviceGroupsByContext.get(context);
    }

    // <editor-fold desc="Report Compiler Errors">
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        for (AntlrClass userClass : this.userClasses) {
            if (this.userClasses.size() > 1) {
                userClass.reportDuplicateUserClass(compilerAnnotationHolder);
            }
        }

        this.reportDuplicateTopLevelNames(compilerAnnotationHolder);

        for (AntlrCompilationUnit compilationUnitState : this.compilationUnits) {
            compilationUnitState.reportNameErrors(compilerAnnotationHolder);
        }

        for (AntlrTopLevelElement topLevelElement : this.topLevelElements) {
            topLevelElement.reportNameErrors(compilerAnnotationHolder);
            topLevelElement.reportErrors(compilerAnnotationHolder);
        }

        for (AntlrClassifier classifier : this.classifiers) {
            if (this.userClasses.isEmpty()) {
                classifier.reportAuditErrors(compilerAnnotationHolder);
            }
        }

        ImmutableBag<AntlrClass> duplicateServiceGroupKlasses = this.getDuplicateServiceGroupClasses();
        for (AntlrServiceGroup serviceGroup : this.serviceGroups) {
            if (duplicateServiceGroupKlasses.contains(serviceGroup.getKlass())) {
                serviceGroup.reportDuplicateServiceGroupClass(compilerAnnotationHolder);
            }
        }

        this.reportUnreferencedPrivateProperties(compilerAnnotationHolder);
    }

    private void reportDuplicateTopLevelNames(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        ImmutableList<String> topLevelNames =
            this.topLevelElements.collect(AntlrTopLevelElement::getName).toImmutable();

        ImmutableBag<String> duplicateTopLevelNames = topLevelNames
            .toBag()
            .selectByOccurrences(occurrences -> occurrences > 1)
            .toImmutable();

        this.topLevelElements.select(
                topLevelElement -> duplicateTopLevelNames.contains(topLevelElement.getName())
            ).forEachWith(AntlrTopLevelElement::reportDuplicateTopLevelName, compilerAnnotationHolder);
    }

    private ImmutableBag<AntlrClass> getDuplicateServiceGroupClasses() {
        return this.serviceGroups.collect(AntlrServiceGroup::getKlass)
            .toBag()
            .selectByOccurrences(occurrences -> occurrences > 1)
            .reject(AntlrClass.AMBIGUOUS::equals)
            .reject(AntlrClass.NOT_FOUND::equals)
            .toImmutable();
    }

    private void reportUnreferencedPrivateProperties(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        var criteriaVisitor = new UnreferencedPrivatePropertiesCriteriaVisitor();
        var orderByVisitor = new UnreferencedPrivatePropertiesOrderByVisitor();

        this.visitCriteria(criteriaVisitor);
        this.visitOrderBy(orderByVisitor);

        Set<AntlrDataTypeProperty<?>> dataTypePropertiesReferenced = Sets.union(
            criteriaVisitor.getDataTypePropertiesReferencedByCriteria(),
            orderByVisitor.getDataTypePropertiesReferencedByOrderBy()
        );
        Set<AntlrAssociationEnd> associationEndsReferenced = Sets.union(
            criteriaVisitor.getAssociationEndsReferencedByCriteria(),
            orderByVisitor.getAssociationEndsReferencedByOrderBy()
        );
        this.reportUnreferencedPrivateProperty(
                compilerAnnotationHolder,
                dataTypePropertiesReferenced,
                associationEndsReferenced
            );
    }

    private void visitCriteria(AntlrCriteriaVisitor criteriaVisitor) {
        for (AntlrClassifier classifier : this.classifiers) {
            for (AntlrProperty property : classifier.getAllProperties()) {
                if (property instanceof AntlrParameterizedProperty parameterizedProperty) {
                    AntlrCriteria criteria = parameterizedProperty.getCriteria();
                    criteria.visit(criteriaVisitor);
                }
            }
        }

        for (AntlrAssociation association : this.associations) {
            association.visitCriteria(criteriaVisitor);
        }

        for (AntlrServiceGroup serviceGroup : this.serviceGroups) {
            for (AntlrUrl url : serviceGroup.getUrls()) {
                for (AntlrService service : url.getServices()) {
                    for (AntlrServiceCriteria serviceCriteria : service.getServiceCriterias()) {
                        serviceCriteria.getCriteria().visit(criteriaVisitor);
                    }
                }
            }
        }
    }

    private void visitOrderBy(AntlrOrderByVisitor orderByVisitor) {
        for (AntlrClassifier classifier : this.classifiers) {
            for (AntlrProperty property : classifier.getAllProperties()) {
                if (property instanceof AntlrOrderByOwner orderByOwner) {
                    ImmutableList<AntlrOrderBy> orderBys = orderByOwner.getOrderBys();
                    for (AntlrOrderBy orderBy : orderBys) {
                        orderBy.visit(orderByVisitor);
                    }
                }
            }
        }

        for (AntlrServiceGroup serviceGroup : this.serviceGroups) {
            for (AntlrUrl url : serviceGroup.getUrls()) {
                for (AntlrService service : url.getServices()) {
                    ImmutableList<AntlrOrderBy> orderBys = service.getOrderBys();
                    for (AntlrOrderBy orderBy : orderBys) {
                        orderBy.visit(orderByVisitor);
                    }
                }
            }
        }
    }

    private void reportUnreferencedPrivateProperty(
        @Nonnull CompilerAnnotationHolder compilerAnnotationHolder,
        @Nonnull Set<AntlrDataTypeProperty<?>> dataTypePropertiesReferenced,
        @Nonnull Set<AntlrAssociationEnd> associationEndsReferenced
    ) {
        for (AntlrClassifier classifier : this.classifiers) {
            for (AntlrDataTypeProperty<?> dataTypeProperty : classifier.getAllDataTypeProperties()) {
                ImmutableList<AntlrDataTypeProperty<?>> overriddenProperties =
                    dataTypeProperty.getOverriddenProperties();
                if (
                    dataTypeProperty.isPrivate() &&
                    dataTypeProperty.getType() != AntlrPrimitiveType.TEMPORAL_RANGE &&
                    overriddenProperties.noneSatisfy(dataTypePropertiesReferenced::contains)
                ) {
                    dataTypeProperty.reportUnreferencedPrivateProperty(compilerAnnotationHolder);
                }
            }
        }

        for (AntlrClass klass : this.klasses) {
            for (AntlrAssociationEnd associationEnds : klass.getDeclaredAssociationEnds()) {
                if (
                    associationEnds.isPrivate() &&
                    !associationEndsReferenced.contains(associationEnds) &&
                    !associationEndsReferenced.contains(associationEnds.getOpposite())
                ) {
                    associationEnds.reportUnreferencedPrivateProperty(compilerAnnotationHolder);
                }
            }
        }
    }

    // </editor-fold>

    @Nonnull
    public DomainModelBuilder build(ImmutableList<CompilationUnit> compilationUnits) {
        ImmutableList<SourceCodeBuilderImpl> sourceCodeBuilders = compilationUnits.collect(CompilationUnit::build);
        ImmutableList<EnumerationBuilder> enumerationBuilders =
            this.enumerations.collect(AntlrEnumeration::build).toImmutable();
        ImmutableList<InterfaceBuilder> interfaceBuilders =
            this.interfaces.collect(AntlrInterface::build1).toImmutable();
        ImmutableList<KlassBuilder> classBuilders = this.klasses.collect(AntlrClass::build1).toImmutable();

        ImmutableList<ClassifierBuilder<?>> classifierBuilders =
            this.classifiers.<ClassifierBuilder<?>>collect(AntlrClassifier::getElementBuilder).toImmutable();

        ImmutableList<AssociationBuilder> associationBuilders =
            this.associations.collect(AntlrAssociation::build).toImmutable();
        this.interfaces.each(AntlrInterface::build2);
        this.klasses.each(AntlrClass::build2);

        ImmutableList<ProjectionBuilder> projectionBuilders =
            this.projections.collect(AntlrProjection::build).toImmutable();
        this.projections.each(AntlrProjection::build2);
        ImmutableList<ServiceGroupBuilder> serviceGroupBuilders =
            this.serviceGroups.collect(AntlrServiceGroup::build).toImmutable();

        ImmutableList<TopLevelElementBuilder> topLevelElementBuilders =
            this.topLevelElements.collect(AntlrTopLevelElement::getElementBuilder).toImmutable();

        compilationUnits.each(CompilationUnit::build2);

        return new DomainModelBuilder(
            sourceCodeBuilders,
            topLevelElementBuilders,
            enumerationBuilders,
            classifierBuilders,
            interfaceBuilders,
            classBuilders,
            associationBuilders,
            projectionBuilders,
            serviceGroupBuilders
        );
    }
}

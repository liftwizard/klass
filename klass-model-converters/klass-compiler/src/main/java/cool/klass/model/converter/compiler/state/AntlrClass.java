package cool.klass.model.converter.compiler.state;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.error.CompilerErrorHolder;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEnd;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.property.AntlrEnumerationProperty;
import cool.klass.model.converter.compiler.state.property.AntlrParameterizedProperty;
import cool.klass.model.converter.compiler.state.property.AntlrPrimitiveProperty;
import cool.klass.model.converter.compiler.state.property.AntlrProperty;
import cool.klass.model.meta.domain.ClassModifierImpl.ClassModifierBuilder;
import cool.klass.model.meta.domain.KlassImpl.KlassBuilder;
import cool.klass.model.meta.domain.property.AbstractDataTypeProperty.DataTypePropertyBuilder;
import cool.klass.model.meta.domain.property.AssociationEndImpl.AssociationEndBuilder;
import cool.klass.model.meta.grammar.KlassParser.ClassDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ParameterizedPropertyContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;

public class AntlrClass extends AntlrPackageableElement implements AntlrType, AntlrTopLevelElement
{
    @Nonnull
    public static final AntlrClass AMBIGUOUS = new AntlrClass(
            new ClassDeclarationContext(null, -1),
            null,
            true,
            new ParserRuleContext(),
            "ambiguous class",
            -1,
            null,
            false)
    {
        @Override
        public void enterDataTypeProperty(@Nonnull AntlrDataTypeProperty<?> antlrDataTypeProperty)
        {
            throw new UnsupportedOperationException(this.getClass().getSimpleName()
                    + ".enterDataTypeProperty() not implemented yet");
        }

        @Override
        public void enterAssociationEnd(@Nonnull AntlrAssociationEnd antlrAssociationEnd)
        {
            throw new UnsupportedOperationException(this.getClass().getSimpleName()
                    + ".enterAssociationEnd() not implemented yet");
        }

        @Override
        public void enterParameterizedProperty(@Nonnull AntlrParameterizedProperty parameterizedPropertyState)
        {
            throw new UnsupportedOperationException(this.getClass().getSimpleName()
                    + ".enterParameterizedProperty() not implemented yet");
        }
    };
    @Nonnull
    public static final AntlrClass NOT_FOUND = new AntlrClass(
            new ClassDeclarationContext(null, -1),
            null,
            true,
            new ParserRuleContext(),
            "not found class",
            -1,
            null,
            false)
    {
        @Override
        public void enterDataTypeProperty(@Nonnull AntlrDataTypeProperty<?> antlrDataTypeProperty)
        {
            throw new UnsupportedOperationException(this.getClass().getSimpleName()
                    + ".enterDataTypeProperty() not implemented yet");
        }

        @Override
        public void enterAssociationEnd(@Nonnull AntlrAssociationEnd antlrAssociationEnd)
        {
            throw new UnsupportedOperationException(this.getClass().getSimpleName()
                    + ".enterAssociationEnd() not implemented yet");
        }

        @Override
        public void enterParameterizedProperty(@Nonnull AntlrParameterizedProperty parameterizedPropertyState)
        {
            throw new UnsupportedOperationException(this.getClass().getSimpleName()
                    + ".enterParameterizedProperty() not implemented yet");
        }
    };

    // TODO: Unified list of dataType and parameterized properties

    private final MutableList<AntlrDataTypeProperty<?>>               dataTypePropertyStates   = Lists.mutable.empty();
    private final MutableOrderedMap<String, AntlrDataTypeProperty<?>> dataTypePropertiesByName =
            OrderedMapAdapter.adapt(new LinkedHashMap<>());

    private final MutableList<AntlrAssociationEnd>               associationEndStates  = Lists.mutable.empty();
    private final MutableOrderedMap<String, AntlrAssociationEnd> associationEndsByName =
            OrderedMapAdapter.adapt(new LinkedHashMap<>());

    private final MutableList<AntlrParameterizedProperty>                                     parameterizedPropertyStates      = Lists.mutable.empty();
    private final MutableOrderedMap<String, AntlrParameterizedProperty>                       parameterizedPropertiesByName    =
            OrderedMapAdapter.adapt(new LinkedHashMap<>());
    private final MutableOrderedMap<ParameterizedPropertyContext, AntlrParameterizedProperty> parameterizedPropertiesByContext =
            OrderedMapAdapter.adapt(new LinkedHashMap<>());

    private final MutableList<AntlrClassModifier>               classModifierStates  = Lists.mutable.empty();
    private final MutableOrderedMap<String, AntlrClassModifier> classModifiersByName =
            OrderedMapAdapter.adapt(new LinkedHashMap<>());

    private final boolean isUser;

    private KlassBuilder klassBuilder;

    public AntlrClass(
            @Nonnull ParserRuleContext elementContext,
            CompilationUnit compilationUnit,
            boolean inferred,
            @Nonnull ParserRuleContext nameContext,
            @Nonnull String name,
            int ordinal,
            String packageName,
            boolean isUser)
    {
        super(elementContext, compilationUnit, inferred, nameContext, name, ordinal, packageName);
        this.isUser = isUser;
    }

    public MutableList<AntlrDataTypeProperty<?>> getDataTypeProperties()
    {
        return this.dataTypePropertyStates;
    }

    public int getNumMembers()
    {
        return this.dataTypePropertyStates.size()
                + this.parameterizedPropertyStates.size()
                + this.associationEndStates.size();
    }

    public void enterDataTypeProperty(@Nonnull AntlrDataTypeProperty<?> antlrDataTypeProperty)
    {
        this.dataTypePropertyStates.add(antlrDataTypeProperty);
        this.dataTypePropertiesByName.compute(
                antlrDataTypeProperty.getName(),
                (name, builder) -> builder == null
                        ? antlrDataTypeProperty
                        : AntlrPrimitiveProperty.AMBIGUOUS);
    }

    public void enterAssociationEnd(@Nonnull AntlrAssociationEnd antlrAssociationEnd)
    {
        this.associationEndStates.add(antlrAssociationEnd);
        this.associationEndsByName.compute(
                antlrAssociationEnd.getName(),
                (name, builder) -> builder == null
                        ? antlrAssociationEnd
                        : AntlrAssociationEnd.AMBIGUOUS);
    }

    public void enterParameterizedProperty(@Nonnull AntlrParameterizedProperty parameterizedPropertyState)
    {
        this.parameterizedPropertyStates.add(parameterizedPropertyState);
        this.parameterizedPropertiesByName.compute(
                parameterizedPropertyState.getName(),
                (name, builder) -> builder == null
                        ? parameterizedPropertyState
                        : AntlrParameterizedProperty.AMBIGUOUS);

        AntlrParameterizedProperty duplicate = this.parameterizedPropertiesByContext.put(
                parameterizedPropertyState.getElementContext(),
                parameterizedPropertyState);
        if (duplicate != null)
        {
            throw new AssertionError();
        }
    }

    public void enterClassModifier(@Nonnull AntlrClassModifier classModifierState)
    {
        this.classModifierStates.add(classModifierState);
        this.classModifiersByName.compute(
                classModifierState.getName(),
                (name, builder) -> builder == null
                        ? classModifierState
                        : AntlrClassModifier.AMBIGUOUS);
    }

    public AntlrParameterizedProperty getParameterizedPropertyByContext(ParameterizedPropertyContext ctx)
    {
        return this.parameterizedPropertiesByContext.get(ctx);
    }

    public KlassBuilder build1()
    {
        if (this.klassBuilder != null)
        {
            throw new IllegalStateException();
        }

        this.klassBuilder = new KlassBuilder(
                this.elementContext,
                this.inferred,
                this.nameContext,
                this.name,
                this.ordinal,
                this.packageName,
                this.isUser,
                this.isTransient());

        ImmutableList<ClassModifierBuilder> classModifierBuilders = this.classModifierStates
                .collect(AntlrClassModifier::build)
                .toImmutable();
        this.klassBuilder.setClassModifierBuilders(classModifierBuilders);

        ImmutableList<DataTypePropertyBuilder<?, ?, ?>> dataTypePropertyBuilders = this.dataTypePropertyStates
                .<DataTypePropertyBuilder<?, ?, ?>>collect(AntlrDataTypeProperty::build)
                .toImmutable();

        this.klassBuilder.setDataTypePropertyBuilders(dataTypePropertyBuilders);
        return this.klassBuilder;
    }

    public boolean isTransient()
    {
        return this.classModifierStates.anySatisfy(AntlrClassModifier::isTransient);
    }

    public boolean isOptimisticallyLocked()
    {
        return this.classModifierStates.anySatisfy(AntlrClassModifier::isOptimisticallyLocked);
    }

    @Override
    public KlassBuilder getElementBuilder()
    {
        return Objects.requireNonNull(this.klassBuilder);
    }

    public void build2()
    {
        if (this.klassBuilder == null)
        {
            throw new IllegalStateException();
        }

        ImmutableList<AssociationEndBuilder> associationEndBuilders = this.associationEndStates
                .collect(AntlrAssociationEnd::getElementBuilder)
                .toImmutable();

        this.klassBuilder.setAssociationEndBuilders(associationEndBuilders);

        Optional<AntlrAssociationEnd> versionAssociationEnd =
                this.associationEndStates.detectOptional(AntlrAssociationEnd::isVersion);
        Optional<AntlrAssociationEnd> versionedAssociationEnd =
                this.associationEndStates.detectOptional(AntlrAssociationEnd::isVersioned);

        Optional<AssociationEndBuilder> versionAssociationEndBuilder   = versionAssociationEnd.map(AntlrAssociationEnd::getElementBuilder);
        Optional<AssociationEndBuilder> versionedAssociationEndBuilder = versionedAssociationEnd.map(AntlrAssociationEnd::getElementBuilder);

        this.klassBuilder.setVersionPropertyBuilder(versionAssociationEndBuilder);
        this.klassBuilder.setVersionedPropertyBuilder(versionedAssociationEndBuilder);

        this.dataTypePropertyStates.each(AntlrDataTypeProperty::build2);
    }

    @Override
    public void reportNameErrors(@Nonnull CompilerErrorHolder compilerErrorHolder)
    {
        super.reportNameErrors(compilerErrorHolder);
        this.reportKeywordCollision(compilerErrorHolder);

        if (RELADOMO_TYPES.contains(this.getName()))
        {
            String message = String.format("ERR_REL_NME: '%s' is a Reladomo type.", this.getName());
            compilerErrorHolder.add(message, this);
        }

        this.dataTypePropertyStates.forEachWith(AntlrNamedElement::reportNameErrors, compilerErrorHolder);
        this.parameterizedPropertyStates.forEachWith(AntlrNamedElement::reportNameErrors, compilerErrorHolder);
        this.associationEndStates.forEachWith(AntlrNamedElement::reportNameErrors, compilerErrorHolder);
    }

    public void reportErrors(@Nonnull CompilerErrorHolder compilerErrorHolder)
    {
        ImmutableBag<String> duplicateMemberNames = this.getDuplicateMemberNames();

        for (AntlrDataTypeProperty<?> dataTypePropertyState : this.dataTypePropertyStates)
        {
            if (duplicateMemberNames.contains(dataTypePropertyState.getName()))
            {
                dataTypePropertyState.reportDuplicateMemberName(compilerErrorHolder);
            }
            dataTypePropertyState.reportErrors(compilerErrorHolder);
        }

        for (AntlrParameterizedProperty parameterizedPropertyState : this.parameterizedPropertyStates)
        {
            if (duplicateMemberNames.contains(parameterizedPropertyState.getName()))
            {
                parameterizedPropertyState.reportDuplicateMemberName(compilerErrorHolder);
            }
            parameterizedPropertyState.reportErrors(compilerErrorHolder);
        }

        for (AntlrAssociationEnd associationEndState : this.associationEndStates)
        {
            if (duplicateMemberNames.contains(associationEndState.getName()))
            {
                associationEndState.reportDuplicateMemberName(compilerErrorHolder);
            }
            associationEndState.reportErrors(compilerErrorHolder);
        }

        MutableList<AntlrAssociationEnd> versionAssociationEnds = this.associationEndStates.select(AntlrAssociationEnd::isVersion);
        if (versionAssociationEnds.size() > 1)
        {
            for (AntlrAssociationEnd antlrAssociationEnd : versionAssociationEnds)
            {
                antlrAssociationEnd.reportDuplicateVersionProperty(compilerErrorHolder, this);
            }
        }

        MutableList<AntlrAssociationEnd> versionedAssociationEnds = this.associationEndStates.select(AntlrAssociationEnd::isVersioned);
        if (versionedAssociationEnds.size() > 1)
        {
            for (AntlrAssociationEnd versionedAssociationEnd : versionedAssociationEnds)
            {
                versionedAssociationEnd.reportDuplicateVersionedProperty(compilerErrorHolder, this);
            }
        }

        if (versionAssociationEnds.notEmpty() && versionedAssociationEnds.notEmpty())
        {
            String message = String.format("ERR_VER_VER: Class '%s' is a version and has a version.", this.name);
            compilerErrorHolder.add(message, this);
        }

        int numKeyProperties = this.dataTypePropertyStates.count(AntlrDataTypeProperty::isKey);
        if (numKeyProperties == 0)
        {
            String message = String.format("ERR_CLS_KEY: Class '%s' must have at least one key property.", this.name);
            compilerErrorHolder.add(message, this);
        }

        // TODO: Warn if class is owned by multiple
        // TODO: Detect ownership cycles
        // TODO: Check that ID properties aren't part of a composite key

        // TODO: parameterized properties
        // TODO: duplicate class modifiers
    }

    public ImmutableBag<String> getDuplicateMemberNames()
    {
        return this.getMemberNames()
                .toBag()
                .selectByOccurrences(occurrences -> occurrences > 1)
                .toImmutable();
    }

    private ImmutableList<String> getMemberNames()
    {
        MutableList<String> topLevelNames = Lists.mutable.empty();
        this.dataTypePropertyStates.collect(AntlrProperty::getName, topLevelNames);
        this.parameterizedPropertyStates.collect(AntlrNamedElement::getName, topLevelNames);
        this.associationEndStates.collect(AntlrProperty::getName, topLevelNames);
        return topLevelNames.toImmutable();
    }

    @Nonnull
    @Override
    public ClassDeclarationContext getElementContext()
    {
        return (ClassDeclarationContext) super.getElementContext();
    }

    public AntlrDataTypeProperty<?> getDataTypePropertyByName(String name)
    {
        return this.dataTypePropertiesByName.getIfAbsentValue(name, AntlrEnumerationProperty.NOT_FOUND);
    }

    public AntlrAssociationEnd getAssociationEndByName(String name)
    {
        return this.associationEndsByName.getIfAbsentValue(name, AntlrAssociationEnd.NOT_FOUND);
    }

    public MutableList<AntlrAssociationEnd> getAssociationEndStates()
    {
        return this.associationEndStates.asUnmodifiable();
    }

    public int getNumClassModifiers()
    {
        return this.classModifierStates.size();
    }

    @Nonnull
    @Override
    public KlassBuilder getTypeGetter()
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName()
                + ".getTypeBuilder() not implemented yet");
    }

    public boolean hasVersion()
    {
        return this.associationEndStates.anySatisfy(AntlrAssociationEnd::isVersion);
    }
}

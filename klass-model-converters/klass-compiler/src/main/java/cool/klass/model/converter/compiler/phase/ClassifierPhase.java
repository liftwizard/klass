package cool.klass.model.converter.compiler.phase;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.CompilerWalkState;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrClassModifier;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.AntlrDomainModel;
import cool.klass.model.converter.compiler.state.AntlrEnumeration;
import cool.klass.model.converter.compiler.state.AntlrInterface;
import cool.klass.model.converter.compiler.state.AntlrMultiplicity;
import cool.klass.model.converter.compiler.state.AntlrPrimitiveType;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEndModifier;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEndSignature;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.property.AntlrEnumerationProperty;
import cool.klass.model.converter.compiler.state.property.AntlrPrimitiveProperty;
import cool.klass.model.converter.compiler.state.property.AntlrPropertyModifier;
import cool.klass.model.converter.compiler.state.property.validation.AntlrMaxLengthPropertyValidation;
import cool.klass.model.converter.compiler.state.property.validation.AntlrMaxPropertyValidation;
import cool.klass.model.converter.compiler.state.property.validation.AntlrMinLengthPropertyValidation;
import cool.klass.model.converter.compiler.state.property.validation.AntlrMinPropertyValidation;
import cool.klass.model.meta.domain.api.InheritanceType;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.grammar.KlassParser.AbstractDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndModifierContext;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndSignatureContext;
import cool.klass.model.meta.grammar.KlassParser.ClassDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.ClassModifierContext;
import cool.klass.model.meta.grammar.KlassParser.ClassifierReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationPropertyContext;
import cool.klass.model.meta.grammar.KlassParser.InheritanceTypeContext;
import cool.klass.model.meta.grammar.KlassParser.IntegerLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.InterfaceDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.MaxLengthValidationContext;
import cool.klass.model.meta.grammar.KlassParser.MaxValidationContext;
import cool.klass.model.meta.grammar.KlassParser.MinLengthValidationContext;
import cool.klass.model.meta.grammar.KlassParser.MinValidationContext;
import cool.klass.model.meta.grammar.KlassParser.PrimitivePropertyContext;
import cool.klass.model.meta.grammar.KlassParser.PropertyModifierContext;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.list.mutable.ListAdapter;

public class ClassifierPhase extends AbstractCompilerPhase
{
    @Nullable
    private AntlrDataTypeProperty<?> dataTypePropertyState;

    @Nullable
    private AntlrAssociationEndSignature associationEndSignatureState;

    public ClassifierPhase(CompilerState compilerState)
    {
        super(compilerState);
    }

    @Override
    public void enterInterfaceDeclaration(@Nonnull InterfaceDeclarationContext ctx)
    {
        super.enterInterfaceDeclaration(ctx);

        AntlrInterface interfaceState = new AntlrInterface(
                ctx,
                Optional.of(this.compilerState.getCompilerWalkState().getCurrentCompilationUnit()),
                ctx.interfaceHeader().identifier(),
                ctx.interfaceHeader().identifier().getText(),
                this.compilerState.getOrdinal(ctx),
                this.compilerState.getCompilerWalkState().getPackageNameContext(),
                this.compilerState.getCompilerWalkState().getPackageName());

        this.compilerState.getCompilerWalkState().defineInterface(interfaceState);
    }

    @Override
    public void enterClassDeclaration(@Nonnull ClassDeclarationContext ctx)
    {
        super.enterClassDeclaration(ctx);

        String classOrUserKeyword = ctx.classHeader().classOrUser().getText();

        AntlrClass classState = new AntlrClass(
                ctx,
                Optional.of(this.compilerState.getCompilerWalkState().getCurrentCompilationUnit()),
                ctx.classHeader().identifier(),
                ctx.classHeader().identifier().getText(),
                this.compilerState.getOrdinal(ctx),
                this.compilerState.getCompilerWalkState().getPackageNameContext(),
                this.compilerState.getCompilerWalkState().getPackageName(),
                classOrUserKeyword.equals("user"));
        this.compilerState.getCompilerWalkState().defineClass(classState);
    }

    @Override
    public void enterAbstractDeclaration(AbstractDeclarationContext ctx)
    {
        super.enterAbstractDeclaration(ctx);

        AntlrClass classState = this.compilerState.getCompilerWalkState().getClassState();
        classState.setInheritanceType(InheritanceType.TABLE_PER_SUBCLASS);
    }

    @Override
    public void enterInheritanceType(@Nonnull InheritanceTypeContext ctx)
    {
        super.enterInheritanceType(ctx);

        AntlrClass      classState      = this.compilerState.getCompilerWalkState().getClassState();
        InheritanceType inheritanceType = InheritanceType.byPrettyName(ctx.getText());
        classState.setInheritanceType(inheritanceType);
    }

    @Override
    public void enterClassModifier(@Nonnull ClassModifierContext ctx)
    {
        super.enterClassModifier(ctx);

        AntlrClassifier classifierState = this.compilerState.getCompilerWalkState().getClassifierState();
        int             ordinal         = classifierState.getNumClassModifiers();

        AntlrClassModifier classModifierState = new AntlrClassModifier(
                ctx,
                Optional.of(this.compilerState.getCompilerWalkState().getCurrentCompilationUnit()),
                ctx,
                ctx.getText(),
                ordinal + 1,
                classifierState);

        classifierState.enterClassModifier(classModifierState);
    }

    @Override
    public void enterPrimitiveProperty(@Nonnull PrimitivePropertyContext ctx)
    {
        super.enterPrimitiveProperty(ctx);

        ImmutableList<AntlrPropertyModifier> propertyModifiers = ListAdapter.adapt(ctx.propertyModifier())
                .collectWithIndex(this::getAntlrPropertyModifier)
                .toImmutable();

        String             propertyName       = ctx.identifier().getText();
        boolean            isOptional         = ctx.optionalMarker() != null;
        String             primitiveTypeName  = ctx.primitiveType().getText();
        PrimitiveType      primitiveType      = PrimitiveType.byPrettyName(primitiveTypeName);
        AntlrPrimitiveType primitiveTypeState = AntlrPrimitiveType.valueOf(primitiveType);

        AntlrClassifier classifierState = this.compilerState.getCompilerWalkState().getClassifierState();

        if (this.dataTypePropertyState != null)
        {
            throw new IllegalStateException();
        }
        this.dataTypePropertyState = new AntlrPrimitiveProperty(
                ctx,
                Optional.of(this.compilerState.getCompilerWalkState().getCurrentCompilationUnit()),
                ctx.identifier(),
                propertyName,
                classifierState.getNumMembers() + 1,
                classifierState,
                isOptional,
                propertyModifiers,
                primitiveTypeState);

        classifierState.enterDataTypeProperty(this.dataTypePropertyState);
    }

    @Override
    public void exitPrimitiveProperty(PrimitivePropertyContext ctx)
    {
        Objects.requireNonNull(this.dataTypePropertyState);
        this.dataTypePropertyState = null;
        super.exitPrimitiveProperty(ctx);
    }

    @Override
    public void enterEnumerationProperty(@Nonnull EnumerationPropertyContext ctx)
    {
        super.enterEnumerationProperty(ctx);

        // TODO: Superclass above all modifiers. Modifiers hold their owners.
        ImmutableList<AntlrPropertyModifier> propertyModifiers = ListAdapter.adapt(ctx.propertyModifier())
                .collectWithIndex(this::getAntlrPropertyModifier)
                .toImmutable();

        String           propertyName     = ctx.identifier().getText();
        boolean          isOptional       = ctx.optionalMarker() != null;
        AntlrDomainModel domainModelState = this.compilerState.getDomainModelState();
        String           enumerationName  = ctx.enumerationReference().getText();
        AntlrEnumeration enumerationState = domainModelState.getEnumerationByName(enumerationName);
        AntlrClassifier  classifierState  = this.compilerState.getCompilerWalkState().getClassifierState();

        if (this.dataTypePropertyState != null)
        {
            throw new IllegalStateException();
        }
        this.dataTypePropertyState = new AntlrEnumerationProperty(
                ctx,
                Optional.of(this.compilerState.getCompilerWalkState().getCurrentCompilationUnit()),
                ctx.identifier(),
                propertyName,
                classifierState.getNumMembers() + 1,
                classifierState,
                isOptional,
                propertyModifiers,
                enumerationState);

        classifierState.enterDataTypeProperty(this.dataTypePropertyState);
    }

    @Override
    public void exitEnumerationProperty(EnumerationPropertyContext ctx)
    {
        Objects.requireNonNull(this.dataTypePropertyState);
        this.dataTypePropertyState = null;
    }

    @Override
    public void enterMinLengthValidation(@Nonnull MinLengthValidationContext ctx)
    {
        super.enterMinLengthValidation(ctx);

        IntegerLiteralContext integerLiteralContext = ctx.integerLiteral();
        int                   length                = this.getIntegerFromLiteral(integerLiteralContext);
        AntlrMinLengthPropertyValidation minLengthValidationState = new AntlrMinLengthPropertyValidation(
                ctx,
                Optional.of(this.compilerState.getCompilerWalkState().getCurrentCompilationUnit()),
                this.dataTypePropertyState,
                length);
        this.dataTypePropertyState.addMinLengthValidationState(minLengthValidationState);
    }

    @Override
    public void enterMaxLengthValidation(@Nonnull MaxLengthValidationContext ctx)
    {
        super.enterMaxLengthValidation(ctx);

        IntegerLiteralContext integerLiteralContext = ctx.integerLiteral();
        int                   length                = this.getIntegerFromLiteral(integerLiteralContext);
        AntlrMaxLengthPropertyValidation maxLengthValidationState = new AntlrMaxLengthPropertyValidation(
                ctx,
                Optional.of(this.compilerState.getCompilerWalkState().getCurrentCompilationUnit()),
                this.dataTypePropertyState,
                length);
        this.dataTypePropertyState.addMaxLengthValidationState(maxLengthValidationState);
    }

    @Override
    public void enterMinValidation(@Nonnull MinValidationContext ctx)
    {
        super.enterMinValidation(ctx);

        IntegerLiteralContext integerLiteralContext = ctx.integerLiteral();
        int                   minimum               = this.getIntegerFromLiteral(integerLiteralContext);
        AntlrMinPropertyValidation minValidationState = new AntlrMinPropertyValidation(
                ctx,
                Optional.of(this.compilerState.getCompilerWalkState().getCurrentCompilationUnit()),
                this.dataTypePropertyState,
                minimum);
        this.dataTypePropertyState.addMinValidationState(minValidationState);
    }

    @Override
    public void enterMaxValidation(@Nonnull MaxValidationContext ctx)
    {
        super.enterMaxValidation(ctx);

        IntegerLiteralContext integerLiteralContext = ctx.integerLiteral();
        int                   maximum               = this.getIntegerFromLiteral(integerLiteralContext);
        AntlrMaxPropertyValidation maxValidationState = new AntlrMaxPropertyValidation(
                ctx,
                Optional.of(this.compilerState.getCompilerWalkState().getCurrentCompilationUnit()),
                this.dataTypePropertyState,
                maximum);
        this.dataTypePropertyState.addMaxValidationState(maxValidationState);
    }

    private int getIntegerFromLiteral(@Nonnull IntegerLiteralContext integerLiteralContext)
    {
        String integerText        = integerLiteralContext.getText();
        String withoutUnderscores = integerText.replaceAll("_", "");
        return Integer.decode(withoutUnderscores);
    }

    @Nonnull
    private AntlrPropertyModifier getAntlrPropertyModifier(@Nonnull PropertyModifierContext context, int ordinal)
    {
        return new AntlrPropertyModifier(
                context,
                Optional.of(this.compilerState.getCompilerWalkState().getCurrentCompilationUnit()),
                context,
                context.getText(),
                ordinal + 1);
    }

    @Nonnull
    private AntlrPropertyModifier getAntlrAssociationEndModifier(
            @Nonnull AssociationEndModifierContext context,
            int ordinal)
    {
        return new AntlrPropertyModifier(
                context,
                Optional.of(this.compilerState.getCompilerWalkState().getCurrentCompilationUnit()),
                context,
                context.getText(),
                ordinal + 1);
    }

    @Override
    public void enterAssociationEndSignature(AssociationEndSignatureContext ctx)
    {
        super.enterAssociationEndSignature(ctx);

        ClassifierReferenceContext classifierReferenceContext  = ctx.classifierReference();
        String                     associationEndSignatureName = ctx.identifier().getText();

        String                    classifierName         = classifierReferenceContext.identifier().getText();
        AntlrDomainModel          domainModelState       = this.compilerState.getDomainModelState();
        CompilerWalkState         compilerWalkState      = this.compilerState.getCompilerWalkState();
        AntlrClassifier           resultTypeState        = domainModelState.getClassifierByName(classifierName);
        Optional<CompilationUnit> currentCompilationUnit = Optional.of(compilerWalkState.getCurrentCompilationUnit());
        AntlrMultiplicity         multiplicityState      = new AntlrMultiplicity(
                ctx.multiplicity(),
                currentCompilationUnit);
        AntlrClassifier           classifierState        = compilerWalkState.getClassifierState();

        if (associationEndSignatureState != null)
        {
            throw new IllegalStateException();
        }
        this.associationEndSignatureState = new AntlrAssociationEndSignature(
                ctx,
                currentCompilationUnit,
                ctx.identifier(),
                associationEndSignatureName,
                classifierState.getNumMembers() + 1,
                classifierState,
                resultTypeState,
                multiplicityState);

        resultTypeState.enterAssociationEndSignature(this.associationEndSignatureState);
    }

    @Override
    public void exitAssociationEndSignature(AssociationEndSignatureContext ctx)
    {
        Objects.requireNonNull(this.associationEndSignatureState);
        this.associationEndSignatureState = null;
        super.exitAssociationEndSignature(ctx);
    }

    @Override
    public void enterAssociationEndModifier(@Nonnull AssociationEndModifierContext ctx)
    {
        super.enterAssociationEndModifier(ctx);

        if (this.associationEndSignatureState == null)
        {
            return;
        }

        AntlrAssociationEndModifier antlrAssociationEndModifier = new AntlrAssociationEndModifier(
                ctx,
                Optional.of(this.compilerState.getCompilerWalkState().getCurrentCompilationUnit()),
                ctx,
                ctx.getText(),
                this.associationEndSignatureState.getNumModifiers() + 1,
                this.associationEndSignatureState);
        this.associationEndSignatureState.enterAssociationEndModifier(antlrAssociationEndModifier);
    }
}

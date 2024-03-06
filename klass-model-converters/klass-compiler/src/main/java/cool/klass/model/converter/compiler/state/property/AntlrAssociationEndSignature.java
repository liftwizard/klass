package cool.klass.model.converter.compiler.state.property;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationState;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.AntlrClassifierReference;
import cool.klass.model.converter.compiler.state.AntlrClassifierReferenceOwner;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.converter.compiler.state.order.AntlrOrderBy;
import cool.klass.model.meta.domain.order.OrderByImpl.OrderByBuilder;
import cool.klass.model.meta.domain.property.AssociationEndSignatureImpl.AssociationEndSignatureBuilder;
import cool.klass.model.meta.domain.property.ModifierImpl.ModifierBuilder;
import cool.klass.model.meta.grammar.KlassParser.AssociationEndSignatureContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.eclipse.collections.api.list.ImmutableList;

public class AntlrAssociationEndSignature
        extends AntlrReferenceProperty<AntlrClassifier>
        implements AntlrClassifierReferenceOwner
{
    public static final AntlrAssociationEndSignature AMBIGUOUS = new AntlrAssociationEndSignature(
            new AssociationEndSignatureContext(AMBIGUOUS_PARENT, -1),
            Optional.empty(),
            -1,
            AMBIGUOUS_IDENTIFIER_CONTEXT,
            AntlrClassifier.AMBIGUOUS);

    @Nonnull
    private final AntlrClassifier owningClassifierState;

    private AssociationEndSignatureBuilder associationEndSignatureBuilder;

    private AntlrClassifierReference classifierReferenceState;

    public AntlrAssociationEndSignature(
            @Nonnull AssociationEndSignatureContext elementContext,
            @Nonnull Optional<CompilationUnit> compilationUnit,
            int ordinal,
            @Nonnull IdentifierContext nameContext,
            @Nonnull AntlrClassifier owningClassifierState)
    {
        super(elementContext, compilationUnit, ordinal, nameContext);
        this.owningClassifierState = Objects.requireNonNull(owningClassifierState);
    }

    @Nonnull
    @Override
    public Optional<IAntlrElement> getSurroundingElement()
    {
        return Optional.of(this.owningClassifierState);
    }

    @Nonnull
    @Override
    public AssociationEndSignatureBuilder build()
    {
        if (this.associationEndSignatureBuilder != null)
        {
            throw new IllegalStateException();
        }

        // TODO: 🔗 Set association end's opposite
        this.associationEndSignatureBuilder = new AssociationEndSignatureBuilder(
                (AssociationEndSignatureContext) this.elementContext,
                this.getMacroElementBuilder(),
                this.getSourceCodeBuilder(),
                this.ordinal,
                this.getNameContext(),
                this.getType().getElementBuilder(),
                this.owningClassifierState.getElementBuilder(),
                this.multiplicityState.getMultiplicity());

        ImmutableList<ModifierBuilder> modifierBuilders = this.getModifiers()
                .collect(AntlrModifier::build)
                .toImmutable();

        this.associationEndSignatureBuilder.setModifierBuilders(modifierBuilders);

        Optional<OrderByBuilder> orderByBuilder = this.orderByState.map(AntlrOrderBy::build);
        this.associationEndSignatureBuilder.setOrderByBuilder(orderByBuilder);

        return this.associationEndSignatureBuilder;
    }

    @Override
    public void reportErrors(@Nonnull CompilerAnnotationState compilerAnnotationHolder)
    {
        super.reportErrors(compilerAnnotationHolder);

        if (this.orderByState != null)
        {
            this.orderByState.ifPresent(o -> o.reportErrors(compilerAnnotationHolder));
        }

        this.reportInvalidMultiplicity(compilerAnnotationHolder);
    }

    @Nonnull
    @Override
    public AntlrClassifier getOwningClassifierState()
    {
        return Objects.requireNonNull(this.owningClassifierState);
    }

    @Override
    @Nonnull
    public AssociationEndSignatureBuilder getElementBuilder()
    {
        return Objects.requireNonNull(this.associationEndSignatureBuilder);
    }

    @Override
    protected IdentifierContext getTypeIdentifier()
    {
        return this.getElementContext().classifierReference().identifier();
    }

    @Nonnull
    @Override
    public AssociationEndSignatureContext getElementContext()
    {
        return (AssociationEndSignatureContext) super.getElementContext();
    }

    @Nonnull
    @Override
    public AntlrClassifier getType()
    {
        return this.classifierReferenceState.getClassifierState();
    }

    @Override
    public void enterClassifierReference(@Nonnull AntlrClassifierReference classifierReferenceState)
    {
        if (this.classifierReferenceState != null)
        {
            throw new AssertionError();
        }
        this.classifierReferenceState = Objects.requireNonNull(classifierReferenceState);
    }
}

package cool.klass.model.converter.compiler.state.projection;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.error.CompilerErrorState;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.AntlrIdentifierElement;
import cool.klass.model.converter.compiler.state.property.AntlrAssociationEnd;
import cool.klass.model.converter.compiler.state.property.AntlrReferenceProperty;
import cool.klass.model.meta.domain.projection.ProjectionImpl.ProjectionBuilder;
import cool.klass.model.meta.domain.projection.ProjectionProjectionReferenceImpl.ProjectionProjectionReferenceBuilder;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.ProjectionProjectionReferenceContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.tuple.Pair;

public class AntlrProjectionProjectionReference
        extends AntlrIdentifierElement
        implements AntlrProjectionChild
{
    @Nonnull
    private final AntlrClassifier           classifier;
    @Nonnull
    private final AntlrProjectionParent     antlrProjectionParent;
    @Nonnull
    private final AntlrReferenceProperty<?> referenceProperty;
    @Nonnull
    private final AntlrProjection           referencedProjectionState;

    private ProjectionProjectionReferenceBuilder projectionProjectionReferenceBuilder;

    public AntlrProjectionProjectionReference(
            @Nonnull ProjectionProjectionReferenceContext elementContext,
            @Nonnull Optional<CompilationUnit> compilationUnit,
            int ordinal,
            @Nonnull IdentifierContext nameContext,
            @Nonnull AntlrClassifier classifier,
            @Nonnull AntlrProjectionParent antlrProjectionParent,
            @Nonnull AntlrReferenceProperty<?> referenceProperty,
            @Nonnull AntlrProjection referencedProjectionState)
    {
        super(elementContext, compilationUnit, ordinal, nameContext);
        this.classifier                = Objects.requireNonNull(classifier);
        this.antlrProjectionParent     = Objects.requireNonNull(antlrProjectionParent);
        this.referenceProperty         = Objects.requireNonNull(referenceProperty);
        this.referencedProjectionState = Objects.requireNonNull(referencedProjectionState);
    }

    @Override
    public boolean isContext()
    {
        return true;
    }

    @Override
    public Pair<Token, Token> getContextBefore()
    {
        return this.getEntireContext();
    }

    @Nonnull
    @Override
    public ProjectionProjectionReferenceBuilder build()
    {
        if (this.projectionProjectionReferenceBuilder != null)
        {
            throw new IllegalStateException();
        }

        this.projectionProjectionReferenceBuilder = new ProjectionProjectionReferenceBuilder(
                (ProjectionProjectionReferenceContext) this.elementContext,
                this.getMacroElementBuilder(),
                this.getSourceCodeBuilder(),
                this.ordinal,
                this.getNameContext(),
                this.antlrProjectionParent.getElementBuilder(),
                this.referenceProperty.getElementBuilder());

        return this.projectionProjectionReferenceBuilder;
    }

    @Override
    public void build2()
    {
        ProjectionBuilder referencedProjectionBuilder = this.referencedProjectionState.getElementBuilder();
        this.projectionProjectionReferenceBuilder.setReferencedProjectionBuilder(referencedProjectionBuilder);
    }

    @Nonnull
    @Override
    public AntlrProjectionParent getParent()
    {
        return this.antlrProjectionParent;
    }

    @Override
    public void reportDuplicateMemberName(@Nonnull CompilerErrorState compilerErrorHolder)
    {
        String message = String.format("Duplicate member: '%s'.", this.getName());
        compilerErrorHolder.add("ERR_DUP_PRJ", message, this);
    }

    @Override
    public void reportErrors(@Nonnull CompilerErrorState compilerErrorHolder)
    {
        if (this.antlrProjectionParent.getClassifier() == AntlrClass.NOT_FOUND)
        {
            return;
        }

        if (this.referenceProperty == AntlrAssociationEnd.NOT_FOUND)
        {
            String message = String.format("Not found: '%s'.", this.getName());
            compilerErrorHolder.add("ERR_PAE_NFD", message, this);
        }

        if (this.referenceProperty == AntlrAssociationEnd.AMBIGUOUS
                || this.referenceProperty == AntlrReferenceProperty.AMBIGUOUS)
        {
            String message = String.format("Not found: '%s'.", this.getName());
            compilerErrorHolder.add("ERR_PAE_AMB", message, this);
        }

        if (this.classifier != this.referencedProjectionState.getClassifier()
                && !this.classifier.isSubClassOf(this.referencedProjectionState.getClassifier()))
        {
            String message = String.format(
                    "Type mismatch: '%s' has type '%s' but '%s' has type '%s'.",
                    this.getName(),
                    this.classifier.getName(),
                    this.referencedProjectionState.getName(),
                    this.referencedProjectionState.getClassifier().getName());
            compilerErrorHolder.add("ERR_PRR_KLS", message, this);
        }
    }

    @Override
    public void reportNameErrors(@Nonnull CompilerErrorState compilerErrorHolder)
    {
        // Intentionally blank. Reference to a named element that gets its name checked.
    }

    @Nonnull
    @Override
    protected Pattern getNamePattern()
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName()
                + ".getNamePattern() not implemented yet");
    }
}

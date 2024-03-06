package cool.klass.model.converter.compiler.state.projection;

import java.util.Objects;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.error.CompilerErrorState;
import cool.klass.model.converter.compiler.state.AntlrNamedElement;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.converter.compiler.state.property.AntlrEnumerationProperty;
import cool.klass.model.converter.compiler.state.property.AntlrPrimitiveProperty;
import cool.klass.model.meta.domain.projection.ProjectionDataTypePropertyImpl.ProjectionDataTypePropertyBuilder;
import cool.klass.model.meta.grammar.KlassParser.HeaderContext;
import org.antlr.v4.runtime.ParserRuleContext;

public class AntlrProjectionDataTypeProperty
        extends AntlrNamedElement
        implements AntlrProjectionChild
{
    @Nonnull
    public static final AntlrProjectionDataTypeProperty AMBIGUOUS = new AntlrProjectionDataTypeProperty(
            new ParserRuleContext(),
            null,
            true,
            new ParserRuleContext(), "ambiguous projection member",
            -1,
            new HeaderContext(null, -1),
            "ambiguous header",
            AntlrProjection.AMBIGUOUS,
            AntlrPrimitiveProperty.AMBIGUOUS);

    @Nonnull
    private final HeaderContext headerContext;
    @Nonnull
    private final String headerText;
    @Nonnull
    private final AntlrProjectionParent antlrProjectionParent;
    @Nonnull
    private final AntlrDataTypeProperty<?> dataTypeProperty;

    private ProjectionDataTypePropertyBuilder projectionDataTypePropertyBuilder;

    public AntlrProjectionDataTypeProperty(
            @Nonnull ParserRuleContext elementContext,
            @Nullable CompilationUnit compilationUnit,
            boolean inferred,
            @Nonnull ParserRuleContext nameContext,
            @Nonnull String name,
            int ordinal,
            @Nonnull HeaderContext headerContext,
            @Nonnull String headerText,
            @Nonnull AntlrProjectionParent antlrProjectionParent,
            @Nonnull AntlrDataTypeProperty<?> dataTypeProperty)
    {
        super(elementContext, compilationUnit, inferred, nameContext, name, ordinal);
        this.antlrProjectionParent = Objects.requireNonNull(antlrProjectionParent);
        this.headerText = Objects.requireNonNull(headerText);
        this.headerContext = Objects.requireNonNull(headerContext);
        this.dataTypeProperty = Objects.requireNonNull(dataTypeProperty);
    }

    @Nonnull
    public AntlrDataTypeProperty<?> getDataTypeProperty()
    {
        return this.dataTypeProperty;
    }

    @Nonnull
    @Override
    public ProjectionDataTypePropertyBuilder build()
    {
        if (this.projectionDataTypePropertyBuilder != null)
        {
            throw new IllegalStateException();
        }
        this.projectionDataTypePropertyBuilder = new ProjectionDataTypePropertyBuilder(
                this.elementContext,
                this.inferred,
                this.nameContext,
                this.name,
                this.ordinal,
                this.headerContext,
                this.headerText,
                this.antlrProjectionParent.getElementBuilder(),
                this.dataTypeProperty.getElementBuilder());
        return this.projectionDataTypePropertyBuilder;
    }

    @Nonnull
    public ProjectionDataTypePropertyBuilder getElementBuilder()
    {
        return Objects.requireNonNull(this.projectionDataTypePropertyBuilder);
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
        String message = String.format("ERR_DUP_PRJ: Duplicate member: '%s'.", this.getName());
        compilerErrorHolder.add(message, this);
    }

    @Override
    public void reportErrors(@Nonnull CompilerErrorState compilerErrorHolder)
    {
        if (this.dataTypeProperty == AntlrEnumerationProperty.NOT_FOUND)
        {
            String message = String.format(
                    "ERR_PRJ_DTP: Cannot find member '%s.%s'.",
                    this.antlrProjectionParent.getKlass().getName(),
                    this.name);
            compilerErrorHolder.add(message, this);
        }

        if (this.headerText.trim().isEmpty())
        {
            compilerErrorHolder.add("Empty header string.", this, this.headerContext);
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

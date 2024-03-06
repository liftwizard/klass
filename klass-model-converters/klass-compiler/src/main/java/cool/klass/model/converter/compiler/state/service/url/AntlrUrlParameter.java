package cool.klass.model.converter.compiler.state.service.url;

import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.error.CompilerErrorHolder;
import cool.klass.model.converter.compiler.state.AntlrMultiplicity;
import cool.klass.model.converter.compiler.state.AntlrNamedElement;
import cool.klass.model.converter.compiler.state.AntlrType;
import cool.klass.model.meta.domain.service.url.UrlParameter.UrlParameterBuilder;
import org.antlr.v4.runtime.ParserRuleContext;

public abstract class AntlrUrlParameter extends AntlrNamedElement
{
    @Nonnull
    protected final AntlrMultiplicity multiplicityState;
    @Nonnull
    protected final AntlrUrl          urlState;

    public AntlrUrlParameter(
            @Nonnull ParserRuleContext elementContext,
            CompilationUnit compilationUnit,
            boolean inferred,
            @Nonnull ParserRuleContext nameContext,
            @Nonnull String name,
            @Nonnull AntlrMultiplicity multiplicityState,
            @Nonnull AntlrUrl urlState)
    {
        super(elementContext, compilationUnit, inferred, nameContext, name);
        this.multiplicityState = Objects.requireNonNull(multiplicityState);
        this.urlState = Objects.requireNonNull(urlState);
    }

    public void reportDuplicateParameterName(@Nonnull CompilerErrorHolder compilerErrorHolder)
    {
        String message = String.format("ERR_DUP_PAR: Duplicate parameter: '%s'.", this.name);

        compilerErrorHolder.add(
                this.compilationUnit,
                message,
                this.nameContext,
                this.urlState.getServiceGroup().getElementContext());
    }

    @Nonnull
    public abstract AntlrType getType();

    public abstract UrlParameterBuilder build();

    @Nonnull
    public abstract UrlParameterBuilder getUrlParameterBuilder();
}

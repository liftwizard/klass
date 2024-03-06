package cool.klass.model.converter.compiler.state.service.url;

import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.error.CompilerErrorState;
import cool.klass.model.converter.compiler.state.AntlrNamedElement;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.meta.domain.service.url.UrlConstantImpl.UrlConstantBuilder;
import org.antlr.v4.runtime.ParserRuleContext;

public class AntlrUrlConstant extends AntlrNamedElement
{
    public AntlrUrlConstant(
            @Nonnull ParserRuleContext elementContext,
            @Nullable CompilationUnit compilationUnit,
            boolean inferred,
            @Nonnull ParserRuleContext nameContext,
            @Nonnull String name,
            int ordinal)
    {
        super(elementContext, compilationUnit, inferred, nameContext, name, ordinal);
    }

    @Nonnull
    @Override
    public Optional<IAntlrElement> getSurroundingElement()
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName()
                + ".getSurroundingContext() not implemented yet");
    }

    @Override
    public boolean omitParentFromSurroundingElements()
    {
        return true;
    }

    @Nonnull
    public UrlConstantBuilder build()
    {
        return new UrlConstantBuilder(this.elementContext, this.inferred, this.nameContext, this.name, this.ordinal);
    }

    @Nonnull
    @Override
    protected Pattern getNamePattern()
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName()
                + ".getNamePattern() not implemented yet");
    }

    @Override
    public void reportNameErrors(@Nonnull CompilerErrorState compilerErrorHolder)
    {
        // TODO: URLs can contain almost anything. The parser is probably already more strict than any error checking that needs to happen here.
        // https://stackoverflow.com/questions/7109143/what-characters-are-valid-in-a-url
    }
}

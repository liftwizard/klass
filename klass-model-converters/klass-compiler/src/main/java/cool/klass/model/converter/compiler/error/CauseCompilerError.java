package cool.klass.model.converter.compiler.error;

import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ImmutableList;

public class CauseCompilerError extends AbstractCompilerError
{
    public CauseCompilerError(
            @Nonnull CompilationUnit compilationUnit,
            @Nonnull Optional<CauseCompilerError> macroCause,
            @Nonnull ImmutableList<ParserRuleContext> offendingContexts,
            @Nonnull ImmutableList<IAntlrElement> sourceContexts)
    {
        super(compilationUnit, macroCause, offendingContexts, sourceContexts);
    }

    @Nonnull
    @Override
    public String toString()
    {
        String contextString   = this.getContextString();
        String locationMessage = this.getOptionalLocationMessage();
        String causeString     = this.getCauseString();

        return "\n"
                + "Which was generated by macro at location " + this.getShortLocationString() + "\n"
                + contextString
                + locationMessage
                + causeString;
    }
}

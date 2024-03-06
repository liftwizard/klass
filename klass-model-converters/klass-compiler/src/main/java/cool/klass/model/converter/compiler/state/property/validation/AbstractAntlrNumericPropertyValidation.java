package cool.klass.model.converter.compiler.state.property.validation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.state.property.AntlrDataTypeProperty;
import cool.klass.model.meta.domain.property.validation.AbstractNumericPropertyValidation.NumericPropertyValidationBuilder;
import org.antlr.v4.runtime.ParserRuleContext;

public abstract class AbstractAntlrNumericPropertyValidation extends AbstractAntlrPropertyValidation
{
    protected final int number;

    protected AbstractAntlrNumericPropertyValidation(
            @Nonnull ParserRuleContext elementContext,
            @Nullable CompilationUnit compilationUnit,
            boolean inferred,
            AntlrDataTypeProperty<?> owningPropertyState,
            int number)
    {
        super(elementContext, compilationUnit, inferred, owningPropertyState);
        this.number = number;
    }

    @Override
    public abstract NumericPropertyValidationBuilder build();

    @Override
    public abstract NumericPropertyValidationBuilder getElementBuilder();
}

package cool.klass.model.meta.domain.value.literal;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.api.value.literal.StringLiteralValue;
import org.antlr.v4.runtime.ParserRuleContext;

public final class StringLiteralValueImpl
        extends AbstractLiteralValue
        implements StringLiteralValue
{
    @Nonnull
    private final String value;

    private StringLiteralValueImpl(
            @Nonnull ParserRuleContext elementContext,
            @Nonnull Optional<Element> macroElement,
            @Nullable SourceCode sourceCode,
            @Nonnull String value)
    {
        super(elementContext, macroElement, sourceCode);
        this.value = Objects.requireNonNull(value);
    }

    @Override
    @Nonnull
    public String getValue()
    {
        return this.value;
    }

    public static final class StringLiteralValueBuilder
            extends AbstractLiteralValueBuilder<StringLiteralValueImpl>
    {
        @Nonnull
        private final String value;

        public StringLiteralValueBuilder(
                @Nonnull ParserRuleContext elementContext,
                @Nonnull Optional<ElementBuilder<?>> macroElement,
                @Nullable SourceCodeBuilder sourceCode,
                @Nonnull String value)
        {
            super(elementContext, macroElement, sourceCode);
            this.value = Objects.requireNonNull(value);
        }

        @Override
        @Nonnull
        protected StringLiteralValueImpl buildUnsafe()
        {
            return new StringLiteralValueImpl(
                    this.elementContext,
                    this.macroElement.map(ElementBuilder::getElement),
                    this.sourceCode.build(),
                    this.value);
        }
    }
}

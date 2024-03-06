package cool.klass.model.meta.domain.value;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import cool.klass.model.meta.domain.api.value.VariableReference;
import cool.klass.model.meta.domain.parameter.ParameterImpl;
import cool.klass.model.meta.domain.parameter.ParameterImpl.ParameterBuilder;
import cool.klass.model.meta.grammar.KlassParser.VariableReferenceContext;

public final class VariableReferenceImpl
        extends AbstractExpressionValue
        implements VariableReference
{
    @Nonnull
    private final ParameterImpl parameter;

    private VariableReferenceImpl(
            @Nonnull VariableReferenceContext elementContext,
            @Nonnull Optional<Element> macroElement,
            @Nullable SourceCode sourceCode,
            @Nonnull ParameterImpl parameter)
    {
        super(elementContext, macroElement, sourceCode);
        this.parameter = Objects.requireNonNull(parameter);
    }

    @Nonnull
    @Override
    public VariableReferenceContext getElementContext()
    {
        return (VariableReferenceContext) super.getElementContext();
    }

    @Override
    @Nonnull
    public ParameterImpl getParameter()
    {
        return this.parameter;
    }

    public static final class VariableReferenceBuilder
            extends AbstractExpressionValueBuilder<VariableReferenceImpl>
    {
        @Nonnull
        private final ParameterBuilder parameterBuilder;

        public VariableReferenceBuilder(
                @Nonnull VariableReferenceContext elementContext,
                @Nonnull Optional<ElementBuilder<?>> macroElement,
                @Nullable SourceCodeBuilder sourceCode,
                @Nonnull ParameterBuilder parameterBuilder)
        {
            super(elementContext, macroElement, sourceCode);
            this.parameterBuilder = Objects.requireNonNull(parameterBuilder);
        }

        @Override
        @Nonnull
        protected VariableReferenceImpl buildUnsafe()
        {
            return new VariableReferenceImpl(
                    (VariableReferenceContext) this.elementContext,
                    this.macroElement.map(ElementBuilder::getElement),
                    this.sourceCode.build(),
                    this.parameterBuilder.getElement());
        }
    }
}

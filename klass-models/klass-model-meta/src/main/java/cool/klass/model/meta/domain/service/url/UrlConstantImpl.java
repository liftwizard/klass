package cool.klass.model.meta.domain.service.url;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.AbstractNamedElement;
import cool.klass.model.meta.domain.api.service.url.UrlConstant;
import org.antlr.v4.runtime.ParserRuleContext;

public final class UrlConstantImpl extends AbstractNamedElement implements AbstractUrlPathSegment, UrlConstant
{
    private UrlConstantImpl(
            @Nonnull ParserRuleContext elementContext,
            boolean inferred,
            @Nonnull ParserRuleContext nameContext,
            @Nonnull String name,
            int ordinal)
    {
        super(elementContext, inferred, nameContext, name, ordinal);
    }

    @Override
    public String toString()
    {
        return this.getName();
    }

    public static final class UrlConstantBuilder
            extends NamedElementBuilder<UrlConstantImpl>
            implements UrlPathSegmentBuilder
    {
        public UrlConstantBuilder(
                @Nonnull ParserRuleContext elementContext,
                boolean inferred,
                @Nonnull ParserRuleContext nameContext,
                @Nonnull String name,
                int ordinal)
        {
            super(elementContext, inferred, nameContext, name, ordinal);
        }

        @Override
        @Nonnull
        protected UrlConstantImpl buildUnsafe()
        {
            return new UrlConstantImpl(this.elementContext, this.inferred, this.nameContext, this.name, this.ordinal);
        }
    }
}

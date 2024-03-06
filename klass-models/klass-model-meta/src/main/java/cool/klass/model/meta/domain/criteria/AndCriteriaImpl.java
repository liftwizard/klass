package cool.klass.model.meta.domain.criteria;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.criteria.AndCriteria;
import org.antlr.v4.runtime.ParserRuleContext;

public final class AndCriteriaImpl extends AbstractBinaryCriteria implements AndCriteria
{
    private AndCriteriaImpl(
            @Nonnull ParserRuleContext elementContext,
            boolean inferred,
            @Nonnull AbstractCriteria left,
            @Nonnull AbstractCriteria right)
    {
        super(elementContext, inferred, left, right);
    }

    public static final class AndCriteriaBuilder extends AbstractBinaryCriteriaBuilder<AndCriteriaImpl>
    {
        public AndCriteriaBuilder(
                @Nonnull ParserRuleContext elementContext,
                boolean inferred,
                @Nonnull AbstractCriteriaBuilder<?> left,
                @Nonnull AbstractCriteriaBuilder<?> right)
        {
            super(elementContext, inferred, left, right);
        }

        @Override
        @Nonnull
        protected AndCriteriaImpl buildUnsafe()
        {
            return new AndCriteriaImpl(
                    this.elementContext,
                    this.inferred,
                    this.left.build(),
                    this.right.build());
        }
    }
}

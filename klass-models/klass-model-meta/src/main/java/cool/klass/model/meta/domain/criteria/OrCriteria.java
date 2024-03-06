package cool.klass.model.meta.domain.criteria;

import javax.annotation.Nonnull;

import org.antlr.v4.runtime.ParserRuleContext;

public final class OrCriteria extends BinaryCriteria
{
    private OrCriteria(
            @Nonnull ParserRuleContext elementContext,
            boolean inferred,
            @Nonnull Criteria left,
            @Nonnull Criteria right)
    {
        super(elementContext, inferred, left, right);
    }

    @Override
    public void visit(@Nonnull CriteriaVisitor visitor)
    {
        visitor.visitOr(this);
    }

    public static final class OrCriteriaBuilder extends BinaryCriteriaBuilder
    {
        public OrCriteriaBuilder(
                @Nonnull ParserRuleContext elementContext,
                boolean inferred,
                @Nonnull CriteriaBuilder left,
                @Nonnull CriteriaBuilder right)
        {
            super(elementContext, inferred, left, right);
        }

        @Nonnull
        @Override
        public OrCriteria build()
        {
            return new OrCriteria(
                    this.elementContext,
                    this.inferred,
                    this.left.build(),
                    this.right.build());
        }
    }
}

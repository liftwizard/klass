package cool.klass.model.converter.compiler.state;

import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.PrimitiveType;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Lists;

public final class AntlrPrimitiveType extends AntlrElement implements AntlrType
{
    public static final AntlrPrimitiveType INTEGER          = new AntlrPrimitiveType(PrimitiveType.INTEGER);
    public static final AntlrPrimitiveType LONG             = new AntlrPrimitiveType(PrimitiveType.LONG);
    public static final AntlrPrimitiveType DOUBLE           = new AntlrPrimitiveType(PrimitiveType.DOUBLE);
    public static final AntlrPrimitiveType FLOAT            = new AntlrPrimitiveType(PrimitiveType.FLOAT);
    public static final AntlrPrimitiveType BOOLEAN          = new AntlrPrimitiveType(PrimitiveType.BOOLEAN);
    public static final AntlrPrimitiveType STRING           = new AntlrPrimitiveType(PrimitiveType.STRING);
    public static final AntlrPrimitiveType INSTANT          = new AntlrPrimitiveType(PrimitiveType.INSTANT);
    public static final AntlrPrimitiveType LOCAL_DATE       = new AntlrPrimitiveType(PrimitiveType.LOCAL_DATE);
    public static final AntlrPrimitiveType TEMPORAL_INSTANT = new AntlrPrimitiveType(PrimitiveType.TEMPORAL_INSTANT);
    public static final AntlrPrimitiveType TEMPORAL_RANGE   = new AntlrPrimitiveType(PrimitiveType.TEMPORAL_RANGE);

    @Nonnull
    public static final AntlrPrimitiveType AMBIGUOUS = new AntlrPrimitiveType(null);

    public static final ImmutableList<AntlrPrimitiveType> PRIMITIVE_TYPES = Lists.immutable.with(
            STRING,
            INTEGER,
            LONG,
            DOUBLE,
            FLOAT,
            BOOLEAN,
            INSTANT,
            LOCAL_DATE,
            TEMPORAL_INSTANT,
            TEMPORAL_RANGE);

    private static final ImmutableMap<PrimitiveType, AntlrPrimitiveType> BY_TYPE = PRIMITIVE_TYPES.groupByUniqueKey(
            AntlrPrimitiveType::getPrimitiveType);

    private final PrimitiveType primitiveType;

    private AntlrPrimitiveType(PrimitiveType primitiveType)
    {
        super(new ParserRuleContext(), null, true);
        this.primitiveType = primitiveType;
    }

    public static AntlrPrimitiveType valueOf(PrimitiveType type)
    {
        return Objects.requireNonNull(BY_TYPE.get(type));
    }

    @Nonnull
    public PrimitiveType getPrimitiveType()
    {
        return Objects.requireNonNull(this.primitiveType);
    }

    @Override
    public String toString()
    {
        return this.primitiveType.toString();
    }

    @Override
    public PrimitiveType getTypeGetter()
    {
        return this.primitiveType;
    }

    public boolean isTemporal()
    {
        return this == TEMPORAL_INSTANT || this == TEMPORAL_RANGE;
    }
}

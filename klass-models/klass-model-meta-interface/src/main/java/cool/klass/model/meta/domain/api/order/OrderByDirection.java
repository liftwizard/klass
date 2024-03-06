package cool.klass.model.meta.domain.api.order;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Lists;

public enum OrderByDirection
{
    ASCENDING("ascending"),
    DESCENDING("descending");

    public static final ImmutableList<OrderByDirection> ORDER_BY_DIRECTIONS = Lists.immutable.with(
            ASCENDING,
            DESCENDING);

    private static final ImmutableMap<String, OrderByDirection> BY_PRETTY_NAME =
            ORDER_BY_DIRECTIONS.groupByUniqueKey(OrderByDirection::getPrettyName);

    @Nonnull
    private final String prettyName;

    OrderByDirection(@Nonnull String prettyName)
    {
        this.prettyName = prettyName;
    }

    public static OrderByDirection byPrettyName(String name)
    {
        return Objects.requireNonNull(BY_PRETTY_NAME.get(name));
    }

    @Nonnull
    public String getPrettyName()
    {
        return this.prettyName;
    }

    @Nonnull
    @Override
    public String toString()
    {
        return this.getPrettyName();
    }
}

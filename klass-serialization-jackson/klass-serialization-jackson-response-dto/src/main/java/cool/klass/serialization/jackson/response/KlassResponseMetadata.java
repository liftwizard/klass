package cool.klass.serialization.jackson.response;

import java.security.Principal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonProperty;
import cool.klass.logging.context.MDCCloseable;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.projection.Projection;

public class KlassResponseMetadata
{
    @Nonnull
    private final Optional<String> criteria;
    @Nonnull
    private final Optional<String> orderBy;
    @Nonnull
    private final Multiplicity     multiplicity;
    @Nonnull
    private final Projection       projection;

    @Nonnull
    private final Instant                           transactionTimestamp;
    @Nonnull
    private final Optional<KlassResponsePagination> pagination;
    @Nonnull
    private final Optional<? extends Principal>     principal;

    public KlassResponseMetadata(
            @Nonnull Optional<String> criteria,
            @Nonnull Optional<String> orderBy,
            @Nonnull Multiplicity multiplicity,
            @Nonnull Projection projection,
            @Nonnull Instant transactionTimestamp,
            @Nonnull Optional<KlassResponsePagination> pagination,
            @Nonnull Optional<? extends Principal> principal)
    {
        this.projection           = Objects.requireNonNull(projection);
        this.multiplicity         = Objects.requireNonNull(multiplicity);
        this.pagination           = Objects.requireNonNull(pagination);
        this.transactionTimestamp = Objects.requireNonNull(transactionTimestamp);
        this.principal            = Objects.requireNonNull(principal);
        this.criteria             = Objects.requireNonNull(criteria);
        this.orderBy              = Objects.requireNonNull(orderBy);
    }

    @JsonProperty
    @Nonnull
    public Optional<String> getCriteria()
    {
        return this.criteria;
    }

    @JsonProperty
    @Nonnull
    public Optional<String> getOrderBy()
    {
        return this.orderBy;
    }

    @JsonProperty
    @Nonnull
    public Multiplicity getMultiplicity()
    {
        return this.multiplicity;
    }

    @JsonProperty
    @Nonnull
    public Projection getProjection()
    {
        return this.projection;
    }

    @JsonProperty
    @Nonnull
    public Instant getTransactionTimestamp()
    {
        return this.transactionTimestamp;
    }

    @JsonProperty
    @Nonnull
    public Optional<KlassResponsePagination> getPagination()
    {
        return this.pagination;
    }

    @JsonProperty
    @Nonnull
    public Optional<? extends Principal> getPrincipal()
    {
        return this.principal;
    }

    @Override
    public String toString()
    {
        return String.format(
                "{\"criteria\":%s,\"orderBy\":%s,\"multiplicity\":%s,\"projection\":%s,\"transactionTimestamp\":%s,\"pagination\":%s,\"principal\":%s}",
                this.criteria.orElse(null),
                this.orderBy.orElse(null),
                this.multiplicity.getPrettyName(),
                this.projection,
                this.transactionTimestamp,
                this.pagination.orElse(null),
                this.principal.orElse(null));
    }

    public void withMDC(MDCCloseable mdc)
    {
        mdc.put("klass.response.criteria", this.criteria.orElse(null));
        mdc.put("klass.response.orderBy", this.orderBy.orElse(null));
        mdc.put("klass.response.multiplicity", this.multiplicity.getPrettyName());
        mdc.put("klass.response.projection.name", String.valueOf(this.projection));
        mdc.put("klass.response.projection.class", this.projection.getKlass().getFullyQualifiedName());
        mdc.put("klass.response.transactionTimestamp", String.valueOf(this.transactionTimestamp));
        mdc.put("klass.response.principal", this.principal.map(Object::toString).orElse(null));
        this.pagination.ifPresent(responsePagination -> responsePagination.withMDC(mdc));
    }
}

/*
 * Copyright 2025 Craig Motlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cool.klass.model.converter.compiler.state;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.PrimitiveTypeBuilder;
import cool.klass.model.meta.domain.api.PrimitiveType;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Lists;

public final class AntlrPrimitiveType extends AntlrElement implements AntlrType {

    public static final AntlrPrimitiveType INTEGER = new AntlrPrimitiveType(PrimitiveType.INTEGER);
    public static final AntlrPrimitiveType LONG = new AntlrPrimitiveType(PrimitiveType.LONG);
    public static final AntlrPrimitiveType DOUBLE = new AntlrPrimitiveType(PrimitiveType.DOUBLE);
    public static final AntlrPrimitiveType FLOAT = new AntlrPrimitiveType(PrimitiveType.FLOAT);
    public static final AntlrPrimitiveType BOOLEAN = new AntlrPrimitiveType(PrimitiveType.BOOLEAN);
    public static final AntlrPrimitiveType STRING = new AntlrPrimitiveType(PrimitiveType.STRING);
    public static final AntlrPrimitiveType INSTANT = new AntlrPrimitiveType(PrimitiveType.INSTANT);
    public static final AntlrPrimitiveType LOCAL_DATE = new AntlrPrimitiveType(PrimitiveType.LOCAL_DATE);
    public static final AntlrPrimitiveType TEMPORAL_INSTANT = new AntlrPrimitiveType(PrimitiveType.TEMPORAL_INSTANT);
    public static final AntlrPrimitiveType TEMPORAL_RANGE = new AntlrPrimitiveType(PrimitiveType.TEMPORAL_RANGE);

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
        TEMPORAL_RANGE
    );

    private static final ImmutableMap<PrimitiveType, AntlrPrimitiveType> BY_TYPE = PRIMITIVE_TYPES.groupByUniqueKey(
        AntlrPrimitiveType::getPrimitiveType
    );

    private final PrimitiveType primitiveType;

    private AntlrPrimitiveType(PrimitiveType primitiveType) {
        super(new ParserRuleContext(), Optional.empty());
        this.primitiveType = primitiveType;
    }

    public static AntlrPrimitiveType valueOf(PrimitiveType type) {
        return Objects.requireNonNull(BY_TYPE.get(type));
    }

    @Nonnull
    @Override
    public Optional<IAntlrElement> getSurroundingElement() {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".getSurroundingContext() not implemented yet"
        );
    }

    @Override
    public String getName() {
        return this.primitiveType.getPrettyName();
    }

    @Nonnull
    public PrimitiveType getPrimitiveType() {
        return Objects.requireNonNull(this.primitiveType);
    }

    @Nonnull
    @Override
    public String toString() {
        Objects.requireNonNull(this.primitiveType);
        return this.primitiveType.toString();
    }

    @Override
    public PrimitiveType getTypeGetter() {
        return this.primitiveType;
    }

    @Nonnull
    @Override
    public PrimitiveTypeBuilder getElementBuilder() {
        return new PrimitiveTypeBuilder(this.getElementContext(), this.getMacroElementBuilder(), this.primitiveType);
    }

    @Override
    public ImmutableList<AntlrType> getPotentialWiderTypes() {
        return switch (this.primitiveType) {
            case INTEGER -> Lists.immutable.with(INTEGER, LONG, FLOAT, DOUBLE);
            case LONG -> Lists.immutable.with(LONG, DOUBLE);
            case DOUBLE -> Lists.immutable.with(DOUBLE);
            case FLOAT -> Lists.immutable.with(FLOAT, DOUBLE);
            case BOOLEAN -> Lists.immutable.with(BOOLEAN);
            case STRING -> Lists.immutable.with(STRING);
            case INSTANT -> Lists.immutable.with(INSTANT, TEMPORAL_INSTANT, TEMPORAL_RANGE);
            case LOCAL_DATE -> Lists.immutable.with(LOCAL_DATE);
            case TEMPORAL_INSTANT -> Lists.immutable.with(TEMPORAL_INSTANT);
            case TEMPORAL_RANGE -> Lists.immutable.with(TEMPORAL_RANGE);
        };
    }
}

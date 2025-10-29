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

package cool.klass.model.meta.domain.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.DataType.DataTypeGetter;
import cool.klass.model.meta.domain.api.visitor.PrimitiveTypeVisitor;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Lists;

/**
 * Predefined native types.
 */
public enum PrimitiveType implements Element, DataType, DataTypeGetter {
    INTEGER("Integer", true, Integer.class) {
        @Override
        public void visit(@Nonnull PrimitiveTypeVisitor visitor) {
            try {
                visitor.visitInteger();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    },
    LONG("Long", true, Long.class) {
        @Override
        public void visit(@Nonnull PrimitiveTypeVisitor visitor) {
            try {
                visitor.visitLong();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    },
    DOUBLE("Double", true, Double.class) {
        @Override
        public void visit(@Nonnull PrimitiveTypeVisitor visitor) {
            try {
                visitor.visitDouble();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    },
    FLOAT("Float", true, Float.class) {
        @Override
        public void visit(@Nonnull PrimitiveTypeVisitor visitor) {
            try {
                visitor.visitFloat();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    },
    BOOLEAN("Boolean", false, Boolean.class) {
        @Override
        public void visit(@Nonnull PrimitiveTypeVisitor visitor) {
            try {
                visitor.visitBoolean();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    },
    STRING("String", false, String.class) {
        @Override
        public void visit(@Nonnull PrimitiveTypeVisitor visitor) {
            try {
                visitor.visitString();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    },
    INSTANT("Instant", false, Instant.class) {
        @Override
        public void visit(@Nonnull PrimitiveTypeVisitor visitor) {
            try {
                visitor.visitInstant();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    },
    // TODO: OffsetDateTime
    LOCAL_DATE("LocalDate", false, LocalDate.class) {
        @Override
        public void visit(@Nonnull PrimitiveTypeVisitor visitor) {
            try {
                visitor.visitLocalDate();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    },
    TEMPORAL_INSTANT("TemporalInstant", false, Instant.class) {
        @Override
        public void visit(@Nonnull PrimitiveTypeVisitor visitor) {
            try {
                visitor.visitTemporalInstant();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    },
    TEMPORAL_RANGE("TemporalRange", false, Instant.class) {
        @Override
        public void visit(@Nonnull PrimitiveTypeVisitor visitor) {
            try {
                visitor.visitTemporalRange();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };

    public static final ImmutableList<PrimitiveType> PRIMITIVE_TYPES = Lists.immutable.with(
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

    public static final ImmutableList<PrimitiveType> ID_PRIMITIVE_TYPES = Lists.immutable.with(INTEGER, LONG, STRING);

    private static final ImmutableMap<String, PrimitiveType> BY_PRETTY_NAME = PRIMITIVE_TYPES.groupByUniqueKey(
        PrimitiveType::getPrettyName
    );

    @Nonnull
    private final String prettyName;

    private final boolean isNumeric;

    @Nonnull
    private final Class<?> javaClass;

    PrimitiveType(@Nonnull String prettyName, boolean isNumeric, @Nonnull Class<?> javaClass) {
        this.prettyName = Objects.requireNonNull(prettyName);
        this.isNumeric = isNumeric;
        this.javaClass = Objects.requireNonNull(javaClass);
    }

    public static PrimitiveType byPrettyName(String name) {
        return Objects.requireNonNull(BY_PRETTY_NAME.get(name));
    }

    @Nonnull
    public String getPrettyName() {
        return this.prettyName;
    }

    @Override
    public String getDataTypeName() {
        return this.getPrettyName();
    }

    @Nonnull
    @Override
    public String toString() {
        return this.getPrettyName();
    }

    public abstract void visit(@Nonnull PrimitiveTypeVisitor visitor);

    public boolean isTemporal() {
        return this.isTemporalRange() || this.isTemporalInstant();
    }

    public boolean isTemporalRange() {
        return this == TEMPORAL_RANGE;
    }

    public boolean isTemporalInstant() {
        return this == TEMPORAL_INSTANT;
    }

    public boolean isId() {
        return ID_PRIMITIVE_TYPES.contains(this);
    }

    public boolean isNumeric() {
        return this.isNumeric;
    }

    public Class<?> getJavaClass() {
        return this.javaClass;
    }

    @Nonnull
    @Override
    public PrimitiveType getType() {
        return this;
    }

    @Override
    public Optional<Element> getMacroElement() {
        return Optional.empty();
    }

    @Override
    public String getName() {
        return this.getPrettyName();
    }
}

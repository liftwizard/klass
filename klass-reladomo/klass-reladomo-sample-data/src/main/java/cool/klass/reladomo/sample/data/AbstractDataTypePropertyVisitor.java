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

package cool.klass.reladomo.sample.data;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.EnumerationLiteral;
import cool.klass.model.meta.domain.api.NamedElement;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.visitor.DataTypePropertyVisitor;
import org.eclipse.collections.api.bag.sorted.MutableSortedBag;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.SortedBags;

public abstract class AbstractDataTypePropertyVisitor implements DataTypePropertyVisitor {

    private final MutableSortedBag<DataTypeProperty> propertyCounts = SortedBags.mutable.empty(
        AbstractDataTypePropertyVisitor.getDataTypePropertyComparator()
    );

    private Object result;

    private static Comparator<DataTypeProperty> getDataTypePropertyComparator() {
        Comparator<DataTypeProperty> byClassifierName = Comparator.comparing((dtp) ->
            dtp.getOwningClassifier().getName()
        );
        return byClassifierName.thenComparing(NamedElement::getName);
    }

    public Object getResult() {
        return this.result;
    }

    @Override
    public void visitEnumerationProperty(@Nonnull EnumerationProperty enumerationProperty) {
        ImmutableList<EnumerationLiteral> enumerationLiterals = enumerationProperty.getType().getEnumerationLiterals();
        // TODO: Compiler error for enumeration with <2 literals
        this.result = enumerationLiterals.get(this.getIndex() - 1);
    }

    @Override
    public void visitString(@Nonnull PrimitiveProperty primitiveProperty) {
        // TODO: Something more reliable, or ban shared foreign keys
        if (primitiveProperty.getKeysMatchingThisForeignKey().size() > 1) {
            throw new AssertionError(primitiveProperty);
        }
        if (primitiveProperty.getKeysMatchingThisForeignKey().size() == 1) {
            Pair<AssociationEnd, DataTypeProperty> pair = primitiveProperty
                .getKeysMatchingThisForeignKey()
                .keyValuesView()
                .getOnly();

            AssociationEnd associationEnd = pair.getOne();
            DataTypeProperty keyProperty = pair.getTwo();
            this.result = String.format(
                "%s %s %d %s",
                associationEnd.getType().getName(),
                keyProperty.getName(),
                this.getIndex(),
                this.getEmoji()
            );
        } else {
            this.result = String.format(
                "%s %s %d %s",
                primitiveProperty.getOwningClassifier().getName(),
                primitiveProperty.getName(),
                this.getNumber(primitiveProperty),
                this.getEmoji()
            );
        }
    }

    @Override
    public void visitInteger(@Nonnull PrimitiveProperty primitiveProperty) {
        this.result = this.getAdjustment(primitiveProperty);
    }

    @Override
    public void visitLong(@Nonnull PrimitiveProperty primitiveProperty) {
        if (primitiveProperty.isForeignKey()) {
            this.result = (long) this.getIndex();
        } else if (primitiveProperty.isKey()) {
            this.result = (long) this.getNumber(primitiveProperty);
        } else {
            this.result = 100_000_000_000L * this.getNumber(primitiveProperty);
        }
    }

    @Override
    public void visitDouble(@Nonnull PrimitiveProperty primitiveProperty) {
        this.result = this.getAdjustment(primitiveProperty) + 0.0123456789;
    }

    @Override
    public void visitFloat(@Nonnull PrimitiveProperty primitiveProperty) {
        this.result = this.getAdjustment(primitiveProperty) + 0.01234567f;
    }

    @Override
    public void visitBoolean(PrimitiveProperty primitiveProperty) {
        this.result = this.getBoolean();
    }

    @Override
    public void visitInstant(@Nonnull PrimitiveProperty primitiveProperty) {
        this.result = this.getUniqueLocalDateTime(primitiveProperty).toInstant(ZoneOffset.UTC);
    }

    @Override
    public void visitLocalDate(@Nonnull PrimitiveProperty primitiveProperty) {
        this.result = this.getUniqueLocalDateTime(primitiveProperty).toLocalDate();
    }

    @Override
    public void visitTemporalInstant(PrimitiveProperty primitiveProperty) {
        throw new UnsupportedOperationException(
            this.getClass().getSimpleName() + ".visitTemporalInstant() not implemented yet"
        );
    }

    @Override
    public void visitTemporalRange(@Nonnull PrimitiveProperty primitiveProperty) {
        if (!primitiveProperty.isSystem()) {
            throw new AssertionError();
        }
    }

    protected abstract boolean getBoolean();

    @Nonnull
    protected abstract String getEmoji();

    protected abstract int getIndex();

    @Nonnull
    protected abstract LocalDateTime getLocalDateTime();

    private int getAdjustment(@Nonnull PrimitiveProperty primitiveProperty) {
        return primitiveProperty.isForeignKey() ? this.getIndex() : this.getNumber(primitiveProperty);
    }

    private LocalDateTime getUniqueLocalDateTime(@Nonnull PrimitiveProperty primitiveProperty) {
        if (primitiveProperty.isForeignKey()) {
            return this.getLocalDateTime();
        }

        int occurrences = this.propertyCounts.occurrencesOf(primitiveProperty);
        return this.getLocalDateTime().plus(occurrences, ChronoUnit.YEARS);
    }

    private int getNumber(DataTypeProperty dataTypeProperty) {
        int occurrences = this.propertyCounts.addOccurrences(dataTypeProperty, 1);
        int oldOccurrences = occurrences - 1;
        return oldOccurrences * 2 + this.getIndex();
    }
}

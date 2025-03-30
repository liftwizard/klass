/*
 * Copyright 2024 Craig Motlin
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

package cool.klass.model.meta.domain.api.property;

import cool.klass.model.meta.domain.api.visitor.DataTypePropertyVisitor;
import cool.klass.model.meta.domain.api.visitor.PrimitiveTypeVisitor;

public class DataTypePropertyVisitorAdaptor implements PrimitiveTypeVisitor {

    private final DataTypePropertyVisitor visitor;
    private final PrimitiveProperty primitiveProperty;

    public DataTypePropertyVisitorAdaptor(DataTypePropertyVisitor visitor, PrimitiveProperty primitiveProperty) {
        this.visitor = visitor;
        this.primitiveProperty = primitiveProperty;
    }

    @Override
    public void visitString() {
        this.visitor.visitString(this.primitiveProperty);
    }

    @Override
    public void visitInteger() {
        this.visitor.visitInteger(this.primitiveProperty);
    }

    @Override
    public void visitLong() {
        this.visitor.visitLong(this.primitiveProperty);
    }

    @Override
    public void visitDouble() {
        this.visitor.visitDouble(this.primitiveProperty);
    }

    @Override
    public void visitFloat() {
        this.visitor.visitFloat(this.primitiveProperty);
    }

    @Override
    public void visitBoolean() {
        this.visitor.visitBoolean(this.primitiveProperty);
    }

    @Override
    public void visitInstant() {
        this.visitor.visitInstant(this.primitiveProperty);
    }

    @Override
    public void visitLocalDate() {
        this.visitor.visitLocalDate(this.primitiveProperty);
    }

    @Override
    public void visitTemporalInstant() {
        this.visitor.visitTemporalInstant(this.primitiveProperty);
    }

    @Override
    public void visitTemporalRange() {
        this.visitor.visitTemporalRange(this.primitiveProperty);
    }
}

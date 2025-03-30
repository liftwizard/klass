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

package cool.klass.model.meta.domain.api.visitor;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.PrimitiveType;

public class PrimitiveToJavaTypeVisitor implements PrimitiveTypeVisitor {

    private String result;

    public static String getJavaType(@Nonnull PrimitiveType primitiveType) {
        PrimitiveToJavaTypeVisitor primitiveToJavaTypeVisitor = new PrimitiveToJavaTypeVisitor();
        primitiveType.visit(primitiveToJavaTypeVisitor);
        return primitiveToJavaTypeVisitor.getResult();
    }

    public String getResult() {
        return this.result;
    }

    @Override
    public void visitString() {
        this.result = "String";
    }

    @Override
    public void visitInteger() {
        this.result = "Integer";
    }

    @Override
    public void visitLong() {
        this.result = "Long";
    }

    @Override
    public void visitDouble() {
        this.result = "Double";
    }

    @Override
    public void visitFloat() {
        this.result = "Float";
    }

    @Override
    public void visitBoolean() {
        this.result = "Boolean";
    }

    @Override
    public void visitInstant() {
        this.result = "Instant";
    }

    @Override
    public void visitLocalDate() {
        this.result = "LocalDate";
    }

    @Override
    public void visitTemporalInstant() {
        this.result = "Instant";
    }

    @Override
    public void visitTemporalRange() {
        this.result = "Instant";
    }
}

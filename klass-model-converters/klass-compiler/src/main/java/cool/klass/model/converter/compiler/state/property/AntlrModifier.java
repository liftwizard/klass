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

package cool.klass.model.converter.compiler.state.property;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.state.AntlrClassifier;
import cool.klass.model.converter.compiler.state.AntlrNamedElement;
import cool.klass.model.converter.compiler.state.AntlrOrdinalElement;
import cool.klass.model.converter.compiler.state.IAntlrElement;
import cool.klass.model.meta.domain.property.ModifierImpl.ModifierBuilder;
import cool.klass.model.meta.grammar.KlassParser.ClassifierModifierContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

// TODO: Specific subclasses for the specific antlr context types
public class AntlrModifier extends AntlrOrdinalElement {

    public static final AntlrModifier AMBIGUOUS = new AntlrModifier(
        new ClassifierModifierContext(AMBIGUOUS_PARENT, -1),
        Optional.empty(),
        -1,
        AntlrClassifier.AMBIGUOUS
    );

    public static final AntlrModifier NOT_FOUND = new AntlrModifier(
        new ClassifierModifierContext(NOT_FOUND_PARENT, -1),
        Optional.empty(),
        -1,
        AntlrClassifier.NOT_FOUND
    );

    public static final ImmutableList<String> AUDIT_PROPERTY_NAMES = Lists.immutable.with(
        "createdBy",
        "createdOn",
        "lastUpdatedBy"
    );

    private final AntlrNamedElement surroundingElement;
    private ModifierBuilder elementBuilder;

    public AntlrModifier(
        @Nonnull ParserRuleContext elementContext,
        @Nonnull Optional<CompilationUnit> compilationUnit,
        int ordinal,
        @Nonnull AntlrNamedElement surroundingElement
    ) {
        super(elementContext, compilationUnit, ordinal);
        this.surroundingElement = surroundingElement;
    }

    @Nonnull
    @Override
    public Optional<IAntlrElement> getSurroundingElement() {
        return Optional.of(this.surroundingElement);
    }

    public Token getKeywordToken() {
        ParserRuleContext elementContext = this.getElementContext();
        int childCount = elementContext.getChildCount();
        if (childCount != 1) {
            throw new AssertionError();
        }
        return elementContext.getStart();
    }

    public String getKeyword() {
        return this.getKeywordToken().getText();
    }

    public boolean is(String name) {
        return this.getKeyword().equals(name);
    }

    public boolean isAudit() {
        return this.is("audited") || AUDIT_PROPERTY_NAMES.contains(this.getKeyword());
    }

    public boolean isCreatedBy() {
        return this.is("createdBy");
    }

    public boolean isCreatedOn() {
        return this.is("createdOn");
    }

    public boolean isLastUpdatedBy() {
        return this.is("lastUpdatedBy");
    }

    public boolean isUser() {
        return this.is("userId");
    }

    public boolean isPrivate() {
        return this.is("private");
    }

    public boolean isFinal() {
        return this.is("final");
    }

    public boolean isSystem() {
        return this.is("system");
    }

    public boolean isValid() {
        return this.is("valid");
    }

    public boolean isFrom() {
        return this.is("from");
    }

    public boolean isTo() {
        return this.is("to");
    }

    public boolean isVersion() {
        return this.is("version");
    }

    public boolean isUserId() {
        return this.is("userId");
    }

    public boolean isId() {
        return this.is("id");
    }

    public boolean isKey() {
        return this.is("key");
    }

    public boolean isDerived() {
        return this.is("derived");
    }

    public boolean isOwned() {
        return this.is("owned");
    }

    public boolean isTransient() {
        return this.is("transient");
    }

    public boolean isSystemTemporal() {
        return this.is("systemTemporal");
    }

    public boolean isValidTemporal() {
        return this.is("validTemporal");
    }

    public boolean isBitemporal() {
        return this.is("bitemporal");
    }

    public boolean isTemporal() {
        return this.isSystemTemporal() || this.isValidTemporal() || this.isBitemporal();
    }

    @Nonnull
    public ModifierBuilder build() {
        if (this.elementBuilder != null) {
            throw new IllegalStateException();
        }
        this.elementBuilder = new ModifierBuilder(
            this.getElementContext(),
            this.getMacroElementBuilder(),
            this.getSourceCodeBuilder(),
            this.ordinal,
            this.surroundingElement.getElementBuilder()
        );
        return this.elementBuilder;
    }

    @Override
    @Nonnull
    public ModifierBuilder getElementBuilder() {
        return Objects.requireNonNull(this.elementBuilder);
    }

    @Override
    public String toString() {
        return this.getKeyword();
    }
}

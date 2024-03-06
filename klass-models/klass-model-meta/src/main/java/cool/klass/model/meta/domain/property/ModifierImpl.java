package cool.klass.model.meta.domain.property;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cool.klass.model.meta.domain.AbstractNamedElement;
import cool.klass.model.meta.domain.api.Element;
import cool.klass.model.meta.domain.api.modifier.Modifier;
import cool.klass.model.meta.domain.api.modifier.ModifierOwner;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode.SourceCodeBuilder;
import org.antlr.v4.runtime.ParserRuleContext;

public final class ModifierImpl
        extends AbstractNamedElement
        implements Modifier
{
    @Nonnull
    private final ModifierOwner modifierOwner;

    public ModifierImpl(
            @Nonnull ParserRuleContext elementContext,
            @Nonnull Optional<Element> macroElement,
            @Nullable SourceCode sourceCode,
            int ordinal,
            @Nonnull ParserRuleContext nameContext,
            @Nonnull ModifierOwner modifierOwner)
    {
        super(elementContext, macroElement, sourceCode, ordinal, nameContext);
        this.modifierOwner = Objects.requireNonNull(modifierOwner);
    }

    @Override
    public ModifierOwner getModifierOwner()
    {
        return this.modifierOwner;
    }

    public static final class ModifierBuilder
            extends NamedElementBuilder<ModifierImpl>
    {
        @Nonnull
        private final ElementBuilder<?> surroundingElementBuilder;

        public ModifierBuilder(
                @Nonnull ParserRuleContext elementContext,
                @Nonnull Optional<ElementBuilder<?>> macroElement,
                @Nullable SourceCodeBuilder sourceCode,
                int ordinal,
                @Nonnull ParserRuleContext nameContext,
                @Nonnull ElementBuilder<?> surroundingElementBuilder)
        {
            super(elementContext, macroElement, sourceCode, ordinal, nameContext);
            this.surroundingElementBuilder = Objects.requireNonNull(surroundingElementBuilder);
        }

        @Override
        @Nonnull
        protected ModifierImpl buildUnsafe()
        {
            return new ModifierImpl(
                    this.elementContext,
                    this.macroElement.map(ElementBuilder::getElement),
                    this.sourceCode.build(),
                    this.ordinal,
                    this.getNameContext(),
                    (ModifierOwner) this.surroundingElementBuilder.getElement());
        }
    }
}

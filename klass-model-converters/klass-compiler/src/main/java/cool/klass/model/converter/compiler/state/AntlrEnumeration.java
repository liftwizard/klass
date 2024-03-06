package cool.klass.model.converter.compiler.state;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.meta.domain.EnumerationImpl.EnumerationBuilder;
import cool.klass.model.meta.domain.EnumerationLiteralImpl.EnumerationLiteralBuilder;
import cool.klass.model.meta.grammar.KlassParser.EnumerationBodyContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;

public class AntlrEnumeration
        extends AntlrPackageableElement
        implements AntlrType, AntlrTopLevelElement
{
    //<editor-fold desc="AMBIGUOUS">
    public static final AntlrEnumeration AMBIGUOUS = new AntlrEnumeration(
            new EnumerationDeclarationContext(AMBIGUOUS_PARENT, -1),
            Optional.empty(),
            -1,
            new IdentifierContext(AMBIGUOUS_PARENT, -1),
            AntlrCompilationUnit.AMBIGUOUS)
    {
        @Override
        public String toString()
        {
            return AntlrEnumeration.class.getSimpleName() + ".AMBIGUOUS";
        }
    };
    //</editor-fold>

    //<editor-fold desc="NOT_FOUND">
    public static final AntlrEnumeration NOT_FOUND = new AntlrEnumeration(
            new EnumerationDeclarationContext(NOT_FOUND_PARENT, -1),
            Optional.empty(),
            -1,
            NOT_FOUND_IDENTIFIER_CONTEXT,
            AntlrCompilationUnit.NOT_FOUND)
    {
        @Override
        public String toString()
        {
            return AntlrEnumeration.class.getSimpleName() + ".NOT_FOUND";
        }
    };
    //</editor-fold>

    private final MutableList<AntlrEnumerationLiteral>               enumerationLiteral        = Lists.mutable.empty();
    private final MutableOrderedMap<String, AntlrEnumerationLiteral> enumerationLiteralsByName =
            OrderedMapAdapter.adapt(
                    new LinkedHashMap<>());

    private EnumerationBuilder enumerationBuilder;

    public AntlrEnumeration(
            @Nonnull EnumerationDeclarationContext elementContext,
            @Nonnull Optional<CompilationUnit> compilationUnit,
            int ordinal,
            @Nonnull IdentifierContext nameContext,
            @Nonnull AntlrCompilationUnit compilationUnitState)
    {
        super(elementContext, compilationUnit, ordinal, nameContext, compilationUnitState);
    }

    public int getNumLiterals()
    {
        return this.enumerationLiteral.size();
    }

    public void enterEnumerationLiteral(@Nonnull AntlrEnumerationLiteral enumerationLiteral)
    {
        this.enumerationLiteral.add(enumerationLiteral);
        this.enumerationLiteralsByName.compute(
                enumerationLiteral.getName(),
                (name, builder) -> builder == null
                        ? enumerationLiteral
                        : AntlrEnumerationLiteral.AMBIGUOUS);
    }

    public EnumerationBuilder build()
    {
        if (this.enumerationBuilder != null)
        {
            throw new IllegalStateException();
        }

        this.enumerationBuilder = new EnumerationBuilder(
                this.getElementContext(),
                this.getMacroElementBuilder(),
                this.getSourceCodeBuilder(),
                this.ordinal,
                this.getElementContext().identifier(),
                this.getPackageName());

        ImmutableList<EnumerationLiteralBuilder> enumerationLiteralBuilders = this.enumerationLiteral
                .collect(AntlrEnumerationLiteral::build)
                .toImmutable();

        this.enumerationBuilder.setEnumerationLiteralBuilders(enumerationLiteralBuilders);
        return this.enumerationBuilder;
    }

    @Nonnull
    @Override
    public EnumerationDeclarationContext getElementContext()
    {
        return (EnumerationDeclarationContext) super.getElementContext();
    }

    @Override
    public EnumerationBodyContext getBodyContext()
    {
        return this.getElementContext().enumerationBody();
    }

    @Nonnull
    @Override
    public EnumerationBuilder getElementBuilder()
    {
        return Objects.requireNonNull(this.enumerationBuilder);
    }

    //<editor-fold desc="Report Compiler Errors">
    @Override
    public void reportNameErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder)
    {
        super.reportNameErrors(compilerAnnotationHolder);
        this.enumerationLiteral.forEachWith(AntlrEnumerationLiteral::reportNameErrors, compilerAnnotationHolder);
    }

    public void reportErrors(CompilerAnnotationHolder compilerAnnotationHolder)
    {
        this.logDuplicateLiteralNames(compilerAnnotationHolder);
        this.logDuplicatePrettyNames(compilerAnnotationHolder);
    }
    //</editor-fold>

    public void logDuplicateLiteralNames(CompilerAnnotationHolder compilerAnnotationHolder)
    {
        MutableBag<String> duplicateNames = this.enumerationLiteral
                .collect(AntlrNamedElement::getName)
                .toBag()
                .selectByOccurrences(occurrences -> occurrences > 1);

        this.enumerationLiteral
                .asLazy()
                .select(enumerationLiteral -> duplicateNames.contains(enumerationLiteral.getName()))
                .forEachWith(AntlrEnumerationLiteral::reportDuplicateName, compilerAnnotationHolder);
    }

    public void logDuplicatePrettyNames(CompilerAnnotationHolder compilerAnnotationHolder)
    {
        MutableBag<String> duplicatePrettyNames = this.enumerationLiteral
                .collect(AntlrEnumerationLiteral::getPrettyName)
                .select(Optional::isPresent)
                .collect(Optional::get)
                .toBag()
                .selectByOccurrences(occurrences -> occurrences > 1);

        this.enumerationLiteral
                .asLazy()
                .select(each -> each.getPrettyName().isPresent())
                .select(each -> duplicatePrettyNames.contains(each.getPrettyName().get()))
                .forEachWith(AntlrEnumerationLiteral::reportDuplicatePrettyName, compilerAnnotationHolder);
    }

    @Override
    public String toString()
    {
        return String.format("%s.%s", this.getPackageName(), this.getName());
    }

    @Override
    public EnumerationBuilder getTypeGetter()
    {
        return Objects.requireNonNull(this.enumerationBuilder);
    }
}

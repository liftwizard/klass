package cool.klass.model.converter.compiler.state;

import java.util.Objects;

import cool.klass.model.converter.compiler.phase.BuildAntlrStatePhase;
import cool.klass.model.meta.domain.DomainModel.DomainModelBuilder;
import cool.klass.model.meta.domain.Enumeration.EnumerationBuilder;
import cool.klass.model.meta.domain.EnumerationLiteral.EnumerationLiteralBuilder;
import cool.klass.model.meta.grammar.KlassParser.EnumerationDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationLiteralContext;
import cool.klass.model.meta.grammar.KlassParser.EnumerationPrettyNameContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;

public class AntlrEnumeration
{
    private final String                                      packageName;
    private final EnumerationDeclarationContext               ctx;
    private final MutableList<AntlrEnumerationLiteral>        enumerationLiteralStates = Lists.mutable.empty();
    private       MutableMap<String, AntlrEnumerationLiteral> enumerationLiteralsByName;

    private EnumerationBuilder enumerationBuilder;

    public AntlrEnumeration(String packageName, EnumerationDeclarationContext ctx)
    {
        this.packageName = packageName;
        this.ctx = ctx;
    }

    public void addLiteral(
            EnumerationLiteralContext ctx,
            EnumerationPrettyNameContext prettyNameContext,
            String literalName,
            String prettyName)
    {
        AntlrEnumerationLiteral enumerationLiteralState =
                new AntlrEnumerationLiteral(ctx, prettyNameContext, literalName, prettyName);
        this.enumerationLiteralStates.add(enumerationLiteralState);
    }

    public void postProcess(BuildAntlrStatePhase buildAntlrStatePhase)
    {
        MutableBag<String> duplicateNames = this.getDuplicateLiteralNames();
        this.checkDuplicateLiteralNames(buildAntlrStatePhase, duplicateNames);
        this.checkDuplicatePrettyNames(buildAntlrStatePhase);

        this.enumerationLiteralsByName = this.enumerationLiteralStates.toMap(
                AntlrEnumerationLiteral::getLiteralName,
                each ->
                {
                    if (duplicateNames.contains(each.getLiteralName()))
                    {
                        return AntlrEnumerationLiteral.AMBIGUOUS;
                    }
                    return each;
                });
    }

    private MutableBag<String> getDuplicateLiteralNames()
    {
        return this.enumerationLiteralStates
                .collect(AntlrEnumerationLiteral::getLiteralName)
                .toBag()
                .selectByOccurrences(occurrences -> occurrences > 1);
    }

    private void checkDuplicateLiteralNames(
            BuildAntlrStatePhase buildAntlrStatePhase,
            MutableBag<String> duplicateNames)
    {
        this.enumerationLiteralStates
                .asLazy()
                .select(each -> duplicateNames.contains(each.getLiteralName()))
                .each(each ->
                {
                    String message = String.format("Duplicate enumeration literal: '%s'.", each.getLiteralName());
                    buildAntlrStatePhase.error(message, each.getCtx(), this.ctx);
                });
    }

    private void checkDuplicatePrettyNames(BuildAntlrStatePhase buildAntlrStatePhase)
    {
        MutableBag<String> duplicatePrettyNames = this.enumerationLiteralStates
                .collect(AntlrEnumerationLiteral::getPrettyName)
                .reject(Objects::isNull)
                .toBag()
                .selectByOccurrences(occurrences -> occurrences > 1);
        this.enumerationLiteralStates
                .asLazy()
                .select(each -> duplicatePrettyNames.contains(each.getPrettyName()))
                .each(each ->
                {
                    String message = String.format("Duplicate enumeration pretty name: '%s'.", each.getPrettyName());
                    buildAntlrStatePhase.error(message, each.getPrettyNameContext(), this.ctx);
                });
    }

    public void build(DomainModelBuilder domainModelBuilder)
    {
        IdentifierContext identifier = this.ctx.identifier();
        this.enumerationBuilder = new EnumerationBuilder(this.ctx, identifier, identifier.getText(), this.packageName);
        ImmutableList<EnumerationLiteralBuilder> enumerationLiteralBuilders = this.enumerationLiteralStates
                .collectWith(AntlrEnumerationLiteral::build, this.enumerationBuilder)
                .toImmutable();
        this.enumerationBuilder.setEnumerationLiterals(enumerationLiteralBuilders);

        domainModelBuilder.enumeration(this.enumerationBuilder);
    }
}

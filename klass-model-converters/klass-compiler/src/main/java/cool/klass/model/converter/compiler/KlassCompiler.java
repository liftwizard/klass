package cool.klass.model.converter.compiler;

import java.util.IdentityHashMap;
import java.util.Set;

import cool.klass.model.converter.compiler.error.CompilerErrorHolder;
import cool.klass.model.converter.compiler.phase.AssociationPhase;
import cool.klass.model.converter.compiler.phase.ClassPhase;
import cool.klass.model.converter.compiler.phase.DeclarationsByNamePhase;
import cool.klass.model.converter.compiler.phase.EnumerationsPhase;
import cool.klass.model.converter.compiler.phase.ResolveTypeErrorsPhase;
import cool.klass.model.converter.compiler.phase.ResolveTypeReferencesPhase;
import cool.klass.model.converter.compiler.phase.ResolveTypesPhase;
import cool.klass.model.converter.compiler.phase.TopLevelElementNameCountPhase;
import cool.klass.model.converter.compiler.phase.TopLevelElementNameDuplicatePhase;
import cool.klass.model.converter.compiler.state.AntlrDomainModel;
import cool.klass.model.meta.domain.DomainModel;
import cool.klass.model.meta.domain.DomainModel.DomainModelBuilder;
import cool.klass.model.meta.grammar.KlassListener;
import cool.klass.model.meta.grammar.KlassParser.CompilationUnitContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.eclipse.collections.impl.set.mutable.SetAdapter;

public class KlassCompiler
{
    private final CompilerErrorHolder compilerErrorHolder;

    public KlassCompiler(CompilerErrorHolder compilerErrorHolder)
    {
        this.compilerErrorHolder = compilerErrorHolder;
    }

    public DomainModel compile(Set<String> classpathLocations)
    {
        MutableSet<CompilationUnit> compilationUnits =
                SetAdapter.adapt(classpathLocations).collect(CompilationUnit::getCompilationUnit);

        MapIterable<CompilationUnitContext, CompilationUnit> compilationUnitsByContext =
                compilationUnits.groupByUniqueKey(
                        CompilationUnit::getCompilationUnitContext,
                        MapAdapter.adapt(new IdentityHashMap<>()));

        TopLevelElementNameCountPhase phase1 = new TopLevelElementNameCountPhase();
        KlassListener phase2 = new TopLevelElementNameDuplicatePhase(
                compilationUnitsByContext,
                this.compilerErrorHolder,
                phase1);
        DeclarationsByNamePhase    phase3 = new DeclarationsByNamePhase();
        ResolveTypeReferencesPhase phase4 = new ResolveTypeReferencesPhase(phase3);
        ResolveTypesPhase          phase5 = new ResolveTypesPhase(phase4);
        KlassListener phase6 = new ResolveTypeErrorsPhase(
                this.compilerErrorHolder,
                compilationUnitsByContext,
                phase5);

        AntlrDomainModel domainModelState = new AntlrDomainModel(this.compilerErrorHolder);

        KlassListener phase7 = new EnumerationsPhase(
                this.compilerErrorHolder,
                compilationUnitsByContext,
                domainModelState);

        KlassListener phase8 = new ClassPhase(
                this.compilerErrorHolder,
                compilationUnitsByContext,
                domainModelState);

        // TODO: Phase for inference on classes?
        // Like adding temporal and audit properties, and version types and version associations

        KlassListener phase9 = new AssociationPhase(
                this.compilerErrorHolder,
                compilationUnitsByContext,
                domainModelState);

        /*
        DomainModelBuilderPhase domainModelBuilderPhase = new DomainModelBuilderPhase(
                this.compilerErrorHolder,
                compilationUnitsByContext,
                this.domainModelBuilder);
        */

        this.executeCompilerPhase(compilationUnits, phase1);
        this.executeCompilerPhase(compilationUnits, phase2);
        this.executeCompilerPhase(compilationUnits, phase3);
        this.executeCompilerPhase(compilationUnits, phase4);
        this.executeCompilerPhase(compilationUnits, phase5);
        this.executeCompilerPhase(compilationUnits, phase6);
        this.executeCompilerPhase(compilationUnits, phase7);
        this.executeCompilerPhase(compilationUnits, phase8);
        this.executeCompilerPhase(compilationUnits, phase9);

        domainModelState.reportErrors();

        if (!this.compilerErrorHolder.hasCompilerErrors())
        {
            DomainModelBuilder domainModelBuilder = domainModelState.build();
            return domainModelBuilder.build();
        }
        return null;
    }

    protected void executeCompilerPhase(
            MutableSet<CompilationUnit> compilationUnits,
            KlassListener compilerPhase)
    {
        ParseTreeWalker parseTreeWalker = new ParseTreeWalker();
        for (CompilationUnit compilationUnit : compilationUnits)
        {
            CompilationUnitContext compilationUnitContext = compilationUnit.getCompilationUnitContext();
            parseTreeWalker.walk(compilerPhase, compilationUnitContext);
        }
    }
}

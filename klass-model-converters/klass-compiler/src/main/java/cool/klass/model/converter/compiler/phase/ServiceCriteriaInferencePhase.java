package cool.klass.model.converter.compiler.phase;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.state.service.AntlrService;
import cool.klass.model.meta.grammar.KlassParser;
import cool.klass.model.meta.grammar.KlassParser.ServiceDeclarationContext;

public class ServiceCriteriaInferencePhase extends AbstractCompilerPhase
{
    public ServiceCriteriaInferencePhase(CompilerState compilerState)
    {
        super(compilerState);
    }

    @Override
    public void exitServiceDeclaration(@Nonnull ServiceDeclarationContext ctx)
    {
        AntlrService serviceState = this.compilerState.getCompilerWalkState().getServiceState();
        if (serviceState.needsVersionCriteriaInferred())
        {
            // TODO: ♻️ Get names from model (system, version, number, version)
            String sourceCodeText = "            version: this.system equalsEdgePoint && this.version.number == version;";
            this.compilerState.runNonRootCompilerMacro(
                    ctx,
                    ServiceCriteriaInferencePhase.class,
                    sourceCodeText,
                    KlassParser::serviceCriteriaDeclaration);
        }

        if (serviceState.needsConflictCriteriaInferred())
        {
            // TODO: ♻️ Get names from model (version, version)
            String sourceCodeText = "            conflict: this.version.number == version;";
            this.compilerState.runNonRootCompilerMacro(
                    ctx,
                    ServiceCriteriaInferencePhase.class,
                    sourceCodeText,
                    KlassParser::serviceCriteriaDeclaration);
        }

        super.exitServiceDeclaration(ctx);
    }
}

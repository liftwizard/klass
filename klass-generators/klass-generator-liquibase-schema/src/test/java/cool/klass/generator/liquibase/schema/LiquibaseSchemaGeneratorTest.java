package cool.klass.generator.liquibase.schema;

import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import cool.klass.model.meta.loader.compiler.DomainModelCompilerLoader;
import io.liftwizard.junit.rule.log.marker.LogMarkerTestRule;
import io.liftwizard.junit.rule.match.file.FileMatchRule;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

public class LiquibaseSchemaGeneratorTest
{
    @Rule
    public final FileMatchRule fileMatchRule = new FileMatchRule(this.getClass());

    @Rule
    public final TestRule logMarkerTestRule = new LogMarkerTestRule();

    @Test
    public void smokeTest()
    {
        ImmutableList<String> klassSourcePackages = Lists.immutable.with("cool.klass.generator.liquibase.schema");

        var domainModelCompilerLoader = new DomainModelCompilerLoader(
                klassSourcePackages,
                Thread.currentThread().getContextClassLoader(),
                DomainModelCompilerLoader::logCompilerError);

        DomainModelWithSourceCode domainModel = domainModelCompilerLoader.load();

        this.fileMatchRule.assertFileContents(
                this.getClass().getCanonicalName() + ".xml",
                SchemaGenerator.getSourceCode(domainModel));
    }
}

package cool.klass.generator.klass.projection;

import cool.klass.model.meta.domain.api.PackageableElement;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import cool.klass.model.meta.loader.compiler.DomainModelCompilerLoader;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.file.FileMatchExtension;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@ExtendWith(LogMarkerTestExtension.class)
public class KlassProjectionGeneratorTest
{
    public static final String FULLY_QUALIFIED_PACKAGE = "${package}";

    @RegisterExtension
    final FileMatchExtension fileMatchExtension = new FileMatchExtension(this.getClass());

    @Test
    public void smokeTest()
    {
        ImmutableList<String> klassSourcePackages = Lists.immutable.with(FULLY_QUALIFIED_PACKAGE);

        var domainModelCompilerLoader = new DomainModelCompilerLoader(
                klassSourcePackages,
                Thread.currentThread().getContextClassLoader(),
                DomainModelCompilerLoader::logCompilerError);

        DomainModelWithSourceCode domainModel = domainModelCompilerLoader.load();
        ImmutableList<String>     packageNames = domainModel
                .getClassifiers()
                .asLazy()
                .collect(PackageableElement::getPackageName)
                .distinct()
                .toImmutableList();
        for (String packageName : packageNames)
        {
            String sourceCode = KlassProjectionSourceCodeGenerator.getPackageSourceCode(domainModel, packageName);

            String resourceClassPathLocation = packageName + ".klass";

            this.fileMatchExtension.assertFileContents(
                    resourceClassPathLocation,
                    sourceCode);
        }
    }
}

package cool.klass.generator.klass.macro;

import java.util.Optional;

import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.loader.compiler.DomainModelCompilerLoader;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.file.FileMatchExtension;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.multimap.list.ImmutableListMultimap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(LogMarkerTestExtension.class)
public class KlassMacroGeneratorTest {
    public static final String FULLY_QUALIFIED_PACKAGE = "${package}";

    @RegisterExtension
    final FileMatchExtension fileMatchExtension = new FileMatchExtension(this.getClass());

    @Test
    void smokeTest() {
        ImmutableList<String> klassSourcePackages = Lists.immutable.with(FULLY_QUALIFIED_PACKAGE);

        var domainModelCompilerLoader = new DomainModelCompilerLoader(
            klassSourcePackages,
            Thread.currentThread().getContextClassLoader(),
            DomainModelCompilerLoader::logCompilerError,
            ColorSchemeProvider.getByName("dark")
        );

        DomainModelWithSourceCode domainModel = domainModelCompilerLoader.load();
        ImmutableList<SourceCode> sourceCodesFromMacros = domainModel
            .getSourceCodes()
            .select(each -> each.getMacroSourceCode().isPresent());
        ImmutableListMultimap<String, SourceCode> sourceCodesByFullPath = sourceCodesFromMacros.groupBy(
            SourceCode::getFullPathSourceName
        );
        sourceCodesByFullPath.forEachKeyMultiValues((fullPath, sourceCodes) -> {
            if (sourceCodes.size() > 1) {
                fail("Multiple source codes for " + fullPath);
            }
        });

        for (SourceCode sourceCode : domainModel.getSourceCodes()) {
            Optional<SourceCode> macroSourceCode = sourceCode.getMacroSourceCode();
            if (macroSourceCode.isPresent()) {
                String fullPathSourceName = sourceCode.getFullPathSourceName();
                String sourceCodeText     = sourceCode.getSourceCodeText();

                String resourceClassPathLocation = fullPathSourceName + ".klass";

                this.fileMatchExtension.assertFileContents(resourceClassPathLocation, sourceCodeText);
            }
        }
    }
}

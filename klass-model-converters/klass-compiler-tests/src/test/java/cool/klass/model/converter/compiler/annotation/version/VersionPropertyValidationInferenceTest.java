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

package cool.klass.model.converter.compiler.annotation.version;

import java.util.Optional;

import cool.klass.model.converter.compiler.CompilationResult;
import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.KlassCompiler;
import cool.klass.model.converter.compiler.annotation.RootCompilerAnnotation;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.AnsiColorScheme;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.ColorSchemeProvider;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.validation.MaxLengthPropertyValidation;
import cool.klass.model.meta.domain.api.property.validation.MaxPropertyValidation;
import cool.klass.model.meta.domain.api.property.validation.MinPropertyValidation;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.FileSlurper;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(LogMarkerTestExtension.class)
public class VersionPropertyValidationInferenceTest {

    @Test
    public void testVersionClassPropertiesInheritValidationsFromSourceKeys() {
        String sourceCodeText = FileSlurper.slurp(
            "VersionPropertyValidationInferenceTest.klass",
            this.getClass()
        );
        CompilationUnit compilationUnit = CompilationUnit.createFromText(
            0,
            Optional.empty(),
            "VersionPropertyValidationInferenceTest.klass",
            sourceCodeText
        );
        AnsiColorScheme colorScheme = ColorSchemeProvider.getByName("dark");
        KlassCompiler compiler = new KlassCompiler(compilationUnit, colorScheme);
        CompilationResult compilationResult = compiler.compile();

        ImmutableList<RootCompilerAnnotation> errors = compilationResult
            .compilerAnnotations()
            .select(annotation -> annotation.isError());

        assertThat(errors)
            .as("Expected no compiler errors")
            .isEqualTo(Lists.immutable.empty());

        Optional<DomainModelWithSourceCode> domainModelOpt = compilationResult.domainModelWithSourceCode();
        assertThat(domainModelOpt).isPresent();

        DomainModelWithSourceCode domainModel = domainModelOpt.get();

        // Get the versioned class
        Klass versionedClass = domainModel.getClassByName("VersionedEntity");
        assertThat(versionedClass).isNotNull();

        // Get the original key property
        DataTypeProperty<?> entityIdProperty = versionedClass.getDataTypePropertyByName("entityId");
        assertThat(entityIdProperty).isNotNull();

        // Verify the source property has the expected validations
        Optional<MinPropertyValidation> sourceMin = entityIdProperty.getMinPropertyValidation();
        Optional<MaxPropertyValidation> sourceMax = entityIdProperty.getMaxPropertyValidation();

        assertThat(sourceMin).isPresent();
        assertThat(sourceMin.get().getMinValue()).isEqualTo("1");
        assertThat(sourceMax).isPresent();
        assertThat(sourceMax.get().getMaxValue()).isEqualTo("9999999");

        // Get the version class
        Klass versionClass = domainModel.getClassByName("VersionedEntityVersion");
        assertThat(versionClass).isNotNull();

        // Get the key property in the version class
        DataTypeProperty<?> versionEntityIdProperty = versionClass.getDataTypePropertyByName("entityId");
        assertThat(versionEntityIdProperty).isNotNull();

        // Verify that the version class property has the same validations as the source
        Optional<MinPropertyValidation> versionMin = versionEntityIdProperty.getMinPropertyValidation();
        Optional<MaxPropertyValidation> versionMax = versionEntityIdProperty.getMaxPropertyValidation();

        assertThat(versionMin)
            .as("Version class entityId should have minimum validation inherited from source")
            .isPresent();
        assertThat(versionMin.get().getMinValue())
            .as("Version class entityId minimum should be 1 (same as source)")
            .isEqualTo("1");

        assertThat(versionMax)
            .as("Version class entityId should have maximum validation inherited from source")
            .isPresent();
        assertThat(versionMax.get().getMaxValue())
            .as("Version class entityId maximum should be 9999999 (same as source)")
            .isEqualTo("9999999");

        // Also verify audit properties in version class inherit from userId
        DataTypeProperty<?> versionCreatedByIdProperty = versionClass.getDataTypePropertyByName("createdById");
        assertThat(versionCreatedByIdProperty).isNotNull();

        Optional<MaxLengthPropertyValidation> versionCreatedByMaxLength =
            versionCreatedByIdProperty.getMaxLengthPropertyValidation();

        assertThat(versionCreatedByMaxLength)
            .as("Version class createdById should have maxLength validation inherited from userId")
            .isPresent();
        assertThat(versionCreatedByMaxLength.get().getMaxLength())
            .as("Version class createdById maxLength should be 128 (same as userId)")
            .isEqualTo(128);
    }
}

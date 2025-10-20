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

package cool.klass.model.converter.compiler.annotation.audit;

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
import cool.klass.model.meta.domain.api.property.validation.MinLengthPropertyValidation;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.FileSlurper;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(LogMarkerTestExtension.class)
public class AuditPropertyValidationInferenceTest {

    @Test
    public void testAuditPropertiesInheritValidationsFromUserId() {
        String sourceCodeText = FileSlurper.slurp(
            "AuditPropertyValidationInferenceTest.klass",
            this.getClass()
        );
        CompilationUnit compilationUnit = CompilationUnit.createFromText(
            0,
            Optional.empty(),
            "AuditPropertyValidationInferenceTest.klass",
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

        // Get the audited class
        Klass auditedClass = domainModel.getClassByName("AuditedDocument");
        assertThat(auditedClass).isNotNull();

        // Get the inferred audit properties
        DataTypeProperty<?> createdByIdProperty = auditedClass.getDataTypePropertyByName("createdById");
        DataTypeProperty<?> lastUpdatedByIdProperty = auditedClass.getDataTypePropertyByName("lastUpdatedById");

        assertThat(createdByIdProperty).isNotNull();
        assertThat(lastUpdatedByIdProperty).isNotNull();

        // Verify that createdById has the same validations as userId
        Optional<MaxLengthPropertyValidation> createdByMaxLength =
            createdByIdProperty.getMaxLengthPropertyValidation();
        Optional<MinLengthPropertyValidation> createdByMinLength =
            createdByIdProperty.getMinLengthPropertyValidation();

        assertThat(createdByMaxLength)
            .as("createdById should have maxLength validation inherited from userId")
            .isPresent();
        assertThat(createdByMaxLength.get().getMaxLength())
            .as("createdById maxLength should be 256 (same as userId)")
            .isEqualTo(256);

        assertThat(createdByMinLength)
            .as("createdById should have minLength validation inherited from userId")
            .isPresent();
        assertThat(createdByMinLength.get().getMinLength())
            .as("createdById minLength should be 1 (same as userId)")
            .isEqualTo(1);

        // Verify that lastUpdatedById has the same validations as userId
        Optional<MaxLengthPropertyValidation> lastUpdatedByMaxLength =
            lastUpdatedByIdProperty.getMaxLengthPropertyValidation();
        Optional<MinLengthPropertyValidation> lastUpdatedByMinLength =
            lastUpdatedByIdProperty.getMinLengthPropertyValidation();

        assertThat(lastUpdatedByMaxLength)
            .as("lastUpdatedById should have maxLength validation inherited from userId")
            .isPresent();
        assertThat(lastUpdatedByMaxLength.get().getMaxLength())
            .as("lastUpdatedById maxLength should be 256 (same as userId)")
            .isEqualTo(256);

        assertThat(lastUpdatedByMinLength)
            .as("lastUpdatedById should have minLength validation inherited from userId")
            .isPresent();
        assertThat(lastUpdatedByMinLength.get().getMinLength())
            .as("lastUpdatedById minLength should be 1 (same as userId)")
            .isEqualTo(1);
    }
}

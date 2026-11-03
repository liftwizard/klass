/*
 * Copyright 2026 Craig Motlin
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

package cool.klass.deserializer.json.test;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cool.klass.deserializer.json.OperationMode;
import cool.klass.deserializer.json.RequiredPropertiesValidator;
import cool.klass.deserializer.json.type.ObjectNodeTypeCheckingValidator;
import cool.klass.dropwizard.configuration.domain.model.loader.compiler.DomainModelCompilerFactory;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Klass;
import io.dropwizard.jackson.Jackson;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.FileSlurper;
import io.liftwizard.junit.extension.match.file.FileMatchExtension;
import io.liftwizard.junit.extension.match.json.JsonMatchExtension;
import io.liftwizard.serialization.jackson.config.ObjectMapperConfig;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@ExtendWith(LogMarkerTestExtension.class)
class JsonDeserializerValidatorTest {

	@RegisterExtension
	final FileMatchExtension fileMatchExtension = new FileMatchExtension(this.getClass());

	@RegisterExtension
	final JsonMatchExtension jsonMatchExtension = new JsonMatchExtension(this.getClass());

	private final ObjectMapper objectMapper = JsonDeserializerValidatorTest.getObjectMapper();
	private final DomainModel domainModel = JsonDeserializerValidatorTest.getDomainModel(this.objectMapper);

	@Test
	void validateAllCases() throws IOException {
		this.validatePrimitiveCases("PropertiesOptional");
		this.validatePrimitiveCases("PropertiesRequired");

		var ownedNaturalOneToMany = this.validationCase("OwnedNaturalOneToMany");
		this.validate(ownedNaturalOneToMany, "validate_good");
		this.validate(ownedNaturalOneToMany, "validate_backwards_association_end");
		// TODO: This should fail, or there should be an additional validation
		this.validate(ownedNaturalOneToMany, "validate_duplicate_keys");
		this.validate(ownedNaturalOneToMany, "validate_extra_properties");
		this.validate(ownedNaturalOneToMany, "validate_expected_actual_missing");
		this.validate(ownedNaturalOneToMany, "validate_expected_actual_array");
		this.validate(ownedNaturalOneToMany, "validate_expected_actual_object");
		this.validate(ownedNaturalOneToMany, "validate_expected_actual_null");
		this.validate(ownedNaturalOneToMany, "validate_expected_actual_boolean");

		var ownedNaturalOneToOne = this.validationCase("OwnedNaturalOneToOne");
		this.validate(ownedNaturalOneToOne, "validate_good");
		this.validate(ownedNaturalOneToOne, "validate_backwards_association_end");
		this.validate(ownedNaturalOneToOne, "validate_extra_properties");
		this.validate(ownedNaturalOneToOne, "validate_expected_actual_missing");
		this.validate(ownedNaturalOneToOne, "validate_expected_actual_array");
		this.validate(ownedNaturalOneToOne, "validate_expected_actual_object");
		this.validate(ownedNaturalOneToOne, "validate_expected_actual_null");
		this.validate(ownedNaturalOneToOne, "validate_expected_actual_boolean");

		var sharedNaturalManyToOne = this.validationCase("SharedNaturalManyToOne");
		this.validate(sharedNaturalManyToOne, "validate_good");
		this.validate(sharedNaturalManyToOne, "validate_backwards_association_end");
		this.validate(sharedNaturalManyToOne, "validate_extra_properties");
		this.validate(sharedNaturalManyToOne, "validate_expected_actual_missing");
		this.validate(sharedNaturalManyToOne, "validate_expected_actual_array");
		this.validate(sharedNaturalManyToOne, "validate_expected_actual_object");
		this.validate(sharedNaturalManyToOne, "validate_expected_actual_null");
		this.validate(sharedNaturalManyToOne, "validate_expected_actual_boolean");

		var sharedNaturalOneToMany = this.validationCase("SharedNaturalOneToMany");
		this.validate(sharedNaturalOneToMany, "validate_good");
		this.validate(sharedNaturalOneToMany, "validate_backwards_association_end");
		// TODO: This should fail, or there should be an additional validation
		this.validate(sharedNaturalOneToMany, "validate_duplicate_keys");
		this.validate(sharedNaturalOneToMany, "validate_extra_properties");
		this.validate(sharedNaturalOneToMany, "validate_expected_actual_missing");
		this.validate(sharedNaturalOneToMany, "validate_expected_actual_array");
		this.validate(sharedNaturalOneToMany, "validate_expected_actual_object");
		this.validate(sharedNaturalOneToMany, "validate_expected_actual_null");
		this.validate(sharedNaturalOneToMany, "validate_expected_actual_boolean");

		var sharedNaturalOneToOne = this.validationCase("SharedNaturalOneToOne");
		this.validate(sharedNaturalOneToOne, "validate_good");
		this.validate(sharedNaturalOneToOne, "validate_backwards_association_end");
		// TODO: This should fail, or there should be an additional validation
		this.validate(sharedNaturalOneToOne, "validate_duplicate_keys");
		this.validate(sharedNaturalOneToOne, "validate_extra_properties");
		this.validate(sharedNaturalOneToOne, "validate_expected_actual_missing");
		this.validate(sharedNaturalOneToOne, "validate_expected_actual_array");
		this.validate(sharedNaturalOneToOne, "validate_expected_actual_object");
		this.validate(sharedNaturalOneToOne, "validate_expected_actual_null");
		this.validate(sharedNaturalOneToOne, "validate_expected_actual_boolean");
	}

	private void validatePrimitiveCases(String className) throws IOException {
		var validationCase = new ValidationCase(className, this.domainModel.getClassByName(className));
		this.validate(validationCase, "validate_good");
		this.validate(validationCase, "validate_extra_properties");
		this.validate(validationCase, "validate_expected_primitive_actual_missing");
		this.validate(validationCase, "validate_expected_primitive_actual_array");
		this.validate(validationCase, "validate_expected_primitive_actual_object");
		this.validate(validationCase, "validate_expected_primitive_actual_null");
		this.validate(validationCase, "validate_expected_primitive_actual_string");
	}

	@Nonnull
	private ValidationCase validationCase(String name) {
		return new ValidationCase(name, this.domainModel.getClassByName(name + "Source"));
	}

	private void validate(ValidationCase validationCase, String testName) throws IOException {
		String prefix = this.getClass().getSimpleName() + '.' + validationCase.name() + '.' + testName;
		String incomingJson = FileSlurper.slurp(prefix + ".json5", this.getClass());
		var incomingInstance = (ObjectNode) this.objectMapper.readTree(incomingJson);

		ValidationResult result = JsonDeserializerValidatorTest.performValidation(
			validationCase.klass(),
			incomingInstance
		);

		this.jsonMatchExtension.assertFileContents(
			prefix + ".errors.json",
			this.objectMapper.writeValueAsString(result.errors())
		);

		this.jsonMatchExtension.assertFileContents(
			prefix + ".warnings.json",
			this.objectMapper.writeValueAsString(result.warnings())
		);
	}

	@Nonnull
	private static ValidationResult performValidation(Klass klass, @Nonnull ObjectNode incomingInstance) {
		MutableList<String> errors = Lists.mutable.empty();
		MutableList<String> warnings = Lists.mutable.empty();

		ObjectNodeTypeCheckingValidator.validate(errors, incomingInstance, klass);
		RequiredPropertiesValidator.validate(errors, warnings, klass, incomingInstance, OperationMode.CREATE);

		return new ValidationResult(errors.toImmutable(), warnings.toImmutable());
	}

	@Nonnull
	private static ObjectMapper getObjectMapper() {
		ObjectMapper objectMapper = Jackson.newObjectMapper();
		ObjectMapperConfig.configure(objectMapper);
		return objectMapper;
	}

	private static DomainModel getDomainModel(ObjectMapper objectMapper) {
		var domainModelCompilerFactory = new DomainModelCompilerFactory();
		domainModelCompilerFactory.setSourcePackages(List.of("cool.klass.xample.coverage"));
		domainModelCompilerFactory.setColorScheme("dark");
		return domainModelCompilerFactory.createDomainModel(objectMapper);
	}

	private record ValidationCase(String name, Klass klass) {}

	private record ValidationResult(ImmutableList<String> errors, ImmutableList<String> warnings) {}
}

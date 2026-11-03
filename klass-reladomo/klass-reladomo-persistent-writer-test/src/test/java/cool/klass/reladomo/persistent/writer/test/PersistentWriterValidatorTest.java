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

package cool.klass.reladomo.persistent.writer.test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cool.klass.data.store.reladomo.ReladomoDataStore;
import cool.klass.deserializer.json.OperationMode;
import cool.klass.deserializer.json.RequiredPropertiesValidator;
import cool.klass.deserializer.json.type.ObjectNodeTypeCheckingValidator;
import cool.klass.dropwizard.configuration.domain.model.loader.compiler.DomainModelCompilerFactory;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.reladomo.persistent.writer.IncomingCreateDataModelValidator;
import cool.klass.reladomo.persistent.writer.IncomingUpdateDataModelValidator;
import cool.klass.reladomo.persistent.writer.MutationContext;
import io.dropwizard.jackson.Jackson;
import io.liftwizard.dropwizard.configuration.uuid.seed.SeedUUIDSupplier;
import io.liftwizard.junit.extension.liquibase.migrations.LiquibaseTestExtension;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.junit.extension.match.FileSlurper;
import io.liftwizard.junit.extension.match.file.FileMatchExtension;
import io.liftwizard.junit.extension.match.json.JsonMatchExtension;
import io.liftwizard.reladomo.csv.test.extension.CsvTestDataExtension;
import io.liftwizard.reladomo.test.extension.ReladomoExtensionBuilder;
import io.liftwizard.serialization.jackson.config.ObjectMapperConfig;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class PersistentWriterValidatorTest {

	@RegisterExtension
	final FileMatchExtension fileMatchExtension = new FileMatchExtension(this.getClass());

	@RegisterExtension
	final LogMarkerTestExtension logMarkerTestExtension = new LogMarkerTestExtension();

	@RegisterExtension
	final JsonMatchExtension jsonMatchExtension = new JsonMatchExtension(this.getClass());

	@RegisterExtension
	final LiquibaseTestExtension liquibaseTestExtension = new LiquibaseTestExtension(
		"cool/klass/reladomo/persistent/writer/test/migrations.xml"
	);

	@RegisterExtension
	final ReladomoExtensionBuilder reladomoTestExtension = new ReladomoExtensionBuilder().setRuntimeConfigurationPath(
		"reladomo-runtime-configuration/ReladomoRuntimeConfiguration.xml"
	);

	@RegisterExtension
	final CsvTestDataExtension csvTestDataExtension = new CsvTestDataExtension(
		"test-data/cool.klass.xample.coverage.User.csv",
		"test-data/cool.klass.xample.coverage.PropertiesOptional.csv",
		"test-data/cool.klass.xample.coverage.PropertiesOptionalVersion.csv",
		"test-data/cool.klass.xample.coverage.PropertiesRequired.csv",
		"test-data/cool.klass.xample.coverage.PropertiesRequiredVersion.csv",
		"test-data/cool.klass.xample.coverage.FinalProperties.csv"
	);

	private final ReladomoDataStore reladomoDataStore = PersistentWriterValidatorTest.getReladomoDataStore();
	private final ObjectMapper objectMapper = PersistentWriterValidatorTest.getObjectMapper();
	private final DomainModel domainModel = PersistentWriterValidatorTest.getDomainModel(this.objectMapper);

	@Test
	void validateAllCases() throws JsonProcessingException {
		ValidationCase createPropertiesOptional = this.createCase(
			"Create_PropertiesOptional",
			"PropertiesOptional",
			"propertiesOptionalId"
		);
		this.validate(createPropertiesOptional, "validate_good");
		this.validate(createPropertiesOptional, "validate_mutation_context");
		this.validate(createPropertiesOptional, "validate_extra_properties");
		this.validate(createPropertiesOptional, "validate_expected_primitive_actual_missing");
		this.validate(createPropertiesOptional, "validate_expected_primitive_actual_array");
		this.validate(createPropertiesOptional, "validate_expected_primitive_actual_object");
		this.validate(createPropertiesOptional, "validate_expected_primitive_actual_null");
		this.validate(createPropertiesOptional, "validate_expected_primitive_actual_string");
		this.validate(createPropertiesOptional, "validate_version_expected_primitive_actual_object");

		ValidationCase createPropertiesRequired = this.createCase(
			"Create_PropertiesRequired",
			"PropertiesRequired",
			"propertiesRequiredId"
		);
		this.validate(createPropertiesRequired, "validate_good");
		this.validate(createPropertiesRequired, "validate_extra_properties");
		this.validate(createPropertiesRequired, "validate_mutation_context");
		this.validate(createPropertiesRequired, "validate_expected_primitive_actual_missing");
		this.validate(createPropertiesRequired, "validate_expected_primitive_actual_array");
		this.validate(createPropertiesRequired, "validate_expected_primitive_actual_object");
		this.validate(createPropertiesRequired, "validate_expected_primitive_actual_null");
		this.validate(createPropertiesRequired, "validate_expected_primitive_actual_string");
		this.validate(createPropertiesRequired, "validate_version_expected_primitive_actual_object");

		var createOwnedNaturalOneToMany = new ValidationCase(
			"Create_OwnedNaturalOneToMany",
			this.domainModel.getClassByName("OwnedNaturalOneToManySource"),
			OperationMode.CREATE,
			Maps.immutable.empty(),
			null
		);
		this.validate(createOwnedNaturalOneToMany, "validate_good");
		this.validate(createOwnedNaturalOneToMany, "validate_backwards_association_end");
		// TODO: This should fail, or there should be an additional validation
		this.validate(createOwnedNaturalOneToMany, "validate_duplicate_keys");
		this.validate(createOwnedNaturalOneToMany, "validate_extra_properties");
		this.validate(createOwnedNaturalOneToMany, "validate_expected_actual_missing");
		this.validate(createOwnedNaturalOneToMany, "validate_expected_actual_array");
		this.validate(createOwnedNaturalOneToMany, "validate_expected_actual_object");
		this.validate(createOwnedNaturalOneToMany, "validate_expected_actual_null");
		this.validate(createOwnedNaturalOneToMany, "validate_expected_actual_boolean");

		ValidationCase updatePropertiesOptional = this.updateCase(
			"Update_PropertiesOptional",
			"PropertiesOptional",
			"propertiesOptionalId"
		);
		this.validate(updatePropertiesOptional, "validate_good");
		this.validate(updatePropertiesOptional, "validate_mutation_context");
		this.validate(updatePropertiesOptional, "validate_wrong_version");
		this.validate(updatePropertiesOptional, "validate_extra_properties");
		this.validate(updatePropertiesOptional, "validate_expected_primitive_actual_missing");
		this.validate(updatePropertiesOptional, "validate_expected_primitive_actual_array");
		this.validate(updatePropertiesOptional, "validate_expected_primitive_actual_object");
		this.validate(updatePropertiesOptional, "validate_expected_primitive_actual_null");
		this.validate(updatePropertiesOptional, "validate_expected_primitive_actual_string");
		this.validate(updatePropertiesOptional, "validate_version_expected_primitive_actual_object");

		ValidationCase updatePropertiesRequired = this.updateCase(
			"Update_PropertiesRequired",
			"PropertiesRequired",
			"propertiesRequiredId"
		);
		this.validate(updatePropertiesRequired, "validate_good");
		this.validate(updatePropertiesRequired, "validate_mutation_context");
		this.validate(updatePropertiesRequired, "validate_extra_properties");
		this.validate(updatePropertiesRequired, "validate_expected_primitive_actual_missing");
		this.validate(updatePropertiesRequired, "validate_expected_primitive_actual_array");
		this.validate(updatePropertiesRequired, "validate_expected_primitive_actual_object");
		this.validate(updatePropertiesRequired, "validate_expected_primitive_actual_null");
		this.validate(updatePropertiesRequired, "validate_expected_primitive_actual_string");
		this.validate(updatePropertiesRequired, "validate_version_expected_primitive_actual_object");

		ValidationCase updateFinalProperties = this.updateCase("Update_FinalProperties", "FinalProperties", "id");
		this.validate(updateFinalProperties, "validate_mutate_final");
	}

	@Nonnull
	private ValidationCase createCase(String caseName, String className, String keyPropertyName) {
		Klass klass = this.domainModel.getClassByName(className);
		DataTypeProperty keyProperty = klass.getDataTypePropertyByName(keyPropertyName);
		return new ValidationCase(caseName, klass, OperationMode.CREATE, Maps.immutable.with(keyProperty, 1L), null);
	}

	@Nonnull
	private ValidationCase updateCase(String caseName, String className, String keyPropertyName) {
		Klass klass = this.domainModel.getClassByName(className);
		DataTypeProperty keyProperty = klass.getDataTypePropertyByName(keyPropertyName);
		ImmutableMap<DataTypeProperty, Object> keys = Maps.immutable.with(keyProperty, 1L);
		Object persistentInstance = this.reladomoDataStore.findByKey(klass, keys);
		return new ValidationCase(caseName, klass, OperationMode.REPLACE, keys, persistentInstance);
	}

	private void validate(ValidationCase validationCase, String testName) throws JsonProcessingException {
		String prefix = this.getClass().getSimpleName() + '.' + validationCase.name() + '.' + testName;
		String incomingJson = FileSlurper.slurp(prefix + ".json5", this.getClass());
		var incomingInstance = (ObjectNode) this.objectMapper.readTree(incomingJson);

		ValidationResult result = this.performValidation(validationCase, incomingInstance);

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
	private ValidationResult performValidation(ValidationCase validationCase, @Nonnull ObjectNode incomingInstance) {
		Klass klass = validationCase.klass();
		ImmutableMap<DataTypeProperty, Object> propertyDataFromUrl = validationCase.propertyDataFromUrl();
		propertyDataFromUrl.forEachKey((property) -> assertThat(property.getOwningClassifier()).isSameAs(klass));

		MutableList<String> errors = Lists.mutable.empty();
		MutableList<String> warnings = Lists.mutable.empty();

		ObjectNodeTypeCheckingValidator.validate(errors, incomingInstance, klass);

		RequiredPropertiesValidator.validate(errors, warnings, klass, incomingInstance, validationCase.mode());

		var mutationContext = new MutationContext(
			Optional.of("test user 1"),
			Instant.parse("1999-12-31T23:59:59.999Z"),
			propertyDataFromUrl
		);

		Klass userClass = this.domainModel.getUserClass().get();
		if (validationCase.mode() == OperationMode.CREATE) {
			IncomingCreateDataModelValidator.validate(
				this.reladomoDataStore,
				userClass,
				klass,
				mutationContext,
				incomingInstance,
				errors,
				warnings
			);
		} else {
			IncomingUpdateDataModelValidator.validate(
				this.reladomoDataStore,
				userClass,
				klass,
				mutationContext,
				validationCase.persistentInstance(),
				incomingInstance,
				errors,
				warnings
			);
		}

		return new ValidationResult(errors.toImmutable(), warnings.toImmutable());
	}

	@Nonnull
	private static ObjectMapper getObjectMapper() {
		ObjectMapper objectMapper = Jackson.newObjectMapper();
		ObjectMapperConfig.configure(objectMapper);
		return objectMapper;
	}

	@Nonnull
	private static ReladomoDataStore getReladomoDataStore() {
		String seed = IncomingCreateDataModelValidator.class.getSimpleName();
		var uuidSupplier = new SeedUUIDSupplier(seed);
		return new ReladomoDataStore(uuidSupplier, 1);
	}

	private static DomainModel getDomainModel(ObjectMapper objectMapper) {
		var domainModelCompilerFactory = new DomainModelCompilerFactory();
		domainModelCompilerFactory.setSourcePackages(List.of("cool.klass.xample.coverage"));
		domainModelCompilerFactory.setColorScheme("dark");
		return domainModelCompilerFactory.createDomainModel(objectMapper);
	}

	private record ValidationCase(
		String name,
		Klass klass,
		OperationMode mode,
		ImmutableMap<DataTypeProperty, Object> propertyDataFromUrl,
		Object persistentInstance
	) {}

	private record ValidationResult(ImmutableList<String> errors, ImmutableList<String> warnings) {}
}

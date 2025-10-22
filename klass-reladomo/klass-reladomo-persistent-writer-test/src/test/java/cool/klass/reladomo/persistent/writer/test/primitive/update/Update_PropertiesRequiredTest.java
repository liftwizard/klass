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

package cool.klass.reladomo.persistent.writer.test.primitive.update;

import java.io.IOException;

import javax.annotation.Nonnull;

import cool.klass.deserializer.json.OperationMode;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.reladomo.persistent.writer.test.primitive.PrimitiveValidatorTest;
import io.liftwizard.reladomo.csv.test.extension.CsvTestDataExtension;
import io.liftwizard.reladomo.test.extension.ReladomoExtensionBuilder;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class Update_PropertiesRequiredTest extends AbstractUpdateValidatorTest implements PrimitiveValidatorTest {

	@RegisterExtension
	public final ReladomoExtensionBuilder reladomoTestExtension =
		new ReladomoExtensionBuilder().setRuntimeConfigurationPath(
			"reladomo-runtime-configuration/ReladomoRuntimeConfiguration.xml"
		);

	@RegisterExtension
	public final CsvTestDataExtension csvTestDataExtension = new CsvTestDataExtension(
		"test-data/cool.klass.xample.coverage.User.csv",
		"test-data/cool.klass.xample.coverage.PropertiesRequired.csv",
		"test-data/cool.klass.xample.coverage.PropertiesRequiredVersion.csv"
	);

	private Object persistentInstance;

	@BeforeEach
	void setUp() {
		Klass klass = this.getKlass();
		DataTypeProperty keyProperty = (DataTypeProperty) klass.getPropertyByName("propertiesRequiredId").get();

		ImmutableMap<DataTypeProperty, Object> keys = Maps.immutable.with(keyProperty, 1L);

		this.persistentInstance = this.reladomoDataStore.findByKey(klass, keys);
	}

	@Override
	@Test
	public void validate_good() throws IOException {
		this.validate("validate_good", this.persistentInstance);
	}

	@Test
	@Override
	public void validate_mutation_context() throws IOException {
		this.validate("validate_mutation_context", this.persistentInstance);
	}

	@Override
	@Test
	public void validate_extra_properties() throws IOException {
		this.validate("validate_extra_properties", this.persistentInstance);
	}

	@Override
	@Test
	public void validate_expected_primitive_actual_missing() throws IOException {
		this.validate("validate_expected_primitive_actual_missing", this.persistentInstance);
	}

	@Override
	@Test
	public void validate_expected_primitive_actual_array() throws IOException {
		this.validate("validate_expected_primitive_actual_array", this.persistentInstance);
	}

	@Override
	@Test
	public void validate_expected_primitive_actual_object() throws IOException {
		this.validate("validate_expected_primitive_actual_object", this.persistentInstance);
	}

	@Override
	@Test
	public void validate_expected_primitive_actual_null() throws IOException {
		this.validate("validate_expected_primitive_actual_null", this.persistentInstance);
	}

	@Override
	@Test
	public void validate_expected_primitive_actual_string() throws IOException {
		this.validate("validate_expected_primitive_actual_string", this.persistentInstance);
	}

	@Test
	@Override
	public void validate_version_expected_primitive_actual_object() throws IOException {
		this.validate("validate_version_expected_primitive_actual_object", this.persistentInstance);
	}

	@Nonnull
	@Override
	protected Klass getKlass() {
		return this.domainModel.getClassByName("PropertiesRequired");
	}

	@Nonnull
	@Override
	protected OperationMode getMode() {
		return OperationMode.REPLACE;
	}

	@Override
	protected ImmutableMap<DataTypeProperty, Object> getPropertyDataFromUrl() {
		DataTypeProperty dataTypeProperty = this.getKlass().getDataTypePropertyByName("propertiesRequiredId");
		return Maps.immutable.with(dataTypeProperty, 1L);
	}
}

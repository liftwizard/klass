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

package cool.klass.generator.reladomo.runtimeconfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gs.fw.common.mithra.mithraruntime.CacheType;
import com.gs.fw.common.mithra.mithraruntime.ConnectionManagerType;
import com.gs.fw.common.mithra.mithraruntime.MithraObjectConfigurationType;
import com.gs.fw.common.mithra.mithraruntime.MithraPureObjectConfigurationType;
import com.gs.fw.common.mithra.mithraruntime.MithraRuntime;
import com.gs.fw.common.mithra.mithraruntime.MithraRuntimeMarshaller;
import com.gs.fw.common.mithra.mithraruntime.PropertyType;
import com.gs.fw.common.mithra.mithraruntime.PureObjectsType;
import cool.klass.generator.reladomo.AbstractReladomoGenerator;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.PackageableElement;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

// TODO: Split out into Liftwizard by passing in a list of class names (and Sequence name) into the constructor
public class ReladomoRuntimeConfigurationGenerator extends AbstractReladomoGenerator {

	// Default value will be "ConnectionManagerHolder"
	@Nonnull
	private final String connectionManagerClassName;

	@Nullable
	private final String connectionManagerName;

	@Nonnull
	private final String rootPackageName;

	@Nonnull
	private final CacheType cacheType;

	public ReladomoRuntimeConfigurationGenerator(
		@Nonnull DomainModel domainModel,
		@Nonnull String connectionManagerClassName,
		@Nullable String connectionManagerName,
		@Nonnull String rootPackageName,
		@Nonnull String cacheType
	) {
		super(domainModel);
		this.connectionManagerClassName = Objects.requireNonNull(connectionManagerClassName);
		this.connectionManagerName = connectionManagerName;
		this.rootPackageName = Objects.requireNonNull(rootPackageName);
		this.cacheType = ReladomoRuntimeConfigurationGenerator.getCacheType(cacheType);
	}

	private static CacheType getCacheType(@Nonnull String cacheType) {
		return switch (cacheType) {
			case "partial" -> CacheType.PARTIAL;
			case "full" -> CacheType.FULL;
			case "none" -> CacheType.NONE;
			default -> {
				String message = String.format(
					"Invalid cacheType. Expected one of [partial, full, none] but got: %s",
					cacheType
				);
				throw new RuntimeException(message);
			}
		};
	}

	public void writeRuntimeConfigFile(@Nonnull Path path) throws IOException {
		MithraRuntime mithraRuntime = this.getMithraRuntime();

		MithraRuntimeMarshaller mithraRuntimeMarshaller = new MithraRuntimeMarshaller();
		mithraRuntimeMarshaller.setIndent(true);

		StringBuilder stringBuilder = new StringBuilder();
		mithraRuntimeMarshaller.marshall(stringBuilder, mithraRuntime);
		String xmlString = this.sanitizeXmlString(stringBuilder);

		this.printStringToFile(path, xmlString);
	}

	@Nonnull
	private MithraRuntime getMithraRuntime() {
		MithraRuntime mithraRuntime = new MithraRuntime();
		ConnectionManagerType connectionManager = this.getConnectionManager();
		mithraRuntime.setConnectionManagers(Lists.mutable.with(connectionManager));
		mithraRuntime.setPureObjects(this.getPureObjectsType());
		return mithraRuntime;
	}

	private ImmutableList<PropertyType> getPropertyTypes() {
		if (this.connectionManagerName == null) {
			return Lists.immutable.empty();
		}

		PropertyType propertyType = ReladomoRuntimeConfigurationGenerator.createPropertyType(
			"connectionManagerName",
			this.connectionManagerName
		);
		return Lists.immutable.with(propertyType);
	}

	@Nonnull
	private PureObjectsType getPureObjectsType() {
		PureObjectsType pureObjectsType = new PureObjectsType();
		pureObjectsType.setNotificationIdentifier(this.rootPackageName);
		pureObjectsType.setMithraObjectConfigurations(this.getMithraPureObjectConfigurationTypes().castToList());
		return pureObjectsType;
	}

	@Nonnull
	private static PropertyType createPropertyType(String name, String value) {
		PropertyType propertyType = new PropertyType();
		propertyType.setName(name);
		propertyType.setValue(value);
		return propertyType;
	}

	@Nonnull
	private ConnectionManagerType getConnectionManager() {
		ImmutableList<PropertyType> propertyTypes = this.getPropertyTypes();
		List<PropertyType> properties = propertyTypes.castToList();

		ConnectionManagerType connectionManagerType = new ConnectionManagerType();
		connectionManagerType.setClassName(this.connectionManagerClassName);
		connectionManagerType.setProperties(properties);
		connectionManagerType.setMithraObjectConfigurations(
			this.getConnectionManagerObjectConfigurationTypes().castToList()
		);
		connectionManagerType.setOffHeapReplicationExport(false);
		return connectionManagerType;
	}

	private ImmutableList<MithraPureObjectConfigurationType> getMithraPureObjectConfigurationTypes() {
		return this.domainModel.getClasses()
			.select(Klass::isTransient)
			.collect(PackageableElement::getFullyQualifiedName)
			.collect(ReladomoRuntimeConfigurationGenerator::createMithraPureObjectConfigurationType);
	}

	private ImmutableList<MithraObjectConfigurationType> getConnectionManagerObjectConfigurationTypes() {
		ImmutableList<MithraObjectConfigurationType> objectConfigurationTypes = this.getObjectConfigurationTypes();

		MithraObjectConfigurationType objectSequenceObjectConfigurationType =
			ReladomoRuntimeConfigurationGenerator.createObjectSequenceObjectConfigurationType();

		return Lists.immutable.with(objectSequenceObjectConfigurationType).newWithAll(objectConfigurationTypes);
	}

	private ImmutableList<MithraObjectConfigurationType> getObjectConfigurationTypes() {
		return this.domainModel.getClasses()
			// TODO: Can a class be transient and abstract? Is that redundant?
			.reject(Klass::isTransient)
			.collect(PackageableElement::getFullyQualifiedName)
			.collectWith(ReladomoRuntimeConfigurationGenerator::createMithraObjectConfigurationType, this.cacheType);
	}

	@Nonnull
	private static MithraObjectConfigurationType createObjectSequenceObjectConfigurationType() {
		return ReladomoRuntimeConfigurationGenerator.createMithraObjectConfigurationType(
			"io.liftwizard.reladomo.simseq.ObjectSequence",
			CacheType.NONE
		);
	}

	@Nonnull
	private static MithraObjectConfigurationType createMithraObjectConfigurationType(
		String fullyQualifiedClassName,
		CacheType cacheType
	) {
		MithraObjectConfigurationType mithraObjectConfigurationType = new MithraObjectConfigurationType();
		mithraObjectConfigurationType.setCacheType(cacheType);
		mithraObjectConfigurationType.setClassName(fullyQualifiedClassName);
		mithraObjectConfigurationType.setOffHeapReplicationExport(false);
		return mithraObjectConfigurationType;
	}

	@Nonnull
	private static MithraPureObjectConfigurationType createMithraPureObjectConfigurationType(
		String fullyQualifiedClassName
	) {
		MithraPureObjectConfigurationType mithraPureObjectConfigurationType = new MithraPureObjectConfigurationType();
		mithraPureObjectConfigurationType.setClassName(fullyQualifiedClassName);
		return mithraPureObjectConfigurationType;
	}
}

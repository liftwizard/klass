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

package cool.klass.generator.reladomo.classlist;

import java.io.IOException;
import java.nio.file.Path;

import javax.annotation.Nonnull;

import com.gs.fw.common.mithra.generator.metamodel.Mithra;
import com.gs.fw.common.mithra.generator.metamodel.MithraGeneratorMarshaller;
import com.gs.fw.common.mithra.generator.metamodel.MithraInterfaceResourceType;
import com.gs.fw.common.mithra.generator.metamodel.MithraObjectResourceType;
import com.gs.fw.common.mithra.generator.metamodel.MithraPureObjectResourceType;
import cool.klass.generator.reladomo.AbstractReladomoGenerator;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.NamedElement;
import org.eclipse.collections.api.list.ImmutableList;

public class ReladomoClassListGenerator extends AbstractReladomoGenerator {

	public ReladomoClassListGenerator(@Nonnull DomainModel domainModel) {
		super(domainModel);
	}

	public void writeClassListFile(@Nonnull Path path) throws IOException {
		MithraGeneratorMarshaller mithraGeneratorMarshaller = new MithraGeneratorMarshaller();
		mithraGeneratorMarshaller.setIndent(true);

		Mithra mithra = this.generateMithra();

		StringBuilder stringBuilder = new StringBuilder();
		mithraGeneratorMarshaller.marshall(stringBuilder, mithra);
		String xmlString = this.sanitizeXmlString(stringBuilder);

		this.printStringToFile(path, xmlString);
	}

	@Nonnull
	private Mithra generateMithra() {
		ImmutableList<MithraObjectResourceType> objectResources = this.domainModel.getClasses()
			.reject(Klass::isTransient)
			.collect(NamedElement::getName)
			.collect(this::getObjectResource);

		ImmutableList<MithraPureObjectResourceType> pureObjectResources = this.domainModel.getClasses()
			.select(Klass::isTransient)
			.collect(NamedElement::getName)
			.collect(this::getPureObjectResource);

		ImmutableList<MithraInterfaceResourceType> interfaceResources = this.domainModel.getInterfaces()
			.collect(NamedElement::getName)
			.collect(this::getInterfaceResource);

		Mithra mithra = new Mithra();
		mithra.setMithraObjectResources(objectResources.castToList());
		mithra.setMithraPureObjectResources(pureObjectResources.castToList());
		mithra.setMithraInterfaceResources(interfaceResources.castToList());
		return mithra;
	}

	@Nonnull
	private MithraObjectResourceType getObjectResource(String className) {
		MithraObjectResourceType objectResource = new MithraObjectResourceType();
		objectResource.setName(className);
		return objectResource;
	}

	@Nonnull
	private MithraPureObjectResourceType getPureObjectResource(String className) {
		MithraPureObjectResourceType pureObjectResource = new MithraPureObjectResourceType();
		pureObjectResource.setName(className);
		return pureObjectResource;
	}

	@Nonnull
	private MithraInterfaceResourceType getInterfaceResource(String interfaceName) {
		MithraInterfaceResourceType interfaceResource = new MithraInterfaceResourceType();
		interfaceResource.setName(interfaceName);
		return interfaceResource;
	}
}

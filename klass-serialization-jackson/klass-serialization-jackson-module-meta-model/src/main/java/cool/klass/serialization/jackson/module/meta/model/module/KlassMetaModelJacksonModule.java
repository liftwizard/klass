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

package cool.klass.serialization.jackson.module.meta.model.module;

import com.fasterxml.jackson.databind.module.SimpleModule;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.projection.Projection;
import cool.klass.serialization.jackson.module.meta.model.domain.MultiplicitySerializer;
import cool.klass.serialization.jackson.module.meta.model.domain.ProjectionSerializer;

public class KlassMetaModelJacksonModule extends SimpleModule {

	public KlassMetaModelJacksonModule() {
		this.addSerializer(Multiplicity.class, new MultiplicitySerializer());
		this.addSerializer(Projection.class, new ProjectionSerializer());
	}
}

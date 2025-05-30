/*
 * Copyright 2024 Craig Motlin
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

package cool.klass.xample.coverage;

import java.sql.Timestamp;

import javax.annotation.Nonnull;

import com.gs.fw.common.mithra.util.DefaultInfinityTimestamp;

public class PropertiesOptional extends PropertiesOptionalAbstract {

    public PropertiesOptional(Timestamp system) {
        super(system);
        // You must not modify this constructor. Mithra calls this internally.
        // You can call this constructor. You can also add new constructors.
    }

    public PropertiesOptional() {
        this(DefaultInfinityTimestamp.getDefaultInfinity());
    }

    @Nonnull
    public String getOptionalDerived() {
        return "cool.klass.xample.coverage.PropertiesOptional.getOptionalDerived";
    }
}

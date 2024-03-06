/*
 * Copyright 2020 Craig Motlin
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

package cool.klass.model.meta.domain.json.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, property = "@type")
@JsonSubTypes(
        {
                @Type(value = AndCriteriaDTO.class, name = "klass.model.meta.domain.AndCriteriaDTO"),
                @Type(value = OrCriteriaDTO.class, name = "klass.model.meta.domain.OrCriteriaDTO"),
                @Type(value = OperatorCriteriaDTO.class, name = "klass.model.meta.domain.OperatorCriteriaDTO"),
        })
public interface CriteriaDTO
{
}
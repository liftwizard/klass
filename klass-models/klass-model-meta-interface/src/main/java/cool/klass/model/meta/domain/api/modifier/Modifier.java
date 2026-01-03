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

package cool.klass.model.meta.domain.api.modifier;

import cool.klass.model.meta.domain.api.OrdinalElement;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

public interface Modifier extends OrdinalElement {
    String CREATED_BY = "createdBy";
    String CREATED_ON = "createdOn";
    String LAST_UPDATED_BY = "lastUpdatedBy";
    ImmutableList<String> AUDIT_PROPERTY_NAMES = Lists.immutable.with(CREATED_BY, CREATED_ON, LAST_UPDATED_BY);

    ModifierOwner getModifierOwner();

    String getKeyword();

    default boolean is(String name) {
        return this.getKeyword().equals(name);
    }

    default boolean isKey() {
        return this.is("key");
    }

    default boolean isID() {
        return this.is("id");
    }

    default boolean isValid() {
        return this.is("valid");
    }

    default boolean isSystem() {
        return this.is("system");
    }

    default boolean isFrom() {
        return this.is("from");
    }

    default boolean isTo() {
        return this.is("to");
    }

    default boolean isFinal() {
        return this.is("final");
    }

    default boolean isPrivate() {
        return this.is("private");
    }

    default boolean isAudit() {
        return Modifier.AUDIT_PROPERTY_NAMES.contains(this.getKeyword());
    }

    default boolean isCreatedBy() {
        return this.is(Modifier.CREATED_BY);
    }

    default boolean isCreatedOn() {
        return this.is(Modifier.CREATED_ON);
    }

    default boolean isLastUpdatedBy() {
        return this.is(Modifier.LAST_UPDATED_BY);
    }

    default boolean isVersion() {
        return this.is("version");
    }

    default boolean isDerived() {
        return this.is("derived");
    }

    default boolean isUserId() {
        return this.is("userId");
    }
}

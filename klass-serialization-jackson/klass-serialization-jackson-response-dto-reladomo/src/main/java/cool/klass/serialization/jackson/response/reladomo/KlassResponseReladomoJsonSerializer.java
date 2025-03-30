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

package cool.klass.serialization.jackson.response.reladomo;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.gs.fw.common.mithra.MithraObject;
import cool.klass.data.store.DataStore;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.serialization.jackson.response.KlassResponse;
import cool.klass.serialization.jackson.response.KlassResponseMetadata;

// TODO: Split into one for non-null lists and one for nullable MithraObjects
public class KlassResponseReladomoJsonSerializer extends JsonSerializer<KlassResponse> {

    @Nonnull
    private final DomainModel domainModel;

    @Nonnull
    private final DataStore dataStore;

    public KlassResponseReladomoJsonSerializer(@Nonnull DomainModel domainModel, @Nonnull DataStore dataStore) {
        this.domainModel = Objects.requireNonNull(domainModel);
        this.dataStore = Objects.requireNonNull(dataStore);
    }

    @Override
    public void serialize(
        @Nonnull KlassResponse klassResponse,
        @Nonnull JsonGenerator jsonGenerator,
        @Nonnull SerializerProvider serializerProvider
    ) throws IOException {
        Class<?> activeViewClass = serializerProvider.getActiveView();
        if (activeViewClass != null) {
            String detailMessage =
                "Expected no active view while serializing KlassResponse but got %s".formatted(
                        activeViewClass.getCanonicalName()
                    );
            throw new IllegalStateException(detailMessage);
        }

        KlassResponseMetadata metadata = klassResponse.getMetadata();

        jsonGenerator.writeStartObject();
        try {
            jsonGenerator.writeObjectField("_metadata", metadata);
            this.serializeData(klassResponse, jsonGenerator, serializerProvider);
        } finally {
            jsonGenerator.writeEndObject();
        }
    }

    private void serializeData(
        @Nonnull KlassResponse klassResponse,
        @Nonnull JsonGenerator jsonGenerator,
        @Nonnull SerializerProvider serializerProvider
    ) throws IOException {
        if (klassResponse.getMetadata().getMultiplicity().isToOne()) {
            this.serializeDataOne(klassResponse, jsonGenerator, serializerProvider);
        } else {
            this.serializeDataMany(klassResponse, jsonGenerator, serializerProvider);
        }
    }

    private void serializeDataOne(
        @Nonnull KlassResponse klassResponse,
        @Nonnull JsonGenerator jsonGenerator,
        @Nonnull SerializerProvider serializerProvider
    ) throws IOException {
        MithraObject mithraObject = (MithraObject) klassResponse.getData();

        if (mithraObject == null) {
            jsonGenerator.writeNullField("_data");
            return;
        }

        jsonGenerator.writeFieldName("_data");
        ReladomoContextJsonSerializer reladomoJsonSerializer = this.getReladomoContextJsonSerializer(klassResponse);
        reladomoJsonSerializer.serialize(mithraObject, jsonGenerator, serializerProvider);
    }

    private void serializeDataMany(
        @Nonnull KlassResponse klassResponse,
        @Nonnull JsonGenerator jsonGenerator,
        @Nonnull SerializerProvider serializerProvider
    ) throws IOException {
        Object data = klassResponse.getData();
        if (!(data instanceof List<?>)) {
            String detailMessage =
                "%s cannot be cast to %s".formatted(data.getClass().getCanonicalName(), List.class.getCanonicalName());
            throw new ClassCastException(detailMessage);
        }

        List<MithraObject> mithraList = (List<MithraObject>) data;
        jsonGenerator.writeArrayFieldStart("_data");
        try {
            ReladomoContextJsonSerializer reladomoJsonSerializer = this.getReladomoContextJsonSerializer(klassResponse);
            for (MithraObject eachMithraObject : mithraList) {
                reladomoJsonSerializer.serialize(eachMithraObject, jsonGenerator, serializerProvider);
            }
        } finally {
            jsonGenerator.writeEndArray();
        }
    }

    @Nonnull
    private ReladomoContextJsonSerializer getReladomoContextJsonSerializer(@Nonnull KlassResponse klassResponse) {
        KlassResponseMetadata metadata = klassResponse.getMetadata();

        return new ReladomoContextJsonSerializer(this.domainModel, this.dataStore, metadata);
    }
}

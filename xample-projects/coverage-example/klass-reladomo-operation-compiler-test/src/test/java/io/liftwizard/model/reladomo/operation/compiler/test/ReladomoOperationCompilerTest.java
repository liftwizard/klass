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

package io.liftwizard.model.reladomo.operation.compiler.test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.gs.fw.common.mithra.finder.Operation;
import com.gs.fw.common.mithra.finder.RelatedFinder;
import cool.klass.xample.coverage.OwnedNaturalOneToManySourceFinder;
import cool.klass.xample.coverage.OwnedNaturalOneToManyTargetFinder;
import cool.klass.xample.coverage.PropertiesOptional;
import cool.klass.xample.coverage.PropertiesOptionalFinder;
import cool.klass.xample.coverage.PropertiesOptionalFinder.PropertiesOptionalSingleFinder;
import cool.klass.xample.coverage.PropertiesRequiredFinder;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.model.reladomo.operation.compiler.ReladomoOperationCompiler;
import io.liftwizard.reladomo.test.extension.ReladomoInitializeExtension;
import org.eclipse.collections.impl.factory.primitive.BooleanSets;
import org.eclipse.collections.impl.factory.primitive.DoubleSets;
import org.eclipse.collections.impl.factory.primitive.FloatSets;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ReladomoOperationCompilerTest {

    public static final Timestamp TIMESTAMP_1 = Timestamp.valueOf("2010-12-31 00:00:00.0");
    public static final Timestamp TIMESTAMP_2 = Timestamp.valueOf("2011-01-01 00:00:00.0");

    public static final Instant INSTANT_1 = Instant.parse("2010-12-31T23:59:00.0Z");
    public static final Instant INSTANT_2 = Instant.parse("2011-01-01T23:59:00.0Z");

    public static final Set<Date> DATES = new LinkedHashSet<>(
        Arrays.asList(new Date(TIMESTAMP_1.getTime()), new Date(TIMESTAMP_2.getTime()), null)
    );

    public static final Set<Timestamp> TIMESTAMPS = new LinkedHashSet<>(
        Arrays.asList(Timestamp.from(INSTANT_1), Timestamp.from(INSTANT_2), null)
    );

    public static final PropertiesOptionalSingleFinder<PropertiesOptional, Object, PropertiesOptional> FINDER =
        PropertiesOptionalFinder.getFinderInstance();

    @RegisterExtension
    private final LogMarkerTestExtension logMarkerTestExtension = new LogMarkerTestExtension();

    @RegisterExtension
    private final ReladomoInitializeExtension initializeTestExtension = new ReladomoInitializeExtension(
        "reladomo-runtime-configuration/ReladomoRuntimeConfiguration.xml"
    );

    @Test
    void invalidClassName() {
        try {
            var reladomoOperationCompiler = new ReladomoOperationCompiler();
            reladomoOperationCompiler.compile(
                PropertiesRequiredFinder.getFinderInstance(),
                "InvalidClassName.requiredString = \"Value\""
            );
            fail("");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(
                "Expected 'this' or <PropertiesRequired> but found: <InvalidClassName> in InvalidClassName.requiredString = \"Value\""
            );
        }
    }

    @Test
    void invalidAttributeName() {
        try {
            var reladomoOperationCompiler = new ReladomoOperationCompiler();
            reladomoOperationCompiler.compile(
                PropertiesRequiredFinder.getFinderInstance(),
                "this.invalidAttributeName = \"Value\""
            );
            fail("");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(
                "Could not find attribute 'invalidAttributeName' on type 'PropertiesRequired' in this.invalidAttributeName = \"Value\". Valid attributes: [propertiesRequiredId, createdById, createdOn, lastUpdatedById, requiredString, requiredInteger, requiredLong, requiredDouble, requiredFloat, requiredBoolean, requiredInstant, requiredLocalDate, systemFrom, systemTo]"
            );
        }
    }

    @Test
    void invalidRelationshipName() {
        try {
            var reladomoOperationCompiler = new ReladomoOperationCompiler();
            reladomoOperationCompiler.compile(
                OwnedNaturalOneToManySourceFinder.getFinderInstance(),
                "OwnedNaturalOneToManySource.invalidRelationshipName.value = \"value\""
            );
            fail("");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(
                "Could not find relationship 'invalidRelationshipName' on type 'OwnedNaturalOneToManySource' in OwnedNaturalOneToManySource.invalidRelationshipName.value = \"value\". Valid relationships: [targets]"
            );
        }
    }

    @Test
    void invalidParameterType() {
        try {
            var reladomoOperationCompiler = new ReladomoOperationCompiler();
            reladomoOperationCompiler.compile(
                PropertiesRequiredFinder.getFinderInstance(),
                "PropertiesRequired.requiredString = 1"
            );
            fail("");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(
                "Expected <String> but found: <1> in PropertiesRequired.requiredString = 1"
            );
        }
    }

    @Test
    void invalidDateFormat() {
        try {
            var reladomoOperationCompiler = new ReladomoOperationCompiler();
            reladomoOperationCompiler.compile(
                PropertiesRequiredFinder.getFinderInstance(),
                "PropertiesRequired.requiredInstant = \"Invalid\""
            );
            fail("");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(
                "Expected <Instant> but found: <\"Invalid\"> in PropertiesRequired.requiredInstant = \"Invalid\""
            );
        }
    }

    @Test
    void nullForList() {
        this.assertCompiles(
            "this.optionalString in [\"example\", null]",
            FINDER.optionalString().in(new LinkedHashSet<>(Arrays.asList("example", null)))
        );
    }

    @Test
    void nullityOperation() {
        this.assertCompiles("this.optionalBoolean is null", FINDER.optionalBoolean().isNull());
        this.assertCompiles("this.optionalInteger is null", FINDER.optionalInteger().isNull());
        this.assertCompiles("this.optionalLong is null", FINDER.optionalLong().isNull());
        this.assertCompiles("this.optionalFloat is null", FINDER.optionalFloat().isNull());
        this.assertCompiles("this.optionalDouble is null", FINDER.optionalDouble().isNull());
        this.assertCompiles("this.optionalLocalDate is null", FINDER.optionalLocalDate().isNull());
        this.assertCompiles("this.optionalInstant is null", FINDER.optionalInstant().isNull());
        this.assertCompiles("this.optionalString is null", FINDER.optionalString().isNull());
        this.assertCompiles("this.system is null", FINDER.system().isNull());

        this.assertCompiles("this.optionalBoolean == null", FINDER.optionalBoolean().isNull());
        this.assertCompiles("this.optionalInteger == null", FINDER.optionalInteger().isNull());
        this.assertCompiles("this.optionalLong == null", FINDER.optionalLong().isNull());
        this.assertCompiles("this.optionalFloat == null", FINDER.optionalFloat().isNull());
        this.assertCompiles("this.optionalDouble == null", FINDER.optionalDouble().isNull());
        this.assertCompiles("this.optionalLocalDate == null", FINDER.optionalLocalDate().isNull());
        this.assertCompiles("this.optionalInstant == null", FINDER.optionalInstant().isNull());
        this.assertCompiles("this.optionalString == null", FINDER.optionalString().isNull());
        this.assertCompiles("this.system == null", FINDER.system().isNull());

        this.assertCompiles("this.optionalBoolean is not null", FINDER.optionalBoolean().isNotNull());
        this.assertCompiles("this.optionalInteger is not null", FINDER.optionalInteger().isNotNull());
        this.assertCompiles("this.optionalLong is not null", FINDER.optionalLong().isNotNull());
        this.assertCompiles("this.optionalFloat is not null", FINDER.optionalFloat().isNotNull());
        this.assertCompiles("this.optionalDouble is not null", FINDER.optionalDouble().isNotNull());
        this.assertCompiles("this.optionalLocalDate is not null", FINDER.optionalLocalDate().isNotNull());
        this.assertCompiles("this.optionalInstant is not null", FINDER.optionalInstant().isNotNull());
        this.assertCompiles("this.optionalString is not null", FINDER.optionalString().isNotNull());
        this.assertCompiles("this.system is not null", FINDER.system().isNotNull());

        this.assertCompiles("this.optionalBoolean != null", FINDER.optionalBoolean().isNotNull());
        this.assertCompiles("this.optionalInteger != null", FINDER.optionalInteger().isNotNull());
        this.assertCompiles("this.optionalLong != null", FINDER.optionalLong().isNotNull());
        this.assertCompiles("this.optionalFloat != null", FINDER.optionalFloat().isNotNull());
        this.assertCompiles("this.optionalDouble != null", FINDER.optionalDouble().isNotNull());
        this.assertCompiles("this.optionalLocalDate != null", FINDER.optionalLocalDate().isNotNull());
        this.assertCompiles("this.optionalInstant != null", FINDER.optionalInstant().isNotNull());
        this.assertCompiles("this.optionalString != null", FINDER.optionalString().isNotNull());
        this.assertCompiles("this.system != null", FINDER.system().isNotNull());
    }

    @Test
    void equalsEdgePointOperation() {
        this.assertCompiles("this.system equalsEdgePoint", FINDER.system().equalsEdgePoint());
    }

    @Test
    void numberFormats() {
        this.assertCompiles("this.optionalFloat = 42.0f", FINDER.optionalFloat().eq(42));
        this.assertCompiles("this.optionalFloat = 42.0d", FINDER.optionalFloat().eq(42));
        this.assertCompiles("this.optionalFloat = 42", FINDER.optionalFloat().eq(42));
        this.assertCompiles("this.optionalDouble = 42.0f", FINDER.optionalDouble().eq(42));
        this.assertCompiles("this.optionalDouble = 42.0d", FINDER.optionalDouble().eq(42));
        this.assertCompiles("this.optionalDouble = 42", FINDER.optionalDouble().eq(42));
        this.assertCompiles("this.optionalLong = 10_000_000_000", FINDER.optionalLong().eq(10_000_000_000L));
        this.assertCompiles("this.optionalInteger = 1_000_000_000", FINDER.optionalInteger().eq(1_000_000_000L));
    }

    @Test
    void equalityOperation() {
        this.assertCompiles("this.optionalBoolean = true", FINDER.optionalBoolean().eq(true));
        this.assertCompiles("this.optionalInteger = 4", FINDER.optionalInteger().eq(4));
        this.assertCompiles("this.optionalLong = 5", FINDER.optionalLong().eq(5L));
        this.assertCompiles("this.optionalFloat = 6.6", FINDER.optionalFloat().eq(6.6f));
        this.assertCompiles("this.optionalDouble = 7.7", FINDER.optionalDouble().eq(7.7));
        this.assertCompiles(
            "this.optionalLocalDate = \"2010-12-31\"",
            FINDER.optionalLocalDate().eq(new Date(TIMESTAMP_1.getTime()))
        );
        this.assertCompiles(
            "this.optionalInstant = \"2010-12-31T23:59:00.0Z\"",
            FINDER.optionalInstant().eq(Timestamp.from(INSTANT_1))
        );
        this.assertCompiles("this.optionalString = \"Value\"", FINDER.optionalString().eq("Value"));
        this.assertCompiles("this.system = \"2010-12-31T23:59:00.0Z\"", FINDER.system().eq(Timestamp.from(INSTANT_1)));

        this.assertCompiles("this.optionalBoolean != true", FINDER.optionalBoolean().notEq(true));
        this.assertCompiles("this.optionalInteger != 4", FINDER.optionalInteger().notEq(4));
        this.assertCompiles("this.optionalLong != 5", FINDER.optionalLong().notEq(5L));
        this.assertCompiles("this.optionalFloat != 6.6", FINDER.optionalFloat().notEq(6.6f));
        this.assertCompiles("this.optionalDouble != 7.7", FINDER.optionalDouble().notEq(7.7));
        this.assertCompiles(
            "this.optionalLocalDate != \"2010-12-31\"",
            FINDER.optionalLocalDate().notEq(new Date(TIMESTAMP_1.getTime()))
        );
        this.assertCompiles(
            "this.optionalInstant != \"2010-12-31T23:59:00.0Z\"",
            FINDER.optionalInstant().notEq(Timestamp.from(INSTANT_1))
        );
        this.assertCompiles("this.optionalString != \"Value\"", FINDER.optionalString().notEq("Value"));

        this.assertCompiles("this.optionalBoolean = true", FINDER.optionalBoolean().nonPrimitiveEq(Boolean.TRUE));
        this.assertCompiles("this.optionalInteger = 4", FINDER.optionalInteger().nonPrimitiveEq(4));
        this.assertCompiles("this.optionalLong = 5", FINDER.optionalLong().nonPrimitiveEq(5L));
        this.assertCompiles("this.optionalFloat = 6.6", FINDER.optionalFloat().nonPrimitiveEq((float) 6.6));
        this.assertCompiles("this.optionalDouble = 7.7", FINDER.optionalDouble().nonPrimitiveEq(7.7));
        this.assertCompiles(
            "this.optionalLocalDate = \"2010-12-31\"",
            FINDER.optionalLocalDate().nonPrimitiveEq(new Date(TIMESTAMP_1.getTime()))
        );
        this.assertCompiles(
            "this.optionalInstant = \"2010-12-31T23:59:00.0Z\"",
            FINDER.optionalInstant().nonPrimitiveEq(Timestamp.from(INSTANT_1))
        );
        this.assertCompiles("this.optionalString = \"Value\"", FINDER.optionalString().nonPrimitiveEq("Value"));
        this.assertCompiles(
            "this.system = \"2010-12-31T23:59:00.0Z\"",
            FINDER.system().nonPrimitiveEq(Timestamp.from(INSTANT_1))
        );
    }

    @Test
    void inequalityOperation() {
        this.assertCompiles("this.optionalInteger > 4", FINDER.optionalInteger().greaterThan(4));
        this.assertCompiles("this.optionalLong > 5", FINDER.optionalLong().greaterThan(5L));
        this.assertCompiles("this.optionalFloat > 6.6", FINDER.optionalFloat().greaterThan(6.6f));
        this.assertCompiles("this.optionalDouble > 7.7", FINDER.optionalDouble().greaterThan(7.7));
        this.assertCompiles(
            "this.optionalLocalDate > \"2010-12-31\"",
            FINDER.optionalLocalDate().greaterThan(new Date(TIMESTAMP_1.getTime()))
        );
        this.assertCompiles(
            "this.optionalInstant > \"2010-12-31T23:59:00.0Z\"",
            FINDER.optionalInstant().greaterThan(Timestamp.from(INSTANT_1))
        );
        this.assertCompiles("this.optionalString > \"Value\"", FINDER.optionalString().greaterThan("Value"));

        this.assertCompiles("this.optionalInteger >= 4", FINDER.optionalInteger().greaterThanEquals(4));
        this.assertCompiles("this.optionalLong >= 5", FINDER.optionalLong().greaterThanEquals(5L));
        this.assertCompiles("this.optionalFloat >= 6.6", FINDER.optionalFloat().greaterThanEquals(6.6f));
        this.assertCompiles("this.optionalDouble >= 7.7", FINDER.optionalDouble().greaterThanEquals(7.7));
        this.assertCompiles(
            "this.optionalLocalDate >= \"2010-12-31\"",
            FINDER.optionalLocalDate().greaterThanEquals(new Date(TIMESTAMP_1.getTime()))
        );
        this.assertCompiles(
            "this.optionalInstant >= \"2010-12-31T23:59:00.0Z\"",
            FINDER.optionalInstant().greaterThanEquals(Timestamp.from(INSTANT_1))
        );
        this.assertCompiles("this.optionalString >= \"Value\"", FINDER.optionalString().greaterThanEquals("Value"));

        this.assertCompiles("this.optionalInteger < 4", FINDER.optionalInteger().lessThan(4));
        this.assertCompiles("this.optionalLong < 5", FINDER.optionalLong().lessThan(5L));
        this.assertCompiles("this.optionalFloat < 6.6", FINDER.optionalFloat().lessThan(6.6f));
        this.assertCompiles("this.optionalDouble < 7.7", FINDER.optionalDouble().lessThan(7.7));
        this.assertCompiles(
            "this.optionalLocalDate < \"2010-12-31\"",
            FINDER.optionalLocalDate().lessThan(new Date(TIMESTAMP_1.getTime()))
        );
        this.assertCompiles(
            "this.optionalInstant < \"2010-12-31T23:59:00.0Z\"",
            FINDER.optionalInstant().lessThan(Timestamp.from(INSTANT_1))
        );
        this.assertCompiles("this.optionalString < \"Value\"", FINDER.optionalString().lessThan("Value"));

        this.assertCompiles("this.optionalInteger <= 4", FINDER.optionalInteger().lessThanEquals(4));
        this.assertCompiles("this.optionalLong <= 5", FINDER.optionalLong().lessThanEquals(5L));
        this.assertCompiles("this.optionalFloat <= 6.6", FINDER.optionalFloat().lessThanEquals(6.6f));
        this.assertCompiles("this.optionalDouble <= 7.7", FINDER.optionalDouble().lessThanEquals(7.7));
        this.assertCompiles(
            "this.optionalLocalDate <= \"2010-12-31\"",
            FINDER.optionalLocalDate().lessThanEquals(new Date(TIMESTAMP_1.getTime()))
        );
        this.assertCompiles(
            "this.optionalInstant <= \"2010-12-31T23:59:00.0Z\"",
            FINDER.optionalInstant().lessThanEquals(Timestamp.from(INSTANT_1))
        );
        this.assertCompiles("this.optionalString <= \"Value\"", FINDER.optionalString().lessThanEquals("Value"));
    }

    @Test
    void stringLikeOperations() {
        this.assertCompiles("this.optionalString endsWith \"Value\"", FINDER.optionalString().endsWith("Value"));
        this.assertCompiles("this.optionalString contains \"Value\"", FINDER.optionalString().contains("Value"));
        this.assertCompiles("this.optionalString startsWith \"Value\"", FINDER.optionalString().startsWith("Value"));
        this.assertCompiles(
            "this.optionalString wildCardEquals \"Value?\"",
            FINDER.optionalString().wildCardEq("Value?")
        );

        this.assertCompiles("this.optionalString not endsWith \"Value\"", FINDER.optionalString().notEndsWith("Value"));
        this.assertCompiles("this.optionalString not contains \"Value\"", FINDER.optionalString().notContains("Value"));
        this.assertCompiles(
            "this.optionalString not startsWith \"Value\"",
            FINDER.optionalString().notStartsWith("Value")
        );
        this.assertCompiles(
            "this.optionalString not wildCardEquals \"Value?\"",
            FINDER.optionalString().wildCardNotEq("Value?")
        );
    }

    @Test
    void stringDerivedAttributes() {
        this.assertCompiles(
            "lower( this.optionalString ) = \"value\"",
            FINDER.optionalString().toLowerCase().eq("value")
        );
        this.assertCompiles(
            "substring(this.optionalString, 2, 3) = \"value\"",
            FINDER.optionalString().substring(2, 3).eq("value")
        );
        this.assertCompiles(
            "substring(lower(this.optionalString), 2, 3) = \"value\"",
            FINDER.optionalString().toLowerCase().substring(2, 3).eq("value")
        );
    }

    @Test
    void numberDerivedAttributes() {
        this.assertCompiles("abs(this.optionalInteger) = 1", FINDER.optionalInteger().absoluteValue().eq(1));
    }

    @Test
    void instantDerivedAttributes() {
        this.assertCompiles("year(this.optionalInstant) = 1999", FINDER.optionalInstant().year().eq(1999));
        this.assertCompiles("month(this.optionalInstant) = 12", FINDER.optionalInstant().month().eq(12));
        this.assertCompiles("dayOfMonth(this.optionalInstant) = 31", FINDER.optionalInstant().dayOfMonth().eq(31));

        this.assertCompiles("year(this.optionalLocalDate) = 1999", FINDER.optionalLocalDate().year().eq(1999));
        this.assertCompiles("month(this.optionalLocalDate) = 12", FINDER.optionalLocalDate().month().eq(12));
        this.assertCompiles("dayOfMonth(this.optionalLocalDate) = 31", FINDER.optionalLocalDate().dayOfMonth().eq(31));
    }

    @Test
    void inOperation() {
        this.assertCompiles(
            "this.optionalBoolean in [true, false]",
            FINDER.optionalBoolean().in(BooleanSets.mutable.with(true, false))
        );
        this.assertCompiles("this.optionalInteger in [4, 5]", FINDER.optionalInteger().in(IntSets.mutable.with(4, 5)));
        this.assertCompiles("this.optionalLong in [5, 6]", FINDER.optionalLong().in(LongSets.mutable.with(5L, 6L)));
        this.assertCompiles(
            "this.optionalFloat in [6.6, 7.7]",
            FINDER.optionalFloat().in(FloatSets.mutable.with(6.6f, 7.7f))
        );
        this.assertCompiles(
            "this.optionalDouble in [7.7, 8.8]",
            FINDER.optionalDouble().in(DoubleSets.mutable.with(7.7, 8.8))
        );
        this.assertCompiles(
            "this.optionalLocalDate in [\"2010-12-31\", \"2011-01-01\", null]",
            FINDER.optionalLocalDate().in(DATES)
        );
        this.assertCompiles(
            "this.optionalInstant in [\"2010-12-31T23:59:00.0Z\", \"2011-01-01T23:59:00.0Z\", null]",
            FINDER.optionalInstant().in(TIMESTAMPS)
        );
        this.assertCompiles(
            "this.optionalString in [\"Value\", \"Value2\", null]",
            FINDER.optionalString().in(new LinkedHashSet<>(Arrays.asList("Value", "Value2", null)))
        );

        this.assertCompiles(
            "this.optionalInteger not in [4, 5]",
            FINDER.optionalInteger().notIn(IntSets.mutable.with(4, 5))
        );
        this.assertCompiles(
            "this.optionalLong not in [5, 6]",
            FINDER.optionalLong().notIn(LongSets.mutable.with(5L, 6L))
        );
        this.assertCompiles(
            "this.optionalFloat not in [6.6, 7.7]",
            FINDER.optionalFloat().notIn(FloatSets.mutable.with(6.6f, 7.7f))
        );
        this.assertCompiles(
            "this.optionalDouble not in [7.7, 8.8]",
            FINDER.optionalDouble().notIn(DoubleSets.mutable.with(7.7, 8.8))
        );
        this.assertCompiles(
            "this.optionalLocalDate not in [\"2010-12-31\", \"2011-01-01\", null]",
            FINDER.optionalLocalDate().notIn(DATES)
        );
        this.assertCompiles(
            "this.optionalInstant not in [\"2010-12-31T23:59:00.0Z\", \"2011-01-01T23:59:00.0Z\", null]",
            FINDER.optionalInstant().notIn(TIMESTAMPS)
        );
        this.assertCompiles(
            "this.optionalString not in [\"Value\", \"Value2\", null]",
            FINDER.optionalString().notIn(new LinkedHashSet<>(Arrays.asList("Value", "Value2", null)))
        );
    }

    @Test
    void relationshipNavigation() {
        RelatedFinder finder = OwnedNaturalOneToManySourceFinder.getFinderInstance();

        this.assertCompiles(
            finder,
            "OwnedNaturalOneToManySource.targets.value = \"value\"",
            OwnedNaturalOneToManySourceFinder.targets().value().eq("value")
        );
        this.assertCompiles(
            finder,
            "OwnedNaturalOneToManySource.targets exists",
            OwnedNaturalOneToManySourceFinder.targets().exists()
        );
        this.assertCompiles(
            finder,
            "OwnedNaturalOneToManySource.targets not exists",
            OwnedNaturalOneToManySourceFinder.targets().notExists()
        );
        this.assertCompiles(
            finder,
            "OwnedNaturalOneToManySource.targets not exists",
            OwnedNaturalOneToManySourceFinder.targets().recursiveNotExists()
        );

        Operation innerOperation = OwnedNaturalOneToManyTargetFinder.source().value().eq("value");
        this.assertCompiles(
            finder,
            "OwnedNaturalOneToManySource.targets { OwnedNaturalOneToManyTarget.source.value = \"value\" } not exists",
            OwnedNaturalOneToManySourceFinder.targets().notExists(innerOperation)
        );
        this.assertCompiles(
            finder,
            "OwnedNaturalOneToManySource.targets { OwnedNaturalOneToManyTarget.source.value = \"value\" } not exists",
            OwnedNaturalOneToManySourceFinder.targets().recursiveNotExists(innerOperation)
        );
    }

    @Test
    void conjunctionOperations() {
        this.assertCompiles(
            "this.optionalBoolean = true & this.optionalInteger = 4",
            FINDER.optionalBoolean().eq(true).and(FINDER.optionalInteger().eq(4))
        );

        this.assertCompiles(
            "this.optionalBoolean = true && this.optionalInteger = 4",
            FINDER.optionalBoolean().eq(true).and(FINDER.optionalInteger().eq(4))
        );

        this.assertCompiles(
            "this.optionalBoolean = true and this.optionalInteger = 4",
            FINDER.optionalBoolean().eq(true).and(FINDER.optionalInteger().eq(4))
        );

        this.assertCompiles(
            "this.optionalBoolean = true | this.optionalInteger = 4",
            FINDER.optionalBoolean().eq(true).or(FINDER.optionalInteger().eq(4))
        );

        this.assertCompiles(
            "this.optionalBoolean = true || this.optionalInteger = 4",
            FINDER.optionalBoolean().eq(true).or(FINDER.optionalInteger().eq(4))
        );

        this.assertCompiles(
            "this.optionalBoolean = true or this.optionalInteger = 4",
            FINDER.optionalBoolean().eq(true).or(FINDER.optionalInteger().eq(4))
        );
    }

    private void assertCompiles(String sourceCodeText, Operation operation) {
        this.assertCompiles(FINDER, sourceCodeText, operation);
    }

    private void assertCompiles(RelatedFinder finder, String sourceCodeText, Operation operation) {
        Operation compiledOperation = this.compile(finder, sourceCodeText);
        if (compiledOperation != null) {
            assertThat(compiledOperation).isEqualTo(operation);
        }
    }

    @Nullable
    private Operation compile(RelatedFinder finder, String sourceCodeText) {
        var reladomoOperationCompiler = new ReladomoOperationCompiler();
        return reladomoOperationCompiler.compile(finder, sourceCodeText);
    }
}

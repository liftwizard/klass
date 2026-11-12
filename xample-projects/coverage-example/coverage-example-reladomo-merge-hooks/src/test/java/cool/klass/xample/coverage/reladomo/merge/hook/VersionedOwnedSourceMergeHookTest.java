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

package cool.klass.xample.coverage.reladomo.merge.hook;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import com.gs.fw.common.mithra.MithraManagerProvider;
import com.gs.fw.common.mithra.list.merge.TopLevelMergeOptions;
import cool.klass.xample.coverage.VersionedOwnedOneToManyGrandchild;
import cool.klass.xample.coverage.VersionedOwnedOneToManyGrandchildList;
import cool.klass.xample.coverage.VersionedOwnedOneToManyTarget;
import cool.klass.xample.coverage.VersionedOwnedOneToManyTargetList;
import cool.klass.xample.coverage.VersionedOwnedOneToOneGrandchild;
import cool.klass.xample.coverage.VersionedOwnedOneToOneTarget;
import cool.klass.xample.coverage.VersionedOwnedSource;
import cool.klass.xample.coverage.VersionedOwnedSourceFinder;
import cool.klass.xample.coverage.VersionedOwnedSourceList;
import cool.klass.xample.coverage.VersionedOwnedSourceVersion;
import cool.klass.xample.coverage.VersionedOwnedSourceVersionFinder;
import io.liftwizard.junit.extension.liquibase.migrations.LiquibaseTestExtension;
import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.reladomo.test.extension.ReladomoInitializeExtension;
import io.liftwizard.reladomo.test.extension.ReladomoLoadDataExtension;
import io.liftwizard.reladomo.test.extension.ReladomoTestFile;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(LogMarkerTestExtension.class)
class VersionedOwnedSourceMergeHookTest
{
	private static final Instant FIRST_MERGE = Instant.parse("2000-01-02T00:00:00Z");
	private static final Instant SECOND_MERGE = Instant.parse("2000-01-03T00:00:00Z");

	@Order(1)
	@RegisterExtension
	private final LiquibaseTestExtension liquibaseTestExtension = new LiquibaseTestExtension(
		"cool/klass/xample/coverage/liquibase/schema/migrations-initial-schema.xml"
	);

	@Order(2)
	@RegisterExtension
	private final ReladomoInitializeExtension initializeTestExtension = new ReladomoInitializeExtension(
		"reladomo-runtime-configuration/ReladomoRuntimeConfiguration.xml"
	);

	@Order(3)
	@RegisterExtension
	private final ReladomoLoadDataExtension loadDataTestExtension = new ReladomoLoadDataExtension();

	@Test
	@ReladomoTestFile("test-data/VersionedOwnedSource.txt")
	void changesToOwnedChildrenBumpTheVersionOncePerTransaction()
	{
		var incoming = new VersionedOwnedSourceList();
		// Only the owned children change: the detail and one item are edited, one item is removed, one is added.
		incoming.add(
			source("child-changed", "child value", "child detail edited", "a", "child a edited", "c", "child c")
		);
		// The source and its children both change.
		incoming.add(source("both-changed", "both value edited", "both detail", "a", "both a edited"));
		// The incoming detail is attached before the source has a key, so the detail's key is null when its hook runs.
		var detailMissing = new VersionedOwnedSource();
		detailMissing.setValue("no detail");
		detailMissing.setCreatedById("test user 1");
		detailMissing.setCreatedOn(Timestamp.from(Instant.parse("2000-01-01T00:00:00Z")));
		detailMissing.setLastUpdatedById("test user 2");
		detailMissing.setDetail(detail("new detail"));
		detailMissing.setKey("detail-missing");
		incoming.add(detailMissing);
		// Only the grandchildren under item a change: the grandchild and one grandchild in the list are edited, one is removed, one is added.
		incoming.add(
			withGrandchildren(
				source("grandchild-changed", "grandchild value", "grandchild detail", "a", "grandchild a"),
				"grandchild one edited",
				"x",
				"grandchild x edited",
				"z",
				"grandchild z"
			)
		);
		// A new source inserts its version, children, and grandchildren; none of them may bump the new version.
		incoming.add(
			withGrandchildren(
				source("inserted", "new value", "new detail", "a", "new a"),
				"new grandchild one",
				"x",
				"new grandchild x"
			)
		);
		incoming.add(
			withGrandchildren(
				source("unchanged", "same", "same detail", "a", "same a"),
				"same grandchild one",
				"x",
				"same grandchild x"
			)
		);
		// "terminated" is missing, so it, its children, and its grandchildren are terminated; none may bump its version.

		merge(FIRST_MERGE, incoming);

		var start = "2000-01-01T00:00:00Z";
		var first = "2000-01-02T00:00:00Z";
		var infinity = "9999-12-01T23:59:00Z";
		assertEquals(
			List.of(
				"both-changed 1 " + start + " " + first + " test user 1",
				"both-changed 2 " + first + " " + infinity + " test user 2",
				"child-changed 1 " + start + " " + first + " test user 1",
				"child-changed 2 " + first + " " + infinity + " test user 2",
				"detail-missing 1 " + start + " " + first + " test user 1",
				"detail-missing 2 " + first + " " + infinity + " test user 2",
				"grandchild-changed 1 " + start + " " + first + " test user 1",
				"grandchild-changed 2 " + first + " " + infinity + " test user 2",
				"inserted 1 " + first + " " + infinity + " test user 2",
				"terminated 1 " + start + " " + first + " test user 1",
				"unchanged 1 " + start + " " + infinity + " test user 1"
			),
			versionHistory()
		);
	}

	@Test
	@ReladomoTestFile("test-data/VersionedOwnedSource.txt")
	void aLaterTransactionBumpsTheVersionAgain()
	{
		merge(
			FIRST_MERGE,
			list(source("child-changed", "child value", "child detail", "a", "child a", "b", "child b2"))
		);
		merge(
			SECOND_MERGE,
			list(source("child-changed", "child value", "child detail", "a", "child a", "b", "child b3"))
		);

		assertEquals(
			List.of(
				"child-changed 1 2000-01-01T00:00:00Z 2000-01-02T00:00:00Z test user 1",
				"child-changed 2 2000-01-02T00:00:00Z 2000-01-03T00:00:00Z test user 2",
				"child-changed 3 2000-01-03T00:00:00Z 9999-12-01T23:59:00Z test user 2"
			),
			versionHistory()
				.stream()
				.filter((each) -> each.startsWith("child-changed"))
				.toList()
		);
	}

	private static void merge(Instant time, VersionedOwnedSourceList incoming)
	{
		MithraManagerProvider.getMithraManager().executeTransactionalCommand((transaction) ->
		{
			transaction.setProcessingStartTime(time.toEpochMilli());
			VersionedOwnedSourceList existing = VersionedOwnedSourceFinder.findMany(VersionedOwnedSourceFinder.all());
			var mergeHook = new VersionedOwnedSourceMergeHook();
			var mergeOptions = new TopLevelMergeOptions<VersionedOwnedSource>(
				VersionedOwnedSourceFinder.getFinderInstance()
			);
			mergeOptions.doNotCompare(
				VersionedOwnedSourceFinder.systemFrom(),
				VersionedOwnedSourceFinder.systemTo(),
				VersionedOwnedSourceFinder.createdById(),
				VersionedOwnedSourceFinder.createdOn(),
				VersionedOwnedSourceFinder.lastUpdatedById()
			);
			mergeOptions.withMergeHook(mergeHook);
			mergeHook.navigateToOwnedChildren(mergeOptions);
			existing.merge(incoming, mergeOptions);
			return null;
		});
	}

	private static VersionedOwnedSourceList list(VersionedOwnedSource source)
	{
		var result = new VersionedOwnedSourceList();
		result.add(source);
		return result;
	}

	private static VersionedOwnedSource source(String key, String value, String detailValue, String... items)
	{
		var source = new VersionedOwnedSource();
		source.setKey(key);
		source.setValue(value);
		source.setCreatedById("test user 1");
		source.setCreatedOn(Timestamp.from(Instant.parse("2000-01-01T00:00:00Z")));
		source.setLastUpdatedById("test user 2");
		source.setDetail(detail(detailValue));
		var itemList = new VersionedOwnedOneToManyTargetList();
		for (var i = 0; i < items.length; i += 2)
		{
			var item = new VersionedOwnedOneToManyTarget();
			item.setName(items[i]);
			item.setValue(items[i + 1]);
			itemList.add(item);
		}
		source.setItems(itemList);
		return source;
	}

	private static VersionedOwnedSource withGrandchildren(
		VersionedOwnedSource source,
		String grandchildValue,
		String... grandchildren
	)
	{
		VersionedOwnedOneToManyTarget item = source.getItems().getFirst();
		var grandchild = new VersionedOwnedOneToOneGrandchild();
		grandchild.setValue(grandchildValue);
		item.setGrandchild(grandchild);
		var grandchildList = new VersionedOwnedOneToManyGrandchildList();
		for (var i = 0; i < grandchildren.length; i += 2)
		{
			var each = new VersionedOwnedOneToManyGrandchild();
			each.setName(grandchildren[i]);
			each.setValue(grandchildren[i + 1]);
			grandchildList.add(each);
		}
		item.setGrandchildren(grandchildList);
		return source;
	}

	private static VersionedOwnedOneToOneTarget detail(String value)
	{
		var detail = new VersionedOwnedOneToOneTarget();
		detail.setValue(value);
		return detail;
	}

	private static List<String> versionHistory()
	{
		MutableList<String> result = Lists.mutable.empty();
		for (VersionedOwnedSourceVersion each : VersionedOwnedSourceVersionFinder.findMany(
			VersionedOwnedSourceVersionFinder.system().equalsEdgePoint()
		))
		{
			result.add(
				"%s %d %s %s %s".formatted(
					each.getKey(),
					each.getNumber(),
					each.getSystemFrom().toInstant(),
					each.getSystemTo().toInstant(),
					each.getLastUpdatedById()
				)
			);
		}
		result.sort(null);
		return result;
	}
}

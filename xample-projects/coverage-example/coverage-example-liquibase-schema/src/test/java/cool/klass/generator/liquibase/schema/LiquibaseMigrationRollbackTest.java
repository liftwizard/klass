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

package cool.klass.generator.liquibase.schema;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import io.liftwizard.junit.extension.log.marker.LogMarkerTestExtension;
import io.liftwizard.reladomo.connectionmanager.h2.memory.H2InMemoryConnectionManager;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.Scope.Attr;
import liquibase.UpdateSummaryOutputEnum;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.ui.LoggerUIService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(LogMarkerTestExtension.class)
public class LiquibaseMigrationRollbackTest {

	private static final String MIGRATIONS_FILE =
		"cool/klass/xample/coverage/liquibase/schema/migrations-initial-schema.xml";

	@Test
	void everyMigrationHasRollback() throws Exception {
		Scope.child(Attr.ui, new LoggerUIService(), this::verifyMigrationsHaveRollback);
	}

	private void verifyMigrationsHaveRollback() throws SQLException, LiquibaseException {
		try (Connection connection = H2InMemoryConnectionManager.getInstance().getConnection()) {
			Database database = this.createDatabase(connection);

			try (Liquibase liquibase = this.openLiquibase(database)) {
				liquibase.dropAll();

				List<ChangeSet> changeSets = liquibase.getDatabaseChangeLog().getChangeSets();
				assertThat(changeSets).isNotEmpty();

				for (ChangeSet changeSet : changeSets) {
					String changeSetId = changeSet.getId();
					String changeSetTag = "tag-" + changeSetId;

					liquibase.update(changeSetTag);

					liquibase.rollback(1, null);

					liquibase.update(changeSetTag);
				}
			}
		}
	}

	private Liquibase openLiquibase(Database database) throws LiquibaseException {
		Liquibase liquibase = new Liquibase(MIGRATIONS_FILE, new ClassLoaderResourceAccessor(), database);
		liquibase.setShowSummaryOutput(UpdateSummaryOutputEnum.LOG);
		return liquibase;
	}

	private Database createDatabase(Connection connection) throws LiquibaseException {
		DatabaseConnection jdbcConnection = new JdbcConnection(connection);

		Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);

		database.supportsCatalogs();
		database.supportsSchemas();

		return database;
	}
}

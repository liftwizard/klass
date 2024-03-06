package cool.klass.reladomo.persistent.writer.test;

import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cool.klass.data.store.reladomo.ReladomoDataStore;
import cool.klass.deserializer.json.OperationMode;
import cool.klass.dropwizard.configuration.domain.model.loader.compiler.DomainModelCompilerFactory;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.reladomo.persistent.writer.IncomingCreateDataModelValidator;
import io.dropwizard.jackson.Jackson;
import io.liftwizard.dropwizard.configuration.uuid.seed.SeedUUIDSupplier;
import io.liftwizard.junit.rule.liquibase.migrations.LiquibaseTestRule;
import io.liftwizard.junit.rule.log.marker.LogMarkerTestRule;
import io.liftwizard.junit.rule.match.file.FileMatchRule;
import io.liftwizard.junit.rule.match.json.JsonMatchRule;
import io.liftwizard.serialization.jackson.config.ObjectMapperConfig;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.Rule;
import org.junit.rules.TestRule;

public abstract class AbstractValidatorTest
{
    @Rule
    public final FileMatchRule fileMatchRule = new FileMatchRule(this.getClass());

    @Rule
    public final TestRule logMarkerTestRule = new LogMarkerTestRule();

    @Rule
    public final JsonMatchRule jsonMatchRule = new JsonMatchRule(this.getClass());

    @Rule
    public final LiquibaseTestRule liquibaseTestRule = new LiquibaseTestRule(
            "cool/klass/reladomo/persistent/writer/test/migrations.xml");

    protected final MutableList<String> actualErrors      = Lists.mutable.empty();
    protected final MutableList<String> actualWarnings    = Lists.mutable.empty();
    protected final ReladomoDataStore   reladomoDataStore = this.getReladomoDataStore();
    protected final ObjectMapper        objectMapper      = AbstractValidatorTest.getObjectMapper();
    protected final DomainModel         domainModel       = AbstractValidatorTest.getDomainModel(this.objectMapper);

    protected void validate(String testName)
            throws JsonProcessingException
    {
        this.validate(testName, null);
    }

    protected void validate(String testName, Object persistentInstance)
            throws JsonProcessingException
    {
        String incomingJsonName = this.getClass().getSimpleName() + '.' + testName + ".json5";
        String incomingJson     = FileMatchRule.slurp(incomingJsonName, this.getClass());

        ObjectNode incomingInstance = (ObjectNode) this.objectMapper.readTree(incomingJson);
        this.validate(incomingInstance, persistentInstance);

        this.jsonMatchRule.assertFileContents(
                this.getClass().getSimpleName() + '.' + testName + ".errors.json",
                this.objectMapper.writeValueAsString(this.actualErrors));

        this.jsonMatchRule.assertFileContents(
                this.getClass().getSimpleName() + '.' + testName + ".warnings.json",
                this.objectMapper.writeValueAsString(this.actualWarnings));
    }

    protected abstract void validate(@Nonnull ObjectNode incomingInstance, Object persistentInstance);

    @Nonnull
    private static ObjectMapper getObjectMapper()
    {
        ObjectMapper objectMapper = Jackson.newObjectMapper();
        ObjectMapperConfig.configure(objectMapper);
        return objectMapper;
    }

    @Nonnull
    protected abstract Klass getKlass();

    @Nonnull
    protected abstract OperationMode getMode();

    @Nonnull
    private ReladomoDataStore getReladomoDataStore()
    {
        String           seed         = IncomingCreateDataModelValidator.class.getSimpleName();
        SeedUUIDSupplier uuidSupplier = new SeedUUIDSupplier(seed);
        return new ReladomoDataStore(uuidSupplier, 1);
    }

    private static DomainModel getDomainModel(ObjectMapper objectMapper)
    {
        DomainModelCompilerFactory domainModelCompilerFactory = new DomainModelCompilerFactory();
        domainModelCompilerFactory.setSourcePackages(List.of("cool.klass.xample.coverage"));
        return domainModelCompilerFactory.createDomainModel(objectMapper);
    }
}

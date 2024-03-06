package klass.model.meta.domain.dropwizard.test;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import cool.klass.model.converter.bootstrap.writer.KlassBootstrapWriter;
import cool.klass.reladomo.test.rule.ReladomoTestRule;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.dropwizard.util.Duration;
import klass.model.meta.domain.dropwizard.application.KlassBootstrappedMetaModelApplication;
import klass.model.meta.domain.dropwizard.application.KlassBootstrappedMetaModelConfiguration;
import org.json.JSONException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class MetaResourceManualTest
{
    @ClassRule
    public static final DropwizardAppRule<KlassBootstrappedMetaModelConfiguration> RULE = new DropwizardAppRule<>(
            KlassBootstrappedMetaModelApplication.class,
            ResourceHelpers.resourceFilePath("config-test.yml"));

    @Rule
    public final ReladomoTestRule reladomoTestRule = new ReladomoTestRule(
            "reladomo-runtime-configuration/TestReladomoRuntimeConfiguration.xml")
            .transactionTimeout(5, TimeUnit.MINUTES);

    protected Client getClient(String clientName)
    {
        JerseyClientConfiguration jerseyClientConfiguration = new JerseyClientConfiguration();
        jerseyClientConfiguration.setTimeout(Duration.minutes(5));

        return new JerseyClientBuilder(RULE.getEnvironment())
                .using(jerseyClientConfiguration)
                .build(clientName);
    }

    @Before
    public void bootstrap()
    {
        KlassBootstrappedMetaModelApplication application = RULE.getApplication();

        KlassBootstrapWriter klassBootstrapWriter = new KlassBootstrapWriter(
                application.getDomainModel(),
                application.getDataStore());
        klassBootstrapWriter.bootstrapMetaModel();
    }

    @Test
    public void metaEnumeration() throws JSONException
    {
        Client client = this.getClient(
                "klass.model.meta.domain.dropwizard.test.MetaResourceManualTest.metaEnumeration");

        Response response = client.target(
                String.format("http://localhost:%d/api/meta/enumeration/PrimitiveType", RULE.getLocalPort()))
                .request()
                .get();

        this.assertResponseStatus(response, Status.OK);

        String jsonResponse = response.readEntity(String.class);
        //language=JSON
        String expected = ""
                + "{\n"
                + "  \"name\": \"PrimitiveType\",\n"
                + "  \"inferred\": false,\n"
                + "  \"packageName\": \"klass.model.meta.domain\",\n"
                + "  \"ordinal\": 2,\n"
                + "  \"sourceCode\": \"enumeration PrimitiveType\\n{\\n    INTEGER(\\\"Integer\\\"),\\n    LONG(\\\"Long\\\"),\\n    DOUBLE(\\\"Double\\\"),\\n    FLOAT(\\\"Float\\\"),\\n    BOOLEAN(\\\"Boolean\\\"),\\n    STRING(\\\"String\\\"),\\n    INSTANT(\\\"Instant\\\"),\\n    LOCAL_DATE(\\\"LocalDate\\\"),\\n    TEMPORAL_INSTANT(\\\"TemporalInstant\\\"),\\n    TEMPORAL_RANGE(\\\"TemporalRange\\\"),\\n}\",\n"
                + "  \"sourceCodeWithInference\": \"enumeration PrimitiveType\\n{\\n    INTEGER(\\\"Integer\\\"),\\n    LONG(\\\"Long\\\"),\\n    DOUBLE(\\\"Double\\\"),\\n    FLOAT(\\\"Float\\\"),\\n    BOOLEAN(\\\"Boolean\\\"),\\n    STRING(\\\"String\\\"),\\n    INSTANT(\\\"Instant\\\"),\\n    LOCAL_DATE(\\\"LocalDate\\\"),\\n    TEMPORAL_INSTANT(\\\"TemporalInstant\\\"),\\n    TEMPORAL_RANGE(\\\"TemporalRange\\\"),\\n}\",\n"
                + "  \"enumerationLiterals\": [\n"
                + "    {\n"
                + "      \"prettyName\": \"Integer\",\n"
                + "      \"name\": \"INTEGER\",\n"
                + "      \"inferred\": false,\n"
                + "      \"ordinal\": 1,\n"
                + "      \"sourceCode\": \"INTEGER(\\\"Integer\\\")\",\n"
                + "      \"sourceCodeWithInference\": \"INTEGER(\\\"Integer\\\")\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"prettyName\": \"Long\",\n"
                + "      \"name\": \"LONG\",\n"
                + "      \"inferred\": false,\n"
                + "      \"ordinal\": 2,\n"
                + "      \"sourceCode\": \"LONG(\\\"Long\\\")\",\n"
                + "      \"sourceCodeWithInference\": \"LONG(\\\"Long\\\")\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"prettyName\": \"Double\",\n"
                + "      \"name\": \"DOUBLE\",\n"
                + "      \"inferred\": false,\n"
                + "      \"ordinal\": 3,\n"
                + "      \"sourceCode\": \"DOUBLE(\\\"Double\\\")\",\n"
                + "      \"sourceCodeWithInference\": \"DOUBLE(\\\"Double\\\")\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"prettyName\": \"Float\",\n"
                + "      \"name\": \"FLOAT\",\n"
                + "      \"inferred\": false,\n"
                + "      \"ordinal\": 4,\n"
                + "      \"sourceCode\": \"FLOAT(\\\"Float\\\")\",\n"
                + "      \"sourceCodeWithInference\": \"FLOAT(\\\"Float\\\")\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"prettyName\": \"Boolean\",\n"
                + "      \"name\": \"BOOLEAN\",\n"
                + "      \"inferred\": false,\n"
                + "      \"ordinal\": 5,\n"
                + "      \"sourceCode\": \"BOOLEAN(\\\"Boolean\\\")\",\n"
                + "      \"sourceCodeWithInference\": \"BOOLEAN(\\\"Boolean\\\")\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"prettyName\": \"String\",\n"
                + "      \"name\": \"STRING\",\n"
                + "      \"inferred\": false,\n"
                + "      \"ordinal\": 6,\n"
                + "      \"sourceCode\": \"STRING(\\\"String\\\")\",\n"
                + "      \"sourceCodeWithInference\": \"STRING(\\\"String\\\")\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"prettyName\": \"Instant\",\n"
                + "      \"name\": \"INSTANT\",\n"
                + "      \"inferred\": false,\n"
                + "      \"ordinal\": 7,\n"
                + "      \"sourceCode\": \"INSTANT(\\\"Instant\\\")\",\n"
                + "      \"sourceCodeWithInference\": \"INSTANT(\\\"Instant\\\")\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"prettyName\": \"LocalDate\",\n"
                + "      \"name\": \"LOCAL_DATE\",\n"
                + "      \"inferred\": false,\n"
                + "      \"ordinal\": 8,\n"
                + "      \"sourceCode\": \"LOCAL_DATE(\\\"LocalDate\\\")\",\n"
                + "      \"sourceCodeWithInference\": \"LOCAL_DATE(\\\"LocalDate\\\")\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"prettyName\": \"TemporalInstant\",\n"
                + "      \"name\": \"TEMPORAL_INSTANT\",\n"
                + "      \"inferred\": false,\n"
                + "      \"ordinal\": 9,\n"
                + "      \"sourceCode\": \"TEMPORAL_INSTANT(\\\"TemporalInstant\\\")\",\n"
                + "      \"sourceCodeWithInference\": \"TEMPORAL_INSTANT(\\\"TemporalInstant\\\")\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"prettyName\": \"TemporalRange\",\n"
                + "      \"name\": \"TEMPORAL_RANGE\",\n"
                + "      \"inferred\": false,\n"
                + "      \"ordinal\": 10,\n"
                + "      \"sourceCode\": \"TEMPORAL_RANGE(\\\"TemporalRange\\\")\",\n"
                + "      \"sourceCodeWithInference\": \"TEMPORAL_RANGE(\\\"TemporalRange\\\")\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        JSONAssert.assertEquals(expected, jsonResponse, JSONCompareMode.STRICT);
    }

    @Test
    public void metaInterface() throws JSONException
    {
        Client client = this.getClient(
                "klass.model.meta.domain.dropwizard.test.MetaResourceManualTest.metaInterface");

        Response response = client.target(
                String.format("http://localhost:%d/api/meta/interface/PackageableElement", RULE.getLocalPort()))
                .request()
                .get();

        this.assertResponseStatus(response, Status.OK);

        String jsonResponse = response.readEntity(String.class);
        //language=JSON
        String expected = "{\n"
                + "  \"name\": \"PackageableElement\",\n"
                + "  \"inferred\": false,\n"
                + "  \"packageName\": \"klass.model.meta.domain\",\n"
                + "  \"ordinal\": 10,\n"
                + "  \"sourceCode\": \"interface PackageableElement implements NamedElement\\n{\\n    packageName: String;\\n\\n    // fullyQualifiedName: String = packageName + \\\".\\\" + name;\\n}\",\n"
                + "  \"sourceCodeWithInference\": \"interface PackageableElement\\n    implements NamedElement\\n{\\n    packageName: String;\\n    name: String key;\\n    ordinal: Integer;\\n    inferred: Boolean;\\n    sourceCode: String maximumLength(100000);\\n    sourceCodeWithInference: String maximumLength(100000);\\n}\\n\",\n"
                + "  \"superInterfaces\": [\n"
                + "    {\n"
                + "      \"superInterface\": {\n"
                + "        \"name\": \"NamedElement\",\n"
                + "        \"packageName\": \"klass.model.meta.domain\",\n"
                + "        \"superInterfaces\": [\n"
                + "          {\n"
                + "            \"superInterface\": {\n"
                + "              \"name\": \"Element\",\n"
                + "              \"packageName\": \"klass.model.meta.domain\"\n"
                + "            }\n"
                + "          }\n"
                + "        ]\n"
                + "      }\n"
                + "    }\n"
                + "  ],\n"
                + "  \"classifierModifiers\": [],\n"
                + "  \"primitiveProperties\": [\n"
                + "    {\n"
                + "      \"name\": \"inferred\",\n"
                + "      \"inferred\": false,\n"
                + "      \"primitiveType\": \"Boolean\",\n"
                + "      \"optional\": false,\n"
                + "      \"key\": false,\n"
                + "      \"id\": false,\n"
                + "      \"ordinal\": 1,\n"
                + "      \"propertyModifiers\": []\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"name\",\n"
                + "      \"inferred\": false,\n"
                + "      \"primitiveType\": \"String\",\n"
                + "      \"optional\": false,\n"
                + "      \"key\": true,\n"
                + "      \"id\": false,\n"
                + "      \"ordinal\": 1,\n"
                + "      \"propertyModifiers\": [\n"
                + "        {\n"
                + "          \"name\": \"key\",\n"
                + "          \"inferred\": false,\n"
                + "          \"ordinal\": 1\n"
                + "        }\n"
                + "      ]\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"packageName\",\n"
                + "      \"inferred\": false,\n"
                + "      \"primitiveType\": \"String\",\n"
                + "      \"optional\": false,\n"
                + "      \"key\": false,\n"
                + "      \"id\": false,\n"
                + "      \"ordinal\": 1,\n"
                + "      \"propertyModifiers\": []\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"ordinal\",\n"
                + "      \"inferred\": false,\n"
                + "      \"primitiveType\": \"Integer\",\n"
                + "      \"optional\": false,\n"
                + "      \"key\": false,\n"
                + "      \"id\": false,\n"
                + "      \"ordinal\": 2,\n"
                + "      \"propertyModifiers\": []\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"sourceCode\",\n"
                + "      \"inferred\": false,\n"
                + "      \"primitiveType\": \"String\",\n"
                + "      \"optional\": false,\n"
                + "      \"key\": false,\n"
                + "      \"id\": false,\n"
                + "      \"ordinal\": 2,\n"
                + "      \"propertyModifiers\": [],\n"
                + "      \"maxLengthValidation\": {\n"
                + "        \"number\": 100000\n"
                + "      }\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"sourceCodeWithInference\",\n"
                + "      \"inferred\": false,\n"
                + "      \"primitiveType\": \"String\",\n"
                + "      \"optional\": false,\n"
                + "      \"key\": false,\n"
                + "      \"id\": false,\n"
                + "      \"ordinal\": 3,\n"
                + "      \"propertyModifiers\": [],\n"
                + "      \"maxLengthValidation\": {\n"
                + "        \"number\": 100000\n"
                + "      }\n"
                + "    }\n"
                + "  ],\n"
                + "  \"enumerationProperties\": []\n"
                + "}";
        JSONAssert.assertEquals(expected, jsonResponse, JSONCompareMode.STRICT);
    }

    @Test
    public void metaClass() throws JSONException
    {
        Client client = this.getClient(
                "klass.model.meta.domain.dropwizard.test.MetaResourceManualTest.metaClass");

        Response response = client.target(
                String.format("http://localhost:%d/api/meta/class/Classifier", RULE.getLocalPort()))
                .request()
                .get();

        this.assertResponseStatus(response, Status.OK);

        String jsonResponse = response.readEntity(String.class);
        //language=JSON
        String expected = ""
                + "{\n"
                + "  \"name\": \"Classifier\",\n"
                + "  \"inferred\": false,\n"
                + "  \"packageName\": \"klass.model.meta.domain\",\n"
                + "  \"ordinal\": 13,\n"
                + "  \"sourceCode\": \"class Classifier\\n    abstract(table-per-class)\\n    implements PackageableElement\\n{\\n}\",\n"
                + "  \"sourceCodeWithInference\": \"class Classifier\\n    abstract(table-per-class)\\n    implements PackageableElement\\n{\\n    packageName: String;\\n    name: String key;\\n    ordinal: Integer;\\n    inferred: Boolean;\\n    sourceCode: String maximumLength(100000);\\n    sourceCodeWithInference: String maximumLength(100000);\\n    superInterfaces               : ClassifierInterfaceMapping[0..*] owned;\\n    primitiveProperties: PrimitiveProperty[0..*]\\n        orderBy: this.ordinal;\\n    enumerationProperties: EnumerationProperty[0..*]\\n        orderBy: this.ordinal;\\n    classifierModifiers: ClassifierModifier[0..*]\\n        orderBy: this.ordinal;\\n}\\n\",\n"
                + "  \"superInterfaces\": [\n"
                + "    {\n"
                + "      \"superInterface\": {\n"
                + "        \"name\": \"PackageableElement\",\n"
                + "        \"packageName\": \"klass.model.meta.domain\",\n"
                + "        \"superInterfaces\": [\n"
                + "          {\n"
                + "            \"superInterface\": {\n"
                + "              \"name\": \"NamedElement\",\n"
                + "              \"packageName\": \"klass.model.meta.domain\"\n"
                + "            }\n"
                + "          }\n"
                + "        ]\n"
                + "      }\n"
                + "    }\n"
                + "  ],\n"
                + "  \"classifierModifiers\": [],\n"
                + "  \"primitiveProperties\": [\n"
                + "    {\n"
                + "      \"name\": \"inferred\",\n"
                + "      \"inferred\": false,\n"
                + "      \"primitiveType\": \"Boolean\",\n"
                + "      \"optional\": false,\n"
                + "      \"key\": false,\n"
                + "      \"id\": false,\n"
                + "      \"ordinal\": 1,\n"
                + "      \"propertyModifiers\": []\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"name\",\n"
                + "      \"inferred\": false,\n"
                + "      \"primitiveType\": \"String\",\n"
                + "      \"optional\": false,\n"
                + "      \"key\": true,\n"
                + "      \"id\": false,\n"
                + "      \"ordinal\": 1,\n"
                + "      \"propertyModifiers\": [\n"
                + "        {\n"
                + "          \"name\": \"key\",\n"
                + "          \"inferred\": false,\n"
                + "          \"ordinal\": 1\n"
                + "        }\n"
                + "      ]\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"packageName\",\n"
                + "      \"inferred\": false,\n"
                + "      \"primitiveType\": \"String\",\n"
                + "      \"optional\": false,\n"
                + "      \"key\": false,\n"
                + "      \"id\": false,\n"
                + "      \"ordinal\": 1,\n"
                + "      \"propertyModifiers\": []\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"ordinal\",\n"
                + "      \"inferred\": false,\n"
                + "      \"primitiveType\": \"Integer\",\n"
                + "      \"optional\": false,\n"
                + "      \"key\": false,\n"
                + "      \"id\": false,\n"
                + "      \"ordinal\": 2,\n"
                + "      \"propertyModifiers\": []\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"sourceCode\",\n"
                + "      \"inferred\": false,\n"
                + "      \"primitiveType\": \"String\",\n"
                + "      \"optional\": false,\n"
                + "      \"key\": false,\n"
                + "      \"id\": false,\n"
                + "      \"ordinal\": 2,\n"
                + "      \"propertyModifiers\": [],\n"
                + "      \"maxLengthValidation\": {\n"
                + "        \"number\": 100000\n"
                + "      }\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"sourceCodeWithInference\",\n"
                + "      \"inferred\": false,\n"
                + "      \"primitiveType\": \"String\",\n"
                + "      \"optional\": false,\n"
                + "      \"key\": false,\n"
                + "      \"id\": false,\n"
                + "      \"ordinal\": 3,\n"
                + "      \"propertyModifiers\": [],\n"
                + "      \"maxLengthValidation\": {\n"
                + "        \"number\": 100000\n"
                + "      }\n"
                + "    }\n"
                + "  ],\n"
                + "  \"enumerationProperties\": [],\n"
                + "  \"associationEnds\": [\n"
                + "    {\n"
                + "      \"name\": \"classifierModifiers\",\n"
                + "      \"inferred\": false,\n"
                + "      \"ordinal\": 2,\n"
                + "      \"sourceCode\": \"classifierModifiers: ClassifierModifier[0..*]\\n        orderBy: this.ordinal;\",\n"
                + "      \"sourceCodeWithInference\": \"classifierModifiers: ClassifierModifier[0..*]\\n        orderBy: this.ordinal;\",\n"
                + "      \"direction\": \"target\",\n"
                + "      \"multiplicity\": \"0..*\",\n"
                + "      \"owningClass\": {\n"
                + "        \"name\": \"Classifier\",\n"
                + "        \"packageName\": \"klass.model.meta.domain\"\n"
                + "      },\n"
                + "      \"resultType\": {\n"
                + "        \"name\": \"ClassifierModifier\",\n"
                + "        \"packageName\": \"klass.model.meta.domain\"\n"
                + "      },\n"
                + "      \"owningAssociation\": {\n"
                + "        \"name\": \"ClassifierHasModifiers\"\n"
                + "      },\n"
                + "      \"associationEndModifiers\": []\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"enumerationProperties\",\n"
                + "      \"inferred\": false,\n"
                + "      \"ordinal\": 2,\n"
                + "      \"sourceCode\": \"enumerationProperties: EnumerationProperty[0..*]\\n        orderBy: this.ordinal;\",\n"
                + "      \"sourceCodeWithInference\": \"enumerationProperties: EnumerationProperty[0..*]\\n        orderBy: this.ordinal;\",\n"
                + "      \"direction\": \"target\",\n"
                + "      \"multiplicity\": \"0..*\",\n"
                + "      \"owningClass\": {\n"
                + "        \"name\": \"Classifier\",\n"
                + "        \"packageName\": \"klass.model.meta.domain\"\n"
                + "      },\n"
                + "      \"resultType\": {\n"
                + "        \"name\": \"EnumerationProperty\",\n"
                + "        \"packageName\": \"klass.model.meta.domain\"\n"
                + "      },\n"
                + "      \"owningAssociation\": {\n"
                + "        \"name\": \"ClassifierHasEnumerationProperties\"\n"
                + "      },\n"
                + "      \"associationEndModifiers\": []\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"primitiveProperties\",\n"
                + "      \"inferred\": false,\n"
                + "      \"ordinal\": 2,\n"
                + "      \"sourceCode\": \"primitiveProperties: PrimitiveProperty[0..*]\\n        orderBy: this.ordinal;\",\n"
                + "      \"sourceCodeWithInference\": \"primitiveProperties: PrimitiveProperty[0..*]\\n        orderBy: this.ordinal;\",\n"
                + "      \"direction\": \"target\",\n"
                + "      \"multiplicity\": \"0..*\",\n"
                + "      \"owningClass\": {\n"
                + "        \"name\": \"Classifier\",\n"
                + "        \"packageName\": \"klass.model.meta.domain\"\n"
                + "      },\n"
                + "      \"resultType\": {\n"
                + "        \"name\": \"PrimitiveProperty\",\n"
                + "        \"packageName\": \"klass.model.meta.domain\"\n"
                + "      },\n"
                + "      \"owningAssociation\": {\n"
                + "        \"name\": \"ClassifierHasPrimitiveProperties\"\n"
                + "      },\n"
                + "      \"associationEndModifiers\": []\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"superInterfaces\",\n"
                + "      \"inferred\": false,\n"
                + "      \"ordinal\": 2,\n"
                + "      \"sourceCode\": \"superInterfaces               : ClassifierInterfaceMapping[0..*] owned;\",\n"
                + "      \"sourceCodeWithInference\": \"superInterfaces               : ClassifierInterfaceMapping[0..*] owned;\",\n"
                + "      \"direction\": \"target\",\n"
                + "      \"multiplicity\": \"0..*\",\n"
                + "      \"owningClass\": {\n"
                + "        \"name\": \"Classifier\",\n"
                + "        \"packageName\": \"klass.model.meta.domain\"\n"
                + "      },\n"
                + "      \"resultType\": {\n"
                + "        \"name\": \"ClassifierInterfaceMapping\",\n"
                + "        \"packageName\": \"klass.model.meta.domain\"\n"
                + "      },\n"
                + "      \"owningAssociation\": {\n"
                + "        \"name\": \"ClassifierHasClassifierInterfaceMapping\"\n"
                + "      },\n"
                + "      \"associationEndModifiers\": [\n"
                + "        {\n"
                + "          \"name\": \"owned\",\n"
                + "          \"inferred\": false,\n"
                + "          \"ordinal\": 1\n"
                + "        }\n"
                + "      ]\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        JSONAssert.assertEquals(expected, jsonResponse, JSONCompareMode.STRICT);
    }

    @Test
    public void metaAssociation() throws JSONException
    {
        Client client = this.getClient(
                "klass.model.meta.domain.dropwizard.test.MetaResourceManualTest.metaAssociation");

        Response response = client.target(
                String.format(
                        "http://localhost:%d/api/meta/association/ClassifierHasClassifierInterfaceMapping",
                        RULE.getLocalPort()))
                .request()
                .get();

        this.assertResponseStatus(response, Status.OK);

        String jsonResponse = response.readEntity(String.class);
        //language=JSON
        String expected = "{\n"
                + "  \"name\": \"ClassifierHasClassifierInterfaceMapping\",\n"
                + "  \"inferred\": false,\n"
                + "  \"packageName\": \"klass.model.meta.domain\",\n"
                + "  \"ordinal\": 51,\n"
                + "  \"sourceCode\": \"association ClassifierHasClassifierInterfaceMapping\\n{\\n    subClassifier: Classifier[1..1];\\n    superInterfaces               : ClassifierInterfaceMapping[0..*] owned;\\n\\n    relationship this.name == ClassifierInterfaceMapping.classifierName\\n}\",\n"
                + "  \"sourceCodeWithInference\": \"association ClassifierHasClassifierInterfaceMapping\\n{\\n    subClassifier: Classifier[1..1];\\n    superInterfaces               : ClassifierInterfaceMapping[0..*] owned;\\n\\n    relationship this.name == ClassifierInterfaceMapping.classifierName\\n}\",\n"
                + "  \"associationEnds\": [\n"
                + "    {\n"
                + "      \"name\": \"subClassifier\",\n"
                + "      \"inferred\": false,\n"
                + "      \"ordinal\": 1,\n"
                + "      \"sourceCode\": \"subClassifier: Classifier[1..1];\",\n"
                + "      \"sourceCodeWithInference\": \"subClassifier: Classifier[1..1];\",\n"
                + "      \"direction\": \"source\",\n"
                + "      \"multiplicity\": \"1..1\",\n"
                + "      \"owningClass\": {\n"
                + "        \"name\": \"ClassifierInterfaceMapping\",\n"
                + "        \"packageName\": \"klass.model.meta.domain\"\n"
                + "      },\n"
                + "      \"resultType\": {\n"
                + "        \"name\": \"Classifier\",\n"
                + "        \"packageName\": \"klass.model.meta.domain\"\n"
                + "      },\n"
                + "      \"owningAssociation\": {\n"
                + "        \"name\": \"ClassifierHasClassifierInterfaceMapping\"\n"
                + "      },\n"
                + "      \"associationEndModifiers\": []\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"superInterfaces\",\n"
                + "      \"inferred\": false,\n"
                + "      \"ordinal\": 2,\n"
                + "      \"sourceCode\": \"superInterfaces               : ClassifierInterfaceMapping[0..*] owned;\",\n"
                + "      \"sourceCodeWithInference\": \"superInterfaces               : ClassifierInterfaceMapping[0..*] owned;\",\n"
                + "      \"direction\": \"target\",\n"
                + "      \"multiplicity\": \"0..*\",\n"
                + "      \"owningClass\": {\n"
                + "        \"name\": \"Classifier\",\n"
                + "        \"packageName\": \"klass.model.meta.domain\"\n"
                + "      },\n"
                + "      \"resultType\": {\n"
                + "        \"name\": \"ClassifierInterfaceMapping\",\n"
                + "        \"packageName\": \"klass.model.meta.domain\"\n"
                + "      },\n"
                + "      \"owningAssociation\": {\n"
                + "        \"name\": \"ClassifierHasClassifierInterfaceMapping\"\n"
                + "      },\n"
                + "      \"associationEndModifiers\": [\n"
                + "        {\n"
                + "          \"name\": \"owned\",\n"
                + "          \"inferred\": false,\n"
                + "          \"ordinal\": 1\n"
                + "        }\n"
                + "      ]\n"
                + "    }\n"
                + "  ],\n"
                + "  \"criteria\": {\n"
                + "    \"id\": 2\n"
                + "  }\n"
                + "}";
        JSONAssert.assertEquals(expected, jsonResponse, JSONCompareMode.STRICT);
    }

    public void assertResponseStatus(@Nonnull Response response, Status status)
    {
        response.bufferEntity();
        String entityAsString = response.readEntity(String.class);
        assertThat(entityAsString, response.getStatusInfo(), is(status));
    }
}

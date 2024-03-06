package cool.klass.generator.service.test;

import java.time.Instant;

import cool.klass.generator.service.ServiceResourceGenerator;
import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.KlassCompiler;
import cool.klass.model.converter.compiler.error.CompilerError;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.service.ServiceGroup;
import cool.klass.test.constants.KlassTestConstants;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ServiceResourceGeneratorTest
{
    @Test
    public void stackOverflow()
    {
        CompilationUnit compilationUnit = CompilationUnit.createFromText(
                "example.klass",
                KlassTestConstants.STACK_OVERFLOW_SOURCE_CODE_TEXT);
        CompilerState         compilerState  = new CompilerState(compilationUnit);
        KlassCompiler         klassCompiler  = new KlassCompiler(compilerState);
        DomainModel           domainModel    = klassCompiler.compile();
        ImmutableList<String> compilerErrors = compilerState.getCompilerErrors().collect(CompilerError::toString);
        assertThat(compilerErrors, is(Lists.immutable.empty()));
        assertThat(domainModel, notNullValue());

        Instant now = Instant.parse("2019-12-31T23:59:59.999Z");

        ServiceResourceGenerator serviceResourceGenerator = new ServiceResourceGenerator(
                domainModel,
                "StackOverflow",
                "com.stackoverflow",
                now);

        ServiceGroup serviceGroup           = domainModel.getServiceGroups().getOnly();
        String       serviceGroupSourceCode = serviceResourceGenerator.getServiceGroupSourceCode(serviceGroup);

        //<editor-fold desc="expected java code">
        //language=JAVA
        String expectedSourceCode = ""
                + "package com.stackoverflow.service.resource;\n"
                + "\n"
                + "import java.sql.Timestamp;\n"
                + "import java.util.List;\n"
                + "import java.util.Set;\n"
                + "\n"
                + "import javax.annotation.*;\n"
                + "import javax.ws.rs.*;\n"
                + "import javax.ws.rs.core.*;\n"
                + "import javax.ws.rs.core.Response.Status;\n"
                + "\n"
                + "import com.stackoverflow.*;\n"
                + "import com.stackoverflow.meta.constants.StackOverflowDomainModel;\n"
                + "import com.stackoverflow.json.view.*;\n"
                + "import com.codahale.metrics.annotation.*;\n"
                + "import com.fasterxml.jackson.annotation.JsonView;\n"
                + "import com.gs.fw.common.mithra.finder.*;\n"
                + "import cool.klass.data.store.*;\n"
                + "import org.eclipse.collections.impl.factory.primitive.LongSets;\n"
                + "import org.eclipse.collections.impl.set.mutable.*;\n"
                + "import org.eclipse.collections.impl.utility.*;\n"
                + "\n"
                + "/**\n"
                + " * Auto-generated by {@link cool.klass.generator.service.ServiceResourceGenerator}\n"
                + " */\n"
                + "@Generated(\n"
                + "        value = \"cool.klass.generator.service.ServiceResourceGenerator\",\n"
                + "        date = \"2019-12-31T23:59:59.999Z\")\n"
                + "@Path(\"/\")\n"
                + "public class QuestionResource\n"
                + "{\n"
                + "    private final DataStore dataStore;\n"
                + "\n"
                + "    public QuestionResource(DataStore dataStore)\n"
                + "    {\n"
                + "        this.dataStore = dataStore;\n"
                + "    }\n"
                + "\n"
                + "    @Timed\n"
                + "    @ExceptionMetered\n"
                + "    @GET\n"
                + "    @Path(\"/api/question/{id}\") // ?{version}\n"
                + "    @Produces(MediaType.APPLICATION_JSON)\n"
                + "    @JsonView(QuestionReadProjection_JsonView.class)\n"
                + "    public Question method0(\n"
                + "            @PathParam(\"id\") Long id,\n"
                + "            @Nullable @QueryParam(\"version\") Integer version)\n"
                + "    {\n"
                + "        // Question\n"
                + "\n"
                + "        // this.id == id\n"
                + "        Operation queryOperation     = QuestionFinder.id().eq(id);\n"
                + "        // this.system equalsEdgePoint\n"
                + "        //     && this.version.number == version\n"
                + "        Operation versionOperation   = version == null\n"
                + "                ? QuestionFinder.all()\n"
                + "                : QuestionFinder.system().equalsEdgePoint()\n"
                + "                .and(QuestionFinder.version().number().eq(version));\n"
                + "\n"
                + "        QuestionList result = QuestionFinder.findMany(queryOperation.and(versionOperation));\n"
                + "        // Deep fetch using projection QuestionReadProjection\n"
                + "        result.deepFetch(QuestionFinder.answers());\n"
                + "        result.deepFetch(QuestionFinder.version());\n"
                + "\n"
                + "        if (result.isEmpty())\n"
                + "        {\n"
                + "            throw new ClientErrorException(\"Url valid, data not found.\", Status.GONE);\n"
                + "        }\n"
                + "        return Iterate.getOnly(result);\n"
                + "    }\n"
                + "\n"
                + "    @Timed\n"
                + "    @ExceptionMetered\n"
                + "    @PUT\n"
                + "    @Path(\"/api/question/{id}\") // ?{version}\n"
                + "    @Produces(MediaType.APPLICATION_JSON)\n"
                + "    @JsonView(QuestionWriteProjection_JsonView.class)\n"
                + "    public Question method1(\n"
                + "            @PathParam(\"id\") Long id,\n"
                + "            @Nullable @QueryParam(\"version\") Integer version)\n"
                + "    {\n"
                + "        // Question\n"
                + "\n"
                + "        // this.id == id\n"
                + "        Operation queryOperation     = QuestionFinder.id().eq(id);\n"
                + "        // this.version.number == version\n"
                + "        Operation conflictOperation  = QuestionFinder.version().number().eq(version);\n"
                + "\n"
                + "        QuestionList result = QuestionFinder.findMany(queryOperation);\n"
                + "        // Deep fetch using projection QuestionWriteProjection\n"
                + "\n"
                + "        boolean hasConflict = !result.asEcList().allSatisfy(conflictOperation::matches);\n"
                + "        if (!hasConflict)\n"
                + "        {\n"
                + "            throw new ClientErrorException(Status.CONFLICT);\n"
                + "        }\n"
                + "        if (result.isEmpty())\n"
                + "        {\n"
                + "            throw new ClientErrorException(\"Url valid, data not found.\", Status.GONE);\n"
                + "        }\n"
                + "        return Iterate.getOnly(result);\n"
                + "    }\n"
                + "\n"
                + "    @Timed\n"
                + "    @ExceptionMetered\n"
                + "    @DELETE\n"
                + "    @Path(\"/api/question/{id}\") // ?{version}\n"
                + "    @Produces(MediaType.APPLICATION_JSON)\n"
                + "    @JsonView(QuestionWriteProjection_JsonView.class)\n"
                + "    public Question method2(\n"
                + "            @PathParam(\"id\") Long id,\n"
                + "            @Nullable @QueryParam(\"version\") Integer version,\n"
                + "            @Context SecurityContext securityContext)\n"
                + "    {\n"
                + "        // Question\n"
                + "\n"
                + "        String    userPrincipalName  = securityContext.getUserPrincipal().getName();\n"
                + "        // this.id == id\n"
                + "        Operation queryOperation     = QuestionFinder.id().eq(id);\n"
                + "        // this.createdById == user\n"
                + "        Operation authorizeOperation = QuestionFinder.createdById().eq(userPrincipalName);\n"
                + "        // this.version.number == version\n"
                + "        Operation conflictOperation  = QuestionFinder.version().number().eq(version);\n"
                + "\n"
                + "        QuestionList result = QuestionFinder.findMany(queryOperation);\n"
                + "        // Deep fetch using projection QuestionWriteProjection\n"
                + "\n"
                + "        boolean isAuthorized = !result.asEcList().allSatisfy(authorizeOperation::matches);\n"
                + "        if (!isAuthorized)\n"
                + "        {\n"
                + "            throw new ForbiddenException();\n"
                + "        }\n"
                + "        boolean hasConflict = !result.asEcList().allSatisfy(conflictOperation::matches);\n"
                + "        if (!hasConflict)\n"
                + "        {\n"
                + "            throw new ClientErrorException(Status.CONFLICT);\n"
                + "        }\n"
                + "        if (result.isEmpty())\n"
                + "        {\n"
                + "            throw new ClientErrorException(\"Url valid, data not found.\", Status.GONE);\n"
                + "        }\n"
                + "        return Iterate.getOnly(result);\n"
                + "    }\n"
                + "\n"
                + "    @Timed\n"
                + "    @ExceptionMetered\n"
                + "    @GET\n"
                + "    @Path(\"/api/question/in\") // ?{ids}\n"
                + "    @Produces(MediaType.APPLICATION_JSON)\n"
                + "    @JsonView(QuestionReadProjection_JsonView.class)\n"
                + "    public List<Question> method3(@QueryParam(\"ids\") Set<Long> ids)\n"
                + "    {\n"
                + "        // Question\n"
                + "\n"
                + "        // this.id in ids\n"
                + "        Operation queryOperation     = QuestionFinder.id().in(SetAdapter.adapt(ids).collectLong(x -> x, LongSets.mutable.empty()));\n"
                + "\n"
                + "        QuestionList result = QuestionFinder.findMany(queryOperation);\n"
                + "        // Deep fetch using projection QuestionReadProjection\n"
                + "        result.deepFetch(QuestionFinder.answers());\n"
                + "        result.deepFetch(QuestionFinder.version());\n"
                + "\n"
                + "        return result;\n"
                + "    }\n"
                + "\n"
                + "    @Timed\n"
                + "    @ExceptionMetered\n"
                + "    @GET\n"
                + "    @Path(\"/api/question/firstTwo\")\n"
                + "    @Produces(MediaType.APPLICATION_JSON)\n"
                + "    @JsonView(QuestionReadProjection_JsonView.class)\n"
                + "    public List<Question> method4()\n"
                + "    {\n"
                + "        // Question\n"
                + "\n"
                + "        // this.id in (1, 2)\n"
                + "        Operation queryOperation     = QuestionFinder.id().in(LongSets.immutable.with(1, 2));\n"
                + "\n"
                + "        QuestionList result = QuestionFinder.findMany(queryOperation);\n"
                + "        // Deep fetch using projection QuestionReadProjection\n"
                + "        result.deepFetch(QuestionFinder.answers());\n"
                + "        result.deepFetch(QuestionFinder.version());\n"
                + "\n"
                + "        return result;\n"
                + "    }\n"
                + "\n"
                + "\n"
                + "    @Timed\n"
                + "    @ExceptionMetered\n"
                + "    @GET\n"
                + "    @Path(\"/api/question\")\n"
                + "    @Produces(MediaType.APPLICATION_JSON)\n"
                + "    @JsonView(QuestionReadProjection_JsonView.class)\n"
                + "    public List<Question> method6()\n"
                + "    {\n"
                + "        // Question\n"
                + "\n"
                + "        // this.title startsWith \"Why do\"\n"
                + "        Operation queryOperation     = QuestionFinder.title().startsWith(\"Why do\");\n"
                + "\n"
                + "        QuestionList result = QuestionFinder.findMany(queryOperation);\n"
                + "        // Deep fetch using projection QuestionReadProjection\n"
                + "        result.deepFetch(QuestionFinder.answers());\n"
                + "        result.deepFetch(QuestionFinder.version());\n"
                + "\n"
                + "        return result;\n"
                + "    }\n"
                + "\n"
                + "    @Timed\n"
                + "    @ExceptionMetered\n"
                + "    @GET\n"
                + "    @Path(\"/api/user/{userId}/questions\")\n"
                + "    @Produces(MediaType.APPLICATION_JSON)\n"
                + "    @JsonView(QuestionWriteProjection_JsonView.class)\n"
                + "    public List<Question> method7(@PathParam(\"userId\") String userId)\n"
                + "    {\n"
                + "        // Question\n"
                + "\n"
                + "        // this.createdById == userId\n"
                + "        Operation queryOperation     = QuestionFinder.createdById().eq(userId);\n"
                + "\n"
                + "        QuestionList result = QuestionFinder.findMany(queryOperation);\n"
                + "        // Deep fetch using projection QuestionWriteProjection\n"
                + "\n"
                + "        result.setOrderBy(QuestionFinder.createdOn().ascendingOrderBy());\n"
                + "\n"
                + "        return result;\n"
                + "    }\n"
                + "}\n";
        //</editor-fold>

        assertThat(serviceGroupSourceCode, serviceGroupSourceCode, is(expectedSourceCode));
    }
}

package cool.klass.generator.service;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.DataType;
import cool.klass.model.meta.domain.DomainModel;
import cool.klass.model.meta.domain.Klass;
import cool.klass.model.meta.domain.criteria.Criteria;
import cool.klass.model.meta.domain.projection.BaseProjectionListener;
import cool.klass.model.meta.domain.projection.Projection;
import cool.klass.model.meta.domain.projection.ProjectionAssociationEnd;
import cool.klass.model.meta.domain.projection.ProjectionDataTypeProperty;
import cool.klass.model.meta.domain.projection.ProjectionWalker;
import cool.klass.model.meta.domain.service.Service;
import cool.klass.model.meta.domain.service.ServiceGroup;
import cool.klass.model.meta.domain.service.ServiceMultiplicity;
import cool.klass.model.meta.domain.service.ServiceProjectionDispatch;
import cool.klass.model.meta.domain.service.Verb;
import cool.klass.model.meta.domain.service.url.Url;
import cool.klass.model.meta.domain.service.url.UrlParameter;
import cool.klass.model.meta.domain.service.url.UrlPathParameter;
import cool.klass.model.meta.domain.service.url.UrlPathSegment;
import cool.klass.model.meta.domain.service.url.UrlQueryParameter;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Stacks;

public class ServiceResourceGenerator
{
    @Nonnull
    private final DomainModel domainModel;
    @Nonnull
    private final Instant     now;

    public ServiceResourceGenerator(@Nonnull DomainModel domainModel, @Nonnull Instant now)
    {
        this.domainModel = Objects.requireNonNull(domainModel);
        this.now = Objects.requireNonNull(now);
    }

    public void writeServiceResourceFiles(@Nonnull Path outputPath) throws IOException
    {
        for (ServiceGroup serviceGroup : this.domainModel.getServiceGroups())
        {
            // TODO: Instead of inferring a resource name, change the DSL to include it in the declaration, like this:
            // service QuestionResource on Question

            String packageRelativePath = serviceGroup
                    .getKlass()
                    .getPackageName()
                    .replaceAll("\\.", "/");
            Path serviceGroupDirectory = outputPath
                    .resolve(packageRelativePath)
                    .resolve("service")
                    .resolve("resource");
            serviceGroupDirectory.toFile().mkdirs();
            String fileName               = serviceGroup.getKlass().getName() + "Resource.java";
            Path   serviceGroupOutputPath = serviceGroupDirectory.resolve(fileName);

            this.printStringToFile(serviceGroupOutputPath, this.getServiceGroupSourceCode(serviceGroup));
        }
    }

    private void printStringToFile(@Nonnull Path path, String contents) throws FileNotFoundException
    {
        try (PrintStream printStream = new PrintStream(new FileOutputStream(path.toFile())))
        {
            printStream.print(contents);
        }
    }

    @Nonnull
    public String getServiceGroupSourceCode(@Nonnull ServiceGroup serviceGroup)
    {
        Klass  klass               = serviceGroup.getKlass();
        String packageName         = klass.getPackageName() + ".service.resource";
        String serviceResourceName = klass.getName() + "Resource";

        String serviceMethodsSourceCode = serviceGroup
                .getUrls()
                .flatCollect(Url::getServices)
                .collectWithIndex(this::getServiceSourceCode)
                .makeString("\n");

        //language=JAVA
        String sourceCode = ""
                + "package " + packageName + ";\n"
                + "\n"
                + "import java.util.List;\n"
                + "import java.util.Set;\n"
                + "\n"
                + "import javax.annotation.Generated;\n"
                + "import javax.ws.rs.*;\n"
                + "import javax.ws.rs.core.*;\n"
                + "import javax.ws.rs.core.Response.Status;\n"
                + "\n"
                + "import " + klass.getPackageName() + ".*;\n"
                + "import com.codahale.metrics.annotation.*;\n"
                + "import org.eclipse.collections.api.list.MutableList;\n"
                + "import com.gs.fw.common.mithra.MithraObject;\n"
                + "import com.gs.fw.common.mithra.finder.Operation;\n"
                + "import cool.klass.model.meta.domain.DomainModel;\n"
                + "import cool.klass.model.meta.domain.projection.Projection;\n"
                + "import cool.klass.serializer.json.ReladomoJsonTree;\n"
                + "import org.eclipse.collections.impl.factory.primitive.*;\n"
                + "import org.eclipse.collections.impl.set.mutable.SetAdapter;\n"
                + "import org.eclipse.collections.impl.utility.Iterate;\n"
                + "\n"
                + "/**\n"
                + " * Auto-generated by {@link cool.klass.generator.service.ServiceResourceGenerator}\n"
                + " */\n"
                + "@Generated(\n"
                + "        value = \"cool.klass.generator.service.ServiceResourceGenerator\",\n"
                + "        date = \"" + this.now + "\")\n"
                + "@Path(\"/\")\n"
                + "public class " + serviceResourceName + "\n"
                + "{\n"
                + "    private final DomainModel domainModel;\n"
                + "\n"
                + "    public " + serviceResourceName + "(DomainModel domainModel)\n"
                + "    {\n"
                + "        this.domainModel = domainModel;\n"
                + "    }\n"
                + "\n"
                + ""
                + serviceMethodsSourceCode
                + "\n"
                + "    private List<ReladomoJsonTree> applyProjection(\n"
                + "            MutableList<? extends MithraObject> mithraObjects,\n"
                + "            String projectionName)\n"
                + "    {\n"
                + "        Projection projection = this.domainModel.getProjectionByName(projectionName);\n"
                + "        return mithraObjects.<ReladomoJsonTree>collect(mithraObject -> new ReladomoJsonTree(\n"
                + "                mithraObject,\n"
                + "                projection.getChildren()));\n"
                + "    }\n"
                + "}\n";
        return sourceCode;
    }

    @Nonnull
    private String getServiceSourceCode(Service service, int index)
    {
        Url  url  = service.getUrl();
        Verb verb = service.getVerb();

        if (verb == Verb.POST)
        {
            // TODO: POST
            return "";
        }

        ServiceMultiplicity       serviceMultiplicity = service.getServiceMultiplicity();
        ServiceProjectionDispatch projectionDispatch  = service.getProjectionDispatch();

        ServiceGroup                     serviceGroup    = url.getServiceGroup();
        ImmutableList<UrlPathSegment>    urlPathSegments = url.getUrlPathSegments();
        ImmutableList<UrlQueryParameter> queryParameters = url.getQueryParameters();
        ImmutableList<UrlParameter>      urlParameters   = url.getUrlParameters();

        Klass  klass           = serviceGroup.getKlass();
        String klassName       = klass.getName();
        String returnType      = this.getReturnType(serviceMultiplicity);
        String projectionName  = projectionDispatch.getProjection().getName();
        String returnStatement = this.getReturnStatement(serviceMultiplicity, projectionName);

        String urlPathString = urlPathSegments.makeString("/", "/", "");
        String queryParametersString = queryParameters.isEmpty()
                ? ""
                : queryParameters.makeString(" // ?", "&", "");

        boolean hasAuthorizeCriteria = service.isAuthorizeClauseRequired();

        int     numParameters      = service.getNumParameters();
        boolean lineWrapParameters = numParameters > 1;

        String parameterPrefix = lineWrapParameters ? "\n" : "";
        String parameterIndent = lineWrapParameters ? "            " : "";

        ImmutableList<String> parameterStrings1 = urlParameters
                .collectWith(this::getParameterSourceCode, parameterIndent);
        // TODO: This version query parameter should be inferred during the compilation of the service
        ImmutableList<String> parameterStrings = hasAuthorizeCriteria
                ? parameterStrings1.newWith(parameterIndent + "@Context SecurityContext securityContext")
                : parameterStrings1;

        String userPrincipalNameLocalVariable = hasAuthorizeCriteria
                ? "        String    userPrincipalName  = securityContext.getUserPrincipal().getName();\n"
                : "";

        String parametersSourceCode = parameterStrings.makeString(",\n");

        String finderName                   = klassName + "Finder";
        String queryOperationSourceCode     = this.getOperation(finderName, service.getQueryCriteria(), "query");
        String authorizeOperationSourceCode = this.getOperation(finderName, service.getAuthorizeCriteria(), "authorize");
        String validateOperationSourceCode  = this.getOperation(finderName, service.getValidateCriteria(), "validate");
        String conflictOperationSourceCode  = this.getOperation(finderName, service.getConflictCriteria(), "conflict");
        String versionOperationSourceCode   = this.getOptionalOperation(finderName, service.getVersionCriteria(), "version");

        String authorizePredicateSourceCode = this.checkPredicate(service.getAuthorizeCriteria(), "authorize", "isAuthorized", "ForbiddenException()");
        String validatePredicateSourceCode  = this.checkPredicate(service.getValidateCriteria(), "validate", "isValidated", "BadRequestException()");
        String conflictPredicateSourceCode  = this.checkPredicate(service.getConflictCriteria(), "conflict", "hasConflict", "ClientErrorException(Status.CONFLICT)");

        String executeOperationSourceCode = this.getExecuteOperationSourceCode(
                service.getQueryCriteria(),
                service.getVersionCriteria(),
                klassName);

        Projection                  projection                  = projectionDispatch.getProjection();
        // TODO: Fix deep fetching redundant stuff
        DeepFetchProjectionListener deepFetchProjectionListener = new DeepFetchProjectionListener();
        ProjectionWalker.walk(projection, deepFetchProjectionListener);
        ImmutableList<String> deepFetchStrings = deepFetchProjectionListener.getResult();
        String deepFetchString = deepFetchStrings
                .collect(each -> "        result.deepFetch(" + finderName + "." + each + ");\n")
                .makeString("");

        //language=JAVA
        String sourceCode = ""
                + "    @Timed\n"
                + "    @ExceptionMetered\n"
                + "    @" + verb.name() + "\n"
                + "    @Path(\"" + urlPathString + "\")" + queryParametersString + "\n"
                + "    @Produces(MediaType.APPLICATION_JSON)\n"
                + "    public " + returnType + " method" + index + "(" + parameterPrefix + parametersSourceCode + ")\n"
                + "    {\n"
                + "        // " + klassName + "\n"
                + "\n"
                + userPrincipalNameLocalVariable
                + queryOperationSourceCode
                + authorizeOperationSourceCode
                + validateOperationSourceCode
                + conflictOperationSourceCode
                + versionOperationSourceCode
                + "\n"
                + executeOperationSourceCode
                + "        // Deep fetch using projection " + projectionName + "\n"
                + deepFetchString
                + "\n"
                + authorizePredicateSourceCode
                + validatePredicateSourceCode
                + conflictPredicateSourceCode
                + returnStatement
                + "    }\n";

        return sourceCode;
    }

    @Nonnull
    private String getReturnType(ServiceMultiplicity serviceMultiplicity)
    {
        boolean uniqueResult = serviceMultiplicity == ServiceMultiplicity.ONE;
        return uniqueResult
                ? "ReladomoJsonTree"
                : "List<ReladomoJsonTree>";
    }

    @Nonnull
    private String getReturnStatement(
            ServiceMultiplicity serviceMultiplicity,
            String projectionName)
    {
        boolean uniqueResult = serviceMultiplicity == ServiceMultiplicity.ONE;

        if (uniqueResult)
        {
            return ""
                    + "        if (result.isEmpty())\n"
                    + "        {\n"
                    + "            throw new ClientErrorException(\"Url valid, data not found.\", Status.GONE);\n"
                    + "        }\n"
                    + "        MithraObject mithraObject = Iterate.getOnly(result);\n"
                    + "\n"
                    + "        Projection projection = this.domainModel.getProjectionByName(\""
                    + projectionName
                    + "\");\n"
                    + "        return new ReladomoJsonTree(mithraObject, projection.getChildren());\n";
        }

        //language=JAVA
        return "        return this.applyProjection(result.asEcList(), \"" + projectionName + "\");\n";
    }

    @Nonnull
    private String getOperation(
            String finderName,
            Optional<Criteria> optionalCriteria,
            String criteriaName)
    {
        return optionalCriteria
                .map(criteria -> this.getOperation(finderName, criteria, criteriaName))
                .orElse("");
    }

    @Nonnull
    private String getOptionalOperation(
            String finderName,
            Optional<Criteria> optionalCriteria,
            String criteriaName)
    {
        return optionalCriteria
                .map(criteria -> this.getOptionalOperation(finderName, criteria, criteriaName))
                .orElse("");
    }

    private String checkPredicate(
            Optional<Criteria> optionalCriteria,
            String criteriaName,
            String flagName,
            String exceptionName)
    {
        return optionalCriteria
                .map(criteria -> this.checkPredicate(criteriaName, flagName, exceptionName))
                .orElse("");
    }

    private String getExecuteOperationSourceCode(
            Optional<Criteria> queryCriteria,
            @Nonnull Optional<Criteria> versionCriteria,
            String klassName)
    {
        if (!queryCriteria.isPresent())
        {
            return "";
        }

        String versionClause = versionCriteria.isPresent() ? ".and(versionOperation)" : "";
        return MessageFormat.format(
                "        {0}List result = {0}Finder.findMany(queryOperation{1});\n",
                klassName,
                versionClause);
    }

    @Nonnull
    private String getOperation(String finderName, @Nonnull Criteria criteria, String criteriaName)
    {
        String operation           = this.getOperation(finderName, criteria);
        String comment             = this.getComment(criteria);
        String paddedOperationName = String.format("%-18s", criteriaName + "Operation");
        return comment +
                "        Operation " + paddedOperationName + " = " + operation + ";\n";
    }

    @Nonnull
    private String getOptionalOperation(String finderName, @Nonnull Criteria criteria, String criteriaName)
    {
        String operation           = this.getOperation(finderName, criteria);
        String comment             = this.getComment(criteria);
        String paddedOperationName = String.format("%-18s", criteriaName + "Operation");

        return comment
                + "        Operation " + paddedOperationName + " = " + criteriaName + " == null\n"
                + "                ? " + finderName + ".all()\n"
                + "                : " + operation + ";\n";
    }

    private String checkPredicate(String criteriaName, String flagName, String exceptionName)
    {
        return ""
                + "        boolean " + flagName + " = !result.asEcList().allSatisfy(" + criteriaName + "Operation::matches);\n"
                + "        if (!" + flagName + ")\n"
                + "        {\n"
                + "            throw new " + exceptionName + ";\n"
                + "        }\n";
    }

    @Nonnull
    private String getOperation(String finderName, Criteria criteria)
    {
        StringBuilder stringBuilder = new StringBuilder();
        criteria.visit(new OperationCriteriaVisitor(finderName, stringBuilder));
        return stringBuilder.toString();
    }

    @Nonnull
    private String getComment(@Nonnull Criteria criteria)
    {
        return "        // " + criteria.getSourceCode().replaceAll("\\s+", " ") + "\n";
    }

    private String getParameterSourceCode(@Nonnull UrlParameter urlParameter, String indent)
    {
        // TODO: Hibernate validation annotations

        DataType parameterType = urlParameter.getType();
        String typeString = urlParameter.getMultiplicity().isToMany()
                ? "Set<" + parameterType.getName() + ">"
                : parameterType.getName();

        return String.format(
                "%s@%s(\"%s\") %s %s",
                indent,
                this.getAnnotation(urlParameter),
                urlParameter.getName(),
                typeString,
                urlParameter.getName());
    }

    @Nonnull
    private String getAnnotation(UrlParameter urlParameter)
    {
        if (urlParameter instanceof UrlPathParameter)
        {
            return "PathParam";
        }
        if (urlParameter instanceof UrlQueryParameter)
        {
            return "QueryParam";
        }
        throw new AssertionError();
    }

    private static final class DeepFetchProjectionListener extends BaseProjectionListener
    {
        private final MutableStack<String> stack  = Stacks.mutable.empty();
        private final MutableList<String>  result = Lists.mutable.empty();

        @Override
        public void enterProjectionAssociationEnd(ProjectionAssociationEnd projectionAssociationEnd)
        {
            this.stack.push(projectionAssociationEnd.getAssociationEnd().getName());
        }

        @Override
        public void exitProjectionAssociationEnd(ProjectionAssociationEnd projectionAssociationEnd)
        {
            this.stack.pop();
        }

        @Override
        public void exitProjectionDataTypeProperty(ProjectionDataTypeProperty projectionDataTypeProperty)
        {
            if (this.stack.isEmpty())
            {
                return;
            }
            String string = this.stack
                    .toList()
                    .asReversed()
                    .collect(each -> each + "()")
                    .makeString(".");
            this.result.add(string);
        }

        public ImmutableList<String> getResult()
        {
            return this.result.distinct().toImmutable();
        }
    }
}

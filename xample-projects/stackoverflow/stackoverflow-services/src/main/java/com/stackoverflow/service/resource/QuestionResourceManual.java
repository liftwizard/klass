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

package com.stackoverflow.service.resource;

import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gs.fw.common.mithra.MithraManagerProvider;
import com.gs.fw.common.mithra.finder.Operation;
import com.stackoverflow.Question;
import com.stackoverflow.QuestionFinder;
import com.stackoverflow.QuestionList;
import cool.klass.data.store.DataStore;
import cool.klass.deserializer.json.OperationMode;
import cool.klass.deserializer.json.RequiredPropertiesValidator;
import cool.klass.deserializer.json.type.ObjectNodeTypeCheckingValidator;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.projection.Projection;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.reladomo.persistent.writer.IncomingCreateDataModelValidator;
import cool.klass.reladomo.persistent.writer.IncomingUpdateDataModelValidator;
import cool.klass.reladomo.persistent.writer.MutationContext;
import cool.klass.reladomo.persistent.writer.PersistentCreator;
import cool.klass.reladomo.persistent.writer.PersistentReplacer;
import cool.klass.serialization.jackson.response.KlassResponseBuilder;
import io.dropwizard.auth.Auth;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.eclipse.collections.impl.set.mutable.SetAdapter;
import org.eclipse.collections.impl.utility.Iterate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/manual")
public class QuestionResourceManual {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionResourceManual.class);

    @Nonnull
    private final DomainModel domainModel;

    @Nonnull
    private final DataStore dataStore;

    @Nonnull
    private final Clock clock;

    public QuestionResourceManual(
        @Nonnull DomainModel domainModel,
        @Nonnull DataStore dataStore,
        @Nonnull Clock clock
    ) {
        this.domainModel = Objects.requireNonNull(domainModel);
        this.dataStore = Objects.requireNonNull(dataStore);
        this.clock = Objects.requireNonNull(clock);
    }

    @Nonnull
    @Timed
    @ExceptionMetered
    @GET
    @Path("/question/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response method0(@PathParam("id") Long id, @Nullable @QueryParam("version") Integer version) {
        // Question

        // this.id == id
        Operation queryOperation = QuestionFinder.id().eq(id);
        // this.system equalsEdgePoint && this.version.number == version
        Operation versionOperation = version == null
            ? QuestionFinder.all()
            : QuestionFinder.system().equalsEdgePoint().and(QuestionFinder.version().number().eq(version));

        Operation operation = queryOperation.and(versionOperation);
        QuestionList result = QuestionFinder.findMany(operation);
        // Deep fetch using projection QuestionReadProjection
        result.deepFetch(QuestionFinder.answers());
        result.deepFetch(QuestionFinder.tags().tag());
        result.deepFetch(QuestionFinder.version());

        if (result.isEmpty()) {
            throw new ClientErrorException("Url valid, data not found.", Status.GONE);
        }

        Projection projection = this.domainModel.getProjectionByName("QuestionReadProjection");
        // Use a fixed UTC instant to avoid timezone conversion issues
        Instant transactionInstant = Instant.parse("9999-12-01T23:59:00Z");
        Object persistentInstance = Iterate.getOnly(result);

        var responseBuilder = new KlassResponseBuilder(
            persistentInstance,
            projection,
            Multiplicity.ONE_TO_ONE,
            transactionInstant
        ).setCriteria(operation.toString());

        return Response.ok().entity(responseBuilder.build()).build();
    }

    @Timed
    @ExceptionMetered
    @PUT
    @Path("/question/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response method1(
        @PathParam("id") Long id,
        @Nonnull @QueryParam("version") Optional<Integer> optionalVersion,
        @Nonnull @NotNull ObjectNode incomingInstance,
        @Nonnull @Auth Principal principal
    ) {
        Klass klass = this.domainModel.getClassByName("Question");

        MutableList<String> errors = Lists.mutable.empty();
        MutableList<String> warnings = Lists.mutable.empty();
        ObjectNodeTypeCheckingValidator.validate(errors, incomingInstance, klass);
        RequiredPropertiesValidator.validate(errors, warnings, klass, incomingInstance, OperationMode.REPLACE);

        if (errors.notEmpty()) {
            Response response = Response.status(Status.BAD_REQUEST).entity(errors).build();
            throw new BadRequestException("Incoming data failed validation.", response);
        }

        // this.id == id
        Operation queryOperation = QuestionFinder.id().eq(id);
        QuestionList result = QuestionFinder.findMany(queryOperation);
        result.deepFetch(QuestionFinder.tags().tag());
        result.deepFetch(QuestionFinder.version());

        if (result.isEmpty()) {
            throw new ClientErrorException("Url valid, data not found.", Status.GONE);
        }

        // this.version.number == version
        Operation conflictOperation = QuestionFinder.version().number().eq(optionalVersion.get());
        boolean hasConflict = !result.asEcList().allSatisfy(conflictOperation::matches);
        if (hasConflict) {
            throw new ClientErrorException(Status.CONFLICT);
        }

        if (result.size() > 1) {
            throw new InternalServerErrorException("TODO");
        }
        Object persistentInstance = result.get(0);

        String userPrincipalName = principal.getName();
        Optional<String> userId = Optional.of(userPrincipalName);
        Instant transactionInstant = Instant.now(this.clock);

        DataTypeProperty idProperty = (DataTypeProperty) klass.getPropertyByName("id").get();
        ImmutableMap<DataTypeProperty, Object> propertyDataFromUrl = Maps.immutable.with(idProperty, id);

        MutationContext mutationContext = new MutationContext(userId, transactionInstant, propertyDataFromUrl);

        Klass userKlass = this.domainModel.getUserClass().get();
        IncomingUpdateDataModelValidator.validate(
            this.dataStore,
            userKlass,
            klass,
            mutationContext,
            persistentInstance,
            incomingInstance,
            errors,
            warnings
        );
        if (errors.notEmpty()) {
            Response response = Response.status(Status.BAD_REQUEST).entity(errors).build();
            throw new BadRequestException("Incoming data failed validation.", response);
        }

        PersistentReplacer replacer = new PersistentReplacer(mutationContext, this.dataStore);
        replacer.synchronize(klass, persistentInstance, incomingInstance);

        Projection projection = this.domainModel.getProjectionByName("QuestionReadProjection");

        var responseBuilder = new KlassResponseBuilder(
            persistentInstance,
            projection,
            Multiplicity.ONE_TO_ONE,
            transactionInstant
        ).setCriteria(queryOperation.toString());

        return Response.ok().entity(responseBuilder.build()).build();
    }

    @Nonnull
    @Timed
    @ExceptionMetered
    @DELETE
    @Path("/question/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response method2(
        @PathParam("id") Long id,
        @QueryParam("version") Integer version,
        @Nonnull @Context SecurityContext securityContext
    ) {
        // Question

        String userPrincipalName = securityContext.getUserPrincipal().getName();
        // this.id == id
        Operation queryOperation = QuestionFinder.id().eq(id);
        // this.createdById == user
        Operation authorizeOperation = QuestionFinder.createdById().eq(userPrincipalName);
        // this.version.number == version
        Operation conflictOperation = QuestionFinder.version().number().eq(version);

        QuestionList result = QuestionFinder.findMany(queryOperation);
        // Deep fetch using projection QuestionWriteProjection

        boolean isAuthorized = !result.asEcList().allSatisfy(authorizeOperation::matches);
        if (!isAuthorized) {
            throw new ForbiddenException();
        }
        boolean hasConflict = !result.asEcList().allSatisfy(conflictOperation::matches);
        if (!hasConflict) {
            throw new ClientErrorException(Status.CONFLICT);
        }
        if (result.isEmpty()) {
            throw new ClientErrorException("Url valid, data not found.", Status.GONE);
        }

        Question persistentInstance = Iterate.getOnly(result);

        Projection projection = this.domainModel.getProjectionByName("QuestionReadProjection");
        // Use a fixed UTC instant to avoid timezone conversion issues
        Instant transactionInstant = Instant.parse("9999-12-01T23:59:00Z");

        var responseBuilder = new KlassResponseBuilder(
            persistentInstance,
            projection,
            Multiplicity.ONE_TO_ONE,
            transactionInstant
        ).setCriteria(queryOperation.toString());

        return Response.ok().entity(responseBuilder.build()).build();
    }

    @Nonnull
    @Timed
    @ExceptionMetered
    @GET
    @Path("/question/in")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getQuestionsById(@Nonnull @QueryParam("ids") Set<Long> ids) {
        // Question

        // this.id in ids
        Operation queryOperation = QuestionFinder.id()
            .in(SetAdapter.adapt(ids).collectLong(x -> x, LongSets.mutable.empty()));

        QuestionList result = QuestionFinder.findMany(queryOperation);
        // Deep fetch using projection QuestionReadProjection
        result.deepFetch(QuestionFinder.answers());
        result.deepFetch(QuestionFinder.version());

        Projection projection = this.domainModel.getProjectionByName("QuestionReadProjection");
        // Use a fixed UTC instant to avoid timezone conversion issues
        Instant transactionInstant = Instant.parse("9999-12-01T23:59:00Z");

        var responseBuilder = new KlassResponseBuilder(
            result,
            projection,
            Multiplicity.ONE_TO_ONE,
            transactionInstant
        ).setCriteria(queryOperation.toString());

        return Response.ok().entity(responseBuilder.build()).build();
    }

    @Nonnull
    @Timed
    @ExceptionMetered
    @GET
    @Path("/question/firstTwo")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFirstTwoQuestions() {
        // Question

        // this.id in (1, 2)
        Operation queryOperation = QuestionFinder.id().in(LongSets.immutable.with(1, 2));

        QuestionList result = QuestionFinder.findMany(queryOperation);
        // Deep fetch using projection QuestionReadProjection
        result.deepFetch(QuestionFinder.answers());
        result.deepFetch(QuestionFinder.version());

        Projection projection = this.domainModel.getProjectionByName("QuestionReadProjection");
        // Use a fixed UTC instant to avoid timezone conversion issues
        Instant transactionInstant = Instant.parse("9999-12-01T23:59:00Z");

        var responseBuilder = new KlassResponseBuilder(
            result,
            projection,
            Multiplicity.ONE_TO_ONE,
            transactionInstant
        ).setCriteria(queryOperation.toString());

        return Response.ok().entity(responseBuilder.build()).build();
    }

    @Nonnull
    @Timed
    @ExceptionMetered
    @GET
    @Path("/question/{id}/version/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getQuestionByIdAndVersion(@PathParam("id") Long id, @PathParam("version") Integer version) {
        Operation queryOperation = QuestionFinder.id()
            .eq(id)
            .and(QuestionFinder.system().equalsEdgePoint())
            .and(QuestionFinder.version().number().eq(version));
        /*
        Operation authorizeOperation = QuestionFinder.all();
        Operation validateOperation  = QuestionFinder.all();
        Operation conflictOperation  = QuestionFinder.all();
        */

        QuestionList result = QuestionFinder.findMany(queryOperation);
        result.deepFetch(QuestionFinder.answers());
        result.deepFetch(QuestionFinder.version());
        // TODO: Deep fetch using projection QuestionWriteProjection

        /*
        boolean isAuthorized = !result.asEcList().allSatisfy(authorizeOperation::matches);
        boolean isValidated  = !result.asEcList().allSatisfy(validateOperation::matches);
        boolean hasConflict  = !result.asEcList().allSatisfy(conflictOperation::matches);
        */
        if (result.isEmpty()) {
            throw new ClientErrorException("Url valid, data not found.", Status.GONE);
        }

        Projection projection = this.domainModel.getProjectionByName("QuestionReadProjection");
        // Use a fixed UTC instant to avoid timezone conversion issues
        Instant transactionInstant = Instant.parse("9999-12-01T23:59:00Z");

        var responseBuilder = new KlassResponseBuilder(
            Iterate.getOnly(result),
            projection,
            Multiplicity.ONE_TO_ONE,
            transactionInstant
        ).setCriteria(queryOperation.toString());

        return Response.ok().entity(responseBuilder.build()).build();
    }

    @Nonnull
    @Timed
    @ExceptionMetered
    @DELETE
    @Path("/question/{id}?{version}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteQuestionById(
        @PathParam("id") Long id,
        @QueryParam("version") Integer version,
        @Nonnull @Context SecurityContext securityContext
    ) {
        /*
        String    userPrincipalName  = securityContext.getUserPrincipal().getName();
        */
        Operation queryOperation = QuestionFinder.id().eq(id);
        /*
        Operation authorizeOperation = QuestionFinder.createdById().eq(userPrincipalName);
        Operation validateOperation  = QuestionFinder.id().eq(QuestionVersionFinder.id()).and(QuestionVersionFinder.number().eq(version));
        Operation conflictOperation  = QuestionFinder.all();
        */

        QuestionList result = QuestionFinder.findMany(queryOperation);
        // TODO: Deep fetch using projection QuestionWriteProjection

        /*
        boolean isAuthorized = !result.asEcList().allSatisfy(authorizeOperation::matches);
        boolean isValidated  = !result.asEcList().allSatisfy(validateOperation::matches);
        boolean hasConflict  = !result.asEcList().allSatisfy(conflictOperation::matches);
        */
        if (result.isEmpty()) {
            throw new ClientErrorException("Url valid, data not found.", Status.GONE);
        }

        Projection projection = this.domainModel.getProjectionByName("QuestionReadProjection");
        // Use a fixed UTC instant to avoid timezone conversion issues
        Instant transactionInstant = Instant.parse("9999-12-01T23:59:00Z");

        var responseBuilder = new KlassResponseBuilder(
            Iterate.getOnly(result),
            projection,
            Multiplicity.ONE_TO_ONE,
            transactionInstant
        ).setCriteria(queryOperation.toString());

        return Response.ok().entity(responseBuilder.build()).build();
    }

    @Timed
    @ExceptionMetered
    @POST
    @Path("/question")
    @Produces(MediaType.APPLICATION_JSON)
    public Response method5(
        @Nonnull ObjectNode incomingInstance,
        @Nonnull @Context UriInfo uriInfo,
        @Nonnull @Auth Principal principal
    ) {
        Klass klass = this.domainModel.getClassByName("Question");

        MutableList<String> errors = Lists.mutable.empty();
        MutableList<String> warnings = Lists.mutable.empty();
        ObjectNodeTypeCheckingValidator.validate(errors, incomingInstance, klass);
        RequiredPropertiesValidator.validate(errors, warnings, klass, incomingInstance, OperationMode.CREATE);
        if (errors.notEmpty()) {
            Response response = Response.status(Status.BAD_REQUEST).entity(errors).build();
            throw new BadRequestException("Incoming data failed validation.", response);
        }

        String userPrincipalName = principal.getName();
        Optional<String> userId = Optional.of(userPrincipalName);

        Instant transactionInstant = this.clock.instant();
        MutationContext mutationContext = new MutationContext(userId, transactionInstant, Maps.immutable.empty());

        Klass userKlass = this.domainModel.getUserClass().get();
        IncomingCreateDataModelValidator.validate(
            this.dataStore,
            userKlass,
            klass,
            mutationContext,
            incomingInstance,
            errors,
            warnings
        );
        if (errors.notEmpty()) {
            Response response = Response.status(Status.BAD_REQUEST).entity(errors).build();
            throw new BadRequestException("Incoming data failed validation.", response);
        }
        if (warnings.notEmpty()) {
            LOGGER.info("warnings = {}", warnings.makeString("\n", "\n", "\n"));
            warnings.clear();
        }

        Question persistentInstance = MithraManagerProvider.getMithraManager()
            .executeTransactionalCommand(tx -> {
                tx.setProcessingStartTime(transactionInstant.toEpochMilli());

                Question question = new Question();
                question.generateAndSetId();

                PersistentCreator creator = new PersistentCreator(mutationContext, this.dataStore);
                creator.synchronize(klass, question, incomingInstance);
                question.insert();
                return question;
            });

        UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
        uriBuilder.path(Long.toString(persistentInstance.getId()));

        Projection projection = this.domainModel.getProjectionByName("QuestionReadProjection");

        var responseBuilder = new KlassResponseBuilder(
            persistentInstance,
            projection,
            Multiplicity.ONE_TO_ONE,
            transactionInstant
        ).setCriteria(QuestionFinder.id().eq(persistentInstance.getId()).toString());

        return Response.created(uriBuilder.build()).entity(responseBuilder.build()).build();
    }

    @Timed
    @ExceptionMetered
    @GET
    @Path("/question")
    @Produces(MediaType.APPLICATION_JSON)
    public Response method6() {
        // Question

        // this.title startsWith "Why do"
        Operation queryOperation = QuestionFinder.title().startsWith("Why do");

        QuestionList result = QuestionFinder.findMany(queryOperation);
        // Deep fetch using projection QuestionReadProjection
        result.deepFetch(QuestionFinder.answers());
        result.deepFetch(QuestionFinder.version());

        Projection projection = this.domainModel.getProjectionByName("QuestionReadProjection");
        // Use a fixed UTC instant to avoid timezone conversion issues
        Instant transactionInstant = Instant.parse("9999-12-01T23:59:00Z");

        var responseBuilder = new KlassResponseBuilder(
            result,
            projection,
            Multiplicity.ZERO_TO_MANY,
            transactionInstant
        ).setCriteria(queryOperation.toString());

        return Response.ok().entity(responseBuilder.build()).build();
    }

    @Timed
    @ExceptionMetered
    @GET
    @Path("/user/{userId}/questions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response method7(@PathParam("userId") String userId) {
        // Question

        // this.createdById == userId
        Operation queryOperation = QuestionFinder.createdById().eq(userId);

        QuestionList result = QuestionFinder.findMany(queryOperation);

        Projection projection = this.domainModel.getProjectionByName("QuestionReadProjection");
        // Use a fixed UTC instant to avoid timezone conversion issues
        Instant transactionInstant = Instant.parse("9999-12-01T23:59:00Z");

        var responseBuilder = new KlassResponseBuilder(
            result,
            projection,
            Multiplicity.ZERO_TO_MANY,
            transactionInstant
        ).setCriteria(queryOperation.toString());

        return Response.ok().entity(responseBuilder.build()).build();
    }

    @Timed
    @ExceptionMetered
    @GET
    @Path("/set")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> setService() {
        return Sets.mutable.empty();
    }

    @Timed
    @ExceptionMetered
    @GET
    @Path("/map")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Set<String>> mapService() {
        return Maps.mutable.empty();
    }
}

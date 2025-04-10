package com.stackoverflow.service.resource;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

import com.stackoverflow.*;
import com.stackoverflow.json.view.*;
import com.codahale.metrics.annotation.*;
import com.fasterxml.jackson.annotation.JsonView;
import com.gs.fw.common.mithra.finder.*;
import cool.klass.data.store.*;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.Multiplicity;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cool.klass.deserializer.json.*;
import cool.klass.deserializer.json.type.*;
import cool.klass.reladomo.persistent.writer.*;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.eclipse.collections.impl.set.mutable.*;
import org.eclipse.collections.impl.utility.*;

/**
 * Auto-generated by {@link cool.klass.generator.service.ServiceResourceGenerator}
 */
@Path("/")
public class QuestionResource
{
    @Nonnull
    private final DomainModel domainModel;
    @Nonnull
    private final DataStore   dataStore;
    @Nonnull
    private final Clock       clock;

    public QuestionResource(
            @Nonnull DomainModel domainModel,
            @Nonnull DataStore dataStore,
            @Nonnull Clock clock)
    {
        this.domainModel = Objects.requireNonNull(domainModel);
        this.dataStore   = Objects.requireNonNull(dataStore);
        this.clock       = Objects.requireNonNull(clock);
    }

    @Timed
    @ExceptionMetered
    @GET
    @Path("/question/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @JsonView(QuestionReadProjection_JsonView.class)
    public Question method0(
            @PathParam("id") Long id)
    {
        // Question

        Operation queryOperation     = QuestionFinder.id().eq(id);

        QuestionList result = QuestionFinder.findMany(queryOperation);
        // Deep fetch using projection QuestionReadProjection
        result.deepFetch(QuestionFinder.answers());
        result.deepFetch(QuestionFinder.tags());
        result.deepFetch(QuestionFinder.version());
        result.deepFetch(QuestionFinder.createdBy());
        result.deepFetch(QuestionFinder.lastUpdatedBy());

        if (result.isEmpty())
        {
            throw new ClientErrorException("Url valid, data not found.", Status.GONE);
        }
        return Iterate.getOnly(result);
    }

    @Timed
    @ExceptionMetered
    @PUT
    @Path("/question/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public void method1(
            @PathParam("id") Long id,
            @Nonnull @NotNull ObjectNode incomingInstance)
    {
        Klass klass = this.domainModel.getClassByName("Question");

        MutableList<String> errors = Lists.mutable.empty();
        MutableList<String> warnings = Lists.mutable.empty();
        ObjectNodeTypeCheckingValidator.validate(errors, incomingInstance, klass);
        RequiredPropertiesValidator.validate(
                errors,
                warnings,
                klass,
                incomingInstance,
                OperationMode.REPLACE);

        if (errors.notEmpty())
        {
            Response response = Response
                    .status(Status.BAD_REQUEST)
                    .entity(errors)
                    .build();
            throw new BadRequestException("Incoming data failed validation.", response);
        }

        if (warnings.notEmpty())
        {
            Response response = Response
                    .status(Status.BAD_REQUEST)
                    .entity(warnings)
                    .build();
            throw new BadRequestException("Incoming data failed validation.", response);
        }

        Operation queryOperation     = QuestionFinder.id().eq(id);

        QuestionList result = QuestionFinder.findMany(queryOperation);
        result.deepFetch(QuestionFinder.tags());
        result.deepFetch(QuestionFinder.version());

        if (result.isEmpty())
        {
            throw new ClientErrorException("Url valid, data not found.", Status.GONE);
        }


        if (result.size() > 1)
        {
            throw new InternalServerErrorException("TODO");
        }
        Object persistentInstance = result.get(0);

        Instant            transactionInstant = Instant.now(this.clock);
        MutationContext    mutationContext    = new MutationContext(Optional.empty(), transactionInstant, Maps.immutable.empty());
        Klass              userKlass          = this.domainModel.getUserClass().get();
        IncomingUpdateDataModelValidator.validate(
                this.dataStore,
                userKlass,
                klass,
                mutationContext,
                persistentInstance,
                incomingInstance,
                errors,
                warnings);
        if (errors.notEmpty())
        {
            Response response = Response
                    .status(Status.BAD_REQUEST)
                    .entity(errors)
                    .build();
            throw new BadRequestException("Incoming data failed validation.", response);
        }
        if (warnings.notEmpty())
        {
            Response response = Response
                    .status(Status.BAD_REQUEST)
                    .entity(warnings)
                    .build();
            throw new BadRequestException("Incoming data failed validation.", response);
        }

        PersistentReplacer replacer           = new PersistentReplacer(mutationContext, this.dataStore);
        replacer.synchronize(klass, persistentInstance, incomingInstance);
    }

    @Timed
    @ExceptionMetered
    @DELETE
    @Path("/question/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public void method2(
            @PathParam("id") Long id,
            @Context SecurityContext securityContext)
    {
        Klass klass = this.domainModel.getClassByName("Question");
        String    userPrincipalName  = securityContext.getUserPrincipal().getName();
        Operation queryOperation     = QuestionFinder.id().eq(id);
        Operation authorizeOperation = QuestionFinder.createdById().eq(userPrincipalName);

        QuestionList result = QuestionFinder.findMany(queryOperation);
        result.deepFetch(QuestionFinder.tags());
        result.deepFetch(QuestionFinder.version());

        if (result.isEmpty())
        {
            throw new ClientErrorException("Url valid, data not found.", Status.GONE);
        }

        boolean isAuthorized = !result.asEcList().allSatisfy(authorizeOperation::matches);
        if (!isAuthorized)
        {
            throw new ForbiddenException();
        }

        if (result.size() > 1)
        {
            throw new InternalServerErrorException("TODO");
        }

        Object persistentInstance = result.get(0);

        // TODO: Create a mutation context with now and the principal
        Instant           transactionInstant = Instant.now(this.clock);
        MutationContext   mutationContext    = new MutationContext(Optional.empty(), transactionInstant, Maps.immutable.empty());
        PersistentDeleter deleter            = new PersistentDeleter(mutationContext, this.dataStore);
        deleter.deleteOrTerminate(klass, persistentInstance);
    }

    @Timed
    @ExceptionMetered
    @GET
    @Path("/question/in") // ?{ids}
    @Produces(MediaType.APPLICATION_JSON)
    @JsonView(QuestionReadProjection_JsonView.class)
    public List<Question> method3(@QueryParam("ids") Set<Long> ids)
    {
        // Question

        Operation queryOperation     = QuestionFinder.id().in(SetAdapter.adapt(ids).collectLong(x -> x, LongSets.mutable.empty()));

        QuestionList result = QuestionFinder.findMany(queryOperation);
        // Deep fetch using projection QuestionReadProjection
        result.deepFetch(QuestionFinder.answers());
        result.deepFetch(QuestionFinder.tags());
        result.deepFetch(QuestionFinder.version());
        result.deepFetch(QuestionFinder.createdBy());
        result.deepFetch(QuestionFinder.lastUpdatedBy());

        return result;
    }

    // TODO: POST

    @Timed
    @ExceptionMetered
    @GET
    @Path("/user/{userId}/questions")
    @Produces(MediaType.APPLICATION_JSON)
    @JsonView(QuestionReadProjection_JsonView.class)
    public List<Question> method5(@PathParam("userId") String userId)
    {
        // Question

        Operation queryOperation     = QuestionFinder.createdById().eq(userId);

        QuestionList result = QuestionFinder.findMany(queryOperation);
        // Deep fetch using projection QuestionReadProjection
        result.deepFetch(QuestionFinder.answers());
        result.deepFetch(QuestionFinder.tags());
        result.deepFetch(QuestionFinder.version());
        result.deepFetch(QuestionFinder.createdBy());
        result.deepFetch(QuestionFinder.lastUpdatedBy());

        result.setOrderBy(QuestionFinder.createdOn().ascendingOrderBy());

        return result;
    }
}

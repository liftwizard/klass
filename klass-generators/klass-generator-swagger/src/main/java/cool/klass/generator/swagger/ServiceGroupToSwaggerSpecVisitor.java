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

package cool.klass.generator.swagger;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Association;
import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.DataType;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Enumeration;
import cool.klass.model.meta.domain.api.Interface;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.api.TopLevelElementVisitor;
import cool.klass.model.meta.domain.api.parameter.Parameter;
import cool.klass.model.meta.domain.api.projection.Projection;
import cool.klass.model.meta.domain.api.projection.ProjectionChild;
import cool.klass.model.meta.domain.api.projection.ProjectionDataTypeProperty;
import cool.klass.model.meta.domain.api.projection.ProjectionElement;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.service.Service;
import cool.klass.model.meta.domain.api.service.ServiceGroup;
import cool.klass.model.meta.domain.api.service.ServiceMultiplicity;
import cool.klass.model.meta.domain.api.service.Verb;
import cool.klass.model.meta.domain.api.service.url.Url;
import io.swagger.models.ArrayModel;
import io.swagger.models.HttpMethod;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.DateProperty;
import io.swagger.models.properties.DateTimeProperty;
import io.swagger.models.properties.DoubleProperty;
import io.swagger.models.properties.FloatProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.LongProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;

public class ServiceGroupToSwaggerSpecVisitor implements TopLevelElementVisitor {

    private final Swagger swagger;
    private final DomainModel domainModel;

    public ServiceGroupToSwaggerSpecVisitor(@Nonnull Swagger swagger, @Nonnull DomainModel domainModel) {
        this.swagger = swagger;
        this.domainModel = domainModel;
    }

    @Override
    public void visitEnumeration(Enumeration enumeration) {
        Model enumModel = new ModelImpl()
            .type("string")
            ._enum(enumeration.getEnumerationLiterals()
                .collect(literal -> literal.getName())
                .castToList());
        this.swagger.addDefinition(enumeration.getName(), enumModel);
    }

    @Override
    public void visitInterface(Interface anInterface) {
        // Interfaces don't directly generate Swagger models
    }

    @Override
    public void visitKlass(Klass klass) {
        // Classes are represented through projections, not directly
    }

    @Override
    public void visitAssociation(Association association) {
        // Associations don't generate Swagger elements
    }

    @Override
    public void visitProjection(Projection projection) {
        // Generate a schema definition for this projection
        Model model = this.createProjectionModel(projection);
        this.swagger.addDefinition(projection.getName(), model);
    }

    @Override
    public void visitServiceGroup(ServiceGroup serviceGroup) {
        // Process all URLs in this service group
        for (Url url : serviceGroup.getUrls()) {
            this.processUrl(url);
        }
    }

    private void processUrl(Url url) {
        String pathString = this.convertUrlToSwaggerPath(url);
        Path path = this.swagger.getPath(pathString);
        if (path == null) {
            path = new Path();
            this.swagger.path(pathString, path);
        }

        // Process all services (HTTP methods) for this URL
        for (Service service : url.getServices()) {
            this.processService(path, service);
        }
    }

    private void processService(Path path, Service service) {
        Operation operation = new Operation();

        // Set operation summary and description
        Klass klass = service.getUrl().getServiceGroup().getKlass();
        String verbName = service.getVerb().name();
        String klassName = klass.getName();

        operation.summary(this.generateOperationSummary(service));
        operation.description(this.generateOperationDescription(service));
        operation.operationId(this.generateOperationId(service));

        // Add path parameters
        for (Parameter parameter : service.getUrl().getPathParameters()) {
            PathParameter pathParam = new PathParameter();
            pathParam.setName(parameter.getName());
            pathParam.setRequired(parameter.getMultiplicity().isRequired());
            pathParam.setType(this.getSwaggerType(parameter.getType()));
            pathParam.setFormat(this.getSwaggerFormat(parameter.getType()));
            operation.addParameter(pathParam);
        }

        // Add query parameters
        for (Parameter parameter : service.getUrl().getQueryParameters()) {
            QueryParameter queryParam = new QueryParameter();
            queryParam.setName(parameter.getName());
            queryParam.setRequired(parameter.getMultiplicity().isRequired());
            queryParam.setType(this.getSwaggerType(parameter.getType()));
            queryParam.setFormat(this.getSwaggerFormat(parameter.getType()));
            operation.addParameter(queryParam);
        }

        // Add request body for POST, PUT, PATCH
        if (service.getVerb() == Verb.POST || service.getVerb() == Verb.PUT || service.getVerb() == Verb.PATCH) {
            BodyParameter bodyParam = new BodyParameter();
            bodyParam.setName("body");
            bodyParam.setRequired(true);

            // Use the class as the request model
            RefModel refModel = new RefModel(klassName);
            bodyParam.setSchema(refModel);
            operation.addParameter(bodyParam);
        }

        // Add responses
        this.addResponses(operation, service);

        // Add the operation to the path
        HttpMethod httpMethod = this.convertVerbToHttpMethod(service.getVerb());
        path.set(httpMethod.name().toLowerCase(), operation);
    }

    private String generateOperationSummary(Service service) {
        Klass klass = service.getUrl().getServiceGroup().getKlass();
        String verbName = service.getVerb().name();
        boolean isMany = service.getServiceMultiplicity() == ServiceMultiplicity.MANY;

        return verbName + " " + (isMany ? klass.getName() + " list" : klass.getName());
    }

    private String generateOperationDescription(Service service) {
        StringBuilder desc = new StringBuilder();
        desc.append(this.generateOperationSummary(service));

        if (service.getAuthorizeCriteria().isPresent()) {
            desc.append("\n\nRequires authorization.");
        }

        return desc.toString();
    }

    private String generateOperationId(Service service) {
        Klass klass = service.getUrl().getServiceGroup().getKlass();
        String verbName = service.getVerb().name().toLowerCase();
        String klassName = klass.getName();
        boolean isMany = service.getServiceMultiplicity() == ServiceMultiplicity.MANY;

        // Generate unique operation ID like: getUserById, getUserList, createUser, etc.
        if (service.getVerb() == Verb.GET) {
            if (isMany) {
                return verbName + klassName + "List";
            }
            return verbName + klassName + "ById";
        }

        return verbName + klassName;
    }

    private void addResponses(Operation operation, Service service) {
        Response successResponse = new Response();

        // Determine response model from projection
        if (service.getProjectionDispatch().isPresent()) {
            Projection projection = service.getProjectionDispatch().get().getProjection();

            if (service.getServiceMultiplicity() == ServiceMultiplicity.MANY) {
                // Array response
                ArrayModel arrayModel = new ArrayModel();
                arrayModel.setItems(new RefProperty(projection.getName()));
                successResponse.setResponseSchema(arrayModel);
                successResponse.setDescription("Success - returns list of " + projection.getName());
            } else {
                // Single object response
                RefModel refModel = new RefModel(projection.getName());
                successResponse.setResponseSchema(refModel);
                successResponse.setDescription("Success - returns " + projection.getName());
            }
        } else {
            successResponse.setDescription("Success");
        }

        // Add success response
        if (service.getVerb() == Verb.POST) {
            operation.addResponse("201", successResponse);
        } else {
            operation.addResponse("200", successResponse);
        }

        // Add error responses
        operation.addResponse("400", new Response().description("Bad Request"));

        if (service.getAuthorizeCriteria().isPresent()) {
            operation.addResponse("403", new Response().description("Forbidden"));
        }

        operation.addResponse("404", new Response().description("Not Found"));

        if (service.getConflictCriteria().isPresent()) {
            operation.addResponse("409", new Response().description("Conflict"));
        }

        operation.addResponse("500", new Response().description("Internal Server Error"));
    }

    private String convertUrlToSwaggerPath(Url url) {
        StringBuilder pathBuilder = new StringBuilder();

        for (cool.klass.model.meta.domain.api.Element segment : url.getUrlPathSegments()) {
            pathBuilder.append("/");

            // Check if this segment is a parameter
            String segmentString = segment.toString();
            if (segmentString.contains(":")) {
                // Extract parameter name from format like "{userId: Long[1..1]}"
                String paramName = segmentString.substring(1, segmentString.indexOf(":")).trim();
                pathBuilder.append("{").append(paramName).append("}");
            } else {
                // Static path segment
                pathBuilder.append(segmentString);
            }
        }

        return pathBuilder.toString();
    }

    private HttpMethod convertVerbToHttpMethod(Verb verb) {
        return switch (verb) {
            case GET -> HttpMethod.GET;
            case POST -> HttpMethod.POST;
            case PUT -> HttpMethod.PUT;
            case PATCH -> HttpMethod.PATCH;
            case DELETE -> HttpMethod.DELETE;
        };
    }

    private Model createProjectionModel(Projection projection) {
        ModelImpl model = new ModelImpl();
        model.setType("object");

        Map<String, Property> properties = new LinkedHashMap<>();

        // Process all children of the projection
        for (ProjectionChild child : projection.getChildren()) {
            if (child instanceof ProjectionDataTypeProperty dataTypeProperty) {
                DataTypeProperty property = dataTypeProperty.getProperty();
                Property swaggerProperty = this.createPropertyFromDataType(property.getType());

                // Add description if available
                if (swaggerProperty != null) {
                    swaggerProperty.setDescription(property.getName());
                    properties.put(property.getName(), swaggerProperty);

                    // Mark as required if needed
                    if (property.isRequired()) {
                        model.addRequired(property.getName());
                    }
                }
            } else {
                // Handle reference properties (nested objects)
                Property refProperty = new RefProperty(child.getName());
                properties.put(child.getName(), refProperty);
            }
        }

        model.setProperties(properties);
        return model;
    }

    private Property createPropertyFromDataType(DataType dataType) {
        if (dataType instanceof PrimitiveType primitiveType) {
            return switch (primitiveType) {
                case INTEGER -> new IntegerProperty();
                case LONG -> new LongProperty();
                case DOUBLE -> new DoubleProperty();
                case FLOAT -> new FloatProperty();
                case BOOLEAN -> new BooleanProperty();
                case STRING -> new StringProperty();
                case INSTANT, TEMPORAL_INSTANT -> new DateTimeProperty();
                case LOCAL_DATE -> new DateProperty();
                case TEMPORAL_RANGE -> new StringProperty(); // Temporal range as string for now
            };
        } else if (dataType instanceof Enumeration enumeration) {
            StringProperty stringProperty = new StringProperty();
            stringProperty._enum(enumeration.getEnumerationLiterals()
                .collect(literal -> literal.getName())
                .castToList());
            return stringProperty;
        } else if (dataType instanceof Classifier classifier) {
            return new RefProperty(classifier.getName());
        }

        return new StringProperty(); // Default fallback
    }

    private String getSwaggerType(DataType dataType) {
        if (dataType instanceof PrimitiveType primitiveType) {
            return switch (primitiveType) {
                case INTEGER, LONG -> "integer";
                case DOUBLE, FLOAT -> "number";
                case BOOLEAN -> "boolean";
                case STRING, TEMPORAL_RANGE -> "string";
                case INSTANT, TEMPORAL_INSTANT, LOCAL_DATE -> "string";
            };
        }
        return "string"; // Default for enums and other types
    }

    private String getSwaggerFormat(DataType dataType) {
        if (dataType instanceof PrimitiveType primitiveType) {
            return switch (primitiveType) {
                case INTEGER -> "int32";
                case LONG -> "int64";
                case DOUBLE -> "double";
                case FLOAT -> "float";
                case INSTANT, TEMPORAL_INSTANT -> "date-time";
                case LOCAL_DATE -> "date";
                default -> null;
            };
        }
        return null;
    }
}

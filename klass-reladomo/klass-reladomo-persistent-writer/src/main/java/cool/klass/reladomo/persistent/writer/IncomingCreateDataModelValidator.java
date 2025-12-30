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

package cool.klass.reladomo.persistent.writer;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cool.klass.data.store.DataStore;
import cool.klass.deserializer.json.AssertValuesMatchPropertyVisitor;
import cool.klass.deserializer.json.JsonDataTypeValueVisitor;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.PropertyVisitor;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.OrderedMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.MapAdapter;

public class IncomingCreateDataModelValidator {

	@Nonnull
	protected final DataStore dataStore;

	@Nonnull
	protected final Klass userKlass;

	@Nonnull
	protected final Klass klass;

	@Nonnull
	protected final MutationContext mutationContext;

	@Nonnull
	protected final ObjectNode objectNode;

	@Nonnull
	protected final MutableList<String> errors;

	@Nonnull
	protected final MutableList<String> warnings;

	@Nonnull
	protected final MutableStack<String> contextStack;

	@Nonnull
	protected final Optional<AssociationEnd> pathHere;

	protected final boolean isRoot;

	public IncomingCreateDataModelValidator(
		@Nonnull DataStore dataStore,
		@Nonnull Klass userKlass,
		@Nonnull Klass klass,
		@Nonnull MutationContext mutationContext,
		@Nonnull ObjectNode objectNode,
		@Nonnull MutableList<String> errors,
		@Nonnull MutableList<String> warnings,
		@Nonnull MutableStack<String> contextStack,
		@Nonnull Optional<AssociationEnd> pathHere,
		boolean isRoot
	) {
		this.dataStore = Objects.requireNonNull(dataStore);
		this.userKlass = Objects.requireNonNull(userKlass);
		this.klass = Objects.requireNonNull(klass);
		this.mutationContext = Objects.requireNonNull(mutationContext);
		this.objectNode = Objects.requireNonNull(objectNode);
		this.errors = Objects.requireNonNull(errors);
		this.warnings = Objects.requireNonNull(warnings);
		this.contextStack = Objects.requireNonNull(contextStack);
		this.pathHere = Objects.requireNonNull(pathHere);
		this.isRoot = isRoot;
	}

	public static void validate(
		@Nonnull DataStore dataStore,
		@Nonnull Klass userKlass,
		@Nonnull Klass klass,
		@Nonnull MutationContext mutationContext,
		@Nonnull ObjectNode objectNode,
		@Nonnull MutableList<String> errors,
		@Nonnull MutableList<String> warnings
	) {
		IncomingCreateDataModelValidator validator = new IncomingCreateDataModelValidator(
			dataStore,
			userKlass,
			klass,
			mutationContext,
			objectNode,
			errors,
			warnings,
			Stacks.mutable.empty(),
			Optional.empty(),
			true
		);
		validator.validate();
	}

	public void validate() {
		if (this.isRoot) {
			this.contextStack.push(this.klass.getName());
		}
		try {
			this.handleDataTypePropertiesInsideProjection();
			this.handleAssociationEnds();
		} finally {
			if (this.isRoot) {
				this.contextStack.pop();
			}
		}
	}

	// region DataTypeProperties
	private void handleDataTypePropertiesInsideProjection() {
		ImmutableList<DataTypeProperty> dataTypeProperties = this.klass.getDataTypeProperties();
		for (DataTypeProperty dataTypeProperty : dataTypeProperties) {
			this.handleDataTypePropertyInsideProjection(dataTypeProperty);
		}
	}

	private void handleDataTypePropertyInsideProjection(@Nonnull DataTypeProperty dataTypeProperty) {
		if (dataTypeProperty.isID()) {
			// TODO: inside the projection, only check for matching, and only in update mode
			// this.handleIdProperty(dataTypeProperty);
			this.handleKeyProperty(dataTypeProperty);
		} else if (dataTypeProperty.isKey()) {
			if (this.klass == this.userKlass) {
				this.handleAuditProperty(dataTypeProperty);
			} else {
				this.handleKeyProperty(dataTypeProperty);
			}
		} else if (dataTypeProperty.isTemporal()) {
			return;
		}

		if (dataTypeProperty.isCreatedBy() || dataTypeProperty.isLastUpdatedBy()) {
			if (dataTypeProperty.isForeignKey()) {
				return;
			}
			this.handleAuditProperty(dataTypeProperty);
		} else if (dataTypeProperty.isCreatedOn()) {
			// Ignored
		} else if (dataTypeProperty.isVersion()) {
			this.handleVersionProperty(dataTypeProperty);
		}
	}

	private void handleKeyProperty(@Nonnull DataTypeProperty dataTypeProperty) {
		this.contextStack.push(dataTypeProperty.getName());

		try {
			JsonNode jsonDataTypeValue = this.objectNode.path(dataTypeProperty.getName());
			if (jsonDataTypeValue.isMissingNode()) {
				return;
			}

			ImmutableMap<DataTypeProperty, Object> propertyDataFromUrl = this.mutationContext.getPropertyDataFromUrl();
			if (!propertyDataFromUrl.containsKey(dataTypeProperty)) {
				return;
			}

			Object propertyData = propertyDataFromUrl.get(dataTypeProperty);

			PropertyVisitor visitor = new AssertValuesMatchPropertyVisitor(
				jsonDataTypeValue,
				propertyData,
				"url parameter key",
				this.contextStack,
				"Error",
				this.errors
			);
			dataTypeProperty.visit(visitor);
		} finally {
			this.contextStack.pop();
		}
	}

	private void handleAuditProperty(@Nonnull DataTypeProperty dataTypeProperty) {
		this.contextStack.push(dataTypeProperty.getName());

		try {
			JsonNode jsonDataTypeValue = this.objectNode.path(dataTypeProperty.getName());
			if (jsonDataTypeValue.isMissingNode() || !jsonDataTypeValue.isTextual()) {
				return;
			}

			Optional<String> maybeUserId = this.mutationContext.getUserId();
			if (maybeUserId.isEmpty()) {
				return;
			}

			if (maybeUserId.get().equals(jsonDataTypeValue.asText())) {
				return;
			}

			String warning =
				"Warning at %s. Expected audit property '%s' to match current user '%s' but got '%s'.".formatted(
					this.getContextString(),
					dataTypeProperty.getName(),
					maybeUserId.get(),
					jsonDataTypeValue.asText()
				);
			this.warnings.add(warning);
		} finally {
			this.contextStack.pop();
		}
	}

	private void handleVersionProperty(@Nonnull DataTypeProperty dataTypeProperty) {
		this.contextStack.push(dataTypeProperty.getName());

		try {
			JsonNode jsonDataTypeValue = this.objectNode.path(dataTypeProperty.getName());
			if (jsonDataTypeValue.isMissingNode()) {
				return;
			}

			if (!jsonDataTypeValue.isIntegralNumber()) {
				return;
			}

			if (jsonDataTypeValue.asInt() == 1) {
				return;
			}

			String error = "Error at %s. Expected version property '%s' to be 1 but got %s.".formatted(
				this.getContextString(),
				dataTypeProperty.getName(),
				jsonDataTypeValue.asText()
			);
			this.errors.add(error);
		} finally {
			this.contextStack.pop();
		}
	}

	public String getContextString() {
		return this.contextStack.toList().asReversed().makeString(".");
	}

	// endregion

	// region AssociationEnds
	private void handleAssociationEnds() {
		for (AssociationEnd associationEnd : this.klass.getAssociationEnds()) {
			this.handleAssociationEnd(associationEnd);
		}
	}

	private void handleAssociationEnd(@Nonnull AssociationEnd associationEnd) {
		if (this.isBackward(associationEnd)) {
			return;
		}

		if (associationEnd.isVersion()) {
			this.handleVersionAssociationEnd(associationEnd);
			return;
		}
		if (associationEnd.isCreatedBy() || associationEnd.isLastUpdatedBy()) {
			this.handleAuditByAssociationEnd(associationEnd);
			return;
		}

		if (associationEnd.isOwned()) {
			this.handleOwnedAssociationEnd(associationEnd);
		} else {
			this.handleOutsideProjectionReferenceProperty(associationEnd);
		}
	}

	private void handleVersionAssociationEnd(AssociationEnd associationEnd) {
		JsonNode childJsonNode = this.objectNode.path(associationEnd.getName());

		if (childJsonNode.isMissingNode() || childJsonNode.isNull() || !childJsonNode.isObject()) {
			return;
		}

		String associationEndName = associationEnd.getName();
		this.contextStack.push(associationEndName);

		/*
        MapIterable<DataTypeProperty, Object> keys = this.getKeysFromJsonNode(
                childJsonNode,
                associationEnd,
                this.objectNode);
        */

		try {
			ObjectNode childObjectNode = (ObjectNode) childJsonNode;
			IncomingCreateDataModelValidator validator = new IncomingCreateDataModelValidator(
				this.dataStore,
				this.userKlass,
				associationEnd.getType(),
				this.mutationContext,
				childObjectNode,
				this.errors,
				this.warnings,
				this.contextStack,
				Optional.of(associationEnd),
				false
			);
			validator.validate();
		} finally {
			this.contextStack.pop();
		}
	}

	private void handleAuditByAssociationEnd(@Nonnull AssociationEnd associationEnd) {
		JsonNode jsonNode = this.objectNode.path(associationEnd.getName());
		if (jsonNode.isMissingNode() || jsonNode.isNull()) {
			return;
		}

		String associationEndName = associationEnd.getName();
		this.contextStack.push(associationEndName);

		Optional<String> userId = this.mutationContext.getUserId();
		if (userId.isEmpty()) {
			throw new AssertionError("Expected user ID to be present in mutation context.");
		}

		DataTypeProperty userIdProperty = this.userKlass.getKeyProperties().getOnly();
		ImmutableMap<DataTypeProperty, Object> userKeys = Maps.immutable.with(userIdProperty, userId.get());
		Object userPersistentInstance = this.dataStore.findByKey(this.userKlass, userKeys);

		if (userPersistentInstance == null) {
			String error = String.format(
				"Error at %s. Couldn't find user with key %s.",
				this.getContextString(),
				userKeys
			);
			this.errors.add(error);
			return;
		}

		try {
			// TODO: Support a IncomingLastUpdatedByDataModelValidator which allows the current user to be substituted in for lastUpdatedBy.
			IncomingCreateDataModelValidator validator = new IncomingCreateDataModelValidator(
				this.dataStore,
				this.userKlass,
				associationEnd.getType(),
				this.mutationContext,
				(ObjectNode) jsonNode,
				this.errors,
				this.warnings,
				this.contextStack,
				Optional.of(associationEnd),
				false
			);
			validator.validate();
		} finally {
			this.contextStack.pop();
		}
	}

	private void handleOutsideProjectionReferenceProperty(@Nonnull AssociationEnd associationEnd) {
		Multiplicity multiplicity = associationEnd.getMultiplicity();
		JsonNode childJsonNode = this.objectNode.path(associationEnd.getName());

		if (multiplicity.isToOne()) {
			this.handleToOneOutsideProjection(associationEnd, childJsonNode, this.objectNode);
		} else {
			this.handleToManyOutsideProjection(associationEnd, childJsonNode, this.objectNode);
		}
	}

	private void handleOwnedAssociationEnd(@Nonnull AssociationEnd associationEnd) {
		Multiplicity multiplicity = associationEnd.getMultiplicity();

		if (multiplicity.isToOne()) {
			this.handleOwnedToOne(associationEnd);
		} else {
			this.handleOwnedToMany(associationEnd);
		}
	}

	private boolean isBackward(@Nonnull AssociationEnd associationEnd) {
		return this.pathHere.equals(Optional.of(associationEnd.getOpposite()));
	}

	protected void handleToOneOutsideProjection(
		@Nonnull AssociationEnd associationEnd,
		@Nonnull JsonNode childJsonNode,
		@Nonnull ObjectNode parentJsonNode
	) {
		if (associationEnd.isOwned()) {
			throw new AssertionError(
				"Assumption is that all owned association ends are inside projection, all unowned are outside projection"
			);
		}

		if (childJsonNode.isMissingNode() || childJsonNode.isNull()) {
			return;
		}

		if (!associationEnd.hasRealKeys()) {
			return;
		}

		String associationEndName = associationEnd.getName();
		this.contextStack.push(associationEndName);

		try {
			if ((childJsonNode.isMissingNode() || childJsonNode.isNull()) && associationEnd.isRequired()) {
				// TODO: Check for non-private foreign key properties
				// TODO: Can we remove this?
				return;
			}

			if (childJsonNode.isMissingNode() && !associationEnd.isRequired()) {
				// TODO: Can we remove this?
				return;
			}

			MapIterable<DataTypeProperty, Object> keys = this.getKeysFromJsonNode(
				childJsonNode,
				associationEnd,
				parentJsonNode
			);
			if (keys.contains(null)) {
				return;
			}

			Object childPersistentInstanceWithKey = this.findExistingChildPersistentInstance(associationEnd, keys);
			if (childPersistentInstanceWithKey == null) {
				String keysString = keys
					.keyValuesView()
					.collect((keyValue) -> keyValue.getOne().getName() + ": " + keyValue.getTwo())
					.makeString("{", ", ", "}");

				String error = String.format(
					"Error at '%s'. Could not find existing persistent instance for association end '%s' with key %s.",
					this.getContextString(),
					associationEnd,
					keysString
				);

				this.errors.add(error);
			} else {
				// validate that present properties match, if they are temporal, version, created on, created by
				ObjectNode childObjectNode = (ObjectNode) childJsonNode;
				// TODO: Add isInProject == false
				IncomingCreateDataModelValidator validator = new IncomingCreateDataModelValidator(
					this.dataStore,
					this.userKlass,
					associationEnd.getType(),
					this.mutationContext,
					childObjectNode,
					this.errors,
					this.warnings,
					this.contextStack,
					Optional.of(associationEnd),
					false
				);
				validator.validate();
			}
		} finally {
			this.contextStack.pop();
		}
	}

	protected Object findExistingChildPersistentInstance(
		@Nonnull AssociationEnd associationEnd,
		MapIterable<DataTypeProperty, Object> keys
	) {
		/*
        if (!(this instanceof PersistentCreator) && !(this instanceof PersistentReplacer))
        {
            throw new AssertionError();
        }
        */

		return this.dataStore.findByKey(associationEnd.getType(), keys);
	}

	private void handleToManyOutsideProjection(
		@Nonnull AssociationEnd associationEnd,
		@Nonnull JsonNode childrenJsonNodes,
		@Nonnull JsonNode parentJsonNode
	) {
		for (JsonNode childJsonNode : childrenJsonNodes) {
			this.getKeysFromJsonNode(childJsonNode, associationEnd, parentJsonNode);

			throw new AssertionError(associationEnd);
		}
	}

	public void handleOwnedToOne(@Nonnull AssociationEnd associationEnd) {
		JsonNode childJsonNode = this.objectNode.path(associationEnd.getName());

		String associationEndName = associationEnd.getName();
		this.contextStack.push(associationEndName);

		/*
        MapIterable<DataTypeProperty, Object> keys = this.getKeysFromJsonNode(
                childJsonNode,
                associationEnd,
                this.objectNode);
        */

		try {
			if (!(childJsonNode instanceof ObjectNode)) {
				String error = String.format(
					"Error at '%s'. Expected JSON object for owned association end '%s' but got %s.",
					this.getContextString(),
					associationEnd,
					childJsonNode.getNodeType().toString().toLowerCase(Locale.ROOT)
				);
				this.errors.add(error);
			}
			ObjectNode childObjectNode = (ObjectNode) childJsonNode;
			IncomingCreateDataModelValidator validator = new IncomingCreateDataModelValidator(
				this.dataStore,
				this.userKlass,
				associationEnd.getType(),
				this.mutationContext,
				childObjectNode,
				this.errors,
				this.warnings,
				this.contextStack,
				Optional.of(associationEnd),
				false
			);
			validator.validate();
		} finally {
			this.contextStack.pop();
		}
	}

	public void handleOwnedToMany(@Nonnull AssociationEnd associationEnd) {
		JsonNode incomingChildInstances = this.objectNode.path(associationEnd.getName());

		// TODO: Figure out how to recurse without checking key
		/*
        ImmutableList<JsonNode> incomingInstancesForInsert = this.filterIncomingInstancesForInsert(
                incomingChildInstances,
                associationEnd);

        ImmutableList<JsonNode> incomingInstancesForUpdate = this.filterIncomingInstancesForUpdate(
                incomingChildInstances,
                associationEnd);

        MapIterable<ImmutableList<Object>, JsonNode> incomingChildInstancesByKey = this.indexIncomingJsonInstances(
                incomingInstances,
                associationEnd,
                this.objectNode);
        */

		for (int index = 0; index < incomingChildInstances.size(); index++) {
			String contextString = String.format("%s[%d]", associationEnd.getName(), index);

			this.contextStack.push(contextString);

			try {
				JsonNode childJsonNode = incomingChildInstances.path(index);

				/*
                if (this.jsonNodeNeedsIdInferredOnInsert(childJsonNode, associationEnd))
                {
                    continue;
                }
                */

				ObjectNode childObjectNode = (ObjectNode) childJsonNode;
				IncomingCreateDataModelValidator validator = new IncomingCreateDataModelValidator(
					this.dataStore,
					this.userKlass,
					associationEnd.getType(),
					this.mutationContext,
					childObjectNode,
					this.errors,
					this.warnings,
					this.contextStack,
					Optional.of(associationEnd),
					false
				);
				validator.validate();
				/*
                if (childPersistentInstance == null)
                {
                    // TODO: Implement CreateUpdateDataModelValidator and recurse in create mode
                    return;
                }
                if (childPersistentInstance != null && childJsonNode instanceof ObjectNode)
                {
                    this.synchronizeAssociationEnd(
                            associationEnd,
                            (ObjectNode) childJsonNode,
                            (ObjectNode) childPersistentInstance);
                }
                */
			} finally {
				this.contextStack.pop();
			}
		}
	}

	private ImmutableList<JsonNode> filterIncomingInstancesForUpdate(
		JsonNode incomingChildInstances,
		AssociationEnd associationEnd
	) {
		return Lists.immutable
			.withAll(incomingChildInstances)
			.rejectWith(this::jsonNodeNeedsIdInferredOnInsert, associationEnd);
	}

	private ImmutableList<JsonNode> filterIncomingInstancesForInsert(
		JsonNode incomingChildInstances,
		AssociationEnd associationEnd
	) {
		return Lists.immutable
			.withAll(incomingChildInstances)
			.selectWith(this::jsonNodeNeedsIdInferredOnInsert, associationEnd);
	}

	// endregion

	private boolean jsonNodeNeedsIdInferredOnInsert(JsonNode jsonNode, @Nonnull AssociationEnd associationEnd) {
		return associationEnd
			.getType()
			.getKeyProperties()
			.allSatisfy((keyProperty) -> this.jsonNodeNeedsIdInferredOnInsert(keyProperty, jsonNode, associationEnd));
	}

	private boolean jsonNodeNeedsIdInferredOnInsert(
		@Nonnull DataTypeProperty keyProperty,
		JsonNode jsonNode,
		@Nonnull AssociationEnd associationEnd
	) {
		OrderedMap<AssociationEnd, DataTypeProperty> keyMatchingThisForeignKey =
			keyProperty.getKeysMatchingThisForeignKey();

		AssociationEnd opposite = associationEnd.getOpposite();

		if (keyMatchingThisForeignKey.containsKey(opposite)) {
			return false;
		}

		if (keyMatchingThisForeignKey.notEmpty()) {
			if (keyMatchingThisForeignKey.size() != 1) {
				throw new AssertionError();
			}

			return false;
		}

		return JsonDataTypeValueVisitor.dataTypePropertyIsNullInJson(keyProperty, (ObjectNode) jsonNode);
	}

	private MapIterable<DataTypeProperty, Object> getKeysFromJsonNode(
		@Nonnull JsonNode jsonNode,
		@Nonnull AssociationEnd associationEnd,
		@Nonnull JsonNode parentJsonNode
	) {
		MutableMap<DataTypeProperty, Object> result = MapAdapter.adapt(new LinkedHashMap<>());

		Klass type = associationEnd.getType();
		ImmutableList<DataTypeProperty> keyProperties = type.getKeyProperties();
		ImmutableList<DataTypeProperty> nonForeignKeyProperties = keyProperties.reject(DataTypeProperty::isForeignKey);
		for (DataTypeProperty keyProperty : nonForeignKeyProperties) {
			result.put(keyProperty, this.getKeyFromJsonNode(keyProperty, jsonNode, associationEnd, parentJsonNode));
		}
		return result.toImmutable();
	}

	private Object getKeyFromJsonNode(
		@Nonnull DataTypeProperty keyProperty,
		@Nonnull JsonNode jsonNode,
		@Nonnull AssociationEnd associationEnd,
		@Nonnull JsonNode parentJsonNode
	) {
		OrderedMap<AssociationEnd, DataTypeProperty> keysMatchingThisForeignKey =
			keyProperty.getKeysMatchingThisForeignKey();

		AssociationEnd opposite = associationEnd.getOpposite();

		DataTypeProperty oppositeForeignKey = keysMatchingThisForeignKey.get(opposite);

		if (oppositeForeignKey != null) {
			String oppositeForeignKeyName = oppositeForeignKey.getName();
			Object result = parentJsonNode.path(oppositeForeignKeyName);
			return Objects.requireNonNull(result);
		}

		if (keysMatchingThisForeignKey.notEmpty()) {
			if (keysMatchingThisForeignKey.size() != 1) {
				throw new AssertionError();
			}

			Pair<AssociationEnd, DataTypeProperty> pair = keysMatchingThisForeignKey.keyValuesView().getOnly();

			JsonNode childNode = jsonNode.path(pair.getOne().getName());
			Object result = JsonDataTypeValueVisitor.extractDataTypePropertyFromJson(
				pair.getTwo(),
				(ObjectNode) childNode
			);
			return Objects.requireNonNull(result);
		}

		if (jsonNode instanceof ObjectNode objectNode) {
			Object result = JsonDataTypeValueVisitor.extractDataTypePropertyFromJson(keyProperty, objectNode);

			if (result == null) {
				String error = String.format(
					"Error at %s. Expected value for key property '%s.%s: %s%s' but value was %s.",
					this.getContextString(),
					keyProperty.getOwningClassifier().getName(),
					keyProperty.getName(),
					keyProperty.getType(),
					keyProperty.isOptional() ? "?" : "",
					result
				);
				this.errors.add(error);
			}
			return result;
		}

		throw new AssertionError(jsonNode);
	}
}

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

package cool.klass.deserializer.json;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cool.klass.deserializer.json.context.ContextNode;
import cool.klass.deserializer.json.context.ContextStack;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.Multiplicity;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.OrderedMap;
import org.eclipse.collections.api.tuple.Pair;

// TODO 2024-10-20: Rename "*OutsideProjection()" methods to "*OutsideComposite"
// TODO 2024-10-20: Rename isInProjection to isInComposite
public class RequiredPropertiesValidator {

	@Nonnull
	protected final ContextStack contextStack;

	@Nonnull
	protected final Klass klass;

	@Nonnull
	protected final ObjectNode objectNode;

	@Nonnull
	protected final OperationMode operationMode;

	@Nonnull
	protected final Optional<AssociationEnd> pathHere;

	protected final boolean isRoot;
	protected final boolean isInProjection;

	public RequiredPropertiesValidator(
		@Nonnull ContextStack contextStack,
		@Nonnull Klass klass,
		@Nonnull ObjectNode objectNode,
		@Nonnull OperationMode operationMode,
		@Nonnull Optional<AssociationEnd> pathHere,
		boolean isRoot,
		boolean isInProjection
	) {
		this.contextStack = Objects.requireNonNull(contextStack);
		this.klass = Objects.requireNonNull(klass);
		this.objectNode = Objects.requireNonNull(objectNode);
		this.operationMode = Objects.requireNonNull(operationMode);
		this.pathHere = Objects.requireNonNull(pathHere);
		this.isRoot = isRoot;
		this.isInProjection = isInProjection;
	}

	public static void validate(
		@Nonnull MutableList<String> errors,
		@Nonnull MutableList<String> warnings,
		@Nonnull Klass klass,
		@Nonnull ObjectNode objectNode,
		@Nonnull OperationMode operationMode
	) {
		var validator = new RequiredPropertiesValidator(
			new ContextStack(errors, warnings),
			klass,
			objectNode,
			operationMode,
			Optional.empty(),
			true,
			true
		);
		validator.validate();
	}

	public void validate() {
		if (this.isRoot) {
			var contextNode = new ContextNode(this.klass);
			this.contextStack.runWithContext(contextNode, () -> {
					this.handleDataTypeProperties();
					this.handleAssociationEnds();
				});
		} else {
			this.handleDataTypeProperties();
			this.handleAssociationEnds();
		}
	}

	// region DataTypeProperties
	protected void handleDataTypeProperties() {
		ImmutableList<DataTypeProperty> dataTypeProperties = this.klass.getDataTypeProperties();
		for (DataTypeProperty dataTypeProperty : dataTypeProperties) {
			var contextNode = new ContextNode(dataTypeProperty);
			this.contextStack.runWithContext(contextNode, () -> this.handleDataTypeProperty(dataTypeProperty));
		}
	}

	protected void handleDataTypeProperty(@Nonnull DataTypeProperty dataTypeProperty) {
		if (dataTypeProperty.isID()) {
			this.handleIdProperty(dataTypeProperty);
		} else if (dataTypeProperty.isKey()) {
			this.handleKeyProperty(dataTypeProperty);
		} else if (dataTypeProperty.isCreatedBy() || dataTypeProperty.isLastUpdatedBy()) {
			Severity severity = dataTypeProperty.isPrivate() ? Severity.ERROR : Severity.WARNING;
			this.handleIfPresent(dataTypeProperty, "audit", severity);
		} else if (dataTypeProperty.isCreatedOn()) {
			this.handleCreatedOnProperty(dataTypeProperty);
		} else if (dataTypeProperty.isForeignKey()) {
			Severity severity = dataTypeProperty.isPrivate() ? Severity.ERROR : Severity.WARNING;
			this.handleIfPresent(dataTypeProperty, "foreign key", severity);
		} else if (dataTypeProperty.isDerived()) {
			this.handleIfPresent(dataTypeProperty, "derived", Severity.WARNING);
		} else if (dataTypeProperty.getType() == PrimitiveType.TEMPORAL_RANGE) {
			this.handleIfPresent(dataTypeProperty, "temporal range", Severity.ERROR);
		} else if (dataTypeProperty.getType() == PrimitiveType.TEMPORAL_INSTANT && this.isInProjection) {
			if (this.operationMode == OperationMode.CREATE && this.isInProjection) {
				this.handleIfPresent(dataTypeProperty, "temporal", Severity.WARNING);
			}
		} else if (dataTypeProperty.isPrivate()) {
			this.handleIfPresent(dataTypeProperty, "private", Severity.ERROR);
		} else if (dataTypeProperty.isDerived()) {
			this.handleIfPresent(dataTypeProperty, "derived", Severity.WARNING);
		} else if (dataTypeProperty.isVersion()) {
			this.handleVersionProperty(dataTypeProperty);
		} else if (dataTypeProperty.isPrivate()) {
			this.handleIfPresent(dataTypeProperty, "private", Severity.ERROR);
		} else {
			this.handlePlainProperty(dataTypeProperty);
		}
	}

	protected void handleIdProperty(@Nonnull DataTypeProperty dataTypeProperty) {
		if (this.operationMode == OperationMode.CREATE) {
			return;
		}

		if (this.operationMode == OperationMode.REPLACE && this.isRoot) {
			return;
		}

		if (this.pathHere.isPresent() && this.pathHere.get().getMultiplicity() == Multiplicity.ONE_TO_ONE) {
			JsonNode jsonNode = this.objectNode.path(dataTypeProperty.getName());
			if (jsonNode.isMissingNode() || jsonNode.isNull()) {
				String error = String.format(
					"Expected value for required id property '%s.%s: %s%s' but value was %s.",
					dataTypeProperty.getOwningClassifier().getName(),
					dataTypeProperty.getName(),
					dataTypeProperty.getType(),
					dataTypeProperty.isOptional() ? "?" : "",
					jsonNode.getNodeType().toString().toLowerCase(Locale.ROOT)
				);
				this.contextStack.addError(error);
			}
		}
	}

	protected void handleKeyProperty(@Nonnull DataTypeProperty dataTypeProperty) {
		// TODO: Handle foreign key properties that are also key properties at the root
		if (this.isForeignKeyMatchingKeyOnPath(dataTypeProperty)) {
			this.handleIfPresent(dataTypeProperty, "foreign key matching key on path", Severity.WARNING);
			return;
		}

		// TODO: Exclude path here
		if (this.isForeignKeyMatchingRequiredNested(dataTypeProperty)) {
			this.handleIfPresent(
				dataTypeProperty,
				"foreign key matching key of required nested object",
				Severity.WARNING
			);
			return;
		}

		if (this.isRoot) {
			this.handleIfPresent(dataTypeProperty, "root key", Severity.WARNING);
			return;
		}

		if (dataTypeProperty.isForeignKeyWithOpposite()) {
			this.handleIfPresent(dataTypeProperty, "foreign key", Severity.WARNING);
			return;
		}

		if (this.pathHere.isPresent() && dataTypeProperty.isForeignKeyMatchingKeyOnPath(this.pathHere.get())) {
			this.handleIfPresent(this.pathHere.get(), dataTypeProperty.getName());
			return;
		}

		if (this.operationMode == OperationMode.PATCH) {
			return;
		}

		JsonNode jsonNode = this.objectNode.path(dataTypeProperty.getName());
		if (jsonNode.isMissingNode() || jsonNode.isNull()) {
			String error = String.format(
				"Expected value for key property '%s.%s: %s%s' but value was %s.",
				dataTypeProperty.getOwningClassifier().getName(),
				dataTypeProperty.getName(),
				dataTypeProperty.getType(),
				dataTypeProperty.isOptional() ? "?" : "",
				jsonNode.getNodeType().toString().toLowerCase(Locale.ROOT)
			);
			this.contextStack.addError(error);
		}
	}

	protected boolean isForeignKeyMatchingRequiredNested(DataTypeProperty dataTypeProperty) {
		// TODO: Exclude path here
		return dataTypeProperty.getKeysMatchingThisForeignKey().keysView().anySatisfy(this::isToOneRequired);
	}

	private void handleCreatedOnProperty(@Nonnull DataTypeProperty dataTypeProperty) {
		if (this.isInProjection && this.operationMode == OperationMode.CREATE) {
			this.handleIfPresent(dataTypeProperty, "audit", Severity.WARNING);
		} else if (
			this.isInProjection
			&& (this.operationMode == OperationMode.REPLACE || this.operationMode == OperationMode.PATCH)
		) {
			// Validate in Incoming(Create|Update)DataModelValidator
		} else if (!this.isInProjection) {
			// Validate in Incoming(Create|Update)DataModelValidator
		} else {
			throw new AssertionError();
		}
	}

	protected boolean isToOneRequired(AssociationEnd associationEnd) {
		Multiplicity multiplicity = associationEnd.getMultiplicity();
		return multiplicity.isToOne() && multiplicity.isRequired();
	}

	protected boolean isForeignKeyMatchingKeyOnPath(DataTypeProperty dataTypeProperty) {
		return this.pathHere.map(dataTypeProperty::isForeignKeyMatchingKeyOnPath).orElse(false);
	}

	private enum Severity {
		ERROR,
		WARNING,
	}

	protected void handleIfPresent(@Nonnull DataTypeProperty property, String propertyKind, Severity severity) {
		JsonNode jsonNode = this.objectNode.path(property.getName());
		if (jsonNode.isMissingNode()) {
			return;
		}

		String jsonNodeString = jsonNode.isNull() ? "" : ": " + jsonNode;
		String annotation = String.format(
			"Didn't expect to receive value for %s property '%s.%s: %s%s' but value was %s%s.",
			propertyKind,
			property.getOwningClassifier().getName(),
			property.getName(),
			property.getType(),
			property.isOptional() ? "?" : "",
			jsonNode.getNodeType().toString().toLowerCase(Locale.ROOT),
			jsonNodeString
		);
		switch (severity) {
			case ERROR -> this.contextStack.addError(annotation);
			case WARNING -> this.contextStack.addWarning(annotation);
			default -> throw new AssertionError("Unexpected value: " + severity);
		}
	}

	protected void handleIfPresent(@Nonnull AssociationEnd property, String propertyKind) {
		JsonNode jsonNode = this.objectNode.path(property.getName());
		if (jsonNode.isMissingNode()) {
			return;
		}

		String jsonNodeString = jsonNode.isNull() ? "" : ": " + jsonNode;
		String warning = String.format(
			"Didn't expect to receive value for %s association end '%s.%s: %s[%s]' but value was %s%s.",
			propertyKind,
			property.getOwningClassifier().getName(),
			property.getName(),
			property.getType(),
			property.getMultiplicity().getPrettyName(),
			jsonNode.getNodeType().toString().toLowerCase(Locale.ROOT),
			jsonNodeString
		);
		this.contextStack.addWarning(warning);
	}

	protected void handlePlainProperty(@Nonnull DataTypeProperty property) {
		if (!property.isRequired()) {
			return;
		}

		if (!this.isInProjection) {
			this.handleIfPresent(property, "outside projection", Severity.WARNING);
			return;
		}

		if (this.operationMode == OperationMode.PATCH) {
			return;
		}

		JsonNode jsonNode = this.objectNode.path(property.getName());
		if (jsonNode.isMissingNode() || jsonNode.isNull()) {
			String error = String.format(
				"Expected value for required property '%s.%s: %s%s' but value was %s.",
				property.getOwningClassifier().getName(),
				property.getName(),
				property.getType(),
				property.isOptional() ? "?" : "",
				jsonNode.getNodeType().toString().toLowerCase(Locale.ROOT)
			);
			this.contextStack.addError(error);
		}
	}

	private void handleVersionProperty(DataTypeProperty property) {
		JsonNode jsonNode = this.objectNode.path(property.getName());
		if (jsonNode.isMissingNode() || jsonNode.isNull()) {
			return;
		}

		if (!jsonNode.isIntegralNumber()) {
			return;
		}

		if (jsonNode.asInt() != 1) {
			String error = String.format(
				"Expected value for version property '%s.%s: %s%s' to be 1 during initial creation but value was %s.",
				property.getOwningClassifier().getName(),
				property.getName(),
				property.getType(),
				property.isOptional() ? "?" : "",
				jsonNode.getNodeType().toString().toLowerCase(Locale.ROOT)
			);
			this.contextStack.addError(error);
		}
	}

	// endregion

	// region AssociationEnds
	protected void handleAssociationEnds() {
		for (AssociationEnd associationEnd : this.klass.getAssociationEnds()) {
			this.handleAssociationEnd(associationEnd);
		}
	}

	protected void handleAssociationEnd(AssociationEnd associationEnd) {
		if (this.isBackward(associationEnd)) {
			this.handleIfPresent(associationEnd, "opposite");
		} else if (associationEnd.isVersion()) {
			this.handleVersionAssociationEnd(associationEnd);
		} else if (associationEnd.isCreatedBy() || associationEnd.isLastUpdatedBy()) {
			this.handleAuditAssociationEnd(associationEnd);
		} else if (associationEnd.isOwned()) {
			this.handleOwnedAssociationEnd(associationEnd);
		} else {
			this.handleAssociationEndOutsideProjection(associationEnd);
		}
	}

	public void handleToOne(@Nonnull AssociationEnd associationEnd, JsonNode jsonNode) {
		var contextNode = new ContextNode(associationEnd);
		this.contextStack.runWithContext(contextNode, () -> {
				if (jsonNode instanceof ObjectNode objectNode) {
					this.handleOwnedAssociationEnd(associationEnd, objectNode);
				}
			});
	}

	public void handleToMany(@Nonnull AssociationEnd associationEnd, JsonNode jsonNode) {
		if (!(jsonNode instanceof ArrayNode arrayNode)) {
			return;
		}

		for (int index = 0; index < jsonNode.size(); index++) {
			var contextNode = new ContextNode(associationEnd, index);
			int finalIndex = index;
			this.contextStack.runWithContext(contextNode, () -> {
					JsonNode childJsonNode = jsonNode.path(finalIndex);
					if (childJsonNode instanceof ObjectNode objectNode) {
						this.handleOwnedAssociationEnd(associationEnd, objectNode);
					}
				});
		}
	}

	protected void handleAssociationEndOutsideProjection(AssociationEnd associationEnd) {
		Multiplicity multiplicity = associationEnd.getMultiplicity();

		JsonNode jsonNode = this.objectNode.path(associationEnd.getName());

		if ((jsonNode.isMissingNode() || jsonNode.isNull()) && multiplicity.isRequired()) {
			if (this.operationMode == OperationMode.CREATE && associationEnd.isVersion()) {
				return;
			}

			if (this.operationMode == OperationMode.REPLACE && associationEnd.isFinal()) {
				String warning = String.format(
					"Expected value for required final property '%s.%s: %s[%s]' but value was %s.",
					associationEnd.getOwningClassifier().getName(),
					associationEnd.getName(),
					associationEnd.getType(),
					associationEnd.getMultiplicity().getPrettyName(),
					jsonNode.getNodeType().toString().toLowerCase(Locale.ROOT)
				);
				this.contextStack.addWarning(warning);
				return;
			}

			if (this.operationMode == OperationMode.REPLACE && associationEnd.isPrivate()) {
				String warning = String.format(
					"Expected value for required private property '%s.%s: %s[%s]' but value was %s.",
					associationEnd.getOwningClassifier().getName(),
					associationEnd.getName(),
					associationEnd.getType(),
					associationEnd.getMultiplicity().getPrettyName(),
					jsonNode.getNodeType().toString().toLowerCase(Locale.ROOT)
				);
				this.contextStack.addWarning(warning);
				return;
			}

			if (
				this.operationMode == OperationMode.CREATE
				|| this.operationMode == OperationMode.REPLACE
				|| (this.operationMode == OperationMode.PATCH && jsonNode.isNull())
			) {
				String error = String.format(
					"Expected value for required property '%s.%s: %s[%s]' but value was %s.",
					associationEnd.getOwningClassifier().getName(),
					associationEnd.getName(),
					associationEnd.getType(),
					associationEnd.getMultiplicity().getPrettyName(),
					jsonNode.getNodeType().toString().toLowerCase(Locale.ROOT)
				);
				this.contextStack.addError(error);
				return;
			}
		}

		if (multiplicity.isToOne()) {
			this.handleToOneOutsideProjection(associationEnd, jsonNode);
		} else {
			this.handleToManyOutsideProjection(associationEnd, jsonNode);
		}
	}

	protected void handleToOneOutsideProjection(@Nonnull AssociationEnd associationEnd, @Nonnull JsonNode jsonNode) {
		if (associationEnd.isOwned()) {
			throw new AssertionError(
				"Assumption is that all owned association ends are inside projection, all unowned are outside projection"
			);
		}

		if (jsonNode.isMissingNode() || jsonNode.isNull()) {
			if (associationEnd.isRequired() && this.operationMode != OperationMode.PATCH) {
				String error = String.format(
					"Expected value for required property '%s.%s: %s[%s]' but value was %s.",
					associationEnd.getOwningClassifier().getName(),
					associationEnd.getName(),
					associationEnd.getType(),
					associationEnd.getMultiplicity().getPrettyName(),
					jsonNode.getNodeType().toString().toLowerCase(Locale.ROOT)
				);
				this.contextStack.addError(error);
			}
			return;
		}

		if (!associationEnd.hasRealKeys()) {
			String warning = String.format(
				"Did not expect value for property '%s.%s: %s[%s]' because it's outside the owned projection and it has no keys other than foreign keys.",
				associationEnd.getOwningClassifier().getName(),
				associationEnd.getName(),
				associationEnd.getType(),
				associationEnd.getMultiplicity().getPrettyName()
			);
			this.contextStack.addWarning(warning);
		}

		var contextNode = new ContextNode(associationEnd);
		this.contextStack.runWithContext(contextNode, () -> {
				if (!(jsonNode instanceof ObjectNode objectNode)) {
					return;
				}
				OperationMode nextMode = this.getNextMode(this.operationMode, associationEnd);

				var validator = new RequiredPropertiesValidator(
					this.contextStack,
					associationEnd.getType(),
					objectNode,
					nextMode,
					Optional.of(associationEnd),
					false,
					false
				);
				validator.validate();
			});
	}

	protected void handleToManyOutsideProjection(@Nonnull AssociationEnd associationEnd, @Nonnull JsonNode jsonNode) {
		if (associationEnd.isOwned()) {
			throw new AssertionError(
				"Assumption is that all owned association ends are inside projection, all unowned are outside projection"
			);
		}

		if (jsonNode.isMissingNode() || jsonNode.isNull()) {
			if (associationEnd.isRequired()) {
				String error = String.format(
					"Expected value for required property '%s.%s: %s[%s]' but value was %s.",
					associationEnd.getOwningClassifier().getName(),
					associationEnd.getName(),
					associationEnd.getType(),
					associationEnd.getMultiplicity().getPrettyName(),
					jsonNode.getNodeType().toString().toLowerCase(Locale.ROOT)
				);
				this.contextStack.addError(error);
			}
			return;
		}

		if (!associationEnd.hasRealKeys()) {
			String warning = String.format(
				"Did not expect value for property '%s.%s: %s[%s]' because it's outside the owned projection and it has no keys other than foreign keys.",
				associationEnd.getOwningClassifier().getName(),
				associationEnd.getName(),
				associationEnd.getType(),
				associationEnd.getMultiplicity().getPrettyName()
			);
			this.contextStack.addWarning(warning);
		}

		if (!(jsonNode instanceof ArrayNode)) {
			return;
		}

		for (int index = 0; index < jsonNode.size(); index++) {
			int finalIndex = index;
			var contextNode = new ContextNode(associationEnd, finalIndex);
			this.contextStack.runWithContext(contextNode, () -> {
					JsonNode childJsonNode = jsonNode.path(finalIndex);
					if (!(childJsonNode instanceof ObjectNode)) {
						return;
					}

					OperationMode nextMode = this.getNextMode(this.operationMode, associationEnd);

					var validator = new RequiredPropertiesValidator(
						this.contextStack,
						associationEnd.getType(),
						(ObjectNode) childJsonNode,
						nextMode,
						Optional.of(associationEnd),
						false,
						false
					);
					validator.validate();
				});
		}
	}

	protected void handleErrorIfAbsent(@Nonnull AssociationEnd associationEnd, String propertyKind) {
		JsonNode jsonNode = this.objectNode.path(associationEnd.getName());
		if (!jsonNode.isMissingNode() && !jsonNode.isNull()) {
			return;
		}

		String error = String.format(
			"Expected value for %s property '%s.%s: %s[%s]' but value was %s.",
			propertyKind,
			associationEnd.getOwningClassifier().getName(),
			associationEnd.getName(),
			associationEnd.getType(),
			associationEnd.getMultiplicity().getPrettyName(),
			jsonNode.getNodeType().toString().toLowerCase(Locale.ROOT)
		);
		this.contextStack.addError(error);
	}

	protected void handleOwnedAssociationEnd(@Nonnull AssociationEnd associationEnd, @Nonnull ObjectNode objectNode) {
		OperationMode nextMode = this.getNextMode(this.operationMode, associationEnd);

		if (associationEnd.isVersion()) {
			this.handleVersionAssociationEnd(associationEnd);
		} else {
			this.handlePlainAssociationEnd(associationEnd, objectNode, nextMode);
		}
	}

	protected void handleOwnedAssociationEnd(@Nonnull AssociationEnd associationEnd) {
		Multiplicity multiplicity = associationEnd.getMultiplicity();

		JsonNode jsonNode = this.objectNode.path(associationEnd.getName());

		if ((jsonNode.isMissingNode() || jsonNode.isNull()) && multiplicity.isRequired()) {
			if (this.operationMode == OperationMode.CREATE && associationEnd.isVersion()) {
				return;
			}

			if (this.operationMode == OperationMode.REPLACE && associationEnd.isFinal()) {
				String warning = String.format(
					"Expected value for required final property '%s.%s: %s[%s]' but value was %s.",
					associationEnd.getOwningClassifier().getName(),
					associationEnd.getName(),
					associationEnd.getType(),
					associationEnd.getMultiplicity().getPrettyName(),
					jsonNode.getNodeType().toString().toLowerCase(Locale.ROOT)
				);
				this.contextStack.addWarning(warning);
				return;
			}

			if (this.operationMode == OperationMode.REPLACE && associationEnd.isPrivate()) {
				String warning = String.format(
					"Expected value for required private property '%s.%s: %s[%s]' but value was %s.",
					associationEnd.getOwningClassifier().getName(),
					associationEnd.getName(),
					associationEnd.getType(),
					associationEnd.getMultiplicity().getPrettyName(),
					jsonNode.getNodeType().toString().toLowerCase(Locale.ROOT)
				);
				this.contextStack.addWarning(warning);
				return;
			}

			if (
				this.operationMode == OperationMode.CREATE
				|| this.operationMode == OperationMode.REPLACE
				|| (this.operationMode == OperationMode.PATCH && jsonNode.isNull())
			) {
				String error = String.format(
					"Expected value for required property '%s.%s: %s[%s]' but value was %s.",
					associationEnd.getOwningClassifier().getName(),
					associationEnd.getName(),
					associationEnd.getType(),
					associationEnd.getMultiplicity().getPrettyName(),
					jsonNode.getNodeType().toString().toLowerCase(Locale.ROOT)
				);
				this.contextStack.addError(error);
				return;
			}
		}

		if (multiplicity.isToOne()) {
			this.handleToOne(associationEnd, jsonNode);
		} else {
			this.handleToMany(associationEnd, jsonNode);
		}
	}

	protected void handleVersionAssociationEnd(@Nonnull AssociationEnd associationEnd) {
		JsonNode jsonNode = this.objectNode.path(associationEnd.getName());
		if (this.operationMode == OperationMode.CREATE) {
			if (jsonNode.isMissingNode()) {
				return;
			}

			var contextNode = new ContextNode(associationEnd);
			this.contextStack.runWithContext(contextNode, () -> {
					if (jsonNode instanceof ObjectNode objectNode) {
						OperationMode nextMode = this.getNextMode(this.operationMode, associationEnd);
						var validator = new RequiredPropertiesValidator(
							this.contextStack,
							associationEnd.getType(),
							objectNode,
							nextMode,
							Optional.of(associationEnd),
							false,
							this.isInProjection && associationEnd.isOwned()
						);
						validator.validate();
					}
				});
		} else if (this.operationMode == OperationMode.REPLACE || this.operationMode == OperationMode.PATCH) {
			if (this.klass.getKeyProperties().anySatisfy(DataTypeProperty::isID)) {
				// Classes with ID properties use separate endpoints for create and replace, so we know we're definitely replacing. Therefore the version must be present.
				this.handleErrorIfAbsent(associationEnd, "version");
			}
			// Classes without ID properties use a single endpoint for create and replace. We won't know if we're performing a replacement until querying from the data store. At this point, it's too early to validate anything.
		} else if (this.operationMode == OperationMode.REFERENCE_OUTSIDE_PROJECTION) {
			// TODO: Recurse and check that it matches if present
			this.handleIfPresent(associationEnd, "version");
		} else {
			throw new UnsupportedOperationException(
				this.getClass().getSimpleName() + ".handleVersionAssociationEnd() not implemented yet"
			);
		}
	}

	protected void handleAuditAssociationEnd(@Nonnull AssociationEnd associationEnd) {
		JsonNode jsonNode = this.objectNode.path(associationEnd.getName());
		if (jsonNode.isMissingNode()) {
			return;
		}

		this.handleAssociationEndOutsideProjection(associationEnd);
	}

	protected void handlePlainAssociationEnd(
		@Nonnull AssociationEnd associationEnd,
		@Nonnull ObjectNode objectNode,
		@Nonnull OperationMode nextMode
	) {
		var validator = new RequiredPropertiesValidator(
			this.contextStack,
			associationEnd.getType(),
			objectNode,
			nextMode,
			Optional.of(associationEnd),
			false,
			this.isInProjection && associationEnd.isOwned()
		);
		validator.validate();
	}

	@Nonnull
	protected OperationMode getNextMode(OperationMode operationMode, @Nonnull AssociationEnd associationEnd) {
		if (operationMode == OperationMode.CREATE && associationEnd.isOwned()) {
			return OperationMode.CREATE;
		}
		if (operationMode == OperationMode.REPLACE && associationEnd.isOwned()) {
			return OperationMode.REPLACE;
		}

		if (operationMode == OperationMode.PATCH && associationEnd.isOwned()) {
			return OperationMode.PATCH;
		}
		if (
			(operationMode == OperationMode.CREATE
				|| operationMode == OperationMode.PATCH
				|| operationMode == OperationMode.REPLACE)
			&& !associationEnd.isOwned()
		) {
			return OperationMode.REFERENCE_OUTSIDE_PROJECTION;
		}
		if (operationMode == OperationMode.REFERENCE_OUTSIDE_PROJECTION) {
			return OperationMode.REFERENCE_OUTSIDE_PROJECTION;
		}

		throw new UnsupportedOperationException(
			this.getClass().getSimpleName() + ".getNextMode() not implemented yet: " + operationMode
		);
	}

	protected ImmutableList<Object> getKeysFromJsonNode(
		@Nonnull JsonNode jsonNode,
		@Nonnull AssociationEnd associationEnd,
		@Nonnull JsonNode parentJsonNode
	) {
		Klass type = associationEnd.getType();
		ImmutableList<DataTypeProperty> keyProperties = type.getKeyProperties();
		ImmutableList<DataTypeProperty> nonForeignKeyProperties = keyProperties.reject(DataTypeProperty::isForeignKey);
		return nonForeignKeyProperties.collect((keyProperty) ->
			this.getKeyFromJsonNode(keyProperty, jsonNode, associationEnd, parentJsonNode)
		);
	}

	protected Object getKeyFromJsonNode(
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

		Object result = JsonDataTypeValueVisitor.extractDataTypePropertyFromJson(keyProperty, (ObjectNode) jsonNode);
		return Objects.requireNonNull(result);
	}

	protected boolean isBackward(@Nonnull AssociationEnd associationEnd) {
		return this.pathHere.equals(Optional.of(associationEnd.getOpposite()));
	}
	// endregion AssociationEnds
}

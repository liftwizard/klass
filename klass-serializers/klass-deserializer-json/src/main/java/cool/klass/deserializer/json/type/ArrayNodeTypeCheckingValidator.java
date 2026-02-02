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

package cool.klass.deserializer.json.type;

import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import cool.klass.deserializer.json.context.ContextNode;
import cool.klass.deserializer.json.context.ContextStack;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.NamedElement;
import org.eclipse.collections.api.list.MutableList;

public final class ArrayNodeTypeCheckingValidator {

	@Nonnull
	private final ContextStack contextStack;

	@Nonnull
	private final NamedElement context;

	@Nonnull
	private final Klass klass;

	@Nonnull
	private final JsonNode jsonNode;

	public ArrayNodeTypeCheckingValidator(
		@Nonnull ContextStack contextStack,
		@Nonnull NamedElement context,
		@Nonnull Klass klass,
		@Nonnull JsonNode jsonNode
	) {
		this.contextStack = Objects.requireNonNull(contextStack);
		this.context = context;

		this.jsonNode = Objects.requireNonNull(jsonNode);
		this.klass = Objects.requireNonNull(klass);
	}

	public static void validate(@Nonnull MutableList<String> errors, @Nonnull JsonNode jsonNode, @Nonnull Klass klass) {
		ContextStack contextStack = new ContextStack(errors, null);
		var incomingDataValidator = new ArrayNodeTypeCheckingValidator(contextStack, klass, klass, jsonNode);
		incomingDataValidator.validateIncomingData();
	}

	public void validateIncomingData() {
		if (!(this.jsonNode instanceof ArrayNode arrayNode)) {
			String error = String.format(
				"Expected json array but value was %s: %s.",
				this.jsonNode.getNodeType().toString().toLowerCase(Locale.ROOT),
				this.jsonNode
			);
			this.contextStack.addError(error);
			return;
		}

		this.validateArrayNode(arrayNode);
	}

	private void validateArrayNode(@Nonnull ArrayNode arrayNode) {
		for (int index = 0; index < arrayNode.size(); index++) {
			JsonNode childJsonNode = arrayNode.path(index);
			var contextNode = new ContextNode(this.context, index);
			this.contextStack.runWithContext(contextNode, () -> {
					var validator = new ObjectNodeTypeCheckingValidator(this.contextStack, this.klass, childJsonNode);
					validator.validateIncomingData();
				});
		}
	}
}

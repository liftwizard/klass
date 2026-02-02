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

package cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.dto;

import java.util.LinkedHashMap;

import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme.JsonAnsiColorScheme;
import io.dropwizard.validation.ValidationMethod;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record ColorSchemeDefinition(
	@NotBlank(message = "Color scheme name is required") @Nonnull String name,

	@NotNull(message = "Color scheme description is required") String description,

	@NotEmpty(message = "Color scheme must define at least one rule")
	@NotNull(message = "Rules list cannot be null")
	@Valid
	MutableList<ColorSchemeRule> rules
) {
	private static final Logger LOGGER = LoggerFactory.getLogger(ColorSchemeDefinition.class);

	@ValidationMethod(message = "Color scheme must define a 'background' rule")
	@JsonIgnore
	public boolean hasBackgroundRule() {
		return this.rules.anySatisfy((rule) -> "background".equals(rule.name()));
	}

	@ValidationMethod(message = "Unknown rule name in color scheme")
	@JsonIgnore
	public boolean hasValidRuleNames() {
		ImmutableSet<String> validRuleNames = JsonAnsiColorScheme.getValidRuleNames();

		var schemeRuleNames = this.rules.collect(ColorSchemeRule::name).toSet();

		var invalidRuleNames = schemeRuleNames.reject(validRuleNames::contains);

		if (invalidRuleNames.notEmpty()) {
			LOGGER.warn("Unknown rule name(s) found in color scheme: {}", invalidRuleNames.makeString(", "));
			return false;
		}

		return true;
	}

	public MapIterable<String, ColorSchemeRule> toRuleMap() {
		// TODO 2025-03-16: Change the return type to be immutable, once Eclipse Collections supports it.
		MutableOrderedMap<String, ColorSchemeRule> result = OrderedMapAdapter.adapt(new LinkedHashMap<>());
		return this.rules.groupByUniqueKey(ColorSchemeRule::name, result).asUnmodifiable();
	}
}

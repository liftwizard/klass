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

package cool.klass.service.klass.html;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import cool.klass.generator.klass.html.KlassSourceCodeHtmlGenerator;
import cool.klass.model.meta.domain.api.EnumerationLiteral;
import cool.klass.model.meta.domain.api.property.Property;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import cool.klass.model.meta.domain.api.source.ElementWithSourceCode;
import cool.klass.model.meta.domain.api.source.EnumerationWithSourceCode;
import cool.klass.model.meta.domain.api.source.KlassWithSourceCode;
import cool.klass.model.meta.domain.api.source.NamedElementWithSourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.TopLevelElementWithSourceCode;
import org.eclipse.collections.api.list.ImmutableList;

@Path("/")
public class KlassHtmlResource {

	@Nonnull
	private final DomainModelWithSourceCode domainModel;

	public KlassHtmlResource(@Nonnull DomainModelWithSourceCode domainModel) {
		this.domainModel = Objects.requireNonNull(domainModel);
	}

	@Timed
	@ExceptionMetered
	@GET
	@Path("/meta/code/element/{topLevelElementName}")
	@Produces(MediaType.TEXT_HTML)
	public String topLevelElementNameSourceCode(@PathParam("topLevelElementName") String topLevelElementName) {
		TopLevelElementWithSourceCode topLevelElement = this.domainModel.getTopLevelElementByName(topLevelElementName);

		if (topLevelElement == null) {
			String message = this.domainModel.getTopLevelElements()
				.selectInstancesOf(NamedElementWithSourceCode.class)
				.collect(NamedElementWithSourceCode::getName)
				.toString();
			throw new NotFoundException(message);
		}

		Optional<SourceCode> sourceCode = getSourceCodeObject(topLevelElement, null);
		if (sourceCode.isEmpty()) {
			throw new BadRequestException();
		}
		return KlassSourceCodeHtmlGenerator.getSourceCode(
			this.domainModel,
			sourceCode.get(),
			Optional.of(topLevelElement),
			Optional.empty()
		);
	}

	@Timed
	@ExceptionMetered
	@GET
	@Path("/meta/code/element/{topLevelElementName}/{memberName}")
	@Produces(MediaType.TEXT_HTML)
	public String topLevelElementNameSourceCode(
		@PathParam("topLevelElementName") String topLevelElementName,
		@PathParam("memberName") String memberName
	) {
		TopLevelElementWithSourceCode topLevelElement = this.domainModel.getTopLevelElementByName(topLevelElementName);

		if (topLevelElement == null) {
			String message = this.domainModel.getTopLevelElements()
				.selectInstancesOf(NamedElementWithSourceCode.class)
				.collect(NamedElementWithSourceCode::getName)
				.toString();
			throw new NotFoundException(message);
		}

		Optional<SourceCode> sourceCode = getSourceCodeObject(topLevelElement, memberName);
		if (sourceCode.isEmpty()) {
			throw new BadRequestException();
		}
		return KlassSourceCodeHtmlGenerator.getSourceCode(
			this.domainModel,
			sourceCode.get(),
			Optional.of(topLevelElement),
			Optional.of(memberName)
		);
	}

	@Nonnull
	private static Optional<SourceCode> getSourceCodeObject(
		TopLevelElementWithSourceCode topLevelElement,
		String memberName
	) {
		if (memberName == null) {
			return Optional.of(topLevelElement.getSourceCodeObject());
		}

		if (topLevelElement instanceof KlassWithSourceCode klass) {
			Optional<Property> property = klass.getPropertyByName(memberName);
			if (property.isEmpty()) {
				return Optional.empty();
			}
			if (property.get() instanceof ElementWithSourceCode elementWithSourceCode) {
				return Optional.of(elementWithSourceCode.getSourceCodeObject());
			}
		}

		if (topLevelElement instanceof EnumerationWithSourceCode enumeration) {
			Optional<EnumerationLiteral> enumerationLiteral = enumeration
				.getEnumerationLiterals()
				.detectOptional((each) -> each.getName().equals(memberName));
			if (enumerationLiteral.isEmpty()) {
				return Optional.empty();
			}
			if (enumerationLiteral.get() instanceof ElementWithSourceCode elementWithSourceCode) {
				return Optional.of(elementWithSourceCode.getSourceCodeObject());
			}
		}

		throw new AssertionError(topLevelElement);
	}

	@Timed
	@ExceptionMetered
	@GET
	@Path("/meta/code/file/{fileName}.html")
	@Produces(MediaType.TEXT_HTML)
	public String fileSourceCode(@PathParam("fileName") String fileName) {
		ImmutableList<SourceCode> sourceCodes = this.domainModel.getSourceCodes().select((each) ->
			each.getSourceName().equals(fileName)
		);

		if (sourceCodes.size() != 1) {
			throw new NotFoundException(
				this.domainModel.getSourceCodes().collect(SourceCode::getSourceName).toString()
			);
		}

		SourceCode sourceCode = sourceCodes.getOnly();
		return KlassSourceCodeHtmlGenerator.getSourceCode(
			this.domainModel,
			sourceCode,
			Optional.empty(),
			Optional.empty()
		);
	}
}

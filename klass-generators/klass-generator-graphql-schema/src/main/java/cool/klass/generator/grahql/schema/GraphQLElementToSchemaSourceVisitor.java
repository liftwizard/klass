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

package cool.klass.generator.grahql.schema;

import javax.annotation.Nonnull;

import cool.klass.model.meta.domain.api.Association;
import cool.klass.model.meta.domain.api.Enumeration;
import cool.klass.model.meta.domain.api.Interface;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.NamedElement;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.api.TopLevelElementVisitor;
import cool.klass.model.meta.domain.api.Type;
import cool.klass.model.meta.domain.api.projection.Projection;
import cool.klass.model.meta.domain.api.property.Property;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import cool.klass.model.meta.domain.api.service.ServiceGroup;
import org.eclipse.collections.api.list.ImmutableList;

public class GraphQLElementToSchemaSourceVisitor implements TopLevelElementVisitor {

    private String sourceCode;

    @Override
    public void visitEnumeration(Enumeration enumeration) {
        this.sourceCode = this.getEnumerationSourceCode(enumeration);
    }

    @Override
    public void visitInterface(Interface anInterface) {
        this.sourceCode = "";
    }

    @Override
    public void visitKlass(Klass klass) {
        this.sourceCode = this.getClassSourceCode(klass);
    }

    @Override
    public void visitAssociation(Association association) {
        this.sourceCode = "";
    }

    @Override
    public void visitProjection(Projection projection) {
        this.sourceCode = "";
    }

    @Override
    public void visitServiceGroup(ServiceGroup serviceGroup) {
        this.sourceCode = "";
    }

    @Nonnull
    private String getEnumerationSourceCode(@Nonnull Enumeration enumeration) {
        String enumerationLiteralsSourceCode = enumeration
            .getEnumerationLiterals()
            .collect(NamedElement::getName)
            .collect(name -> String.format("    %s\n", name))
            .makeString("");

        return "" + "enum " + enumeration.getName() + " {\n" + enumerationLiteralsSourceCode + "}\n" + '\n';
    }

    @Nonnull
    private String getInterfaceSourceCode(@Nonnull Interface anInterface) {
        String fieldsSourceCode = anInterface
            .getProperties()
            .collect(GraphQLElementToSchemaSourceVisitor::getPropertySourceCode)
            .collect(name -> String.format("    %s\n", name))
            .makeString("");

        return "" + "interface " + anInterface.getName() + " {\n" + fieldsSourceCode + "}\n" + '\n';
    }

    @Nonnull
    private String getClassSourceCode(@Nonnull Klass klass) {
        String keyword = klass.isAbstract() ? "interface" : "type";

        ImmutableList<String> superClassNames = klass.getSuperClassChain().collect(NamedElement::getName);
        String implementsSourceCode = superClassNames.isEmpty()
            ? ""
            : " implements " + superClassNames.makeString(" & ");

        String fieldsSourceCode = klass
            .getProperties()
            .reject(Property::isDerived)
            .reject(Property::isPrivate)
            .collect(GraphQLElementToSchemaSourceVisitor::getPropertySourceCode)
            .collect(name -> String.format("    %s\n", name))
            .makeString("");

        return "" + keyword + " " + klass.getName() + implementsSourceCode + " {\n" + fieldsSourceCode + "}\n" + '\n';
    }

    private static String getPropertySourceCode(@Nonnull Property property) {
        return String.format(
            "%s: %s%s%s%s",
            property.getName(),
            GraphQLElementToSchemaSourceVisitor.isMany(property) ? "[" : "",
            GraphQLElementToSchemaSourceVisitor.getType(property),
            GraphQLElementToSchemaSourceVisitor.isMany(property) ? "!]" : "",
            property.isRequired() || GraphQLElementToSchemaSourceVisitor.isMany(property) ? "!" : ""
        );
    }

    @Nonnull
    private static String getType(@Nonnull Property property) {
        Type type = property.getType();
        if (type instanceof Enumeration) {
            return "String";
        }
        if (type == PrimitiveType.INTEGER) {
            return "Int";
        }
        if (type == PrimitiveType.DOUBLE) {
            return "Float";
        }
        return type.toString();
    }

    private static boolean isMany(@Nonnull Property property) {
        return (
            property instanceof ReferenceProperty referenceProperty && referenceProperty.getMultiplicity().isToMany()
        );
    }

    public String getSourceCode() {
        return this.sourceCode;
    }
}

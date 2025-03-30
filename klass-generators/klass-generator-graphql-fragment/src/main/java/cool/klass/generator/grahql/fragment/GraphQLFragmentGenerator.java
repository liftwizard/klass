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

package cool.klass.generator.grahql.fragment;

import java.nio.file.Path;

import javax.annotation.Nonnull;

import cool.klass.generator.perpackage.AbstractPerPackageGenerator;
import cool.klass.model.meta.domain.api.DomainModel;

public class GraphQLFragmentGenerator extends AbstractPerPackageGenerator {

    public GraphQLFragmentGenerator(@Nonnull DomainModel domainModel) {
        super(domainModel);
    }

    @Nonnull
    @Override
    protected Path getPluginRelativePath(Path path) {
        return path.resolve("graphql").resolve("fragment");
    }

    @Nonnull
    @Override
    protected String getFileName() {
        return "GraphQLFragment.graphqls";
    }

    @Nonnull
    @Override
    protected String getPackageSourceCode(@Nonnull String fullyQualifiedPackage) {
        return GraphQLFragmentSourceCodeGenerator.getPackageSourceCode(this.domainModel, fullyQualifiedPackage);
    }
}

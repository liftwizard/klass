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

package cool.klass.model.converter.compiler.state.service;

import java.util.LinkedHashMap;
import java.util.Objects;

import javax.annotation.Nonnull;

import cool.klass.model.converter.compiler.annotation.CompilerAnnotationHolder;
import cool.klass.model.converter.compiler.state.AntlrClass;
import cool.klass.model.converter.compiler.state.AntlrCompilationUnit;
import cool.klass.model.converter.compiler.state.AntlrPackageableElement;
import cool.klass.model.converter.compiler.state.AntlrTopLevelElement;
import cool.klass.model.converter.compiler.state.service.url.AntlrUrl;
import cool.klass.model.meta.domain.service.ServiceGroupImpl.ServiceGroupBuilder;
import cool.klass.model.meta.domain.service.url.UrlImpl.UrlBuilder;
import cool.klass.model.meta.grammar.KlassParser.ClassReferenceContext;
import cool.klass.model.meta.grammar.KlassParser.IdentifierContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceGroupBlockContext;
import cool.klass.model.meta.grammar.KlassParser.ServiceGroupDeclarationContext;
import cool.klass.model.meta.grammar.KlassParser.UrlDeclarationContext;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableOrderedMap;
import org.eclipse.collections.impl.bag.strategy.mutable.HashBagWithHashingStrategy;
import org.eclipse.collections.impl.block.factory.HashingStrategies;
import org.eclipse.collections.impl.map.ordered.mutable.OrderedMapAdapter;

public class AntlrServiceGroup extends AntlrPackageableElement implements AntlrTopLevelElement {

    public static final AntlrServiceGroup AMBIGUOUS = new AntlrServiceGroup(
        new ServiceGroupDeclarationContext(AMBIGUOUS_PARENT, -1),
        AntlrCompilationUnit.AMBIGUOUS,
        -1,
        AMBIGUOUS_IDENTIFIER_CONTEXT,
        AntlrClass.AMBIGUOUS
    );

    @Nonnull
    private final AntlrClass klass;

    private final MutableList<AntlrUrl> urls = Lists.mutable.empty();
    private final MutableOrderedMap<UrlDeclarationContext, AntlrUrl> urlsByContext = OrderedMapAdapter.adapt(
        new LinkedHashMap<>()
    );

    private ServiceGroupBuilder serviceGroupBuilder;

    public AntlrServiceGroup(
        @Nonnull ServiceGroupDeclarationContext elementContext,
        @Nonnull AntlrCompilationUnit compilationUnitState,
        int ordinal,
        @Nonnull IdentifierContext nameContext,
        @Nonnull AntlrClass klass
    ) {
        super(elementContext, compilationUnitState, ordinal, nameContext);
        this.klass = Objects.requireNonNull(klass);
    }

    // TODO: Should this be a Classifier rather than a Class?
    // TODO: If so, there should also be an error for PUT/POST on an interface
    @Nonnull
    public AntlrClass getKlass() {
        return this.klass;
    }

    public AntlrUrl getUrlByContext(UrlDeclarationContext ctx) {
        return this.urlsByContext.get(ctx);
    }

    public void enterUrlDeclaration(@Nonnull AntlrUrl url) {
        AntlrUrl duplicate = this.urlsByContext.put(url.getElementContext(), url);
        if (duplicate != null) {
            throw new AssertionError();
        }

        this.urls.add(url);
    }

    public ListIterable<AntlrUrl> getUrls() {
        return this.urls.asUnmodifiable();
    }

    @Nonnull
    @Override
    public ServiceGroupDeclarationContext getElementContext() {
        return (ServiceGroupDeclarationContext) super.getElementContext();
    }

    // <editor-fold desc="Report Compiler Errors">
    @Override
    public void reportErrors(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        this.reportNoUrls(compilerAnnotationHolder);
        this.reportDuplicateUrls(compilerAnnotationHolder);
        this.reportForwardReference(compilerAnnotationHolder);

        for (AntlrUrl url : this.urls) {
            url.reportErrors(compilerAnnotationHolder);
        }

        if (this.klass == AntlrClass.NOT_FOUND) {
            this.reportTypeNotFound(compilerAnnotationHolder);
        }
    }

    @Override
    public void reportDuplicateTopLevelName(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        // Deliberately empty
    }

    private void reportTypeNotFound(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (this.klass != AntlrClass.NOT_FOUND) {
            return;
        }

        ClassReferenceContext reference = this.getElementContext().classReference();
        compilerAnnotationHolder.add(
            "ERR_SRG_TYP",
            String.format("Cannot find class '%s'", reference.getText()),
            this,
            reference
        );
    }

    private void reportDuplicateUrls(CompilerAnnotationHolder compilerAnnotationHolder) {
        HashBagWithHashingStrategy<AntlrUrl> antlrUrls = new HashBagWithHashingStrategy<>(
            HashingStrategies.fromFunction(AntlrUrl::getNormalizedPathSegments)
        );

        antlrUrls.addAll(this.urls);

        MutableBag<AntlrUrl> duplicateUrlMatches = antlrUrls.selectByOccurrences((occurrences) -> occurrences > 1);
        MutableList<AntlrUrl> duplicateUrls = this.urls.select(duplicateUrlMatches::contains);
        if (duplicateUrls.isEmpty()) {
            return;
        }

        for (AntlrUrl url : duplicateUrls) {
            String message = String.format(
                "Duplicate URL: '%s' in service group for class '%s'.",
                url.getElementContext().url().getText(),
                this.klass.getName()
            );

            compilerAnnotationHolder.add("ERR_DUP_URL", message, url, url.getElementContext().url());
        }
    }

    private void reportNoUrls(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        if (this.urls.isEmpty()) {
            String message = String.format(
                "Service group should declare at least one url: '%s'.",
                this.getElementContext().classReference().getText()
            );

            compilerAnnotationHolder.add("ERR_SER_EMP", message, this);
        }
    }

    public void reportDuplicateServiceGroupClass(@Nonnull CompilerAnnotationHolder compilerAnnotationHolder) {
        String message = String.format(
            "Multiple service groups for class: '%s.%s'.",
            this.klass.getPackageName(),
            this.klass.getName()
        );
        compilerAnnotationHolder.add("ERR_DUP_SVC", message, this);
    }

    private void reportForwardReference(CompilerAnnotationHolder compilerAnnotationHolder) {
        if (!this.isForwardReference(this.klass)) {
            return;
        }

        String message = String.format(
            "Service group '%s' is declared on line %d and has a forward reference to class '%s' which is declared later in the source file '%s' on line %d.",
            this,
            this.getElementContext().getStart().getLine(),
            this.klass.getName(),
            this.getCompilationUnit().get().getSourceName(),
            this.klass.getElementContext().getStart().getLine()
        );
        compilerAnnotationHolder.add("ERR_FWD_REF", message, this, this.getElementContext().classReference());
    }

    // </editor-fold>

    public ServiceGroupBuilder build() {
        if (this.serviceGroupBuilder != null) {
            throw new IllegalStateException();
        }

        this.serviceGroupBuilder = new ServiceGroupBuilder(
            (ServiceGroupDeclarationContext) this.elementContext,
            this.getMacroElementBuilder(),
            this.getSourceCodeBuilder(),
            this.ordinal,
            this.getNameContext(),
            this.getPackageName(),
            this.klass.getElementBuilder()
        );

        ImmutableList<UrlBuilder> urlBuilders = this.urls.collect(AntlrUrl::build).toImmutable();

        this.serviceGroupBuilder.setUrlBuilders(urlBuilders);
        return this.serviceGroupBuilder;
    }

    @Override
    public ServiceGroupBlockContext getBlockContext() {
        return this.getElementContext().serviceGroupBlock();
    }

    @Nonnull
    @Override
    public ServiceGroupBuilder getElementBuilder() {
        return this.serviceGroupBuilder;
    }
}

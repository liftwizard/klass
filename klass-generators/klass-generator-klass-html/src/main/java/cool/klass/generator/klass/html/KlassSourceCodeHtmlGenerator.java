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

package cool.klass.generator.klass.html;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import cool.klass.model.converter.compiler.token.categories.TokenCategory;
import cool.klass.model.meta.domain.api.EnumerationLiteral;
import cool.klass.model.meta.domain.api.NamedElement;
import cool.klass.model.meta.domain.api.TopLevelElement;
import cool.klass.model.meta.domain.api.property.Property;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import cool.klass.model.meta.domain.api.source.ElementWithSourceCode;
import cool.klass.model.meta.domain.api.source.SourceCode;
import cool.klass.model.meta.domain.api.source.TopLevelElementWithSourceCode;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Token;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.ListAdapter;

public class KlassSourceCodeHtmlGenerator {

    public static final Converter<String, String> CONVERTER = CaseFormat.UPPER_UNDERSCORE.converterTo(
        CaseFormat.LOWER_HYPHEN
    );

    private final DomainModelWithSourceCode domainModel;

    public KlassSourceCodeHtmlGenerator(DomainModelWithSourceCode domainModel) {
        this.domainModel = Objects.requireNonNull(domainModel);
    }

    public static String getSourceCode(
        Token token,
        DomainModelWithSourceCode domainModel,
        Optional<TopLevelElementWithSourceCode> topLevelElementToHighlight,
        Optional<String> memberNameToHighlight
    ) {
        Optional<TokenCategory> maybeTokenCategory = domainModel.getTokenCategory(token);
        Optional<ElementWithSourceCode> maybeElementByReference = domainModel.getElementByReference(token);
        Optional<ElementWithSourceCode> maybeElementByDeclaration = domainModel.getElementByDeclaration(token);

        String escapedText = StringEscapeUtils.escapeHtml4(token.getText());

        if (maybeTokenCategory.isEmpty() && maybeElementByReference.isEmpty() && maybeElementByDeclaration.isEmpty()) {
            return escapedText;
        }

        if (maybeTokenCategory.isEmpty()) {
            throw new AssertionError(token);
        }

        TokenCategory tokenCategory = maybeTokenCategory.get();

        if (maybeElementByReference.isEmpty() && maybeElementByDeclaration.isEmpty()) {
            return getSpan(escapedText, tokenCategory, false);
        }

        if (maybeElementByDeclaration.isPresent() && maybeElementByReference.isEmpty()) {
            ElementWithSourceCode element = maybeElementByDeclaration.get();

            String idForElement = KlassSourceCodeHtmlGenerator.getIdForElement(element);
            String linkForElement = KlassSourceCodeHtmlGenerator.getLinkForElement(element);
            String escapedName = StringEscapeUtils.escapeHtml4(((NamedElement) element).getName());

            String declarationAnchor = "<a id=\"%s\" href=\"%s\">%s</a>".formatted(
                idForElement,
                linkForElement,
                escapedName
            );

            boolean shouldHighlight = matchesElementToHighlight(
                maybeElementByDeclaration.get(),
                topLevelElementToHighlight,
                memberNameToHighlight
            );

            return getSpan(declarationAnchor, tokenCategory, shouldHighlight);
        }

        if (maybeElementByDeclaration.isEmpty() && maybeElementByReference.isPresent()) {
            String linkForElement = KlassSourceCodeHtmlGenerator.getLinkForElement(maybeElementByReference.get());
            String referenceAnchor = "<a href=\"%s\">%s</a>".formatted(linkForElement, escapedText);

            return getSpan(referenceAnchor, tokenCategory, false);
        }

        throw new AssertionError(token);
    }

    public static String getSourceCode(@Nonnull DomainModelWithSourceCode domainModel, @Nonnull SourceCode sourceCode) {
        return getSourceCode(domainModel, sourceCode, Optional.empty(), Optional.empty());
    }

    public static String getSourceCode(
        @Nonnull DomainModelWithSourceCode domainModel,
        @Nonnull SourceCode sourceCode,
        @Nonnull Optional<TopLevelElementWithSourceCode> topLevelElementToHighlight,
        @Nonnull Optional<String> memberNameToHighlight
    ) {
        Objects.requireNonNull(domainModel);
        Objects.requireNonNull(sourceCode);
        Objects.requireNonNull(topLevelElementToHighlight);
        Objects.requireNonNull(memberNameToHighlight);

        BufferedTokenStream tokenStream = sourceCode.getTokenStream();
        MutableList<Token> tokens = ListAdapter.adapt(tokenStream.getTokens());

        String body = tokens
            .reject(token -> token.getType() == Token.EOF)
            .collect(token -> getSourceCode(token, domainModel, topLevelElementToHighlight, memberNameToHighlight))
            .makeString("");

        // language=HTML
        String prefix = """
            <html>
            <head>
                <link rel='stylesheet' type='text/css' href='/static/css/light.css' media='(prefers-color-scheme: light)'>
                <link rel='stylesheet' type='text/css' href='/static/css/dark.css' media='(prefers-color-scheme: dark)'>
                <link rel='stylesheet' type='text/css' href='/static/css/slider.css'>
                <link rel='stylesheet' type='text/css' href='/static/css/klass-syntax.css'>
                <script type='module' src='https://unpkg.com/dark-mode-toggle'></script>
                <style>
                    :root {
                        font-family: "Lucida Console", Courier, monospace;
                        font-size: 16px;
                    }
                </style>
            </head>
            <body class='klass'>
            <aside>
                <dark-mode-toggle class='slider' legend='Dark Mode' appearance='toggle'></dark-mode-toggle>
            </aside>
            <pre>
            """;
        return prefix + body + "</pre>\n" + "</body>\n" + "</html>\n";
    }

    private static boolean matchesElementToHighlight(
        ElementWithSourceCode element,
        Optional<TopLevelElementWithSourceCode> topLevelElementToHighlight,
        Optional<String> memberNameToHighlight
    ) {
        if (element instanceof TopLevelElement && memberNameToHighlight.isPresent()) {
            return false;
        }
        if (memberNameToHighlight.isEmpty()) {
            return Optional.of(element).equals(topLevelElementToHighlight);
        }
        if (element instanceof Property property) {
            return (
                Optional.of(property.getName()).equals(memberNameToHighlight) &&
                Optional.of(property.getOwningClassifier()).equals(topLevelElementToHighlight)
            );
        }
        if (element instanceof EnumerationLiteral enumerationLiteral) {
            return (
                Optional.of(enumerationLiteral.getName()).equals(memberNameToHighlight) &&
                Optional.of(enumerationLiteral.getType()).equals(topLevelElementToHighlight)
            );
        }
        return false;
    }

    @Nonnull
    private static String getSpan(String text, TokenCategory tokenCategory, boolean matchesHighlight) {
        String tokenCategoryName = tokenCategory.name();
        String className = CONVERTER.convert(tokenCategoryName);
        String highlightClassName = matchesHighlight ? " highlight" : "";
        return "<span class='klass-" + className + highlightClassName + "'>" + text + "</span>";
    }

    private void writeHtmlFile(SourceCode sourceCode, Path outputPath) {
        Path htmlOutputPath = KlassSourceCodeHtmlGenerator.getOutputPath(outputPath, sourceCode);
        String sourceCodeText = getSourceCode(this.domainModel, sourceCode);
        KlassSourceCodeHtmlGenerator.printStringToFile(htmlOutputPath, sourceCodeText);
    }

    @Nonnull
    private static Path getOutputPath(@Nonnull Path outputPath, @Nonnull SourceCode sourceCode) {
        Path parentPath = KlassSourceCodeHtmlGenerator.getParentPath(sourceCode);
        Path absoluteParentPath = outputPath.resolve(parentPath);
        Objects.requireNonNull(absoluteParentPath);
        File absoluteParentFile = absoluteParentPath.toFile();
        Objects.requireNonNull(absoluteParentFile);
        absoluteParentFile.mkdirs();

        String fileName = sourceCode.getSourceName() + ".html";

        return outputPath.resolve(fileName);
    }

    private static Path getParentPath(SourceCode sourceCode) {
        return sourceCode
            .getMacroSourceCode()
            .map(KlassSourceCodeHtmlGenerator::getParentPath)
            .orElseGet(() -> KlassSourceCodeHtmlGenerator.getRootParentPath(sourceCode));
    }

    private static Path getRootParentPath(SourceCode sourceCode) {
        String sourceName = sourceCode.getSourceName();
        File sourceFile = new File(sourceName);
        return sourceFile.toPath().getParent();
    }

    private static void printStringToFile(@Nonnull Path path, String contents) {
        try (
            PrintStream printStream = new PrintStream(new FileOutputStream(path.toFile()), true, StandardCharsets.UTF_8)
        ) {
            printStream.print(contents);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeHtmlFiles(@Nonnull Path outputPath) {
        this.domainModel.getSourceCodes()
            // TODO: Graft in macros
            .select(sourceCode -> sourceCode.getMacroSourceCode().isEmpty())
            .forEachWith(this::writeHtmlFile, outputPath);
    }

    @Nullable
    private static String getIdForElement(ElementWithSourceCode value) {
        if (value instanceof TopLevelElement topLevelElement) {
            return StringEscapeUtils.escapeHtml4(topLevelElement.getName());
        }
        if (value instanceof Property property) {
            return StringEscapeUtils.escapeHtml4(property.getOwningClassifier().getName() + "." + property.getName());
        }
        if (value instanceof EnumerationLiteral enumerationLiteral) {
            return StringEscapeUtils.escapeHtml4(
                enumerationLiteral.getType().getName() + "." + enumerationLiteral.getName()
            );
        }
        throw new AssertionError(value);
    }

    @Nonnull
    private static String getLinkForElement(ElementWithSourceCode element) {
        if (element instanceof TopLevelElement topLevelElement) {
            return String.format("/api/meta/code/element/%s#%s", topLevelElement.getName(), topLevelElement.getName());
        }
        if (element instanceof Property property) {
            return MessageFormat.format(
                "/api/meta/code/element/{0}/{1}#{0}.{1}",
                property.getOwningClassifier().getName(),
                property.getName()
            );
        }
        if (element instanceof EnumerationLiteral enumerationLiteral) {
            return MessageFormat.format(
                "/api/meta/code/element/{0}/{1}#{0}.{1}",
                enumerationLiteral.getType().getName(),
                enumerationLiteral.getName()
            );
        }
        throw new AssertionError(element);
    }
}

package cool.klass.model.converter.compiler.syntax.highlighter.ansi.scheme;

import org.fusesource.jansi.Ansi;

public interface AnsiColorScheme
{
    void background(Ansi ansi);

    void blockComment(Ansi ansi);

    default void lineComment(Ansi ansi)
    {
        this.blockComment(ansi);
    }

    void keyword(Ansi ansi);

    default void packageKeyword(Ansi ansi)
    {
        this.keyword(ansi);
    }

    default void primitiveType(Ansi ansi)
    {
        this.keyword(ansi);
    }

    default void verb(Ansi ansi)
    {
        this.keyword(ansi);
    }

    default void modifier(Ansi ansi)
    {
        this.keyword(ansi);
    }

    default void classifierModifier(Ansi ansi)
    {
        this.modifier(ansi);
    }

    default void dataTypePropertyModifier(Ansi ansi)
    {
        this.modifier(ansi);
    }

    default void associationEndModifier(Ansi ansi)
    {
        this.modifier(ansi);
    }

    default void parameterizedPropertyModifier(Ansi ansi)
    {
        this.modifier(ansi);
    }

    default void parameterModifier(Ansi ansi)
    {
        this.modifier(ansi);
    }

    default void validationModifier(Ansi ansi)
    {
        this.modifier(ansi);
    }

    default void serviceCategoryModifier(Ansi ansi)
    {
        this.modifier(ansi);
    }

    void identifier(Ansi ansi);

    default void packageName(Ansi ansi)
    {
        this.identifier(ansi);
    }

    default void topLevelElementName(Ansi ansi)
    {
        this.identifier(ansi);
    }

    default void enumerationName(Ansi ansi)
    {
        this.topLevelElementName(ansi);
    }

    default void classifierName(Ansi ansi)
    {
        this.topLevelElementName(ansi);
    }

    default void className(Ansi ansi)
    {
        this.classifierName(ansi);
    }

    default void interfaceName(Ansi ansi)
    {
        this.classifierName(ansi);
    }

    default void associationName(Ansi ansi)
    {
        this.topLevelElementName(ansi);
    }

    default void projectionName(Ansi ansi)
    {
        this.topLevelElementName(ansi);
    }

    default void serviceName(Ansi ansi)
    {
        this.topLevelElementName(ansi);
    }

    default void enumerationLiteralName(Ansi ansi)
    {
        this.identifier(ansi);
    }

    default void parameterName(Ansi ansi)
    {
        this.identifier(ansi);
    }

    default void propertyName(Ansi ansi)
    {
        this.identifier(ansi);
    }

    default void dataTypePropertyName(Ansi ansi)
    {
        this.propertyName(ansi);
    }

    default void primitivePropertyName(Ansi ansi)
    {
        this.dataTypePropertyName(ansi);
    }

    default void enumerationPropertyName(Ansi ansi)
    {
        this.dataTypePropertyName(ansi);
    }

    default void referencePropertyName(Ansi ansi)
    {
        this.propertyName(ansi);
    }

    default void parameterizedPropertyName(Ansi ansi)
    {
        this.referencePropertyName(ansi);
    }

    default void associationEndName(Ansi ansi)
    {
        this.referencePropertyName(ansi);
    }

    default void enumerationReference(Ansi ansi)
    {
        this.enumerationName(ansi);
    }

    default void classifierReference(Ansi ansi)
    {
        this.classifierName(ansi);
    }

    default void interfaceReference(Ansi ansi)
    {
        this.interfaceName(ansi);
    }

    default void classReference(Ansi ansi)
    {
        this.className(ansi);
    }

    default void projectionReference(Ansi ansi)
    {
        this.projectionName(ansi);
    }

    default void dataTypePropertyReference(Ansi ansi)
    {
        this.propertyName(ansi);
    }

    default void associationEndReference(Ansi ansi)
    {
        this.associationEndName(ansi);
    }

    default void parameterizedPropertyReference(Ansi ansi)
    {
        this.parameterizedPropertyName(ansi);
    }

    default void propertyReference(Ansi ansi)
    {
        this.propertyName(ansi);
    }

    default void parameterReference(Ansi ansi)
    {
        this.parameterName(ansi);
    }

    void literal(Ansi ansi);

    default void stringLiteral(Ansi ansi)
    {
        this.literal(ansi);
    }

    default void integerLiteral(Ansi ansi)
    {
        this.literal(ansi);
    }

    default void booleanLiteral(Ansi ansi)
    {
        this.literal(ansi);
    }

    default void characterLiteral(Ansi ansi)
    {
        this.literal(ansi);
    }

    default void floatingPointLiteral(Ansi ansi)
    {
        this.literal(ansi);
    }

    default void literalThis(Ansi ansi)
    {
        this.literal(ansi);
    }

    default void literalNative(Ansi ansi)
    {
        this.literal(ansi);
    }

    void punctuation(Ansi ansi);

    default void comma(Ansi ansi)
    {
        this.punctuation(ansi);
    }

    default void dot(Ansi ansi)
    {
        this.punctuation(ansi);
    }

    default void dotDot(Ansi ansi)
    {
        this.dot(ansi);
    }

    default void semi(Ansi ansi)
    {
        this.punctuation(ansi);
    }

    default void operator(Ansi ansi)
    {
        this.punctuation(ansi);
    }

    default void urlConstant(Ansi ansi)
    {
        this.identifier(ansi);
    }
}

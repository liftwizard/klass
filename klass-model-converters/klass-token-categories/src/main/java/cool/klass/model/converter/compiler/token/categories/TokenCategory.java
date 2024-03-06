package cool.klass.model.converter.compiler.token.categories;

public enum TokenCategory
{
    // Comments
    COMMENT(null),
    BLOCK_COMMENT(COMMENT),
    LINE_COMMENT(COMMENT),

    // Keywords
    KEYWORD(null),
    PACKAGE_KEYWORD(KEYWORD),
    KEYWORD_ENUMERATION(KEYWORD),
    KEYWORD_INTERFACE(KEYWORD),
    KEYWORD_USER(KEYWORD),
    KEYWORD_CLASS(KEYWORD),
    KEYWORD_PROJECTION(KEYWORD),
    KEYWORD_SERVICE(KEYWORD),
    KEYWORD_ABSTRACT(KEYWORD),
    KEYWORD_EXTENDS(KEYWORD),
    KEYWORD_IMPLEMENTS(KEYWORD),
    KEYWORD_INHERITANCE_TYPE(KEYWORD),
    KEYWORD_ASSOCIATION(KEYWORD),
    KEYWORD_RELATIONSHIP(KEYWORD),
    KEYWORD_ORDER_BY(KEYWORD),
    KEYWORD_ORDER_BY_DIRECTION(KEYWORD),
    KEYWORD_ON(KEYWORD),
    KEYWORD_MULTIPLICITY(KEYWORD),
    KEYWORD_MULTIPLICITY_CHOICE(KEYWORD_MULTIPLICITY),
    KEYWORD_SERVICE_CRITERIA(KEYWORD_MULTIPLICITY),

    PRIMITIVE_TYPE(KEYWORD),

    VERB(KEYWORD),
    VERB_GET(VERB),
    VERB_POST(VERB),
    VERB_PUT(VERB),
    VERB_PATCH(VERB),
    VERB_DELETE(VERB),

    // Modifiers
    MODIFIER(KEYWORD),
    CLASSIFIER_MODIFIER(MODIFIER),
    DATA_TYPE_PROPERTY_MODIFIER(MODIFIER),
    ASSOCIATION_END_MODIFIER(MODIFIER),
    PARAMETERIZED_PROPERTY_MODIFIER(MODIFIER),
    PARAMETER_MODIFIER(MODIFIER),
    VALIDATION_MODIFIER(MODIFIER),
    SERVICE_CATEGORY_MODIFIER(MODIFIER),

    // Identifiers
    IDENTIFIER(null),
    PACKAGE_NAME(IDENTIFIER),
    TOP_LEVEL_ELEMENT_NAME(IDENTIFIER),
    ENUMERATION_NAME(TOP_LEVEL_ELEMENT_NAME),
    CLASSIFIER_NAME(TOP_LEVEL_ELEMENT_NAME),
    INTERFACE_NAME(CLASSIFIER_NAME),
    CLASS_NAME(CLASSIFIER_NAME),
    ASSOCIATION_NAME(TOP_LEVEL_ELEMENT_NAME),
    PROJECTION_NAME(TOP_LEVEL_ELEMENT_NAME),
    ENUMERATION_LITERAL_NAME(IDENTIFIER),
    PARAMETER_NAME(IDENTIFIER),
    PROPERTY_NAME(IDENTIFIER),
    DATA_TYPE_PROPERTY_NAME(PROPERTY_NAME),
    PRIMITIVE_PROPERTY_NAME(DATA_TYPE_PROPERTY_NAME),
    ENUMERATION_PROPERTY_NAME(DATA_TYPE_PROPERTY_NAME),
    REFERENCE_PROPERTY_NAME(PROPERTY_NAME),
    PARAMETERIZED_PROPERTY_NAME(REFERENCE_PROPERTY_NAME),
    ASSOCIATION_END_NAME(REFERENCE_PROPERTY_NAME),

    // References
    ENUMERATION_REFERENCE(ENUMERATION_NAME),
    INTERFACE_REFERENCE(INTERFACE_NAME),
    // TODO: Split separate tokens for CLASS and CLASSIFIER
    CLASS_REFERENCE(CLASS_NAME),
    PROJECTION_REFERENCE(PROJECTION_NAME),
    DATA_TYPE_PROPERTY_REFERENCE(DATA_TYPE_PROPERTY_NAME),
    ASSOCIATION_END_REFERENCE(ASSOCIATION_END_NAME),
    PARAMETERIZED_PROPERTY_REFERENCE(PARAMETERIZED_PROPERTY_NAME),
    PROPERTY_REFERENCE(PROPERTY_NAME),
    PARAMETER_REFERENCE(PARAMETER_NAME),

    // Literals
    LITERAL(KEYWORD),
    LITERAL_THIS(KEYWORD),
    LITERAL_NATIVE(KEYWORD),
    STRING_LITERAL(LITERAL),
    INTEGER_LITERAL(LITERAL),
    BOOLEAN_LITERAL(LITERAL),
    CHARACTER_LITERAL(LITERAL),
    FLOATING_POINT_LITERAL(LITERAL),
    ASTERISK_LITERAL(INTEGER_LITERAL),

    // Punctuation
    PUNCTUATION(null),
    COMMA(PUNCTUATION),
    DOT(PUNCTUATION),
    DOTDOT(PUNCTUATION),
    SEMICOLON(PUNCTUATION),
    OPERATOR(PUNCTUATION),
    // Punctuation Operators
    OPERATOR_EQ(OPERATOR),
    OPERATOR_NE(OPERATOR),
    OPERATOR_LT(OPERATOR),
    OPERATOR_GT(OPERATOR),
    OPERATOR_LE(OPERATOR),
    OPERATOR_GE(OPERATOR),
    // Word Operators
    WORD_OPERATOR(KEYWORD),
    OPERATOR_IN(WORD_OPERATOR),
    OPERATOR_STRING(WORD_OPERATOR),

    COLON(PUNCTUATION),
    SLASH(PUNCTUATION),
    QUESTION(PUNCTUATION),

    PAIRED_PUNCTUATION(PUNCTUATION),
    PARENTHESES(PAIRED_PUNCTUATION),
    PARENTHESIS_LEFT(PARENTHESES),
    PARENTHESIS_RIGHT(PARENTHESES),
    CURLY_BRACES(PAIRED_PUNCTUATION),
    CURLY_LEFT(CURLY_BRACES),
    CURLY_RIGHT(CURLY_BRACES),
    SQUARE_BRACKETS(PAIRED_PUNCTUATION),
    SQUARE_BRACKET_LEFT(SQUARE_BRACKETS),
    SQUARE_BRACKET_RIGHT(SQUARE_BRACKETS),

    URL_CONSTANT(IDENTIFIER);

    private final TokenCategory parentCategory;

    TokenCategory(TokenCategory parentCategory)
    {
        this.parentCategory = parentCategory;
    }

    public TokenCategory getParentCategory()
    {
        return this.parentCategory;
    }
}

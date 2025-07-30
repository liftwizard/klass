lexer grammar KlassLexerRules;

import JavaLexer;

@lexer::members {
    public static final int WHITESPACE_CHANNEL = 1000;
    public static final int COMMENTS_CHANNEL = 2000;
    public static final int LINE_COMMENTS_CHANNEL = 3000;
}

KEYWORD_PACKAGE           : 'package';
KEYWORD_ENUMERATION       : 'enumeration';
KEYWORD_INTERFACE         : 'interface';
KEYWORD_CLASS             : 'class';
KEYWORD_ASSOCIATION       : 'association';
KEYWORD_PROJECTION        : 'projection';
KEYWORD_SERVICE           : 'service';
KEYWORD_USER              : 'user';
KEYWORD_NATIVE            : 'native';
KEYWORD_RELATIONSHIP      : 'relationship';
KEYWORD_MULTIPLICITY      : 'multiplicity';
KEYWORD_ORDER_BY          : 'orderBy';
KEYWORD_ASCENDING         : 'ascending';
KEYWORD_DESCENDING        : 'descending';
KEYWORD_CRITERIA          : 'criteria';
KEYWORD_ON                : 'on';
KEYWORD_ABSTRACT          : 'abstract';
KEYWORD_EXTENDS           : 'extends';
KEYWORD_IMPLEMENTS        : 'implements';
KEYWORD_ALL               : 'all';
KEYWORD_EQUALS_EDGE_POINT : 'equalsEdgePoint';

KEYWORD_TABLE_PER_SUBCLASS       : 'table-per-subclass';
KEYWORD_TABLE_FOR_ALL_SUBCLASSES : 'table-for-all-subclasses';
KEYWORD_TABLE_PER_CLASS          : 'table-per-class';

PRIMITIVE_TYPE_BOOLEAN          : 'Boolean';
PRIMITIVE_TYPE_INTEGER          : 'Integer';
PRIMITIVE_TYPE_LONG             : 'Long';
PRIMITIVE_TYPE_DOUBLE           : 'Double';
PRIMITIVE_TYPE_FLOAT            : 'Float';
PRIMITIVE_TYPE_STRING           : 'String';
PRIMITIVE_TYPE_INSTANT          : 'Instant';
PRIMITIVE_TYPE_LOCAL_DATE       : 'LocalDate';
PRIMITIVE_TYPE_TEMPORAL_INSTANT : 'TemporalInstant';
PRIMITIVE_TYPE_TEMPORAL_RANGE   : 'TemporalRange';

MODIFIER_CLASSIFIER_SYSTEM_TEMPORAL       : 'systemTemporal';
MODIFIER_CLASSIFIER_VALID_TEMPORAL        : 'validTemporal';
MODIFIER_CLASSIFIER_BITEMPORAL            : 'bitemporal';
MODIFIER_CLASSIFIER_VERSIONED             : 'versioned';
MODIFIER_CLASSIFIER_AUDITED               : 'audited';
MODIFIER_CLASSIFIER_TRANSIENT             : 'transient';

MODIFIER_PROPERTY_KEY             : 'key';
MODIFIER_PROPERTY_PRIVATE         : 'private';
MODIFIER_PROPERTY_VALID           : 'valid';
MODIFIER_PROPERTY_SYSTEM          : 'system';
MODIFIER_PROPERTY_FROM            : 'from';
MODIFIER_PROPERTY_TO              : 'to';
MODIFIER_PROPERTY_CREATED_BY      : 'createdBy';
MODIFIER_PROPERTY_CREATED_ON      : 'createdOn';
MODIFIER_PROPERTY_LAST_UPDATED_BY : 'lastUpdatedBy';
MODIFIER_PROPERTY_DERIVED         : 'derived';

MODIFIER_ASSOCIATION_END_OWNED   : 'owned';
MODIFIER_ASSOCIATION_END_FINAL   : 'final';

// other modifiers (property AND parameter shared)
MODIFIER_VERSION : 'version';
MODIFIER_USER_ID : 'userId';
MODIFIER_ID      : 'id';

VALIDATION_MIN_LENGTH     : 'minLength';
VALIDATION_MINIMUM_LENGTH : 'minimumLength';
VALIDATION_MAX_LENGTH     : 'maxLength';
VALIDATION_MAXIMUM_LENGTH : 'maximumLength';
VALIDATION_MIN            : 'min';
VALIDATION_MINIMUM        : 'minimum';
VALIDATION_MAX            : 'max';
VALIDATION_MAXIMUM        : 'maximum';

PUNCTUATION_LPAREN : '(';
PUNCTUATION_RPAREN : ')';
PUNCTUATION_LBRACE : '{';
PUNCTUATION_RBRACE : '}';
PUNCTUATION_LBRACK : '[';
PUNCTUATION_RBRACK : ']';
PUNCTUATION_SEMI   : ';';
PUNCTUATION_COLON  : ':';
PUNCTUATION_COMMA  : ',';
PUNCTUATION_DOT    : '.';
PUNCTUATION_DOTDOT : '..';
PUNCTUATION_SLASH  : '/';
PUNCTUATION_ASTERISK: '*';
PUNCTUATION_QUESTION: '?';
PUNCTUATION_AMPERSAND: '&';

VERB_GET    : 'GET';
VERB_POST   : 'POST';
VERB_PUT    : 'PUT';
VERB_PATCH  : 'PATCH';
VERB_DELETE : 'DELETE';

LITERAL_NULL  : 'null';
LITERAL_THIS  : 'this';

OPERATOR_EQUALS                : '==';
OPERATOR_NOT_EQUALS            : '!=';
OPERATOR_LESS_THAN             : '<';
OPERATOR_GREATER_THAN          : '>';
OPERATOR_LESS_THAN_OR_EQUAL    : '<=';
OPERATOR_GREATER_THAN_OR_EQUAL : '>=';
OPERATOR_IN                    : 'in';
OPERATOR_CONTAINS              : 'contains';
OPERATOR_STARTS_WITH           : 'startsWith';
OPERATOR_ENDS_WITH             : 'endsWith';
OPERATOR_AND                   : '&&';
OPERATOR_OR                    : '||';

// Channels
WHITESPACE  : [ \t\u000C]+  -> channel(1000);
NEWLINE     : [\r\n]        -> channel(1000);
COMMENT     : '/*' .*? '*/' -> channel(2000);
LINE_COMMENT: '//' ~[\r\n]* -> channel(3000);

// URL identifiers that contain dashes (e.g., /api/user-profile)
// Must contain at least one dash; regular Identifier handles non-dash cases
UrlIdentifier: [a-zA-Z] [a-zA-Z0-9_]* '-' [a-zA-Z0-9_-]*;

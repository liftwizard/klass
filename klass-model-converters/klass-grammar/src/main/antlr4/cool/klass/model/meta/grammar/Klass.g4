grammar Klass;

import KlassLexerRules;

compilationUnit: packageDeclaration topLevelDeclaration* EOF;
urlParameterDeclarationEOF: urlParameterDeclaration EOF;

packageDeclaration: 'package' packageName;

packageName
    :    identifier
    |    packageName '.' identifier
    ;

topLevelDeclaration
    : interfaceDeclaration
    | classDeclaration
    | enumerationDeclaration
    | associationDeclaration
    | projectionDeclaration
    | serviceGroupDeclaration
    ;

// TODO: Consider splitting separate interfaceModifiers from classifierModifiers
// interface
interfaceDeclaration: interfaceHeader interfaceBlock;
interfaceHeader : 'interface' identifier implementsDeclaration? classifierModifier* ;
interfaceBlock: '{' interfaceBody '}';
interfaceBody: interfaceMember*;

// class
classDeclaration: classHeader classBlock;
classHeader : classOrUser identifier abstractDeclaration? extendsDeclaration? implementsDeclaration? classServiceModifier* classifierModifier* ;
classOrUser: 'class' | 'user';
classServiceModifier: serviceCategoryModifier ('(' projectionReference ')')?;
serviceCategoryModifier: 'read' | 'write' | 'create' | 'update' | 'delete';
classBlock: '{' classBody '}';
classBody: classMember*;

// inheritance
abstractDeclaration: 'abstract';
extendsDeclaration: 'extends' classReference;
implementsDeclaration: 'implements' interfaceReference (',' interfaceReference)*;

// enumeration
enumerationDeclaration: 'enumeration' identifier enumerationBlock;
enumerationBlock: '{' enumerationBody '}';
enumerationBody: enumerationLiteral*;
enumerationLiteral: identifier ('(' enumerationPrettyName ')')? ',';
enumerationPrettyName: StringLiteral;

// association
associationDeclaration: 'association' identifier associationBlock;
associationBlock: '{' associationBody '}';
associationBody: associationEnd? associationEnd? relationship? ;
associationEnd
    : identifier ':' classReference multiplicity associationEndModifier* orderByDeclaration? ';'
    | identifier ':' classReference multiplicity associationEndModifier* orderByDeclaration? {notifyErrorListeners("Missing semi-colon after association end declaration.");};
associationEndSignature: identifier ':' classifierReference multiplicity associationEndModifier* ';';
relationship: 'relationship' criteriaExpression;

// projection
projectionDeclaration: 'projection' identifier (parameterDeclarationList)? 'on' classifierReference projectionBlock;
projectionBlock: '{' projectionBody '}';
projectionBody: projectionMember*;
projectionMember: projectionPrimitiveMember | projectionReferenceProperty | projectionParameterizedProperty | projectionProjectionReference;
// TODO: Rename projectionPrimitiveMember --> projectionDataTypeMember
projectionPrimitiveMember: (classifierReference '.')? identifier ':' header ',';
projectionReferenceProperty: (classifierReference '.')? identifier ':' projectionBlock ',';
projectionProjectionReference: (classifierReference '.')? identifier ':' projectionReference ',';
projectionParameterizedProperty: (classifierReference '.')? identifier argumentList ':' projectionBlock ',';
header: StringLiteral;

// service
serviceGroupDeclaration: 'service' identifier 'on' classReference serviceGroupBlock;
serviceGroupBlock : '{' serviceGroupBody '}' ;
serviceGroupBody: urlDeclaration*;
// url
urlDeclaration: url serviceDeclaration+;
url: urlPathSegment+ '/'? queryParameterList?;
urlPathSegment: '/' (urlConstant | urlParameterDeclaration);
urlConstant: identifier | UrlIdentifier;
queryParameterList: '?' urlParameterDeclaration ('&' urlParameterDeclaration)*;
urlParameterDeclaration: '{' parameterDeclaration '}';
// service
serviceDeclaration: verb serviceBlock;
serviceBlock : '{' serviceBody '}' ;
serviceBody: serviceMultiplicityDeclaration? serviceCriteriaDeclaration* serviceProjectionDispatch? serviceOrderByDeclaration? ;
serviceMultiplicityDeclaration: 'multiplicity' ':' serviceMultiplicity ';'
    | 'multiplicity' ':' serviceMultiplicity {notifyErrorListeners("Missing semi-colon after service multiplicity declaration.");};
serviceMultiplicity: one='one' | many='many';
serviceCriteriaDeclaration: serviceCriteriaKeyword ':' criteriaExpression ';'
    | serviceCriteriaKeyword ':' criteriaExpression {notifyErrorListeners("Missing semi-colon after service criteria declaration.");};
// TODO: Optional criteria
serviceCriteriaKeyword: 'criteria' | 'authorize' | 'validate' | 'conflict';
serviceProjectionDispatch: 'projection' ':' projectionReference argumentList? ';'
    | 'projection' ':' projectionReference argumentList? {notifyErrorListeners("Missing semi-colon after service projection dispatch.");};
serviceOrderByDeclaration: orderByDeclaration ';'
    | orderByDeclaration {notifyErrorListeners("Missing semi-colon after service order-by declaration.");};
verb: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

// member
interfaceMember: dataTypeProperty | parameterizedPropertySignature | associationEndSignature;
classMember: dataTypeProperty | parameterizedProperty | associationEndSignature;
dataTypeProperty: primitiveProperty | enumerationProperty;
primitiveProperty: identifier ':' primitiveType optionalMarker? dataTypePropertyModifier* dataTypePropertyValidation* ';'
    | identifier ':' primitiveType optionalMarker? dataTypePropertyModifier* dataTypePropertyValidation* {notifyErrorListeners("Missing semi-colon after primitive property declaration.");};
enumerationProperty: identifier ':' enumerationReference optionalMarker? dataTypePropertyModifier* dataTypePropertyValidation* ';'
    | identifier ':' enumerationReference optionalMarker? dataTypePropertyModifier* dataTypePropertyValidation* {notifyErrorListeners("Missing semi-colon after enumeration property declaration.");};
parameterizedProperty: identifier '(' (parameterDeclaration (',' parameterDeclaration)*)? ')' ':' classReference multiplicity parameterizedPropertyModifier* orderByDeclaration? '{' criteriaExpression '}';
parameterizedPropertySignature: identifier '(' (parameterDeclaration (',' parameterDeclaration)*)? ')' ':' classifierReference multiplicity parameterizedPropertyModifier* ';';
optionalMarker: '?';

dataTypePropertyValidation
    : minLengthValidation
    | maxLengthValidation
    | minValidation
    | maxValidation
    ;

minLengthValidation: minLengthValidationKeyword integerValidationParameter;
maxLengthValidation: maxLengthValidationKeyword integerValidationParameter;
minValidation: minValidationKeyword integerValidationParameter;
maxValidation: maxValidationKeyword integerValidationParameter;

integerValidationParameter : '(' integerLiteral ')' ;

minLengthValidationKeyword : ('minLength' | 'minimumLength') ;
maxLengthValidationKeyword : ('maxLength' | 'maximumLength') ;
minValidationKeyword : ('min' | 'minimum') ;
maxValidationKeyword : ('max' | 'maximum') ;

// parameter
parameterDeclaration: primitiveParameterDeclaration | enumerationParameterDeclaration | invalidParameterDeclaration;
primitiveParameterDeclaration: identifier ':' primitiveType multiplicity parameterModifier*;
enumerationParameterDeclaration: identifier ':' enumerationReference multiplicity parameterModifier*;
invalidParameterDeclaration: identifier {notifyErrorListeners("Missing type after parameter declaration.");};
parameterDeclarationList: '(' parameterDeclaration (',' parameterDeclaration)* ')';

// argument
argumentList: '(' (argument (',' argument)*)? ')';
argument
    : literal
    | literalList
    | nativeLiteral
    | parameterReference
    ;

// multiplicity
multiplicity: '[' multiplicityBody ']';
multiplicityBody: lowerBound=IntegerLiteral '..' upperBound=(IntegerLiteral | '*');

primitiveType: 'Boolean' | 'Integer' | 'Long' | 'Double' | 'Float' | 'String' | 'Instant' | 'LocalDate' | 'TemporalInstant' | 'TemporalRange';

// modifiers
classifierModifier: 'systemTemporal' | 'validTemporal' | 'bitemporal' | 'versioned' | 'audited' | 'transient'
    | identifier {notifyErrorListeners("Unrecognized Classifier modifier: " + _input.LT(-1).getText());};
dataTypePropertyModifier: 'key' | 'private' | 'userId' | 'id' | 'valid' | 'system' | 'from' | 'to' | 'createdBy' | 'createdOn' | 'lastUpdatedBy' | 'version' | 'derived' | 'final'
    | identifier {notifyErrorListeners("Unrecognized DataTypeProperty modifier: " + _input.LT(-1).getText());};
associationEndModifier: 'owned' | 'final' | 'version' | 'private' | 'createdBy' | 'lastUpdatedBy'
    | identifier {notifyErrorListeners("Unrecognized AssociationEnd modifier: " + _input.LT(-1).getText());};
parameterizedPropertyModifier: 'createdBy' | 'lastUpdatedBy'
    | identifier {notifyErrorListeners("Unrecognized ParameterizedProperty modifier: " + _input.LT(-1).getText());};
parameterModifier: 'version' | 'userId' | 'id'
    | identifier {notifyErrorListeners("Unrecognized Parameter modifier: " + _input.LT(-1).getText());};

// order by
orderByDeclaration: 'orderBy' ':' orderByMemberReferencePath (',' orderByMemberReferencePath)*;
orderByMemberReferencePath: thisMemberReferencePath orderByDirection?;
orderByDirection: 'ascending' | 'descending';

// criteria
criteriaExpression
    : left=criteriaExpression '&&' right=criteriaExpression  #CriteriaExpressionAnd
    | left=criteriaExpression '||' right=criteriaExpression  #CriteriaExpressionOr
    | '(' criteriaExpression ')'                             #CriteriaExpressionGroup
    | 'all'                                                  #CriteriaAll
    | source=expressionValue operator target=expressionValue #CriteriaOperator
    | expressionMemberReference 'equalsEdgePoint'            #CriteriaEdgePoint
    | 'native' '(' identifier ')'                            #CriteriaNative
    ;
expressionValue
    : literal
    | literalList
    | thisMemberReferencePath
    | typeMemberReferencePath
    | nativeLiteral
    | parameterReference
    ;
expressionMemberReference: thisMemberReferencePath | typeMemberReferencePath;
literalList: '(' literal (',' literal)* ')';
nativeLiteral: 'user';
operator
    : equalityOperator
    | inequalityOperator
    | inOperator
    | stringOperator
    ;
equalityOperator: '==' | '!=';
inequalityOperator: '<' | '>' | '<=' | '>=';
inOperator: 'in';
stringOperator: 'contains' | 'startsWith' | 'endsWith';

// references
interfaceReference: identifier;
classReference: identifier;
classifierReference: identifier;
enumerationReference: identifier;
projectionReference: identifier;
memberReference: identifier;
associationEndReference: identifier;
parameterReference: identifier;

thisMemberReferencePath: 'this' ('.' associationEndReference)* '.' memberReference;
typeMemberReferencePath: classReference ('.' associationEndReference)* '.' memberReference;

identifier
    : Identifier
    | keywordValidAsIdentifier
    ;

keywordValidAsIdentifier
    : 'package'
    | 'enumeration' | 'interface' | 'class' | 'association' | 'projection' | 'service' | 'user'
    | 'abstract' | 'extends' | 'implements'
    | 'native'
    | 'relationship'
    | 'multiplicity' | 'orderBy'
    | 'criteria' | 'authorize' | 'validate' | 'conflict'
    // classifierModifier
    | 'systemTemporal' | 'validTemporal' | 'bitemporal' | 'versioned' | 'audited' | 'transient'
    // dataTypePropertyModifier
    | 'key' | 'private' | 'userId' | 'id' | 'valid' | 'system' | 'from' | 'to' | 'createdBy' | 'createdOn' | 'lastUpdatedBy' | 'version' | 'derived'
    // associationEndModifier
    | 'owned' | 'final' | 'version'
    // parameterizedPropertyModifier
    | 'createdBy' | 'lastUpdatedBy'
    // parameterModifier
    | 'version' | 'userId' | 'id'
    | 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
    | serviceCategoryModifier
    | inOperator | stringOperator
    // TODO: Split these primitive type keywords out, since they're really only ok as enumeration literals
    | primitiveType
    ;

literal
    :    integerLiteral
    |    floatingPointLiteral
    |    booleanLiteral
    // TODO: Character Reladomo types and character literals?
    |    characterLiteral
    |    stringLiteral
    |    nullLiteral
    ;

integerLiteral       : IntegerLiteral;
floatingPointLiteral : FloatingPointLiteral;
booleanLiteral       : BooleanLiteral;
characterLiteral     : CharacterLiteral;
stringLiteral        : StringLiteral;
nullLiteral          : LITERAL_NULL;

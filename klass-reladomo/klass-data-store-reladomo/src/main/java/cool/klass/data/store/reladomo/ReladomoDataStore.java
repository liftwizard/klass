package cool.klass.data.store.reladomo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.gs.fw.common.mithra.MithraDatedTransactionalObject;
import com.gs.fw.common.mithra.MithraList;
import com.gs.fw.common.mithra.MithraManagerProvider;
import com.gs.fw.common.mithra.MithraObject;
import com.gs.fw.common.mithra.MithraTransaction;
import com.gs.fw.common.mithra.MithraTransactionalObject;
import com.gs.fw.common.mithra.attribute.AsOfAttribute;
import com.gs.fw.common.mithra.attribute.Attribute;
import com.gs.fw.common.mithra.attribute.TimestampAttribute;
import com.gs.fw.common.mithra.finder.AbstractRelatedFinder;
import com.gs.fw.common.mithra.finder.Operation;
import com.gs.fw.common.mithra.finder.RelatedFinder;
import com.gs.fw.common.mithra.util.DefaultInfinityTimestamp;
import com.gs.fw.finder.TransactionalDomainList;
import cool.klass.data.store.DataStore;
import cool.klass.data.store.Transaction;
import cool.klass.data.store.TransactionalCommand;
import cool.klass.model.meta.domain.api.Classifier;
import cool.klass.model.meta.domain.api.Enumeration;
import cool.klass.model.meta.domain.api.EnumerationLiteral;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import cool.klass.model.meta.domain.api.property.EnumerationProperty;
import cool.klass.model.meta.domain.api.property.PrimitiveProperty;
import cool.klass.model.meta.domain.api.property.Property;
import cool.klass.model.meta.domain.api.property.ReferenceProperty;
import cool.klass.model.meta.domain.api.visitor.AssertObjectMatchesDataTypePropertyVisitor;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.OrderedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

// TODO: Refactor this whole thing to use generated getters/setters instead of Reladomo Attribute
public class ReladomoDataStore
        implements DataStore
{
    private static final Marker MARKER = MarkerFactory.getMarker("reladomo transaction stats");
    private static final Logger LOGGER = LoggerFactory.getLogger(ReladomoDataStore.class);

    private static final Converter<String, String> LOWER_TO_UPPER_CAMEL =
            CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_CAMEL);

    private static final Converter<String, String> UPPER_TO_LOWER_CAMEL =
            CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL);

    private final Supplier<UUID> uuidSupplier;
    private final int            retryCount;

    public ReladomoDataStore(@Nonnull Supplier<UUID> uuidSupplier, int retryCount)
    {
        this.uuidSupplier = Objects.requireNonNull(uuidSupplier);
        this.retryCount   = retryCount;
    }

    @Override
    public <Result> Result runInTransaction(@Nonnull TransactionalCommand<Result> transactionalCommand)
    {
        return MithraManagerProvider.getMithraManager().executeTransactionalCommand(transaction ->
        {
            try
            {
                Transaction transactionAdapter = new TransactionAdapter(transaction);
                return transactionalCommand.run(transactionAdapter);
            }
            finally
            {
                ReladomoDataStore.logTransactionalStats(transaction);
            }
        }, this.retryCount);
    }

    @Override
    public void runInTransaction(@Nonnull Runnable runnable)
    {
        MithraManagerProvider.getMithraManager().executeTransactionalCommand(tx ->
        {
            runnable.run();
            return null;
        }, this.retryCount);
    }

    @Override
    public List<Object> findAll(Klass klass)
    {
        RelatedFinder finder = this.getRelatedFinder(klass);
        return finder.findMany(finder.all());
    }

    private static void logTransactionalStats(MithraTransaction reladomoTransaction)
    {
        if (MithraManagerProvider.getMithraManager().getCurrentTransaction() != reladomoTransaction)
        {
            throw new AssertionError();
        }

        MDC.put(
                "total database retrievals",
                String.valueOf(MithraManagerProvider.getMithraManager().getDatabaseRetrieveCount()));
        MDC.put(
                "database retrievals",
                String.valueOf(reladomoTransaction.getDatabaseRetrieveCount()));

        LOGGER.debug(
                MARKER,
                "total database retrievals: {} database retrievals: {}",
                MithraManagerProvider.getMithraManager().getDatabaseRetrieveCount(),
                reladomoTransaction.getDatabaseRetrieveCount());

        MDC.remove("total database retrievals");
        MDC.remove("database retrievals");
    }

    @Override
    public Object findByKey(@Nonnull Klass klass, @Nonnull MapIterable<DataTypeProperty, Object> keys)
    {
        keys.forEachKeyValue((keyProperty, keyValue) ->
        {
            if (keyProperty.getOwningClassifier() != klass)
            {
                String message = "Expected key property '%s' to be owned by the given class: '%s' but got '%s'.".formatted(
                        keyProperty,
                        klass,
                        keyProperty.getOwningClassifier());
                throw new AssertionError(message);
            }
        });
        ImmutableList<DataTypeProperty> keyProperties = klass.getKeyProperties();
        if (keyProperties.size() != keys.size())
        {
            String error = String.format(
                    "Expected keys for properties %s but got the wrong number of keys %s",
                    keyProperties,
                    keys);
            throw new IllegalArgumentException(error);
        }

        RelatedFinder<?> finder = this.getRelatedFinder(klass);
        ImmutableList<Operation> operations = keyProperties.collect(keyProperty ->
        {
            Object key = keys.get(keyProperty);
            if (!keys.containsKey(keyProperty))
            {
                throw new AssertionError("Expected key for property: " + keyProperty);
            }
            if (key == null)
            {
                throw new AssertionError("Expected non-null key for property: " + keyProperty);
            }
            return this.getOperation(finder, keyProperty, key);
        });

        Operation operation = operations.reduce(Operation::and).get();
        return finder.findOne(operation);
    }

    private Operation getOperation(
            @Nonnull RelatedFinder<?> finder,
            @Nonnull DataTypeProperty keyProperty,
            Object key)
    {
        this.assertObjectMatchesType(keyProperty, key);

        Attribute        attribute = finder.getAttributeByName(keyProperty.getName());
        OperationVisitor visitor   = new OperationVisitor(attribute, key);
        keyProperty.visit(visitor);
        return visitor.getResult();
    }

    private void assertObjectMatchesType(DataTypeProperty property, Object object)
    {
        property.visit(new AssertObjectMatchesDataTypePropertyVisitor(object));
    }

    @Nonnull
    @Override
    public Object instantiate(@Nonnull Klass klass, @Nonnull MapIterable<DataTypeProperty, Object> keys)
    {
        keys.each(Objects::requireNonNull);

        Object newInstance = this.instantiateNewInstance(klass);
        this.setKeys(klass, newInstance, keys);
        return newInstance;
    }

    @Nonnull
    private Object instantiateNewInstance(@Nonnull Klass klass)
    {
        try
        {
            Class<?> aClass = Class.forName(klass.getFullyQualifiedName());
            Class<?>[] parameterTypes = klass.isSystemTemporal()
                    ? new Class<?>[]{Timestamp.class}
                    : new Class<?>[]{};
            Constructor<?> constructor = aClass.getConstructor(parameterTypes);
            Object[] constructorArgs = klass.isSystemTemporal()
                    ? new Object[]{DefaultInfinityTimestamp.getDefaultInfinity()}
                    : new Object[]{};
            return constructor.newInstance(constructorArgs);
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    private Object instantiateNewInstance(@Nonnull Klass klass, @Nonnull Instant validTime)
    {
        try
        {
            Class<?>       aClass      = Class.forName(klass.getFullyQualifiedName());
            Constructor<?> constructor = aClass.getConstructor(Timestamp.class, Timestamp.class);
            Timestamp      timestamp   = Timestamp.valueOf(LocalDateTime.ofInstant(validTime, ZoneOffset.UTC));
            // TODO: One of these would be infinity, forgot which one
            return constructor.newInstance(timestamp, timestamp);
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void setKeys(
            @Nonnull Klass klass,
            @Nonnull Object newInstance,
            @Nonnull MapIterable<DataTypeProperty, Object> keys)
    {
        this.generateAndSetId(newInstance, klass);

        ImmutableList<DataTypeProperty> keyProperties = klass.getKeyProperties().reject(DataTypeProperty::isID);
        if (keyProperties.size() != keys.size())
        {
            String error = String.format(
                    "Expected one key for each key property in %s but got %s",
                    keyProperties,
                    keys);
            throw new IllegalArgumentException(error);
        }
        for (DataTypeProperty keyProperty : keyProperties)
        {
            Object key = keys.get(keyProperty);
            Objects.requireNonNull(key, () -> "Expected non-null key for property: " + keyProperty);
            this.setDataTypeProperty(newInstance, keyProperty, key);
        }
    }

    private void generateAndSetId(@Nonnull Object persistentInstance, @Nonnull Klass klass)
    {
        ImmutableList<DataTypeProperty> idProperties = klass.getDataTypeProperties().select(DataTypeProperty::isID);
        if (idProperties.isEmpty())
        {
            return;
        }

        PrimitiveProperty idProperty = (PrimitiveProperty) idProperties.getOnly();

        if (idProperty.getType().isNumeric())
        {
            try
            {
                String methodName             = "generateAndSet" + LOWER_TO_UPPER_CAMEL.convert(idProperty.getName());
                Method generateAndSetIdMethod = persistentInstance.getClass().getMethod(methodName);
                generateAndSetIdMethod.invoke(persistentInstance);
            }
            catch (ReflectiveOperationException e)
            {
                throw new RuntimeException(e);
            }
        }
        else if (idProperty.getType() == PrimitiveType.STRING)
        {
            Objects.requireNonNull(this.uuidSupplier);
            UUID   uuid       = this.uuidSupplier.get();
            String uuidString = uuid.toString();
            this.setDataTypeProperty(persistentInstance, idProperty, uuidString);
        }
        else
        {
            throw new AssertionError(idProperty);
        }
    }

    @Nullable
    @Override
    public Object getDataTypeProperty(@Nonnull Object persistentInstance, @Nonnull DataTypeProperty dataTypeProperty)
    {
        if (!(persistentInstance instanceof MithraObject))
        {
            String detailMessage = "Expected MithraObject but got " + persistentInstance.getClass().getCanonicalName();
            throw new AssertionError(detailMessage);
        }

        // TODO: Code generate accessors to avoid reflection
        if (dataTypeProperty.isDerived())
        {
            return this.getPropertyReflectively(persistentInstance, dataTypeProperty);
        }

        Classifier owningClassifier = dataTypeProperty.getOwningClassifier();
        if (owningClassifier instanceof Klass && !Objects.equals(
                owningClassifier.getName(),
                persistentInstance.getClass().getSimpleName()))
        {
            String detailMessage = "Expected %s but got %s".formatted(
                    owningClassifier.getName(),
                    persistentInstance.getClass().getSimpleName());
            throw new AssertionError(detailMessage);
        }

        RelatedFinder<?> finder        = this.getRelatedFinder((MithraObject) persistentInstance);
        String           attributeName = dataTypeProperty.getName();
        Attribute        attribute     = finder.getAttributeByName(attributeName);
        if (attribute == null)
        {
            String error = "Domain model and generated code are out of sync. Try rerunning a full clean build. Could not find attribute: " + attributeName;
            throw new AssertionError(error);
        }

        if (attribute.isAttributeNull(persistentInstance))
        {
            if (dataTypeProperty.isOptional())
            {
                return null;
            }

            String message = String.format("Found null for required property: '%s'", dataTypeProperty);
            throw new IllegalStateException(message);
        }

        Object result = attribute.valueOf(persistentInstance);

        if (dataTypeProperty.getType() == PrimitiveType.LOCAL_DATE)
        {
            return ((Date) result).toLocalDate();
        }

        if (dataTypeProperty.getType() == PrimitiveType.INSTANT)
        {
            return ((Timestamp) result).toInstant();
        }

        if (dataTypeProperty.isTemporalRange())
        {
            Timestamp infinity = ((AsOfAttribute<?>) attribute).getInfinityDate();
            if (infinity.equals(result))
            {
                return null;
            }
            // TODO: Consider handling here the case where validTo == systemTo + 1 day, but really means infinity
            // TODO: Alternately, just enable future dated rows to turn off this optimization
            return ((Timestamp) result).toInstant();
        }

        if (dataTypeProperty.isTemporalInstant())
        {
            Timestamp infinity = ((TimestampAttribute<?>) attribute).getAsOfAttributeInfinity();
            if (infinity.equals(result))
            {
                return null;
            }
            // TODO: Consider handling here the case where validTo == systemTo + 1 day, but really means infinity
            // TODO: Alternately, just enable future dated rows to turn off this optimization
            return ((Timestamp) result).toInstant();
        }

        if (dataTypeProperty instanceof EnumerationProperty enumerationProperty)
        {
            String              prettyName          = (String) result;
            Enumeration         enumeration         = enumerationProperty.getType();

            Optional<EnumerationLiteral> enumerationLiteral = enumeration.getEnumerationLiterals()
                    .detectOptional(each -> each.getPrettyName().equals(prettyName));

            return enumerationLiteral.orElseThrow(() -> new AssertionError(prettyName));
        }

        return result;
    }

    @Nullable
    private Object getDataTypePropertyLenient(
            @Nonnull Object persistentInstance,
            @Nonnull DataTypeProperty dataTypeProperty)
    {
        if (dataTypeProperty.isDerived())
        {
            return this.getPropertyReflectively(persistentInstance, dataTypeProperty);
        }

        RelatedFinder<?> finder    = this.getRelatedFinder((MithraObject) persistentInstance);
        Attribute        attribute = finder.getAttributeByName(dataTypeProperty.getName());
        if (attribute == null)
        {
            throw new AssertionError(
                    "Domain model and generated code are out of sync. Try rerunning a full clean build. Could not find: "
                            + dataTypeProperty);
        }

        if (attribute.isAttributeNull(persistentInstance))
        {
            return null;
        }

        Object result = attribute.valueOf(persistentInstance);

        if (dataTypeProperty.getType() == PrimitiveType.LOCAL_DATE)
        {
            return ((Date) result).toLocalDate();
        }

        if (dataTypeProperty.getType() == PrimitiveType.INSTANT)
        {
            return ((Timestamp) result).toInstant();
        }

        boolean isTemporal = dataTypeProperty.isTemporal();
        if (isTemporal)
        {
            Timestamp infinity = ((TimestampAttribute<?>) attribute).getAsOfAttributeInfinity();
            if (infinity.equals(result))
            {
                return null;
            }
            // TODO: Consider handling here the case where validTo == systemTo + 1 day, but really means infinity
            // TODO: Alternately, just enable future dated rows to turn off this optimization
            return ((Timestamp) result).toInstant();
        }

        if (dataTypeProperty instanceof EnumerationProperty enumerationProperty)
        {
            String              prettyName          = (String) result;
            Enumeration         enumeration         = enumerationProperty.getType();

            Optional<EnumerationLiteral> enumerationLiteral = enumeration.getEnumerationLiterals()
                    .detectOptional(each -> each.getPrettyName().equals(prettyName));

            return enumerationLiteral.orElseThrow(() -> new AssertionError(prettyName));
        }

        return result;
    }

    private Object getPropertyReflectively(
            @Nonnull Object persistentInstance,
            @Nonnull Property property)
    {
        try
        {
            Classifier owningClassifier   = property.getOwningClassifier();
            String     fullyQualifiedName = owningClassifier.getFullyQualifiedName();
            Class<?>   aClass             = Class.forName(fullyQualifiedName);
            String     methodName         = this.getMethodName(property);
            Method     method             = aClass.getMethod(methodName);
            return method.invoke(persistentInstance);
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    private String getMethodName(Property property)
    {
        String prefix = property.getType() == PrimitiveType.BOOLEAN ? "is" : "get";
        String suffix = LOWER_TO_UPPER_CAMEL.convert(property.getName());
        return prefix + suffix;
    }

    @Override
    public boolean setDataTypeProperty(
            @Nonnull Object persistentInstance,
            @Nonnull DataTypeProperty dataTypeProperty,
            @Nullable Object newValue)
    {
        if (dataTypeProperty.isDerived())
        {
            throw new AssertionError(dataTypeProperty);
        }

        Object oldValue = this.getDataTypePropertyLenient(persistentInstance, dataTypeProperty);
        if (Objects.equals(oldValue, newValue))
        {
            return false;
        }

        RelatedFinder<?> finder    = this.getRelatedFinder((MithraObject) persistentInstance);
        Attribute        attribute = finder.getAttributeByName(dataTypeProperty.getName());

        if (newValue == null)
        {
            if (dataTypeProperty.isRequired())
            {
                String message = String.format(
                        "May not set required property to null: '%s.%s'",
                        dataTypeProperty.getOwningClassifier().getName(),
                        dataTypeProperty);
                throw new IllegalStateException(message);
            }
            attribute.setValueNull(persistentInstance);
        }
        else if (dataTypeProperty instanceof EnumerationProperty)
        {
            attribute.setValue(persistentInstance, ((EnumerationLiteral) newValue).getPrettyName());
        }
        else if (dataTypeProperty.getType() == PrimitiveType.LOCAL_DATE)
        {
            Timestamp timestamp = Timestamp.valueOf(((LocalDate) newValue).atStartOfDay());
            attribute.setValue(persistentInstance, timestamp);
        }
        else if (dataTypeProperty.getType() == PrimitiveType.INSTANT)
        {
            Timestamp timestamp = Timestamp.from((Instant) newValue);
            attribute.setValue(persistentInstance, timestamp);
        }
        else
        {
            attribute.setValue(persistentInstance, newValue);
        }

        return true;
    }

    @Override
    public Object getToOne(Object persistentSourceInstance, @Nonnull ReferenceProperty referenceProperty)
    {
        if (!referenceProperty.getMultiplicity().isToOne())
        {
            throw new AssertionError();
        }

        Object result = this.get(persistentSourceInstance, referenceProperty);
        if (result instanceof List<?> list)
        {
            throw new AssertionError("Expected single object but got " + list.size());
        }
        return result;
    }

    public Object get(Object persistentSourceInstance, @Nonnull ReferenceProperty referenceProperty)
    {
        RelatedFinder<?> finder                = this.getRelatedFinder(referenceProperty.getOwningClassifier());
        String           referencePropertyName = referenceProperty.getName();
        AbstractRelatedFinder relationshipFinder = (AbstractRelatedFinder) finder
                .getRelationshipFinderByName(referencePropertyName);

        if (relationshipFinder == null)
        {
            String error =
                    "Domain model and generated code are out of sync. Try rerunning a full clean build. Could not find relationship for property "
                    + referenceProperty;
            throw new AssertionError(error);
        }

        return relationshipFinder.valueOf(persistentSourceInstance);
    }

    @Nonnull
    @Override
    public List<Object> getToMany(Object persistentSourceInstance, @Nonnull ReferenceProperty referenceProperty)
    {
        if (!referenceProperty.getMultiplicity().isToMany())
        {
            throw new AssertionError();
        }

        Object result = this.get(persistentSourceInstance, referenceProperty);
        if (!(result instanceof List))
        {
            throw new AssertionError("Expected list but got " + result.getClass().getCanonicalName());
        }

        return (List<Object>) result;
    }

    @Override
    public boolean setToOne(
            @Nonnull Object persistentSourceInstance,
            @Nonnull AssociationEnd associationEnd,
            @Nonnull Object persistentTargetInstance)
    {
        Objects.requireNonNull(persistentTargetInstance);

        boolean mutationOccurred = false;

        // A Reladomo bug prevents just calling a method like setQuestion here. Instead we have to call foreign key setters like setQuestionId

        ImmutableList<DataTypeProperty> targetDataTypeProperties = associationEnd
                .getOwningClassifier()
                .getDataTypeProperties();
        for (DataTypeProperty targetDataTypeProperty : targetDataTypeProperties)
        {
            OrderedMap<AssociationEnd, DataTypeProperty> keysMatchingThisForeignKey =
                    targetDataTypeProperty.getKeysMatchingThisForeignKey();

            DataTypeProperty keyInRelatedObject = keysMatchingThisForeignKey.getIfAbsentValue(associationEnd,
                    null);
            if (keyInRelatedObject == null)
            {
                continue;
            }

            DataTypeProperty foreignKey = targetDataTypeProperty;

            Object keyValue = this.getDataTypeProperty(persistentTargetInstance, keyInRelatedObject);

            mutationOccurred |= this.setDataTypeProperty(persistentSourceInstance, foreignKey, keyValue);
        }

        return mutationOccurred;
    }

    @Override
    public void insert(Object persistentInstance)
    {
        if (!(persistentInstance instanceof MithraTransactionalObject))
        {
            throw new AssertionError();
        }

        ((MithraTransactionalObject) persistentInstance).insert();
    }

    @Override
    public void deleteOrTerminate(@Nonnull Object persistentInstance)
    {
        if (persistentInstance instanceof MithraDatedTransactionalObject)
        {
            ((MithraDatedTransactionalObject) persistentInstance).terminate();
        }
        else if (persistentInstance instanceof MithraTransactionalObject)
        {
            ((MithraTransactionalObject) persistentInstance).delete();
        }
        else
        {
            throw new AssertionError();
        }
    }

    @Override
    public void purgeAll(@Nonnull Klass klass)
    {
        if (klass.isAbstract())
        {
            return;
        }

        RelatedFinder<?> relatedFinder = this.getRelatedFinder(klass);
        if (klass.isSystemTemporal())
        {
            this.purgeAll(relatedFinder);
        }
        else
        {
            this.deleteAll(relatedFinder);
        }
    }

    private void purgeAll(RelatedFinder<?> relatedFinder)
    {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ".purgeAll() not implemented yet");
    }

    private void deleteAll(@Nonnull RelatedFinder<?> finder)
    {
        Operation     operation               = finder.all();
        MithraList<?> mithraList              = finder.findMany(operation);
        var           transactionalDomainList = (TransactionalDomainList<?>) mithraList;
        transactionalDomainList.deleteAll();
    }

    @Override
    public boolean isInstanceOf(@Nonnull Object persistentInstance, @Nonnull Classifier classifier)
    {
        try
        {
            Class<?> persistentInstanceClass = persistentInstance.getClass();
            Class<?> domainModelClass        = Class.forName(classifier.getPackageName() + "." + classifier.getName());
            return domainModelClass.isAssignableFrom(persistentInstanceClass);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Klass getMostSpecificSubclass(Object persistentInstance, Klass klass)
    {
        if (!(persistentInstance instanceof MithraObject))
        {
            throw new AssertionError("Expected MithraObject but got " + persistentInstance.getClass().getCanonicalName());
        }

        ImmutableList<Klass> potentialSubClasses = klass
                .getSubClasses()
                .select(subClass ->
                {
                    MithraObject subClassPersistentInstance = this.getSubClassPersistentInstance(
                            klass,
                            subClass,
                            (MithraObject) persistentInstance);
                    return subClassPersistentInstance != null;
                });

        if (potentialSubClasses.isEmpty())
        {
            return klass;
        }

        if (potentialSubClasses.size() == 1)
        {
            Klass onlySubClass = potentialSubClasses.getOnly();
            MithraObject subClassPersistentInstance = this.getSubClassPersistentInstance(
                    klass,
                    onlySubClass,
                    (MithraObject) persistentInstance);

            Klass result = this.getMostSpecificSubclass(subClassPersistentInstance, onlySubClass);
            return result;
        }

        throw new AssertionError("Expected one subclass but got " + potentialSubClasses);
    }

    public MithraObject getSubClassPersistentInstance(
            Klass klass,
            Klass subClass,
            MithraObject persistentInstance)
    {
        RelatedFinder<?> finder = this.getRelatedFinder(klass);

        String relationshipName = UPPER_TO_LOWER_CAMEL.convert(subClass.getName()) + "SubClass";

        AbstractRelatedFinder relationshipFinder = (AbstractRelatedFinder) finder.getRelationshipFinderByName(relationshipName);

        if (relationshipFinder == null)
        {
            String error =
                    "Domain model and generated code are out of sync. Try rerunning a full clean build. Could not find relationship for property "
                            + relationshipName;
            throw new AssertionError(error);
        }

        Object result = relationshipFinder.valueOf(persistentInstance);
        return (MithraObject) result;
    }

    @Override
    public Object getSuperClass(Object persistentInstance, Klass klass)
    {
        RelatedFinder<?> finder = this.getRelatedFinder(klass);

        String relationshipName = UPPER_TO_LOWER_CAMEL.convert(klass.getSuperClass().get().getName()) + "SuperClass";

        AbstractRelatedFinder relationshipFinder = (AbstractRelatedFinder) finder.getRelationshipFinderByName(relationshipName);

        if (relationshipFinder == null)
        {
            String error =
                    "Domain model and generated code are out of sync. Try rerunning a full clean build. Could not find relationship for property "
                            + relationshipName;
            throw new AssertionError(error);
        }

        Object result = relationshipFinder.valueOf(persistentInstance);
        return result;
    }

    private RelatedFinder<?> getRelatedFinder(@Nonnull MithraObject mithraObject)
    {
        return mithraObject.zGetPortal().getFinder();
    }

    @Nonnull
    public RelatedFinder<?> getRelatedFinder(@Nonnull Classifier classifier)
    {
        try
        {
            String   finderName      = classifier.getFullyQualifiedName() + "Finder";
            Class<?> finderClass     = Class.forName(finderName);
            Method   getFinderMethod = finderClass.getMethod("getFinderInstance");
            return (RelatedFinder<?>) getFinderMethod.invoke(null);
        }
        catch (@Nonnull ReflectiveOperationException | IllegalArgumentException | SecurityException e)
        {
            throw new RuntimeException(e);
        }
    }
}

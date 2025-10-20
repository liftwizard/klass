package ${package};

import java.sql.Timestamp;
import cool.klass.data.store.reladomo.UtcInfinityTimestamp;

public class DeleteMeExampleOwnedType
        extends DeleteMeExampleOwnedTypeAbstract
{
    public DeleteMeExampleOwnedType(Timestamp system)
    {
        super(system);
        // You must not modify this constructor. Mithra calls this internally.
        // You can call this constructor. You can also add new constructors.
    }

    public DeleteMeExampleOwnedType()
    {
        this(UtcInfinityTimestamp.getDefaultInfinity());
    }
}

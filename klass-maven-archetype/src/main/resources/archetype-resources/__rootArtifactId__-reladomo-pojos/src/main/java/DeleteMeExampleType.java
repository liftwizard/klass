package ${package};

import java.sql.Timestamp;
import cool.klass.data.store.reladomo.UtcInfinityTimestamp;

public class DeleteMeExampleType
        extends DeleteMeExampleTypeAbstract
{
    public DeleteMeExampleType(Timestamp system)
    {
        super(system);
        // You must not modify this constructor. Mithra calls this internally.
        // You can call this constructor. You can also add new constructors.
    }

    public DeleteMeExampleType()
    {
        this(UtcInfinityTimestamp.getDefaultInfinity());
    }
}

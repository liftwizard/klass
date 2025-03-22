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

package klass.model.meta.domain;

public class AssociationEnd extends AssociationEndAbstract
{
    public AssociationEnd()
    {
        // You must not modify this constructor. Mithra calls this internally.
        // You can call this constructor. You can also add new constructors.
    }

    @Override
    public AssociationEndOrderByList getOrderBys()
    {
        AssociationEndOrderByList orderBys = super.getOrderBys();
        if (orderBys.size() > 1)
        {
            int unused = orderBys.size();
        }
        return orderBys;
    }

    @Override
    public String toString()
    {
        return String.format(
                "%s.%s: %s[%s]",
                this.getOwningClass().getName(),
                this.getName(),
                this.getResultType().getName(),
                this.getMultiplicity());
    }
}

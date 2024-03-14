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

package cool.klass.reladomo.sample.data;

import java.time.LocalDateTime;

import javax.annotation.Nonnull;

public class RequiredDataTypePropertyVisitor
        extends AbstractDataTypePropertyVisitor
{
    private static final LocalDateTime LOCAL_DATE_TIME = LocalDateTime.of(1999, 12, 31, 23, 59);

    @Nonnull
    @Override
    protected String getEmoji()
    {
        return "☝";
    }

    @Override
    protected int getIndex()
    {
        return 1;
    }

    @Override
    protected boolean getBoolean()
    {
        return true;
    }

    @Nonnull
    @Override
    protected LocalDateTime getLocalDateTime()
    {
        return LOCAL_DATE_TIME;
    }
}

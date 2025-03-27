/*
 * Copyright 2025 Craig Motlin
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

package cool.klass.model.converter.compiler.annotation;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Comparators;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

record CompilerAnnotationKey(
        ImmutableList<Integer> lineNumbers,
        int columnNumber,
        String errorCode,
        String filename)
        implements Comparable<CompilerAnnotationKey>
{
    private static final Comparator<Iterable<Integer>> LEXICOGRAPHICAL = Comparators.lexicographical(Comparator.<Integer>naturalOrder());

    public static CompilerAnnotationKey parseAnnotationFilename(String annotationFilename)
    {
        // TestName-LineNumbers-ColumnNumber-ErrorCode.log
        // Where LineNumbers can be a single number or multiple numbers joined by underscores
        String regex = ".*?-(\\d+(?:_\\d+)*?)-(\\d+)-([A-Z]{3}_[A-Z]{3}_[A-Z]{3})\\.log";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(annotationFilename);

        if (!matcher.matches())
        {
            throw new AssertionError("Filename does not match expected pattern: " + annotationFilename);
        }

        String lineNumbersStr = matcher.group(1);
        int columnNumber = Integer.parseInt(matcher.group(2));
        String errorCode = matcher.group(3);

        String[] lineNumbersArray = lineNumbersStr.split("_");
        MutableList<Integer> lineNumbers = Lists.mutable.empty();
        for (String lineNumber : lineNumbersArray)
        {
            lineNumbers.add(Integer.parseInt(lineNumber));
        }

        return new CompilerAnnotationKey(lineNumbers.toImmutable(), columnNumber, errorCode, annotationFilename);
    }

    @Override
    public int compareTo(CompilerAnnotationKey other)
    {
        // TODO 2025-03-28: refactor to using comparators and "andThen" methods. It's fine to delete the assertion below.
        int lineNumbersCompare = LEXICOGRAPHICAL.compare(this.lineNumbers, other.lineNumbers);
        if (lineNumbersCompare != 0)
        {
            return lineNumbersCompare;
        }

        int columnCompare = Integer.compare(this.columnNumber, other.columnNumber);
        if (columnCompare != 0)
        {
            return columnCompare;
        }

        int result = this.errorCode.compareTo(other.errorCode);

        if (result == 0)
        {
            String detailMessage = "Found multiple error codes at the same position: " + this + " and " + other;
            throw new AssertionError(detailMessage);
        }
        return result;
    }
}

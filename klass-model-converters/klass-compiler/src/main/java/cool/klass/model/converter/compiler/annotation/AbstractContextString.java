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

import java.util.Objects;

import javax.annotation.Nonnull;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.fusesource.jansi.Ansi;

public abstract class AbstractContextString {

	private final int line;

	@Nonnull
	private final String string;

	protected AbstractContextString(int line, @Nonnull String string) {
		this.line = line;
		this.string = Objects.requireNonNull(string);
	}

	private static String padLeft(String string, int width) {
		return String.format("%" + width + "s║", string);
	}

	public int getLine() {
		return this.line;
	}

	public String toString(int lineNumberWidth) {
		MutableList<String> strings = ArrayAdapter.adapt(this.string.split("\n"));
		return strings
			.collectWithIndex((string, index) -> this.toString(string, index, lineNumberWidth))
			.makeString("\n");
	}

	private String toString(String string, int offset, int lineNumberWidth) {
		String lineNumberString = this.getLineNumberString(this.line + offset);
		String paddedLineNumberString = AbstractContextString.padLeft(lineNumberString, lineNumberWidth);
		return Ansi.ansi().fgDefault().a(paddedLineNumberString).a(" ").a(string).toString();
	}

	@Override
	public String toString() {
		return this.toString(4);
	}

	@Nonnull
	protected abstract String getLineNumberString(int line);
}

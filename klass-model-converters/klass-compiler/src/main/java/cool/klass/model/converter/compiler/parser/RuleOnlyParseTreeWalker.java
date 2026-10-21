/*
 * Copyright 2026 Craig Motlin
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

package cool.klass.model.converter.compiler.parser;

import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/**
 * Walks only the rule nodes of a parse tree, firing each rule's enter and exit events.
 *
 * <p>Unlike {@link ParseTreeWalker}, it never calls {@code visitTerminal}, {@code visitErrorNode},
 * {@code enterEveryRule}, or {@code exitEveryRule}. Compiler phases don't listen to those events, and dispatching
 * them through every phase's delegation chain costs about as much as the rule events themselves.
 */
public final class RuleOnlyParseTreeWalker
	extends ParseTreeWalker
{
	@Override
	public void walk(ParseTreeListener listener, ParseTree tree)
	{
		if (!(tree instanceof ParserRuleContext ctx))
		{
			return;
		}

		ctx.enterRule(listener);
		// Read children after enterRule, which may add an in-place macro expansion as a new child
		List<ParseTree> children = ctx.children;
		if (children != null)
		{
			int numChildren = children.size();
			for (int i = 0; i < numChildren; i++)
			{
				this.walk(listener, children.get(i));
			}
		}
		ctx.exitRule(listener);
	}
}

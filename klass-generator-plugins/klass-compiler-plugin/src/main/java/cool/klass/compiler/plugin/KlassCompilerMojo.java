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

package cool.klass.compiler.plugin;

import cool.klass.generator.plugin.AbstractGenerateMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * This plugin doesn't emit anything. It just compiles the source code and emits compiler errors if applicable. The goal is to emit compiler errors in the module which contains the code being compiled, rather than a dependent module. This helps with rerunnability.
 */
@Mojo(
	name = "compile",
	defaultPhase = LifecyclePhase.TEST,
	threadSafe = true,
	requiresDependencyResolution = ResolutionScope.TEST
)
public class KlassCompilerMojo extends AbstractGenerateMojo {

	@Override
	protected InputSource getInputSource() {
		return InputSource.FILESYSTEM;
	}

	@Override
	public void execute() throws MojoExecutionException {
		this.getDomainModelFromFiles();
	}
}

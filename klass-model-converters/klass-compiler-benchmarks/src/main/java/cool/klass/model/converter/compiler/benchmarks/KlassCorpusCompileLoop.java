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

package cool.klass.model.converter.compiler.benchmarks;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.Duration;

/**
 * Compiles the corpus repeatedly for a fixed time, as a steady workload to attach a profiler to.
 *
 * <p>Usage: {@code KlassCorpusCompileLoop [warmupSeconds] [runSeconds]}.
 */
public final class KlassCorpusCompileLoop
{
	private KlassCorpusCompileLoop()
	{
		throw new AssertionError("Suppress default constructor for noninstantiability");
	}

	public static void main(String[] args)
	{
		Duration warmup = Duration.ofSeconds(args.length > 0 ? Long.parseLong(args[0]) : 20);
		Duration run = Duration.ofSeconds(args.length > 1 ? Long.parseLong(args[1]) : 60);

		var corpusCompiler = new KlassCorpusCompiler();
		System.out.printf("Corpus: %d models, %d lines%n", KlassSources.ALL.size(), corpusCompiler.lineCount());

		compileFor(corpusCompiler, warmup, "warmup");
		compileFor(corpusCompiler, run, "measured");
	}

	private static void compileFor(KlassCorpusCompiler corpusCompiler, Duration duration, String label)
	{
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		long startCpu = threadMXBean.getCurrentThreadCpuTime();
		long start = System.nanoTime();
		long end = start + duration.toNanos();
		var iterations = 0;
		while (System.nanoTime() < end)
		{
			corpusCompiler.compileAll();
			iterations++;
		}
		double millisPerIteration = (System.nanoTime() - start) / 1_000_000.0 / iterations;
		double cpuMillisPerIteration = (threadMXBean.getCurrentThreadCpuTime() - startCpu) / 1_000_000.0 / iterations;
		System.out.printf(
			"%s: %d corpus compiles, %.1f ms wall and %.1f ms CPU each%n",
			label,
			iterations,
			millisPerIteration,
			cpuMillisPerIteration
		);
	}
}

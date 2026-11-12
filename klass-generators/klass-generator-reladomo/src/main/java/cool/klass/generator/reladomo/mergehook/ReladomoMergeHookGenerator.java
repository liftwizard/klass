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

package cool.klass.generator.reladomo.mergehook;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.PackageableElement;
import cool.klass.model.meta.domain.api.PrimitiveType;
import cool.klass.model.meta.domain.api.property.AssociationEnd;
import cool.klass.model.meta.domain.api.property.DataTypeProperty;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;

// TODO: Consider moving this into its own module.
public class ReladomoMergeHookGenerator
{
	private static final Converter<String, String> LOWER_CAMEL_TO_UPPER_CAMEL = CaseFormat.LOWER_CAMEL.converterTo(
		CaseFormat.UPPER_CAMEL
	);

	@Nonnull
	private final DomainModel domainModel;

	public ReladomoMergeHookGenerator(@Nonnull DomainModel domainModel)
	{
		this.domainModel = Objects.requireNonNull(domainModel);
	}

	public void writeMergeHookFiles(@Nonnull Path outputPath)
	{
		this.domainModel
			.getClasses()
			.reject(Klass::isAbstract)
			.select(Klass::isVersioned)
			.forEachWith(this::writeMergeHookFile, outputPath);
	}

	private void writeMergeHookFile(@Nonnull Klass klass, @Nonnull Path outputPath)
	{
		Path mergeHookOutputPath = this.getMergeHookOutputPath(outputPath, klass);
		String classSourceCode = this.getSourceCode(klass);
		this.printStringToFile(mergeHookOutputPath, classSourceCode);
	}

	@Nonnull
	private Path getMergeHookOutputPath(@Nonnull Path outputPath, @Nonnull PackageableElement packageableElement)
	{
		String packageRelativePath = packageableElement.getPackageName().replaceAll("\\.", "/");
		Path mergeHookDirectory = outputPath
			.resolve(packageRelativePath)
			.resolve("reladomo")
			.resolve("merge")
			.resolve("hook");
		mergeHookDirectory.toFile().mkdirs();
		String fileName = packageableElement.getName() + "MergeHook.java";
		return mergeHookDirectory.resolve(fileName);
	}

	private void printStringToFile(@Nonnull Path path, String contents)
	{
		try (var printStream = new PrintStream(new FileOutputStream(path.toFile()), true, StandardCharsets.UTF_8))
		{
			printStream.print(contents);
		}
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Nonnull
	public String getSourceCode(@Nonnull Klass klass)
	{
		String name = klass.getName();
		Klass versionClass = klass.getVersionProperty().get().getType();
		String versionName = versionClass.getName();

		String setKeyPropertiesSourceCode = klass
			.getKeyProperties()
			.collect(this::getKeyPropertySourceCode)
			.makeString("");

		String setAuditPropertiesOnCreateSourceCode = klass.isAudited() ?
			"""
				        version.setCreatedOn(newObject.getCreatedOn());
				        version.setCreatedById(newObject.getCreatedById());
				        version.setLastUpdatedById(newObject.getLastUpdatedById());
				""" :
			"";

		String setAuditPropertiesOnUpdateSourceCode = klass.isAudited() ?
			"        existingVersion.setLastUpdatedById(incoming.getLastUpdatedById());\n" :
			"";

		MutableList<String> ownedNavigations = Lists.mutable.empty();
		this.collectOwnedNavigations(klass, name + "Finder", Sets.immutable.empty(), ownedNavigations);
		String navigateToOwnedChildrenSourceCode = ownedNavigations
			.collect(
				(navigation) ->
					"        mergeOptions.navigateTo(" +
					navigation +
					").withMergeHook(this.getOwnedChildMergeHook());\n"
			)
			.makeString("");

		// @formatter:off
		// language=JAVA
		String sourceCode = ""
				+ "package " + klass.getPackageName() + ".reladomo.merge.hook;\n"
				+ "\n"
				+ "import java.util.Collections;\n"
				+ "import java.util.IdentityHashMap;\n"
				+ "import java.util.Set;\n"
				+ "\n"
				+ "import javax.annotation.*;\n"
				+ "\n"
				+ "import " + klass.getFullyQualifiedName() + ";\n"
				+ "import " + klass.getFullyQualifiedName() + "Finder;\n"
				+ "import " + versionClass.getFullyQualifiedName() + ";\n"
				+ "import com.gs.fw.common.mithra.MithraManagerProvider;\n"
				+ "import com.gs.fw.common.mithra.MithraTransaction;\n"
				+ "import com.gs.fw.common.mithra.list.merge.MergeBuffer;\n"
				+ "import com.gs.fw.common.mithra.list.merge.MergeHook;\n"
				+ "import com.gs.fw.common.mithra.list.merge.TopLevelMergeOptions;\n"
				+ "\n"
				+ "/**\n"
				+ " * Auto-generated by {@link " + this.getClass().getCanonicalName() + "}\n"
				+ " *\n"
				+ " * <p>Inserts version 1 of a new " + name + ", terminates the version of a terminated " + name + ", and\n"
				+ " * bumps the version once per transaction when the " + name + " or any of its owned descendants changes,\n"
				+ " * matching the service write path. Call {@link #navigateToOwnedChildren(TopLevelMergeOptions)} so that\n"
				+ " * changes to owned descendants reach this hook.\n"
				+ " *\n"
				+ " * <p>The merge visits an object's owned descendants right after calling this hook for the object, so a\n"
				+ " * change to a descendant bumps the version of the " + name + " this hook saw last. That does not depend on\n"
				+ " * the child's foreign key, which the merge may not have set yet. Subclasses that override a callback must\n"
				+ " * call super.\n"
				+ " */\n"
				+ "public class " + name + "MergeHook extends MergeHook<" + name + ">\n"
				+ "{\n"
				+ "    private final Set<" + name + "> versionsHandled = Collections.newSetFromMap(new IdentityHashMap<>());\n"
				+ "    private MithraTransaction transaction;\n"
				+ "    private " + name + " currentExisting;\n"
				+ "    private " + name + " currentIncoming;\n"
				+ "\n"
				+ "    /**\n"
				+ "     * Navigates to every owned descendant of " + name + ", with merge hooks that bump its version.\n"
				+ "     */\n"
				+ "    @SuppressWarnings(\"unchecked\")\n"
				+ "    public void navigateToOwnedChildren(@Nonnull TopLevelMergeOptions<" + name + "> mergeOptions)\n"
				+ "    {\n"
				+ navigateToOwnedChildrenSourceCode
				+ "    }\n"
				+ "\n"
				+ "    /**\n"
				+ "     * A merge hook for an owned child's navigated merge options, for callers that navigate themselves.\n"
				+ "     */\n"
				+ "    public <T> MergeHook<T> getOwnedChildMergeHook()\n"
				+ "    {\n"
				+ "        return new OwnedChildMergeHook<>();\n"
				+ "    }\n"
				+ "\n"
				+ "    @Override\n"
				+ "    public InsertInstruction beforeInsertOfNew(@Nonnull " + name + " newObject)\n"
				+ "    {\n"
				+ "        this.setCurrent(newObject, newObject);\n"
				+ "        this.getVersionsHandled().add(newObject);\n"
				+ "        " + versionName + " version = new " + versionName + "();\n"
				+ "        version.setNumber(1);\n"
				+ setKeyPropertiesSourceCode
				+ setAuditPropertiesOnCreateSourceCode
				+ "        version.insert();\n"
				+ "        return super.beforeInsertOfNew(newObject);\n"
				+ "    }\n"
				+ "\n"
				+ "    @Override\n"
				+ "    public UpdateInstruction matchedNoDifference(\n"
				+ "            @Nonnull " + name + " existing,\n"
				+ "            " + name + " incoming)\n"
				+ "    {\n"
				+ "        this.setCurrent(existing, incoming);\n"
				+ "        return super.matchedNoDifference(existing, incoming);\n"
				+ "    }\n"
				+ "\n"
				+ "    @Override\n"
				+ "    public UpdateInstruction matchedWithDifferenceBeforeAttributeCopy(\n"
				+ "            @Nonnull " + name + " existing,\n"
				+ "            " + name + " incoming)\n"
				+ "    {\n"
				+ "        this.setCurrent(existing, incoming);\n"
				+ "        this.bumpVersion(existing, incoming);\n"
				+ "        return super.matchedWithDifferenceBeforeAttributeCopy(existing, incoming);\n"
				+ "    }\n"
				+ "\n"
				+ "    @Override\n"
				+ "    public DeleteOrTerminateInstruction beforeDeleteOrTerminate(\n"
				+ "            @Nonnull " + name + " existing,\n"
				+ "            MergeBuffer<" + name + "> mergeBuffer)\n"
				+ "    {\n"
				+ "        this.setCurrent(existing, null);\n"
				+ "        this.getVersionsHandled().add(existing);\n"
				+ "        existing.getVersion().terminate();\n"
				+ "        return super.beforeDeleteOrTerminate(existing, mergeBuffer);\n"
				+ "    }\n"
				+ "\n"
				+ "    private void setCurrent(" + name + " existing, " + name + " incoming)\n"
				+ "    {\n"
				+ "        this.currentExisting = existing;\n"
				+ "        this.currentIncoming = incoming;\n"
				+ "    }\n"
				+ "\n"
				+ "    private void bumpVersion(@Nonnull " + name + " existing, " + name + " incoming)\n"
				+ "    {\n"
				+ "        if (!this.getVersionsHandled().add(existing))\n"
				+ "        {\n"
				+ "            return;\n"
				+ "        }\n"
				+ "        " + versionName + " existingVersion = existing.getVersion();\n"
				+ "        existingVersion.setNumber(existingVersion.getNumber() + 1);\n"
				+ setAuditPropertiesOnUpdateSourceCode
				+ "    }\n"
				+ "\n"
				+ "    private Set<" + name + "> getVersionsHandled()\n"
				+ "    {\n"
				+ "        MithraTransaction rootTransaction = MithraManagerProvider.getMithraManager().getCurrentTransaction();\n"
				+ "        while (rootTransaction != null && rootTransaction.getParent() != null)\n"
				+ "        {\n"
				+ "            rootTransaction = rootTransaction.getParent();\n"
				+ "        }\n"
				+ "        if (rootTransaction != this.transaction)\n"
				+ "        {\n"
				+ "            this.transaction = rootTransaction;\n"
				+ "            this.versionsHandled.clear();\n"
				+ "        }\n"
				+ "        return this.versionsHandled;\n"
				+ "    }\n"
				+ "\n"
				+ "    private final class OwnedChildMergeHook<T> extends MergeHook<T>\n"
				+ "    {\n"
				+ "        @Override\n"
				+ "        public InsertInstruction beforeInsertOfNew(T newObject)\n"
				+ "        {\n"
				+ "            this.bumpCurrentVersion();\n"
				+ "            return super.beforeInsertOfNew(newObject);\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public UpdateInstruction matchedWithDifferenceBeforeAttributeCopy(T existing, T incoming)\n"
				+ "        {\n"
				+ "            this.bumpCurrentVersion();\n"
				+ "            return super.matchedWithDifferenceBeforeAttributeCopy(existing, incoming);\n"
				+ "        }\n"
				+ "\n"
				+ "        @Override\n"
				+ "        public DeleteOrTerminateInstruction beforeDeleteOrTerminate(T existing, MergeBuffer<T> mergeBuffer)\n"
				+ "        {\n"
				+ "            this.bumpCurrentVersion();\n"
				+ "            return super.beforeDeleteOrTerminate(existing, mergeBuffer);\n"
				+ "        }\n"
				+ "\n"
				+ "        private void bumpCurrentVersion()\n"
				+ "        {\n"
				+ "            if (" + name + "MergeHook.this.currentExisting == null)\n"
				+ "            {\n"
				+ "                throw new IllegalStateException(\"An owned child of " + name + " changed before any " + name + " was merged\");\n"
				+ "            }\n"
				+ "            " + name + "MergeHook.this.bumpVersion(" + name + "MergeHook.this.currentExisting, " + name + "MergeHook.this.currentIncoming);\n"
				+ "        }\n"
				+ "    }\n"
				+ "}\n";
		// @formatter:on

		return sourceCode;
	}

	/**
	 * Collects a deep finder navigation, like {@code OwnerFinder.items().grandchildren()}, for every owned descendant,
	 * so a change at any depth reaches the owned child merge hook. Stops at an association end already on the path,
	 * so a class that owns itself is navigated one level deep.
	 */
	private void collectOwnedNavigations(
		@Nonnull Klass klass,
		@Nonnull String navigation,
		@Nonnull ImmutableSet<AssociationEnd> associationEndsOnPath,
		@Nonnull MutableList<String> result
	)
	{
		klass
			.getAssociationEnds()
			.select(AssociationEnd::isOwned)
			.reject(AssociationEnd::isVersion)
			.reject(associationEndsOnPath::contains)
			.forEach((associationEnd) ->
			{
				String childNavigation = navigation + "." + associationEnd.getName() + "()";
				result.add(childNavigation);
				this.collectOwnedNavigations(
					associationEnd.getType(),
					childNavigation,
					associationEndsOnPath.newWith(associationEnd),
					result
				);
			});
	}

	private String getKeyPropertySourceCode(DataTypeProperty keyProperty)
	{
		String name = LOWER_CAMEL_TO_UPPER_CAMEL.convert(keyProperty.getName());
		String prefix = keyProperty.getType() == PrimitiveType.BOOLEAN ? "is" : "get";
		return "        version.set" + name + "(newObject." + prefix + name + "());\n";
	}
}

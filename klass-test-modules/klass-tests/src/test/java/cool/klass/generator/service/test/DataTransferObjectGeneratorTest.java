package cool.klass.generator.service.test;

import java.time.Instant;

import cool.klass.generator.dto.DataTransferObjectsGenerator;
import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.CompilerState;
import cool.klass.model.converter.compiler.KlassCompiler;
import cool.klass.model.converter.compiler.error.CompilerError;
import cool.klass.model.meta.domain.api.DomainModel;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.test.constants.KlassTestConstants;
import com.stackoverflow.meta.constants.StackOverflowDomainModel;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class DataTransferObjectGeneratorTest
{
    @Test
    public void stackOverflow()
    {
        CompilationUnit compilationUnit = CompilationUnit.createFromText(
                "example.klass",
                KlassTestConstants.STACK_OVERFLOW_SOURCE_CODE_TEXT);
        CompilerState         compilerState  = new CompilerState(compilationUnit);
        KlassCompiler         klassCompiler  = new KlassCompiler(compilerState);
        DomainModel           domainModel    = klassCompiler.compile();
        ImmutableList<String> compilerErrors = compilerState.getCompilerErrors().collect(CompilerError::toString);
        assertThat(compilerErrors, is(Lists.immutable.empty()));
        assertThat(domainModel, notNullValue());

        Instant now = Instant.parse("2019-12-31T23:59:59.999Z");

        DataTransferObjectsGenerator dataTransferObjectsGenerator = new DataTransferObjectsGenerator(domainModel, now);

        Klass  klass           = StackOverflowDomainModel.Question;
        String klassSourceCode = dataTransferObjectsGenerator.getClassSourceCode(klass);

        //<editor-fold desc="expected java code">
        //language=JAVA
        String expectedSourceCode = ""
                + "package com.stackoverflow.dto;\n"
                + "\n"
                + "import java.time.Instant;\n"
                + "import java.util.*;\n"
                + "\n"
                + "import javax.annotation.*;\n"
                + "import javax.validation.constraints.*;\n"
                + "\n"
                + "/**\n"
                + " * Auto-generated by {@link cool.klass.generator.dto.DataTransferObjectsGenerator}\n"
                + " */\n"
                + "@Generated(\n"
                + "        value = \"cool.klass.generator.dto.DataTransferObjectsGenerator\",\n"
                + "        date = \"2019-12-31T23:59:59.999Z\")\n"
                + "public class QuestionDTO\n"
                + "{\n"
                + "    private Long id;\n"
                + "    @NotNull\n"
                + "    private String title;\n"
                + "    @NotNull\n"
                + "    private String body;\n"
                + "    @NotNull\n"
                + "    private StatusDTO status;\n"
                + "    @NotNull\n"
                + "    private Boolean deleted;\n"
                + "    private Instant system;\n"
                + "    private Instant systemFrom;\n"
                + "    private Instant systemTo;\n"
                + "    @NotNull\n"
                + "    private String createdById;\n"
                + "    @NotNull\n"
                + "    private Instant createdOn;\n"
                + "    @NotNull\n"
                + "    private String lastUpdatedById;\n"
                + "\n"
                + "    private List<AnswerDTO> answers;\n"
                + "    private List<QuestionTagMappingDTO> tags;\n"
                + "    private QuestionVersionDTO version;\n"
                + "\n"
                + "    public Long getId()\n"
                + "    {\n"
                + "        return id;\n"
                + "    }\n"
                + "\n"
                + "    public void setId(Long id)\n"
                + "    {\n"
                + "        this.id = id;\n"
                + "    }\n"
                + "\n"
                + "    public String getTitle()\n"
                + "    {\n"
                + "        return title;\n"
                + "    }\n"
                + "\n"
                + "    public void setTitle(String title)\n"
                + "    {\n"
                + "        this.title = title;\n"
                + "    }\n"
                + "\n"
                + "    public String getBody()\n"
                + "    {\n"
                + "        return body;\n"
                + "    }\n"
                + "\n"
                + "    public void setBody(String body)\n"
                + "    {\n"
                + "        this.body = body;\n"
                + "    }\n"
                + "\n"
                + "    public StatusDTO getStatus()\n"
                + "    {\n"
                + "        return status;\n"
                + "    }\n"
                + "\n"
                + "    public void setStatus(StatusDTO status)\n"
                + "    {\n"
                + "        this.status = status;\n"
                + "    }\n"
                + "\n"
                + "    public Boolean getDeleted()\n"
                + "    {\n"
                + "        return deleted;\n"
                + "    }\n"
                + "\n"
                + "    public void setDeleted(Boolean deleted)\n"
                + "    {\n"
                + "        this.deleted = deleted;\n"
                + "    }\n"
                + "\n"
                + "    public Instant getSystem()\n"
                + "    {\n"
                + "        return system;\n"
                + "    }\n"
                + "\n"
                + "    public void setSystem(Instant system)\n"
                + "    {\n"
                + "        this.system = system;\n"
                + "    }\n"
                + "\n"
                + "    public Instant getSystemFrom()\n"
                + "    {\n"
                + "        return systemFrom;\n"
                + "    }\n"
                + "\n"
                + "    public void setSystemFrom(Instant systemFrom)\n"
                + "    {\n"
                + "        this.systemFrom = systemFrom;\n"
                + "    }\n"
                + "\n"
                + "    public Instant getSystemTo()\n"
                + "    {\n"
                + "        return systemTo;\n"
                + "    }\n"
                + "\n"
                + "    public void setSystemTo(Instant systemTo)\n"
                + "    {\n"
                + "        this.systemTo = systemTo;\n"
                + "    }\n"
                + "\n"
                + "    public String getCreatedById()\n"
                + "    {\n"
                + "        return createdById;\n"
                + "    }\n"
                + "\n"
                + "    public void setCreatedById(String createdById)\n"
                + "    {\n"
                + "        this.createdById = createdById;\n"
                + "    }\n"
                + "\n"
                + "    public Instant getCreatedOn()\n"
                + "    {\n"
                + "        return createdOn;\n"
                + "    }\n"
                + "\n"
                + "    public void setCreatedOn(Instant createdOn)\n"
                + "    {\n"
                + "        this.createdOn = createdOn;\n"
                + "    }\n"
                + "\n"
                + "    public String getLastUpdatedById()\n"
                + "    {\n"
                + "        return lastUpdatedById;\n"
                + "    }\n"
                + "\n"
                + "    public void setLastUpdatedById(String lastUpdatedById)\n"
                + "    {\n"
                + "        this.lastUpdatedById = lastUpdatedById;\n"
                + "    }\n"
                + "    public List<AnswerDTO> getAnswers()\n"
                + "    {\n"
                + "        return answers;\n"
                + "    }\n"
                + "\n"
                + "    public void setAnswers(List<AnswerDTO> answers)\n"
                + "    {\n"
                + "        this.answers = answers;\n"
                + "    }\n"
                + "\n"
                + "    public List<QuestionTagMappingDTO> getTags()\n"
                + "    {\n"
                + "        return tags;\n"
                + "    }\n"
                + "\n"
                + "    public void setTags(List<QuestionTagMappingDTO> tags)\n"
                + "    {\n"
                + "        this.tags = tags;\n"
                + "    }\n"
                + "\n"
                + "    public QuestionVersionDTO getVersion()\n"
                + "    {\n"
                + "        return version;\n"
                + "    }\n"
                + "\n"
                + "    public void setVersion(QuestionVersionDTO version)\n"
                + "    {\n"
                + "        this.version = version;\n"
                + "    }\n"
                + "}\n";
        //</editor-fold>

        assertThat(klassSourceCode, klassSourceCode, is(expectedSourceCode));
    }
}

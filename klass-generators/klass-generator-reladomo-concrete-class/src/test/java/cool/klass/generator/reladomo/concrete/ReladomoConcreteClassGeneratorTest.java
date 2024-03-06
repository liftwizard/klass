package cool.klass.generator.reladomo.concrete;

import java.util.Optional;

import cool.klass.model.converter.compiler.CompilationResult;
import cool.klass.model.converter.compiler.CompilationUnit;
import cool.klass.model.converter.compiler.KlassCompiler;
import cool.klass.model.converter.compiler.annotation.RootCompilerAnnotation;
import cool.klass.model.meta.domain.api.Klass;
import cool.klass.model.meta.domain.api.source.DomainModelWithSourceCode;
import org.eclipse.collections.api.list.ImmutableList;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class ReladomoConcreteClassGeneratorTest
{
    @Test
    public void smokeTest()
    {
        //<editor-fold desc="sourceCode">
        //language=Klass
        String klassSourceCode = """
                package cool.klass.test

                class ClassWithDerivedProperty
                {
                    key                : String key;

                    derivedRequiredString      : String    derived;
                    derivedRequiredInteger     : Integer   derived;
                    derivedRequiredLong        : Long      derived;
                    derivedRequiredDouble      : Double    derived;
                    derivedRequiredFloat       : Float     derived;
                    derivedRequiredBoolean     : Boolean   derived;
                    derivedRequiredInstant     : Instant   derived;
                    derivedRequiredLocalDate   : LocalDate derived;

                    derivedOptionalString      : String    ? derived;
                    derivedOptionalInteger     : Integer   ? derived;
                    derivedOptionalLong        : Long      ? derived;
                    derivedOptionalDouble      : Double    ? derived;
                    derivedOptionalFloat       : Float     ? derived;
                    derivedOptionalBoolean     : Boolean   ? derived;
                    derivedOptionalInstant     : Instant   ? derived;
                    derivedOptionalLocalDate   : LocalDate ? derived;
                }
                """;
        //</editor-fold>

        CompilationUnit compilationUnit = CompilationUnit.createFromText(
                0,
                Optional.empty(),
                "example.klass",
                klassSourceCode);
        KlassCompiler     compiler          = new KlassCompiler(compilationUnit);
        CompilationResult compilationResult = compiler.compile();

        if (compilationResult.domainModelWithSourceCode().isEmpty())
        {
            ImmutableList<RootCompilerAnnotation> compilerAnnotations = compilationResult.compilerAnnotations();
            String                                message             = compilerAnnotations.makeString("\n");
            fail(message);
        }
        else
        {
            DomainModelWithSourceCode domainModel = compilationResult.domainModelWithSourceCode().get();
            assertThat(domainModel, notNullValue());

            ReladomoConcreteClassGenerator generator = new ReladomoConcreteClassGenerator(domainModel);

            Klass  klass          = domainModel.getClassByName("ClassWithDerivedProperty");
            String javaSourceCode = generator.getSourceCode(klass);

            //<editor-fold desc="expected java code">
            //language=JAVA
            String expectedSourceCode = """
                    package cool.klass.test;

                    import java.sql.Timestamp;

                    import com.gs.fw.common.mithra.util.DefaultInfinityTimestamp;
                    import cool.klass.test.reladomo.readable.ClassWithDerivedProperty;

                    /**
                     * Auto-generated by {@link cool.klass.generator.reladomo.concrete.ReladomoConcreteClassGenerator}
                     */
                    public class ClassWithDerivedProperty
                            extends ClassWithDerivedPropertyAbstract
                            implements ReladomoReadableClassWithDerivedProperty
                    {
                        public ClassWithDerivedProperty(Timestamp system)
                        {
                            super(system);
                            // You must not modify this constructor. Mithra calls this internally.
                            // You can call this constructor. You can also add new constructors.
                        }

                        public ClassWithDerivedProperty()
                        {
                            this(DefaultInfinityTimestamp.getDefaultInfinity());
                        }

                        @Override
                        public String getDerivedRequiredString()
                        {
                            // TODO implement derived property: derivedRequiredString
                            return "ClassWithDerivedProperty derivedRequiredString 1 \\u261D";
                        }

                        @Override
                        public int getDerivedRequiredInteger()
                        {
                            // TODO implement derived property: derivedRequiredInteger
                            return 1;
                        }

                        @Override
                        public long getDerivedRequiredLong()
                        {
                            // TODO implement derived property: derivedRequiredLong
                            return 100000000000L;
                        }

                        @Override
                        public double getDerivedRequiredDouble()
                        {
                            // TODO implement derived property: derivedRequiredDouble
                            return 1.0123456789;
                        }

                        @Override
                        public float getDerivedRequiredFloat()
                        {
                            // TODO implement derived property: derivedRequiredFloat
                            return 1.0123457f;
                        }

                        @Override
                        public boolean getDerivedRequiredBoolean()
                        {
                            // TODO implement derived property: derivedRequiredBoolean
                            return true;
                        }

                        @Override
                        public Timestamp getDerivedRequiredInstant()
                        {
                            // TODO implement derived property: derivedRequiredInstant
                            return Instant.parse("1999-12-31T23:59:00Z");
                        }

                        @Override
                        public Date getDerivedRequiredLocalDate()
                        {
                            // TODO implement derived property: derivedRequiredLocalDate
                            return LocalDate.parse("1999-12-31");
                        }

                        @Override
                        public String getDerivedOptionalString()
                        {
                            // TODO implement derived property: derivedOptionalString
                            return "ClassWithDerivedProperty derivedOptionalString 1 \\u261D";
                        }

                        @Override
                        public int getDerivedOptionalInteger()
                        {
                            // TODO implement derived property: derivedOptionalInteger
                            return 1;
                        }

                        @Override
                        public long getDerivedOptionalLong()
                        {
                            // TODO implement derived property: derivedOptionalLong
                            return 100000000000L;
                        }

                        @Override
                        public double getDerivedOptionalDouble()
                        {
                            // TODO implement derived property: derivedOptionalDouble
                            return 1.0123456789;
                        }

                        @Override
                        public float getDerivedOptionalFloat()
                        {
                            // TODO implement derived property: derivedOptionalFloat
                            return 1.0123457f;
                        }

                        @Override
                        public boolean getDerivedOptionalBoolean()
                        {
                            // TODO implement derived property: derivedOptionalBoolean
                            return true;
                        }

                        @Override
                        public Timestamp getDerivedOptionalInstant()
                        {
                            // TODO implement derived property: derivedOptionalInstant
                            return Instant.parse("1999-12-31T23:59:00Z");
                        }

                        @Override
                        public Date getDerivedOptionalLocalDate()
                        {
                            // TODO implement derived property: derivedOptionalLocalDate
                            return LocalDate.parse("1999-12-31");
                        }
                    }
                    """;
            //</editor-fold>

            assertThat(javaSourceCode, javaSourceCode, is(expectedSourceCode));
        }
    }
}

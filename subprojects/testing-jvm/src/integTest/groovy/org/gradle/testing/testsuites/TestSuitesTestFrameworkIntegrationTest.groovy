/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.testing.testsuites

import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.JUnitXmlTestExecutionResult

class TestSuitesTestFrameworkIntegrationTest extends AbstractIntegrationSpec {

    def 'can use separate default JUnit frameworks for unit versus integration tests'() {
        given:
        buildFile << """
        plugins {
          id 'java'
        }

        ${mavenCentralRepository()}

        testing {
            suites {
                test {
                    testEngines.register(JUnitVintageTestEngine)
                }
                integTest(JvmTestSuite) // implicitly uses JUnit Jupiter
            }
        }

        tasks.named('check') {
            dependsOn testing.suites.integTest
        }
        """

        file('src/test/java/example/UnitTest.java') << '''
            package example;

            import org.junit.Assert;
            import org.junit.Test;

            public class UnitTest {
                @Test
                public void unitTest() {
                    Assert.assertTrue(true);
                }
            }
        '''

        file('src/integTest/java/it/IntegrationTest.java') << '''
            package it;

            import org.junit.jupiter.api.Assertions;
            import org.junit.jupiter.api.Test;

            public class IntegrationTest {
                @Test
                public void integrationTest() {
                    Assertions.assertTrue(true);
                }
            }
        '''

        when:
        succeeds 'check'

        then:
        result.assertTaskExecuted(':test')
        result.assertTaskExecuted(':integTest')

        def unitTestResults = new JUnitXmlTestExecutionResult(testDirectory)
        unitTestResults.assertTestClassesExecuted('example.UnitTest')
        def integTestResults = new JUnitXmlTestExecutionResult(testDirectory, 'build/test-results/integTest')
        integTestResults.assertTestClassesExecuted('it.IntegrationTest')
    }

    def 'can use JUnit vintage for unit tests and TestNG for integration tests'() {
        given:
        buildFile << """
        plugins {
          id 'java'
        }

        ${mavenCentralRepository()}

        testing {
            suites {
                test {
                    testEngines.register(JUnitVintageTestEngine)
                }
                integTest(JvmTestSuite) {
                    testEngines.register(TestNGTestEngine)
                }
            }
        }

        tasks.named('check') {
            dependsOn testing.suites.integTest
        }
        """

        file('src/test/java/example/UnitTest.java') << '''
            package example;

            import org.junit.Assert;
            import org.junit.Test;

            public class UnitTest {
                @Test
                public void unitTest() {
                    Assert.assertTrue(true);
                }
            }
        '''

        file('src/integTest/java/it/IntegrationTest.java') << '''
            package it;

            import org.testng.annotations.BeforeTest;
            import org.testng.annotations.Test;

            import static org.testng.Assert.assertEquals;

            public class IntegrationTest {

                protected int value = 0;

                @BeforeTest
                public void before() {
                    value = 1;
                }

                @Test
                public void shouldPass() {
                    assertEquals(1, value);
                }
            }
        '''

        when:
        succeeds 'check'

        then:
        result.assertTaskExecuted(':test')
        result.assertTaskExecuted(':integTest')

        def unitTestResults = new JUnitXmlTestExecutionResult(testDirectory)
        unitTestResults.assertTestClassesExecuted('example.UnitTest')
        def integTestResults = new JUnitXmlTestExecutionResult(testDirectory, 'build/test-results/integTest')
        integTestResults.assertTestClassesExecuted('it.IntegrationTest')
    }

    def 'can set explicit versions for different engines'() {
        given:
        buildFile << """
        plugins {
          id 'java'
        }

        ${mavenCentralRepository()}

        testing {
            suites {
                test {
                    testEngines.register(JUnitVintageTestEngine) {
                        version = '5.7.2'
                        apiVersion = '4.12'
                    }
                    targets.all {
                        testTask.configure {
                            doLast {
                                assert testFramework instanceof ${JUnitPlatformTestFramework.canonicalName}
                                assert classpath.files.any { it.name == "junit-vintage-engine-5.7.2.jar" }
                            }
                        }
                    }
                }
                integTest(JvmTestSuite) {
                    testEngines.register(TestNGTestEngine) {
                        version = '1.0.3'
                        apiVersion = '7.5.1'
                    }
                    targets.all {
                        testTask.configure {
                            doLast {
                                assert testFramework instanceof ${JUnitPlatformTestFramework.canonicalName}
                                assert classpath.files.any { it.name == "testng-engine-1.0.3.jar" }
                            }
                        }
                    }
                }
            }
        }

        tasks.named('compileTestJava') {
            doLast {
                assert classpath.files.any { it.name == "junit-4.12.jar" }
            }
        }

        tasks.named('compileIntegTestJava') {
            doLast {
                assert classpath.files.any { it.name == "testng-7.5.1.jar" }
            }
        }

        tasks.named('check') {
            dependsOn testing.suites.integTest
        }
        """

        file('src/test/java/example/UnitTest.java') << '''
            package example;

            import org.junit.Assert;
            import org.junit.Test;

            public class UnitTest {
                @Test
                public void unitTest() {
                    Assert.assertTrue(true);
                }
            }
        '''

        file('src/integTest/java/it/IntegrationTest.java') << '''
            package it;

            import org.testng.annotations.BeforeTest;
            import org.testng.annotations.Test;

            import static org.testng.Assert.assertEquals;

            public class IntegrationTest {

                protected int value = 0;

                @BeforeTest
                public void before() {
                    value = 1;
                }

                @Test
                public void shouldPass() {
                    assertEquals(1, value);
                }
            }
        '''

        when:
        succeeds 'dependencies', 'check'

        then:
        result.assertTaskExecuted(':test')
        result.assertTaskExecuted(':integTest')

        def unitTestResults = new JUnitXmlTestExecutionResult(testDirectory)
        unitTestResults.assertTestClassesExecuted('example.UnitTest')
        def integTestResults = new JUnitXmlTestExecutionResult(testDirectory, 'build/test-results/integTest')
        integTestResults.assertTestClassesExecuted('it.IntegrationTest')
    }
}

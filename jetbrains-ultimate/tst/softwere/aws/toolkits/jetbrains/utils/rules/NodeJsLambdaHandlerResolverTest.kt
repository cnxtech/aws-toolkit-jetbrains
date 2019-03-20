// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package softwere.aws.toolkits.jetbrains.utils.rules

import com.intellij.lang.javascript.psi.JSDefinitionExpression
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.runInEdtAndWait
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import software.aws.toolkits.jetbrains.services.lambda.LambdaHandlerResolver
import software.aws.toolkits.jetbrains.services.lambda.RuntimeGroup

class NodeJsLambdaHandlerResolverTest {
    @Rule
    @JvmField
    val projectRule = NodeJsCodeInsightTestFixtureRule()

    @Test
    fun determineHandler() {
        val content =
            """
                exports.lambdaHandler = async (event, context) => {
                    return "Hello World"
                }
            """.trimIndent()
        assertDetermineHandler(".", "app", content, "lambdaHandler", "app.lambdaHandler")
    }

    @Test
    fun determineHandler_noExports() {
        val content =
            """
                lambdaHandler = async (event, context) => {
                    return "Hello World"
                }
            """.trimIndent()
        assertDetermineHandler(".", "app", content, "lambdaHandler", null)
    }

    @Test
    fun determineHandler_tooFewParameter() {
        val content =
            """
                exports.lambdaHandler = async (event) => {
                    return "Hello World"
                }
            """.trimIndent()
        assertDetermineHandler(".", "app", content, "lambdaHandler", null)
    }

    @Test
    fun determineHandler_es5() {
        val content =
            """
                exports.lambdaHandler = function(event, context) {
                    return "Hello World"
                }
            """.trimIndent()
        assertDetermineHandler(".", "app", content, "lambdaHandler", "app.lambdaHandler")
    }

    @Test
    fun determineHandler_inSubFolder() {
        val content =
            """
                exports.lambdaHandler = async (event, context) => {
                    return "Hello World"
                }
            """.trimIndent()
        assertDetermineHandler("foo/bar", "app", content, "lambdaHandler", "foo/bar/app.lambdaHandler")
    }

    @Test
    fun determineHandler_notAFunction() {
        val content =
            """
                exports.lambdaHandler = "foo"
            """.trimIndent()
        assertDetermineHandler("foo/bar", "app", content, "lambdaHandler", null)
    }

    @Test
    fun exports_async_2parameters_returnTrue() {
        val fileContent = """
            exports.lambdaHandler = async (event, context) => {
                return "Hello World";
            }
        """.trimIndent()
        assertFindPsiElements("app.js", fileContent, "app.lambdaHandler", true)
    }

    @Test
    fun exports_async_3parameters_returnTrue() {
        val fileContent = """
            exports.lambdaHandler = async (event, context, callback) => {
                return "Hello World";
            }
        """.trimIndent()
        assertFindPsiElements("app.js", fileContent, "app.lambdaHandler", true)
    }

    @Test
    fun exports_async_1parameter_returnFalse() {
        val fileContent = """
            exports.lambdaHandler = async (event) => {
                return "Hello World";
            }
        """.trimIndent()
        assertFindPsiElements("app.js", fileContent, "app.lambdaHandler", false)
    }

    @Test
    fun nonexports_async_2parameters_returnFalse() {
        val fileContent = """
            var lambdaHandler = async (event, context) => {
                return "Hello World";
            }
        """.trimIndent()
        assertFindPsiElements("app.js", fileContent, "app.lambdaHandler", false)
    }

    @Test
    fun nonexports_asyncFunction_2parameters_returnFalse() {
        val fileContent = """
            async function lambdaHandler(event, context) {
                return "Hello World";
            }
        """.trimIndent()
        assertFindPsiElements("app.js", fileContent, "app.lambdaHandler", false)
    }

    @Test
    fun lambdaHandlerInSubfolder_handlerNameCorrect() {
        val fileContent = """
            exports.lambdaHandler = async (event, context) => {
                return "Hello World";
            }
        """.trimIndent()
        assertFindPsiElements("foo/app.js", fileContent, "foo/app.lambdaHandler", true)
    }

    @Test
    fun lambdaHandlerInSubfolder_handlerNameIncorrect() {
        val fileContent = """
            exports.lambdaHandler = async (event, context) => {
                return "Hello World";
            }
        """.trimIndent()
        assertFindPsiElements("foo/app.js", fileContent, "app.lambdaHandler", false)
    }

    private fun assertDetermineHandler(subPath: String, fileName: String, content: String, handlerName: String, expectedHandlerFullName: String?) {
        val handlerElement = projectRule.fixture.addLambdaHandler(
            subPath = subPath,
            fileName = fileName,
            handlerName = handlerName,
            fileContent = content
        )

        val resolver = LambdaHandlerResolver.getInstanceOrThrow(RuntimeGroup.NODEJS)

        runInEdtAndWait {
            assertThat(resolver.determineHandler(handlerElement)).isEqualTo(expectedHandlerFullName)
        }
    }

    private fun assertFindPsiElements(relativeFilePath: String, content: String, handler: String, shouldBeFound: Boolean) {
        projectRule.fixture.addFileToProject(relativeFilePath, content)
        val resolver = LambdaHandlerResolver.getInstanceOrThrow(RuntimeGroup.NODEJS)
        runInEdtAndWait {
            val project = projectRule.fixture.project
            val lambdas = resolver.findPsiElements(project, handler, GlobalSearchScope.allScope(project))
            if (shouldBeFound) {
                assertThat(lambdas).hasSize(1)
                assertThat(lambdas[0]).isInstanceOf(JSDefinitionExpression::class.java)
            } else {
                assertThat(lambdas).isEmpty()
            }
        }
    }
}
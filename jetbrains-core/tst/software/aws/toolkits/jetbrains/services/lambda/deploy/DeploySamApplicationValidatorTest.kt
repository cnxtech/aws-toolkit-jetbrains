// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.lambda.deploy

import assertk.Assert
import assertk.assert
import assertk.assertions.contains
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.testFramework.EdtRule
import com.intellij.testFramework.RuleChain
import com.intellij.testFramework.RunsInEdt
import com.intellij.testFramework.runInEdtAndGet
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import software.aws.toolkits.jetbrains.services.cloudformation.Parameter
import software.aws.toolkits.jetbrains.utils.rules.JavaCodeInsightTestFixtureRule
import software.aws.toolkits.resources.message
import javax.swing.DefaultComboBoxModel

@RunsInEdt
class DeploySamApplicationValidatorTest {
    private val projectRule = JavaCodeInsightTestFixtureRule()

    @Rule
    @JvmField
    val ruleChain = RuleChain(projectRule, EdtRule())

    private lateinit var view: DeployServerlessApplicationPanel
    private lateinit var sut: DeploySamApplicationValidator

    @Before
    fun wireMocksTogetherWithValidOptions() {

        val parameters = listOf<Parameter>(
                TestParameter("param1", "value1"),
                TestParameter("param2", "value2")
        )

        view = runInEdtAndGet {
            DeployServerlessApplicationPanel()
        }

        view.withTemplateParameters(parameters)

        view.updateStack.isSelected = true
        view.stacks.model = DefaultComboBoxModel(arrayOf("stack123"))
        view.stacks.selectedItem = "stack123"

        view.s3Bucket.model = DefaultComboBoxModel(arrayOf("bucket123"))
        view.s3Bucket.selectedItem = "bucket123"

        sut = DeploySamApplicationValidator(view)
    }

    @Test
    fun validInputsReturnsNull() {
        assert(sut.validateSettings()).isNull()
    }

    @Test
    fun validInputsWithNewStackReturnsNull() {
        view.createStack.isSelected = true
        view.newStackName.text = "createStack"
        assert(sut.validateSettings()).isNull()

        view.newStackName.text = "n"
        assert(sut.validateSettings()).isNull()

        view.newStackName.text = "n1"
        assert(sut.validateSettings()).isNull()
    }

    @Test
    fun stackMustBeSpecified() {
        view.stacks.selectedItem = null
        assert(sut.validateSettings()).containsMessage(message("serverless.application.deploy.validation.stack.missing"))
    }

    @Test
    fun newStackNameMustBeSpecified() {
        view.createStack.isSelected = true
        view.newStackName.text = null
        assert(sut.validateSettings()).containsMessage(message("serverless.application.deploy.validation.new.stack.name.missing"))
    }

    @Test
    fun invalidStackName_TooLong() {
        view.createStack.isSelected = true
        view.newStackName.text = "x".repeat(DeploySamApplicationValidator.MAX_STACK_NAME_LENGTH + 1)
        assert(sut.validateSettings()).containsMessage(
                message("serverless.application.deploy.validation.new.stack.name.too.long", DeploySamApplicationValidator.MAX_STACK_NAME_LENGTH)
        )
    }

    @Test
    fun invalidStackName_Duplicate() {
        view.createStack.isSelected = true
        view.newStackName.text = "bar"
        view.stacks.model = DefaultComboBoxModel(arrayOf("foo", "bar", "baz"))
        assert(sut.validateSettings()).containsMessage(message("serverless.application.deploy.validation.new.stack.name.duplicate"))
    }

    @Test
    fun invalidStackName_InvalidChars() {
        view.createStack.isSelected = true
        view.newStackName.text = "stack_1"
        assert(sut.validateSettings()).containsMessage(message("serverless.application.deploy.validation.new.stack.name.invalid"))

        view.newStackName.text = "stack 1"
        assert(sut.validateSettings()).containsMessage(message("serverless.application.deploy.validation.new.stack.name.invalid"))

        view.newStackName.text = "stack#1"
        assert(sut.validateSettings()).containsMessage(message("serverless.application.deploy.validation.new.stack.name.invalid"))

        view.newStackName.text = "1stack"
        assert(sut.validateSettings()).containsMessage(message("serverless.application.deploy.validation.new.stack.name.invalid"))

        view.newStackName.text = " stack"
        assert(sut.validateSettings()).containsMessage(message("serverless.application.deploy.validation.new.stack.name.invalid"))

        view.newStackName.text = "stack!@#$%^&*()_+-="
        assert(sut.validateSettings()).containsMessage(message("serverless.application.deploy.validation.new.stack.name.invalid"))
    }

    @Test
    fun templateParameterMissing_Single() {
        val parameters = listOf<Parameter>(
                TestParameter("param1", "value1"),
                TestParameter("param2", "")
        )
        view.withTemplateParameters(parameters)
        assert(sut.validateSettings()).containsMessage("Template values are missing:")
        assert(sut.validateSettings()).containsMessage("param2")
    }

    @Test
    fun templateParameterMissing_Multi() {
        val parameters = listOf<Parameter>(
                TestParameter("param1", ""),
                TestParameter("param2", "")
        )
        view.withTemplateParameters(parameters)
        assert(sut.validateSettings()).containsMessage("Template values are missing:")
        assert(sut.validateSettings()).containsMessage("param1")
        assert(sut.validateSettings()).containsMessage("param2")
    }

    @Test
    fun s3BucketMustBeSpecified() {
        view.s3Bucket.selectedItem = null
        assert(sut.validateSettings()).containsMessage(message("serverless.application.deploy.validation.s3.bucket.empty"))
    }

    private fun Assert<ValidationInfo?>.containsMessage(expectedMessage: String) {
        assert(this.actual).isNotNull { assert(it.actual.message).contains(expectedMessage) }
    }

    private class TestParameter(
        var name: String,
        var defaultValue: String
    ) : Parameter {
        override fun getScalarProperty(key: String): String {
            throw NotImplementedError()
        }

        override fun getOptionalScalarProperty(key: String): String? {
            throw NotImplementedError()
        }

        override fun setScalarProperty(key: String, value: String) {
            throw NotImplementedError()
        }

        var description: String? = null
        var constraintDescription: String? = null

        override val logicalName: String
            get() = name

        override fun defaultValue(): String? = defaultValue

        override fun description(): String? = description

        override fun constraintDescription(): String? = constraintDescription
    }
}
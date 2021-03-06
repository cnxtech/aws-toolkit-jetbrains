// Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.fixtures

import com.intellij.testGuiFramework.framework.Timeouts
import com.intellij.testGuiFramework.impl.button
import com.intellij.testGuiFramework.impl.combobox
import com.intellij.testGuiFramework.impl.jList
import com.intellij.testGuiFramework.util.scenarios.NewProjectDialogModel
import com.intellij.testGuiFramework.util.scenarios.NewProjectDialogModel.Constants.buttonFinish
import com.intellij.testGuiFramework.util.scenarios.NewProjectDialogModel.Constants.buttonNext
import com.intellij.testGuiFramework.util.scenarios.assertProjectPathExists
import com.intellij.testGuiFramework.util.scenarios.connectDialog
import com.intellij.testGuiFramework.util.scenarios.fileSystemUtils
import com.intellij.testGuiFramework.util.scenarios.selectSdk
import com.intellij.testGuiFramework.util.scenarios.typeProjectNameAndLocation
import com.intellij.testGuiFramework.util.scenarios.waitLoadingTemplates
import com.intellij.testGuiFramework.util.step

private const val AWS_GROUP = "AWS"

fun NewProjectDialogModel.assertAwsGroupPresent() {
    with(connectDialog()) {
        // TODO: PyCharm doesnt have Empty Project
        val list = jList(NewProjectDialogModel.Constants.groupEmptyProject, timeout = Timeouts.seconds05)

        step("check '$AWS_GROUP' group is present in the New Project dialog") {
            assert(list.contents().contains(AWS_GROUP)) {
                "'$AWS_GROUP' group is absent"
            }
        }
    }
}

fun NewProjectDialogModel.selectAwsProjectGroup() {
    with(connectDialog()) {
        step("select '$AWS_GROUP' project group") {
            waitLoadingTemplates()
            assertAwsGroupPresent()

            val list = jList(AWS_GROUP)
            step("click '$AWS_GROUP'") { list.clickItem(AWS_GROUP) }
            list.requireSelection(AWS_GROUP)
        }
    }
}

data class ServerlessProjectOptions(val runtime: String, val template: String)

fun NewProjectDialogModel.createServerlessProject(
    projectPath: String,
    templateOptions: ServerlessProjectOptions,
    projectSdk: String
) {
    with(guiTestCase) {
        fileSystemUtils.assertProjectPathExists(projectPath)

        with(connectDialog()) {
            selectAwsProjectGroup()

            val projectType = "AWS Serverless Application"
            step("select project type '$projectType'") {
                jList(projectType).clickItem(projectType)
            }

            step("setup '$projectType'") {
                button(buttonNext).click()

                typeProjectNameAndLocation(projectPath)

                step("select runtime '${templateOptions.runtime}'") {
                    combobox("Runtime:").selectItem(templateOptions.runtime)
                }

                step("select template '${templateOptions.template}'") {
                    combobox("SAM Template:").selectItem(templateOptions.template)
                }

                if (projectSdk.isNotEmpty()) {
                    selectSdk(projectSdk)
                }

                step("close New Project dialog with Finish") {
                    button(buttonFinish).click()
                    waitTillGone()
                }
            }
        }
    }
}
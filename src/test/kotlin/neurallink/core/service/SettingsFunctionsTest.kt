@file:Suppress("unused")

package neurallink.core.service

import neurallink.core.settings.NeuralLinkPluginSettings4
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLoggingLevel
import neurallink.core.settings.NeuralLinkPluginSettings5
import neurallink.core.settings.NeuralLinkPluginSettings6

class SettingsFunctionsTest : StringSpec({
    "loadJson gives error is JSON is not valid" {
        val badJson = """{'field": "value"}"""

        val maybeSettings = loadFromJson(badJson)
        maybeSettings.shouldBeLeft()
    }

    "loadJson returns default Settings if parameter is null" {
        val expectedSettings = NeuralLinkPluginSettings6.default()

        val maybeSettings = loadFromJson(null)
        val actualSettings = maybeSettings.shouldBeRight()
        actualSettings.version shouldBe expectedSettings.version
        actualSettings.taskRemoveRegex shouldBe expectedSettings.taskRemoveRegex
        actualSettings.columnTags.shouldContainAll(expectedSettings.columnTags)
        actualSettings.tagColors.shouldContainAll(expectedSettings.tagColors)
        actualSettings.logLevel shouldBe expectedSettings.logLevel
    }

    "loadJson correctly loads JSON for latest version" {
        val json = """{
            "version":"6",
            "taskRemoveRegex":
            "#kanban/[\\w-]+(\\s|${'$'})",
            "columnTags":[
                "backlog:Backlog:false",
                "scheduled:Scheduled:true",
                "inprogress:In Progress:false",
                "completed:Completed:false"
            ],
            "tagColors":{
                "personal":"13088C",
                "home":"460A60",
                "family":"8E791C",
                "marriage":"196515",
                "work":"D34807"
            },
            "logLevel": "DEBUG",
            "ignorePaths": []
        }""".trimIndent()
        val expectedSettings = NeuralLinkPluginSettings6.default()

        val maybeSettings = loadFromJson(json)
        val actualSettings = maybeSettings.shouldBeRight()
        actualSettings.version shouldBe expectedSettings.version
        actualSettings.taskRemoveRegex shouldBe expectedSettings.taskRemoveRegex
        actualSettings.columnTags.shouldContainAll(expectedSettings.columnTags)
        actualSettings.tagColors.shouldContainAll(expectedSettings.tagColors)
    }

    // *** toJson() ***
    "toJson creates the correct JSON" {
        val expectedJson = """{"version":"6","taskRemoveRegex":"#kanban/[\\w-]+(\\s|${'$'})","columnTags":["backlog:Backlog:false","scheduled:Scheduled:true","inprogress:In Progress:false","completed:Completed:false"],"tagColors":{"personal":"13088C","home":"460A60","family":"8E791C","marriage":"196515","work":"D34807"},"logLevel":"DEBUG","ignorePaths":[]}""".trimIndent()
        val settings = NeuralLinkPluginSettings6.default()

        val actualJson = toJson(settings)
        actualJson shouldBe expectedJson
    }

    // *** upgradeFrom4To5() ***
    "upgradeFrom4To5 correctly upgrades the settings" {
        val settings4 = Json.encodeToString(NeuralLinkPluginSettings4.default())

        val actualSettings5 = upgradeFrom4To5(settings4)
        actualSettings5.logLevel shouldBe KotlinLoggingLevel.DEBUG
    }

    // *** upgradeFrom5To6() ***
    "upgradeFrom5To6 correctly upgrades the settings" {
        val settings5 = Json.encodeToString(NeuralLinkPluginSettings5.default())

        val actualSettings6 = upgradeFrom5to6(settings5)
        actualSettings6.ignorePaths shouldHaveSize 0
    }
})

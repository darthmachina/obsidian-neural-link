@file:Suppress("unused")

package neurallink.core.service

import NeuralLinkPluginSettings4
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLoggingLevel

class SettingsFunctionsTest : StringSpec({
    "loadJson gives error is JSON is not valid" {
        val badJson = """{'field": "value"}"""

        val maybeSettings = loadFromJson(badJson)
        maybeSettings.shouldBeLeft()
    }

    "loadJson returns default Settings if parameter is null" {
        val expectedSettings = NeuralLinkPluginSettings5.default()

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
            "version":"5",
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
            "logLevel": "INFO"
        }""".trimIndent()
        val expectedSettings = NeuralLinkPluginSettings5.default()

        val maybeSettings = loadFromJson(json)
        val actualSettings = maybeSettings.shouldBeRight()
        actualSettings.version shouldBe expectedSettings.version
        actualSettings.taskRemoveRegex shouldBe expectedSettings.taskRemoveRegex
        actualSettings.columnTags.shouldContainAll(expectedSettings.columnTags)
        actualSettings.tagColors.shouldContainAll(expectedSettings.tagColors)
    }

    // *** toJson() ***
    "toJson creates the correct JSON" {
        val expectedJson = """{"version":"5","taskRemoveRegex":"#kanban/[\\w-]+(\\s|${'$'})","columnTags":["backlog:Backlog:false","scheduled:Scheduled:true","inprogress:In Progress:false","completed:Completed:false"],"tagColors":{"personal":"13088C","home":"460A60","family":"8E791C","marriage":"196515","work":"D34807"},"logLevel":"INFO"}""".trimIndent()
        val settings = NeuralLinkPluginSettings5.default()

        val actualJson = toJson(settings)
        actualJson shouldBe expectedJson
    }

    // *** upgradeFrom4To5() ***
    "upgradeFrom4To5 correctly upgrades the settings" {
        val settings4 = Json.encodeToString(NeuralLinkPluginSettings4.default())

        val actualSettings5 = upgradeFrom4To5(settings4)
        actualSettings5.logLevel shouldBe KotlinLoggingLevel.INFO
    }
})

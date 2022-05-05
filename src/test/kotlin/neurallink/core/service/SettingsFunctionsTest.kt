@file:Suppress("unused")

package neurallink.core.service

import NeuralLinkPluginSettings
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.shouldBe

class SettingsFunctionsTest : StringSpec({
    "loadJson gives error is JSON is not valid" {
        val badJson = """{'field": "value"}"""

        val maybeSettings = loadFromJson(badJson)
        maybeSettings.shouldBeLeft()
    }

    "loadJson returns default Settings if parameter is null" {
        val expectedSettings = NeuralLinkPluginSettings.default()

        val maybeSettings = loadFromJson(null)
        val actualSettings = maybeSettings.shouldBeRight()
        actualSettings.version shouldBe expectedSettings.version
        actualSettings.taskRemoveRegex shouldBe expectedSettings.taskRemoveRegex
        actualSettings.columnTags.shouldContainAll(expectedSettings.columnTags)
        actualSettings.tagColors.shouldContainAll(expectedSettings.tagColors)
    }

    "loadJson correctly loads JSON for latest version" {
        val json = """{
            "version":"4",
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
            }
        }""".trimIndent()
        val expectedSettings = NeuralLinkPluginSettings.default()

        val maybeSettings = loadFromJson(json)
        val actualSettings = maybeSettings.shouldBeRight()
        actualSettings.version shouldBe expectedSettings.version
        actualSettings.taskRemoveRegex shouldBe expectedSettings.taskRemoveRegex
        actualSettings.columnTags.shouldContainAll(expectedSettings.columnTags)
        actualSettings.tagColors.shouldContainAll(expectedSettings.tagColors)
    }

    // *** toJson() ***
    "toJson creates the correct JSON" {
        val expectedJson = """{"version":"4","taskRemoveRegex":"#kanban/[\\w-]+(\\s|${'$'})","columnTags":["backlog:Backlog:false","scheduled:Scheduled:true","inprogress:In Progress:false","completed:Completed:false"],"tagColors":{"personal":"13088C","home":"460A60","family":"8E791C","marriage":"196515","work":"D34807"}}""".trimIndent()
        val settings = NeuralLinkPluginSettings.default()

        val actualJson = toJson(settings)
        actualJson shouldBe expectedJson
    }
})

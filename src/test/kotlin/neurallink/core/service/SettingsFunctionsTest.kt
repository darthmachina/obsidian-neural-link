@file:Suppress("unused")

package neurallink.core.service

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldExistInOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe

class SettingsFunctionsTest : StringSpec({
    "loadJson gives error is JSON is not valid" {
        val badJson = """{'field": "value"}"""

        val maybeSettings = loadFromJson(badJson)
        val settingsError = maybeSettings.shouldBeLeft()

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

    }
})

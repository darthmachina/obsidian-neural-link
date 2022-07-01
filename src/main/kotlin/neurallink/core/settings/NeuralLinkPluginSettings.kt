package neurallink.core.settings

import kotlinx.serialization.Serializable
import mu.KotlinLoggingLevel
import neurallink.core.model.StatusTag
import neurallink.core.model.Tag
import neurallink.core.service.LoggingLevelSerializer

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class NeuralLinkPluginSettings6(
    val version: String,
    val taskRemoveRegex: String,
    val columnTags: List<StatusTag>,
    val tagColors: Map<Tag,String>,
    @Serializable(with = LoggingLevelSerializer::class) val logLevel: KotlinLoggingLevel,
    val ignorePaths: List<String>
) {
    companion object {
        fun default(): NeuralLinkPluginSettings6 {
            return NeuralLinkPluginSettings6(
                "5",
                """#kanban/[\w-]+(\s|$)""",
                listOf(
                    StatusTag(Tag("backlog"), "Backlog"),
                    StatusTag(Tag("scheduled"), "Scheduled", true),
                    StatusTag(Tag("inprogress"), "In Progress"),
                    StatusTag(Tag("completed"), "Completed")
                ),
                mapOf(
                    Tag("personal") to "13088C",
                    Tag("home") to "460A60",
                    Tag("family") to "8E791C",
                    Tag("marriage") to "196515",
                    Tag("work") to "D34807"
                ),
                KotlinLoggingLevel.DEBUG,
                emptyList()
            )
        }
    }
}

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class NeuralLinkPluginSettings5(
    val version: String,
    val taskRemoveRegex: String,
    val columnTags: List<StatusTag>,
    val tagColors: Map<Tag,String>,
    @Serializable(with = LoggingLevelSerializer::class) val logLevel: KotlinLoggingLevel
) {
    companion object {
        fun default(): NeuralLinkPluginSettings5 {
            return NeuralLinkPluginSettings5(
                "5",
                """#kanban/[\w-]+(\s|$)""",
                listOf(
                    StatusTag(Tag("backlog"), "Backlog"),
                    StatusTag(Tag("scheduled"), "Scheduled", true),
                    StatusTag(Tag("inprogress"), "In Progress"),
                    StatusTag(Tag("completed"), "Completed")
                ),
                mapOf(
                    Tag("personal") to "13088C",
                    Tag("home") to "460A60",
                    Tag("family") to "8E791C",
                    Tag("marriage") to "196515",
                    Tag("work") to "D34807"
                ),
                KotlinLoggingLevel.DEBUG
            )
        }
    }
}

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class NeuralLinkPluginSettings4(
    val version: String,
    val taskRemoveRegex: String,
    val columnTags: List<StatusTag>,
    val tagColors: Map<Tag,String>
) {
    companion object {
        fun default(): NeuralLinkPluginSettings4 {
            return NeuralLinkPluginSettings4(
                "4",
                """#kanban/[\w-]+(\s|$)""",
                listOf(
                    StatusTag(Tag("backlog"), "Backlog"),
                    StatusTag(Tag("scheduled"), "Scheduled", true),
                    StatusTag(Tag("inprogress"), "In Progress"),
                    StatusTag(Tag("completed"), "Completed")
                ),
                mapOf(
                    Tag("personal") to "13088C",
                    Tag("home") to "460A60",
                    Tag("family") to "8E791C",
                    Tag("marriage") to "196515",
                    Tag("work") to "D34807"
                )
            )
        }
    }
}

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class SettingsVersion(
    val version: String
)

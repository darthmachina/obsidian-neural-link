package service

import NeuralLinkPluginSettings
import Plugin
import SettingsVersion
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import model.TaskModel
import neurallink.core.model.StatusTag
import neurallink.core.model.Tag
import org.reduxkotlin.Store
import store.UpdateSettings

@Suppress("JSON_FORMAT_REDUNDANT")
@OptIn(ExperimentalJsExport::class)
@JsExport
class SettingsService(private val store: Store<TaskModel>, private val plugin: Plugin) {
    /**
     * Processes the results of a `loadData()` call.
     *
     * If there are no settings saved this will be the default settings. If there are settings but at a previous
     * version then the settings will be updated to the latest version by applying the default values for any new
     * settings.
     *
     * Settings are saved to the State but are also returned here for further processing if needed.
     *
     * @return A fully populated `NeuralLinkPluginSettings` object at the current version.
     */
    fun loadFromJson(json: Any?) {
        console.log("loadFromJson()")
        // TODO implement example of versioned settings
        if (json == null) {
            val newSettings = NeuralLinkPluginSettings.default()
            store.dispatch(UpdateSettings(plugin, this, newSettings.taskRemoveRegex, newSettings.columnTags))
        } else {
            console.log(" - jsonSettings: $json")
            when (Json { ignoreUnknownKeys = true }.decodeFromString<SettingsVersion>(json as String).version) {
//                "2" -> {
//                    console.log(" - Version 2 saved, updating settings")
//                    val jsonSettings = Json { ignoreUnknownKeys = true }.decodeFromString<NeuralLinkPluginSettings2>(json as String)
//                    dispatchUpdates(NeuralLinkPluginSettings.default().copy(
//                        taskRemoveRegex = jsonSettings.taskRemoveRegex,
//                        columnTags = jsonSettings.columnTags
//                    ))
//                }
                "4" -> {
                    console.log(" - Version 4, just loading")
                    val settings = Json { ignoreUnknownKeys = true }.decodeFromString<NeuralLinkPluginSettings>(json)
                    dispatchUpdates(settings)
                }
            }
        }
    }

    fun toJson(settings: NeuralLinkPluginSettings): String {
        val json = Json.encodeToString(settings)
        console.log("saveSettings: ", json)
        return json
    }

    private fun dispatchUpdates(settings: NeuralLinkPluginSettings) {
        store.dispatch(UpdateSettings(
            plugin,
            this,
            settings.taskRemoveRegex,
            settings.columnTags,
            settings.tagColors
        ))
    }
}

object StatusTagSerializer : KSerializer<StatusTag> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StatusTag", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): StatusTag {
        val statusTagParts = decoder.decodeString().split(":")
        return StatusTag(Tag(statusTagParts[0]), statusTagParts[1], if (statusTagParts.size == 3) statusTagParts[2].toBoolean() else false)
    }

    override fun serialize(encoder: Encoder, value: StatusTag) {
        encoder.encodeString("${value.tag.value}:${value.displayName}:${value.dateSort}")

    }
}

object TagSerializer : KSerializer<Tag> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Tag", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Tag {
        return Tag(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: Tag) {
        encoder.encodeString(value.value)
    }
}

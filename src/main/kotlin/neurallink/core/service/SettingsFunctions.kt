package neurallink.core.service

import NeuralLinkPluginSettings
import SettingsVersion
import arrow.core.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import neurallink.core.model.StatusTag
import neurallink.core.model.Tag

/**
 * Processes the results of a `loadData()` call.
 *
 * If there are no settings saved this will be the default settings. If there are settings but at a previous
 * version then the settings will be updated to the latest version by applying the default values for any new
 * settings.
 *
 * @return A fully populated `NeuralLinkPluginSettings` object at the current version.
 */
fun loadFromJson(json: Any?) : Either<LoadSettingsError, NeuralLinkPluginSettings> {
    console.log("loadFromJson()")
    return json.toOption()
        .fold(
            ifEmpty = {
                NeuralLinkPluginSettings
                    .default()
                    .right()
            },
            ifSome = {
                Either.catch {
                    when (Json { ignoreUnknownKeys = true }.decodeFromString<SettingsVersion>(json as String).version) {
//                "2" -> {
//                    console.log(" - Version 2 saved, updating settings")
//                    val jsonSettings = Json { ignoreUnknownKeys = true }.decodeFromString<NeuralLinkPluginSettings2>(json as String)
//                    NeuralLinkPluginSettings.default().copy(
//                        taskRemoveRegex = jsonSettings.taskRemoveRegex,
//                        columnTags = jsonSettings.columnTags
//                    )
//                }
                        "4" -> {
                            console.log(" - Version 4, just loading")
                            Json { ignoreUnknownKeys = true }
                                .decodeFromString<NeuralLinkPluginSettings>(json)
                                .right()
                        }
                        else -> {
                            console.log("ERROR Loading JSON", json)
                            Either.Left(LoadSettingsError("Cannot load JSON"))
                        }
                    }
                }
                .mapLeft {
                    LoadSettingsError("Cannot read JSON", it)
                }
                .flatten()
            }
        )
}

fun toJson(settings: NeuralLinkPluginSettings): String {
    return Json.encodeToString(settings)
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

package neurallink.core.service

import NeuralLinkPluginSettings4
import NeuralLinkPluginSettings5
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
import mu.KotlinLogging
import mu.KotlinLoggingLevel
import neurallink.core.model.StatusTag
import neurallink.core.model.Tag

private val logger = KotlinLogging.logger("SettingsFunctions")

/**
 * Processes the results of a `loadData()` call.
 *
 * If there are no settings saved this will be the default settings. If there are settings but at a previous
 * version then the settings will be updated to the latest version by applying the default values for any new
 * settings.
 *
 * @return A fully populated `NeuralLinkPluginSettings` object at the current version.
 */
fun loadFromJson(json: Any?) : Either<LoadSettingsError, NeuralLinkPluginSettings5> {
    logger.debug { "loadFromJson()" }
    return json.toOption()
        .fold(
            ifEmpty = {
                NeuralLinkPluginSettings5
                    .default()
                    .right()
            },
            ifSome = {
                Either.catch {
                    when (Json { ignoreUnknownKeys = true }.decodeFromString<SettingsVersion>(json as String).version) {
                        "5" -> {
                            logger.debug { " - Version 5, just loading" }
                            Json { ignoreUnknownKeys = true }
                                .decodeFromString<NeuralLinkPluginSettings5>(json)
                                .right()
                        }
                        "4" -> upgradeFrom4To5(json).right()
                        else -> {
                            logger.error { "ERROR Loading JSON : $json" }
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

fun toJson(settings: NeuralLinkPluginSettings5): String {
    return Json.encodeToString(settings)
}

fun upgradeFrom4To5(json: String) : NeuralLinkPluginSettings5 {
    logger.debug { "upgradeFrom4to5()" }
    val jsonSettings = Json { ignoreUnknownKeys = true }.decodeFromString<NeuralLinkPluginSettings4>(json)
    return NeuralLinkPluginSettings5.default().copy(
        taskRemoveRegex = jsonSettings.taskRemoveRegex,
        columnTags = jsonSettings.columnTags,
        tagColors = jsonSettings.tagColors
    )
}

object LoggingLevelSerializer : KSerializer<KotlinLoggingLevel> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LogLevel", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): KotlinLoggingLevel {
        return KotlinLoggingLevel.valueOf(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: KotlinLoggingLevel) {
        encoder.encodeString(value.name)
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

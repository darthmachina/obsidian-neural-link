package neurallink.core.service

import neurallink.core.settings.NeuralLinkPluginSettings4
import neurallink.core.settings.NeuralLinkPluginSettings5
import neurallink.core.settings.SettingsVersion
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
import neurallink.core.settings.NeuralLinkPluginSettings6

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
fun loadFromJson(json: Any?) : Either<LoadSettingsError, NeuralLinkPluginSettings6> {
    logger.debug { "loadFromJson()" }
    return json.toOption()
        .fold(
            ifEmpty = {
                NeuralLinkPluginSettings6
                    .default()
                    .right()
            },
            ifSome = {
                Either.catch {
                    when (Json { ignoreUnknownKeys = true }.decodeFromString<SettingsVersion>(json as String).version) {
                        "6" -> {
                            logger.debug { " - Version 6, just loading" }
                            Json { ignoreUnknownKeys = true }
                                .decodeFromString<NeuralLinkPluginSettings6>(json)
                                .right()
                        }
                        "5" -> upgradeFrom5to6(json).right()
                        "4" -> upgradeFrom5to6(upgradeFrom4To5(json)).right()
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

fun upgradeFrom5to6(settings5: NeuralLinkPluginSettings5) : NeuralLinkPluginSettings6 {
    logger.debug { "upgradeFrom5to6(settings5)" }
    return NeuralLinkPluginSettings6.default().copy(
        taskRemoveRegex = settings5.taskRemoveRegex,
        columnTags = settings5.columnTags,
        tagColors = settings5.tagColors,
        logLevel = settings5.logLevel
    )
}

fun upgradeFrom5to6(json: String) : NeuralLinkPluginSettings6 {
    logger.debug { "updateFrom5to6(json)" }
    val jsonSettings = Json { ignoreUnknownKeys = true }.decodeFromString<NeuralLinkPluginSettings5>(json)
    return upgradeFrom5to6(jsonSettings)
}

fun upgradeFrom4to5(settings4: NeuralLinkPluginSettings4) : NeuralLinkPluginSettings5 {
    logger.debug { "upgradeFrom4to5(settings4)" }
    return NeuralLinkPluginSettings5.default().copy(
        taskRemoveRegex = settings4.taskRemoveRegex,
        columnTags = settings4.columnTags,
        tagColors = settings4.tagColors
    )
}

fun upgradeFrom4To5(json: String) : NeuralLinkPluginSettings5 {
    logger.debug { "upgradeFrom4to5(json)" }
    val jsonSettings = Json { ignoreUnknownKeys = true }.decodeFromString<NeuralLinkPluginSettings4>(json)
    return upgradeFrom4to5(jsonSettings)
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

package neurallink.core.service

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun <A> A.log(marker: String, msg: (A) -> Any? ): A {
    logger.debug { "$marker ${msg.invoke(it)}" }
    return this
}
package neurallink.core.service

import mu.KotlinLogging

private val logger = KotlinLogging.logger { "UtilFunctions" }

fun <A> A.log(marker: String, f: () -> Any? ): A {
    logger.debug { "$marker: ${f()}" }
    return this
}

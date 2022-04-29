package neurallink.core

/**
 * Creates a nice String representation of a map
 */
fun mapToString(map: MutableMap<String, String>, prepend: String) : String {
    return prepend + map.map { (key, value) -> "[$key, $value]" }.joinToString(", ")
}
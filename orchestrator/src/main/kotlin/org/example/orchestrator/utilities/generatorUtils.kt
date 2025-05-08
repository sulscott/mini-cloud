package org.example.orchestrator.utilities

/**
 * Extension function on StringBuilder to append an indented line.
 * @param level Number of indentation levels (2 spaces per level)
 * @param line The line of text to append
 */
fun StringBuilder.indent(level: Int, line: String) {
    appendLine("  ".repeat(level) + line)
}
package com.manjee.linkops.domain.model

/**
 * Configuration for firing an Android Intent via ADB
 */
data class IntentConfig(
    val uri: String,
    val action: String = ACTION_VIEW,
    val flags: Set<IntentFlag> = emptySet(),
    val packageName: String? = null
) {
    enum class IntentFlag(val value: String) {
        ACTIVITY_NEW_TASK("--activity-new-task"),
        ACTIVITY_CLEAR_TOP("--activity-clear-top"),
        ACTIVITY_SINGLE_TOP("--activity-single-top"),
        ACTIVITY_CLEAR_TASK("--activity-clear-task")
    }

    /**
     * Converts this config to an ADB shell command
     * Example: am start -a android.intent.action.VIEW -d "myapp://path" --activity-new-task
     */
    fun toAdbCommand(): String {
        val parts = mutableListOf("am", "start", "-a", action, "-d", "\"$uri\"")

        flags.forEach { flag ->
            parts.add(flag.value)
        }

        packageName?.let {
            parts.add("-p")
            parts.add(it)
        }

        return parts.joinToString(" ")
    }

    companion object {
        const val ACTION_VIEW = "android.intent.action.VIEW"
        const val ACTION_SEND = "android.intent.action.SEND"
    }
}

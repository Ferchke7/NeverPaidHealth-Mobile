package dev.neverpaid.health.core.platform

expect object PlatformBridge {
    fun vibrate()
    fun showToast(message: String)
    fun currentTimeMillis(): Long
    fun generateUuid(): String
    fun formatDate(epochMs: Long, pattern: String = "dd MMM yyyy"): String
}

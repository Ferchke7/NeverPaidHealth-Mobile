package dev.neverpaid.health.core.error

enum class AppErrorType {
    Network,
    Storage,
    Validation,
    Server,
    Unknown
}

data class AppError(
    override val message: String,
    val type: AppErrorType = AppErrorType.Unknown,
    override val cause: Throwable? = null
) : Exception(message, cause)

fun Throwable.toAppError(): AppError {
    return AppError(
        message = message ?: "An unexpected error occurred",
        type = AppErrorType.Unknown,
        cause = this
    )
}

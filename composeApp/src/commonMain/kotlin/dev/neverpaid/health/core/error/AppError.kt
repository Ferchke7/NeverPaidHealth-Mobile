package dev.neverpaid.health.core.error

enum class AppErrorType {
    Network,
    Storage,
    Validation,
    Server,
    Unknown
}

data class AppError(
    val message: String,
    val type: AppErrorType = AppErrorType.Unknown,
    val cause: Throwable? = null
)

fun Throwable.toAppError(): AppError {
    return AppError(
        message = localizedMessage ?: "An unexpected error occurred",
        type = AppErrorType.Unknown,
        cause = this
    )
}

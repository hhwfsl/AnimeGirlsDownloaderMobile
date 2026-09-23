package top.kafuumiaki.animegirlsdownloader.core.network

import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import top.kafuumiaki.animegirlsdownloader.core.model.AppError
import java.io.IOException

fun Throwable.toAppError(): AppError = when (this) {
    is HttpException -> when (code()) {
        401, 403 -> AppError(AppError.Code.UNAUTHORIZED, message())
        404 -> AppError(AppError.Code.NOT_FOUND, message())
        413 -> AppError(AppError.Code.TOO_LARGE, message())
        else -> AppError(AppError.Code.UNKNOWN, "HTTP ${code()}")
    }
    is IOException -> AppError(AppError.Code.NETWORK, message)
    is SerializationException -> AppError(AppError.Code.INVALID_DATA, message)
    else -> AppError(AppError.Code.UNKNOWN, message)
}

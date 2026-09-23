package top.kafuumiaki.animegirlsdownloader.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object AvatarImageProcessor {
    fun decode(contentResolver: ContentResolver, uri: Uri, maxDimension: Int = 2048): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val width = info.size.width
                val height = info.size.height
                val scale = min(1f, maxDimension.toFloat() / max(width, height))
                decoder.setTargetSize(max(1, (width * scale).roundToInt()), max(1, (height * scale).roundToInt()))
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > maxDimension * 2) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            contentResolver.openInputStream(uri).use { input ->
                BitmapFactory.decodeStream(input, null, options)
                    ?: error("The selected image could not be decoded.")
            }
        }
    }

    fun cropRect(bitmap: Bitmap, zoom: Float, offsetX: Float, offsetY: Float): Rect {
        val cropSize = max(1, (min(bitmap.width, bitmap.height) / zoom.coerceIn(1f, 3f)).roundToInt())
        val maxLeft = max(0, bitmap.width - cropSize)
        val maxTop = max(0, bitmap.height - cropSize)
        val left = ((1f - offsetX.coerceIn(-1f, 1f)) * maxLeft / 2f).roundToInt().coerceIn(0, maxLeft)
        val top = ((1f - offsetY.coerceIn(-1f, 1f)) * maxTop / 2f).roundToInt().coerceIn(0, maxTop)
        return Rect(left, top, left + cropSize, top + cropSize)
    }

    fun encodeCircularPng(
        bitmap: Bitmap,
        zoom: Float,
        offsetX: Float,
        offsetY: Float,
        outputSize: Int = 512,
    ): ByteArray {
        val output = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(output)
            val path = Path().apply { addCircle(outputSize / 2f, outputSize / 2f, outputSize / 2f, Path.Direction.CW) }
            canvas.clipPath(path)
            canvas.drawBitmap(
                bitmap,
                cropRect(bitmap, zoom, offsetX, offsetY),
                Rect(0, 0, outputSize, outputSize),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
            )
            return ByteArrayOutputStream().use { stream ->
                check(output.compress(Bitmap.CompressFormat.PNG, 100, stream))
                stream.toByteArray()
            }
        } finally {
            output.recycle()
        }
    }
}

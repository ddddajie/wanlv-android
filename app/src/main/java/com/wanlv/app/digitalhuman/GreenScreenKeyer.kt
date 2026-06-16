package com.wanlv.app.digitalhuman

import android.graphics.Bitmap
import android.graphics.Color
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import org.webrtc.VideoFrame

object GreenScreenKeyer {
    fun keyBitmap(source: Bitmap): Bitmap {
        val bitmap = source.copy(Bitmap.Config.ARGB_8888, false)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (index in pixels.indices) {
            val color = pixels[index]
            val red = Color.red(color)
            val green = Color.green(color)
            val blue = Color.blue(color)
            pixels[index] = if (isGreenScreen(red, green, blue)) {
                Color.TRANSPARENT
            } else {
                Color.argb(Color.alpha(color), red, green, blue)
            }
        }
        return Bitmap.createBitmap(pixels, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    }

    fun keyVideoFrame(frame: VideoFrame, maxWidth: Int = 280): Bitmap? {
        val i420 = frame.buffer.toI420() ?: return null
        return try {
            val sourceWidth = i420.width
            val sourceHeight = i420.height
            if (sourceWidth <= 0 || sourceHeight <= 0) return null

            val targetWidth = min(maxWidth, sourceWidth)
            val targetHeight = max(1, sourceHeight * targetWidth / sourceWidth)
            val pixels = IntArray(targetWidth * targetHeight)
            val dataY = i420.dataY
            val dataU = i420.dataU
            val dataV = i420.dataV
            val strideY = i420.strideY
            val strideU = i420.strideU
            val strideV = i420.strideV

            for (targetY in 0 until targetHeight) {
                val sourceY = targetY * sourceHeight / targetHeight
                for (targetX in 0 until targetWidth) {
                    val sourceX = targetX * sourceWidth / targetWidth
                    val y = dataY.uByte(sourceY * strideY + sourceX)
                    val u = dataU.uByte((sourceY / 2) * strideU + sourceX / 2) - 128
                    val v = dataV.uByte((sourceY / 2) * strideV + sourceX / 2) - 128
                    val red = clamp(y + 1.402f * v)
                    val green = clamp(y - 0.344136f * u - 0.714136f * v)
                    val blue = clamp(y + 1.772f * u)
                    pixels[targetY * targetWidth + targetX] = if (isGreenScreen(red, green, blue)) {
                        Color.TRANSPARENT
                    } else {
                        Color.argb(255, red, green, blue)
                    }
                }
            }
            Bitmap.createBitmap(pixels, targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        } finally {
            i420.release()
        }
    }

    private fun isGreenScreen(red: Int, green: Int, blue: Int): Boolean {
        val dominantGreen = green > 70 && green - red > 18 && green - blue > 18
        val greenRatio = green > red * 1.12f && green > blue * 1.12f
        val notTooBrightClothes = !(red > 130 && green > 150 && blue > 130)
        // 重点：数字人源素材带绿幕，这里对偏绿色背景做轻量抠像，避免地图上出现整块绿色底。
        return dominantGreen && greenRatio && notTooBrightClothes
    }

    private fun clamp(value: Float): Int = value.toInt().coerceIn(0, 255)

    private fun ByteBuffer.uByte(index: Int): Int = get(index).toInt() and 0xFF
}


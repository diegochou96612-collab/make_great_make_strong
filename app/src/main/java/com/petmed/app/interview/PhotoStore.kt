package com.petmed.app.interview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream

/**
 * 照片處理。所有進入相簿的照片都會：依 EXIF 轉正 → 縮小 → 重新編碼成 JPEG。
 * 重新編碼後**不再含拍攝位置等 EXIF**（去識別化），而且檔案較小。
 * 檔案存在 App 私有資料夾，不會出現在系統相簿。
 */
object PhotoStore {

    const val MAX_EDGE = 1280

    val topics = listOf("皮膚或毛髮", "耳朵", "眼睛", "嘔吐物", "糞便", "傷口", "藥袋或標籤", "飼料或零食包裝", "疫苗手冊", "其他")

    private fun dir(context: Context, caseId: Long): File =
        File(context.filesDir, "case_photos/$caseId").apply { mkdirs() }

    /** 相機拍攝時寫入的暫存原始檔（處理完會刪除） */
    fun newCameraTarget(context: Context, caseId: Long): File =
        File(dir(context, caseId), "raw_${System.currentTimeMillis()}.jpg")

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** 純函式：決定 inSampleSize，使最長邊不超過 maxEdge 的 2 倍（之後再精確縮到 maxEdge）。 */
    fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        if (width <= 0 || height <= 0 || maxEdge <= 0) return 1
        var s = 1
        while (maxOf(width, height) / (s * 2) >= maxEdge) s *= 2
        return s
    }

    /** 純函式：把最長邊縮到 maxEdge（不放大），回傳新的寬高。 */
    fun fitWithin(width: Int, height: Int, maxEdge: Int): Pair<Int, Int> {
        val longest = maxOf(width, height)
        if (longest <= maxEdge || longest <= 0) return width to height
        val scale = maxEdge.toFloat() / longest
        return maxOf(1, (width * scale).toInt()) to maxOf(1, (height * scale).toInt())
    }

    /** 處理相機拍出的原始檔，成功回傳相簿內的新檔案並刪除原始檔。 */
    fun processCameraFile(context: Context, caseId: Long, raw: File): File? {
        val orientation = try {
            ExifInterface(raw.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: Exception) { ExifInterface.ORIENTATION_NORMAL }
        val out = process(context, caseId, { raw.inputStream() }, orientation)
        raw.delete()
        return out
    }

    /** 處理從系統相簿選的照片。 */
    fun processUri(context: Context, caseId: Long, uri: Uri): File? {
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) { ExifInterface.ORIENTATION_NORMAL }
        return process(context, caseId, { context.contentResolver.openInputStream(uri) }, orientation)
    }

    private fun process(context: Context, caseId: Long, open: () -> InputStream?, orientation: Int): File? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE) }
            var bmp = open()?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

            val (w, h) = fitWithin(bmp.width, bmp.height, MAX_EDGE)
            if (w != bmp.width || h != bmp.height) {
                val scaled = Bitmap.createScaledBitmap(bmp, w, h, true)
                if (scaled !== bmp) bmp.recycle()
                bmp = scaled
            }
            val rotated = rotate(bmp, orientation)
            if (rotated !== bmp) bmp.recycle()

            val out = File(dir(context, caseId), "photo_${System.currentTimeMillis()}.jpg")
            out.outputStream().use { rotated.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            rotated.recycle()
            out
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private fun rotate(src: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            else -> return src
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    /** 讀取縮圖（顯示用） */
    fun decodeThumb(path: String, maxEdge: Int): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge) }
        BitmapFactory.decodeFile(path, opts)
    } catch (e: Exception) { null } catch (e: OutOfMemoryError) { null }

    fun deleteFile(path: String) { runCatching { File(path).delete() } }

    /** 刪除一個狀況的整個相簿資料夾 */
    fun deleteAlbum(context: Context, caseId: Long) {
        runCatching { File(context.filesDir, "case_photos/$caseId").deleteRecursively() }
    }
}

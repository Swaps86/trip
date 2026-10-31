package com.tripsplit.app

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Backup(val trips: List<Trip>, val cats: List<String>, val saved: Long)

fun parseBackup(text: String): Backup? = try {
    val o = JSONObject(text)
    if (o.optString("app") != "TripSplit") null else {
        val a = o.getJSONArray("trips")
        Backup(List(a.length()) { tripFrom(a.getJSONObject(it)) },
            o.optJSONArray("cats")?.strings() ?: emptyList(), o.optLong("saved", 0L))
    }
} catch (e: Exception) { null }

fun backupFileName(prefix: String = "TripSplit_Backup") =
    "${prefix}_${SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())}.json"

fun saveTextToDownloads(ctx: Context, name: String, text: String): Boolean = try {
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, name)
        put(MediaStore.Downloads.MIME_TYPE, "application/json")
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }
    val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
    if (uri == null) false else {
        ctx.contentResolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) }
        true
    }
} catch (e: Exception) { false }

fun shareBackupFile(ctx: Context, name: String, text: String) {
    val dir = File(ctx.cacheDir, "reports").apply { mkdirs() }
    val f = File(dir, name); f.writeText(text)
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
    val i = Intent(Intent.ACTION_SEND).setType("application/json")
        .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    ctx.startActivity(Intent.createChooser(i, "Share backup"))
}

fun readTextFromUri(ctx: Context, uri: Uri): String? = try {
    ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
} catch (e: Exception) { null }

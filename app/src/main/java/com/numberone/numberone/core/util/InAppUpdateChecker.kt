package com.numberone.numberone.core.util

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.numberone.numberone.core.localization.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private data class RemoteVersionInfo(
    val code: Long,
    val name: String,
    val force: Boolean,
    val url: String,
    val notes: String
)

@Composable
fun InAppUpdateDialog() {
    val context = LocalContext.current
    var update by remember { mutableStateOf<RemoteVersionInfo?>(null) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val info = withContext(Dispatchers.IO) {
            runCatching {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentCode = if (Build.VERSION.SDK_INT >= 28) pInfo.longVersionCode else @Suppress("DEPRECATION") pInfo.versionCode.toLong()
                val conn = (URL("https://numberone-vip.github.io/numberone-app/version.json?t=" + System.currentTimeMillis()).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val remoteCode = json.optLong("latestVersionCode", currentCode)
                if (remoteCode > currentCode) {
                    RemoteVersionInfo(
                        code = remoteCode,
                        name = json.optString("latestVersionName", ""),
                        force = json.optBoolean("forceUpdate", false),
                        url = json.optString("downloadUrl", "https://numberone-vip.github.io/numberone-app/"),
                        notes = json.optString("notesAr", "")
                    )
                } else null
            }.getOrNull()
        }
        if (info != null) update = info
    }

    val info = update
    if (info != null && (!dismissed || info.force)) {
        AlertDialog(
            onDismissRequest = { if (!info.force) dismissed = true },
            title = {
                Text(
                    if (info.name.isNotBlank()) tr("تحديث جديد متوفر") + " (v${info.name})"
                    else tr("تحديث جديد متوفر"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(info.notes.ifBlank { tr("يتوفر إصدار أحدث من التطبيق. حدّث الآن للحصول على آخر الميزات والتحسينات.") })
            },
            confirmButton = {
                Button(onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }) { Text(tr("تحديث الآن"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = if (info.force) null else ({
                TextButton(onClick = { dismissed = true }) { Text(tr("لاحقاً")) }
            })
        )
    }
}

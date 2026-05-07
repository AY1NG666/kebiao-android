package com.ayng.kebiao.data.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String,
)

data class UpdateCheckResult(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val updateInfo: UpdateInfo?,
    val error: String? = null,
)

class UpdateManager(private val context: Context) {

    companion object {
        private const val UPDATE_URL = "http://103.236.97.252:8888/version.json"
    }

    private var downloadId: Long = -1L

    /** Get current app version */
    fun getCurrentVersion(): String {
        return try {
            val pkgInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pkgInfo.versionName} (${pkgInfo.versionCode})"
        } catch (e: PackageManager.NameNotFoundException) {
            "未知"
        }
    }

    fun getCurrentVersionCode(): Int {
        return try {
            val pkgInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode
            }
        } catch (_: Exception) {
            0
        }
    }

    /** Check for updates from the server */
    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val json = fetchJson(UPDATE_URL)
            val obj = JSONObject(json)
            val remoteVersionCode = obj.getInt("versionCode")
            val remoteVersionName = obj.getString("versionName")
            val apkUrl = obj.getString("apkUrl")
            val releaseNotes = obj.optString("releaseNotes", "")

            val currentCode = getCurrentVersionCode()
            val updateInfo = UpdateInfo(
                versionCode = remoteVersionCode,
                versionName = remoteVersionName,
                apkUrl = apkUrl,
                releaseNotes = releaseNotes,
            )

            UpdateCheckResult(
                hasUpdate = remoteVersionCode > currentCode,
                currentVersion = getCurrentVersion(),
                updateInfo = updateInfo,
            )
        } catch (e: Exception) {
            UpdateCheckResult(
                hasUpdate = false,
                currentVersion = getCurrentVersion(),
                updateInfo = null,
                error = e.message ?: "检查更新失败",
            )
        }
    }

    /** Download APK via system DownloadManager and register receiver to install */
    fun downloadAndInstall(updateInfo: UpdateInfo, onDownloadStarted: () -> Unit) {
        val fileName = "kebiao-${updateInfo.versionName}.apk"
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        file.delete() // remove old version if exists

        val request = DownloadManager.Request(Uri.parse(updateInfo.apkUrl))
            .setTitle("课表更新")
            .setDescription("正在下载 v${updateInfo.versionName}...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(file))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = dm.enqueue(request)

        // Register broadcast receiver to auto-install when download completes
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk(file)
                    ctx.unregisterReceiver(this)
                }
            }
        }
        context.registerReceiver(
            onComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_NOT_EXPORTED,
        )

        onDownloadStarted()
    }

    /** Trigger APK installation via FileProvider */
    private fun installApk(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Grant permission to installer
        context.grantUriPermission(
            "com.android.packageinstaller",
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )

        context.startActivity(intent)
    }

    private fun fetchJson(urlStr: String): String {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.requestMethod = "GET"
        return conn.inputStream.bufferedReader().use { it.readText() }.also {
            conn.disconnect()
        }
    }
}

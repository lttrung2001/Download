package com.ltbth.download

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.database.Cursor
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


    }

    fun download(link: String): Long {
        val uri = Uri.parse(link)
        val request = DownloadManager.Request(uri)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or
                                        DownloadManager.Request.NETWORK_MOBILE)
            .setTitle(uri.lastPathSegment)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            .setDescription("Android Data download using DownloadManager.")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                File.separator+uri.lastPathSegment)
        val downloadManager = application.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val downloadPreference = downloadManager.enqueue(request)
        Download.downloads.add(Download(downloadPreference, uri.lastPathSegment ?: "unknown name"))
        return downloadPreference
    }

    fun getDownloadStatus(downloadId: Long): DownloadInfo {
        val query = DownloadManager.Query()
        query.setFilterById(downloadId)
        val downloadManager = application.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val cursor = downloadManager.query(query)
        if (cursor?.moveToFirst() == true) {
            return downloadStatus(cursor)
        }
        return DownloadInfo.DontHasInfo
    }

    @SuppressLint("Range")
    private fun downloadStatus(cursor: Cursor): DownloadInfo {
        val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
        val status = cursor.getInt(columnIndex)
        val columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
        val reason = cursor.getInt(columnReason)
        val name = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE))
        var statusText = ""
        var reasonText = ""

        when (status) {
            DownloadManager.STATUS_FAILED -> {
                statusText = "STATUS_FAILED"
                when (reason) {
                    DownloadManager.ERROR_CANNOT_RESUME -> reasonText = "ERROR_CANNOT_RESUME"
                    DownloadManager.ERROR_DEVICE_NOT_FOUND -> reasonText = "ERROR_DEVICE_NOT_FOUND"
                    DownloadManager.ERROR_FILE_ALREADY_EXISTS -> reasonText =
                        "ERROR_FILE_ALREADY_EXISTS"
                    DownloadManager.ERROR_FILE_ERROR -> reasonText = "ERROR_FILE_ERROR"
                    DownloadManager.ERROR_HTTP_DATA_ERROR -> reasonText = "ERROR_HTTP_DATA_ERROR"
                    DownloadManager.ERROR_INSUFFICIENT_SPACE -> reasonText =
                        "ERROR_INSUFFICIENT_SPACE"
                    DownloadManager.ERROR_TOO_MANY_REDIRECTS -> reasonText =
                        "ERROR_TOO_MANY_REDIRECTS"
                    DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> reasonText =
                        "ERROR_UNHANDLED_HTTP_CODE"
                    DownloadManager.ERROR_UNKNOWN -> reasonText = "ERROR_UNKNOWN"
                }
            }
            DownloadManager.STATUS_PAUSED -> {
                statusText = "STATUS_PAUSED"
                when (reason) {
                    DownloadManager.PAUSED_QUEUED_FOR_WIFI -> reasonText = "PAUSED_QUEUED_FOR_WIFI"
                    DownloadManager.PAUSED_UNKNOWN -> reasonText = "PAUSED_UNKNOWN"
                    DownloadManager.PAUSED_WAITING_FOR_NETWORK -> reasonText =
                        "PAUSED_WAITING_FOR_NETWORK"
                    DownloadManager.PAUSED_WAITING_TO_RETRY -> reasonText =
                        "PAUSED_WAITING_TO_RETRY"
                }
            }
            DownloadManager.STATUS_PENDING -> statusText = "STATUS_PENDING"
            DownloadManager.STATUS_RUNNING -> statusText = "STATUS_RUNNING"
            DownloadManager.STATUS_SUCCESSFUL -> {
                statusText = "STATUS_SUCCESSFUL"
            }
        }
        return DownloadInfo.HasInfo(
            name,
            statusText,
            reasonText
        )
    }

    @SuppressLint("Range")
    fun downloadProgress(
        downloadId: Long,
        progressHandle: (bytesDownloaded: Int, bytesTotal: Int) -> Unit
    ) {
        val q = DownloadManager.Query()
        q.setFilterById(downloadId)
        var cursor = downloadManager.query(q)
        downloadIds.add(downloadId)
        Thread {
            var preProgress = -1
            while (downloadIds.find { it == downloadId } != null && run {
                    try {
                        cursor = downloadManager.query(q)
                        true
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        false
                    }
                } && cursor.moveToFirst()) {
                val bytesDownloaded = cursor.getInt(
                    cursor.getColumnIndex(
                        DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                    )
                )
                val bytesTotal = cursor.getInt(
                    cursor.getColumnIndex(
                        DownloadManager.COLUMN_TOTAL_SIZE_BYTES
                    )
                )

                if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                    progressHandle(-1, -1)
                    removeProgressDownload(downloadId)
                    cursor.close()
                    break
                }
                if (preProgress != bytesDownloaded) {
                    if (bytesTotal != -1) {
                        progressHandle(bytesDownloaded, bytesTotal)
                    }
                    preProgress = bytesDownloaded
                }
                cursor.close()
            }
        }.start()
    }

}
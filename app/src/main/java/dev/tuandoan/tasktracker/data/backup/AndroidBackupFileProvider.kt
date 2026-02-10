package dev.tuandoan.tasktracker.data.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * Android implementation of [BackupFileProvider] using ContentResolver for SAF URIs.
 */
class AndroidBackupFileProvider @Inject constructor(@ApplicationContext private val context: Context) :
    BackupFileProvider {

    override suspend fun writeToUri(uri: Uri, content: String) {
        withContext(Dispatchers.IO) {
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: throw IOException("Cannot open output stream for URI: $uri")
            outputStream.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
                stream.flush()
            }
        }
    }

    override suspend fun readFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open input stream for URI: $uri")
        inputStream.use { stream ->
            val reader = stream.bufferedReader(Charsets.UTF_8)
            val content = StringBuilder()
            val buffer = CharArray(DEFAULT_BUFFER_SIZE)
            var charsRead: Int
            while (reader.read(buffer).also { charsRead = it } != -1) {
                content.append(buffer, 0, charsRead)
                if (content.length > MAX_BACKUP_FILE_SIZE_CHARS) {
                    throw IOException(
                        "Backup file exceeds the maximum allowed size of ${MAX_BACKUP_FILE_SIZE_BYTES / (1024 * 1024)} MB",
                    )
                }
            }
            content.toString()
        }
    }

    companion object {
        /**
         * Maximum allowed backup file size in bytes (10 MB).
         * Prevents OutOfMemoryError when a user accidentally selects a very large file.
         */
        const val MAX_BACKUP_FILE_SIZE_BYTES = 10L * 1024 * 1024

        /**
         * Approximate character limit corresponding to [MAX_BACKUP_FILE_SIZE_BYTES].
         * UTF-8 characters are 1-4 bytes; using a 1:1 ratio is a safe conservative limit.
         */
        const val MAX_BACKUP_FILE_SIZE_CHARS = MAX_BACKUP_FILE_SIZE_BYTES.toInt()
    }
}

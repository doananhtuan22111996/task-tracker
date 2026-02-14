package dev.tuandoan.tasktracker.data.backup

import android.net.Uri

/**
 * Interface for reading and writing backup files via the Storage Access Framework.
 */
interface BackupFileProvider {

    /**
     * Writes the given content string to the provided URI.
     *
     * @param uri The SAF URI to write to.
     * @param content The string content to write.
     * @throws java.io.IOException If writing fails.
     */
    suspend fun writeToUri(uri: Uri, content: String)

    /**
     * Reads the full content of the file at the given URI as a string.
     *
     * @param uri The SAF URI to read from.
     * @return The file content as a string.
     * @throws java.io.IOException If reading fails.
     */
    suspend fun readFromUri(uri: Uri): String
}

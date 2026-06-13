package com.example.rpg

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class GitHubUpdateInfo(
    val buildName: String,
    val appVersion: Double,
    val dateAndHour: String,
    val changesList: List<String>,
    val rawNotesSection: String,
    val isMoreRecent: Boolean
)

object GitHubUpdateService {

    private const val DEFAULT_OWNER = "kelvinhx"
    private const val DEFAULT_REPO = "Projeto-de-RPG-para-TV-Android"
    private const val FILE_PATH = "NOTAS_DE_ATUALIZACAO.md"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Checks the GitHub repository for the latest release/notes.
     * Compares the version in raw NOTAS_DE_ATUALIZACAO.md with the local currentVersion.
     */
    suspend fun checkLatestUpdate(
        owner: String = DEFAULT_OWNER,
        repo: String = DEFAULT_REPO,
        currentVersion: Double = 3.0
    ): GitHubUpdateInfo? = withContext(Dispatchers.IO) {
        val repoOwner = owner.trim().ifEmpty { DEFAULT_OWNER }
        val repoName = repo.trim().ifEmpty { DEFAULT_REPO }
        
        val url = "https://raw.githubusercontent.com/$repoOwner/$repoName/main/$FILE_PATH"
        Log.d("GitHubUpdateService", "Fetching update notes from URL: $url")

        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("GitHubUpdateService", "HTTP error fetching raw file: ${response.code} ${response.message}")
                    return@withContext null
                }

                val markdown = response.body?.string() ?: return@withContext null
                return@withContext parseMarkdownNotes(markdown, currentVersion)
            }
        } catch (e: Exception) {
            Log.e("GitHubUpdateService", "Failed to check update from GitHub: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Resolves the real APK download URL by querying the latest GitHub Release or falling back to common locations.
     */
    suspend fun resolveApkUrl(owner: String, repo: String): String? = withContext(Dispatchers.IO) {
        val repoOwner = owner.trim().ifEmpty { DEFAULT_OWNER }
        val repoName = repo.trim().ifEmpty { DEFAULT_REPO }

        // Route 1: Check GitHub API latest release
        val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
        try {
            val request = Request.Builder().url(apiUrl).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val matcher = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.apk)\"", Pattern.CASE_INSENSITIVE).matcher(bodyStr)
                    if (matcher.find()) {
                        val resolvedUrl = matcher.group(1)
                        if (resolvedUrl != null) {
                            Log.d("GitHubUpdateService", "Resolved APK from GitHub Releases: $resolvedUrl")
                            return@withContext resolvedUrl
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GitHubUpdateService", "GitHub Release API lookup failed: ${e.message}")
        }

        // Route 2: Check fallback locations under RAW content in main branch
        val fallbacks = listOf(
            "https://raw.githubusercontent.com/$repoOwner/$repoName/main/app-release.apk",
            "https://raw.githubusercontent.com/$repoOwner/$repoName/main/app/build/outputs/apk/release/app-release.apk",
            "https://raw.githubusercontent.com/$repoOwner/$repoName/main/app/release/app-release.apk"
        )

        for (fallback in fallbacks) {
            try {
                val request = Request.Builder().url(fallback).head().build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d("GitHubUpdateService", "Resolved APK from fallback RAW URL: $fallback")
                        return@withContext fallback
                    }
                }
            } catch (e: Exception) {
                Log.d("GitHubUpdateService", "Fallback HEAD check failed for $fallback: ${e.message}")
            }
        }

        // Standard fallback URL
        return@withContext "https://raw.githubusercontent.com/$repoOwner/$repoName/main/app-release.apk"
    }

    /**
     * Downloads an APK file from GitHub and reports download progress real-time.
     */
    suspend fun downloadApk(
        url: String, 
        destFile: File, 
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (destFile.exists()) {
                destFile.delete()
            }
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("GitHubUpdateService", "Download failed, HTTP code: ${response.code}")
                    return@withContext false
                }
                val body = response.body ?: return@withContext false
                val totalBytes = body.contentLength()
                if (totalBytes <= 0) {
                    Log.w("GitHubUpdateService", "ContentLength was <= 0, progress tracking is fallback")
                }
                
                body.byteStream().use { inputStream ->
                    FileOutputStream(destFile).use { outputStream ->
                        val buffer = ByteArray(1024 * 16)
                        var bytesRead: Int
                        var totalRead = 0L
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (totalBytes > 0) {
                                val progress = totalRead.toFloat() / totalBytes.toFloat()
                                onProgress(progress)
                            } else {
                                onProgress(-1f)
                            }
                        }
                    }
                }
                Log.d("GitHubUpdateService", "Successfully downloaded APK to: ${destFile.absolutePath}")
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e("GitHubUpdateService", "Error downloading APK: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Parses the markdown file to extract the first/latest release block.
     */
    fun parseMarkdownNotes(markdown: String, currentVersion: Double): GitHubUpdateInfo? {
        try {
            // Find sections starting with "## [Build ...]"
            // e.g.: ## [Build v3] - Versão do Aplicativo: 3.0
            val pattern = Pattern.compile(
                "^##\\s*\\[(Build\\s+v[\\w.]+)\\]\\s*-\\s*Versão do Aplicativo:\\s*([\\d.]+)",
                Pattern.CASE_INSENSITIVE or Pattern.MULTILINE
            )
            val matcher = pattern.matcher(markdown)

            if (matcher.find()) {
                val buildName = matcher.group(1) ?: "Build Desconhecida"
                val versionStr = matcher.group(2) ?: "1.0"
                val appVersion = versionStr.toDoubleOrNull() ?: 1.0

                // Get content start from after the header match
                val startIdx = matcher.end()
                
                // Find next section "##" to terminate the release notes block
                val endIdx = markdown.indexOf("## ", startIdx).let {
                    if (it != -1) it else markdown.length
                }

                val notesBlock = markdown.substring(startIdx, endIdx).trim()
                
                // Parse date/hour which is typically on the next line: "**Data e Hora do Envio:** ..."
                val datePattern = Pattern.compile("\\*\\*Data e Hora do Envio:\\*\\*\\s*(.*)", Pattern.CASE_INSENSITIVE)
                val dateMatcher = datePattern.matcher(notesBlock)
                val dateAndHour = if (dateMatcher.find()) {
                    dateMatcher.group(1)?.trim() ?: "Desconhecido"
                } else {
                    "Disponibilizado recientemente"
                }

                // Extract changes list (lines that start with - or list bullets)
                val lines = notesBlock.split("\n")
                val changes = mutableListOf<String>()
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("-") || trimmed.startsWith("*") || 
                        trimmed.startsWith("1.") || trimmed.startsWith("2.") || 
                        trimmed.startsWith("3.") || trimmed.startsWith("4.") || trimmed.startsWith("5.")
                    ) {
                        changes.add(trimmed.replace(Regex("^[-*\\d.]+\\s*"), ""))
                    }
                }

                return GitHubUpdateInfo(
                    buildName = buildName,
                    appVersion = appVersion,
                    dateAndHour = dateAndHour,
                    changesList = changes,
                    rawNotesSection = notesBlock,
                    isMoreRecent = appVersion > currentVersion
                )
            }
        } catch (e: Exception) {
            Log.e("GitHubUpdateService", "Error parsing markdown notes: ${e.message}", e)
        }
        return null
    }
}

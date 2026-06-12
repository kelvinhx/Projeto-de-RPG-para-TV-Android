package com.example.rpg

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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

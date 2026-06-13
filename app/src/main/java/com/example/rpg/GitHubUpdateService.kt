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
import java.util.zip.ZipInputStream

data class GitHubUpdateInfo(
    val buildName: String,
    val appVersion: Double,
    val dateAndHour: String,
    val changesList: List<String>,
    val rawNotesSection: String,
    val isMoreRecent: Boolean
)

data class GitHubArtifact(
    val id: Long,
    val name: String,
    val archiveDownloadUrl: String,
    val expired: Boolean
)

object GitHubUpdateService {

    private const val DEFAULT_OWNER = "kelvinhx"
    private const val DEFAULT_REPO = "Projeto-de-RPG-para-TV-Android"
    private const val FILE_PATH = "NOTAS_DE_ATUALIZACAO.md"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
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
     * Lists artifacts for the repository using GitHub API.
     * This API is public and can be run anonymously if no token is provided.
     */
    suspend fun getArtifactsList(owner: String, repo: String, token: String = ""): List<GitHubArtifact> = withContext(Dispatchers.IO) {
        val repoOwner = owner.trim().ifEmpty { DEFAULT_OWNER }
        val repoName = repo.trim().ifEmpty { DEFAULT_REPO }
        val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/actions/artifacts"

        try {
            val reqBuilder = Request.Builder()
                .url(apiUrl)
                .header("User-Agent", "DungeonMasterTV")
                .header("Accept", "application/vnd.github+json")
            
            if (token.trim().isNotEmpty()) {
                reqBuilder.header("Authorization", "Bearer ${token.trim()}")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    return@withContext parseArtifactsList(bodyStr)
                } else {
                    Log.e("GitHubUpdateService", "Failed to list artifacts: HTTP code ${response.code} ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("GitHubUpdateService", "Error listing GitHub artifacts: ${e.message}", e)
        }
        return@withContext emptyList<GitHubArtifact>()
    }

    /**
     * Resolves the real download URL of the latest Actions artifact ZIP.
     * Checks token first, otherwise constructs a nightly.link download URL.
     */
    suspend fun resolveArtifactZipUrl(
        owner: String, 
        repo: String, 
        token: String = "",
        artifactsList: List<GitHubArtifact>
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        val repoOwner = owner.trim().ifEmpty { DEFAULT_OWNER }
        val repoName = repo.trim().ifEmpty { DEFAULT_REPO }

        // Choose newest artifact whose name contains 'WhatIsRPG', or ends with '-APK', or anything active
        val targetArtifact = artifactsList.firstOrNull { it.name.contains("WhatIsRPG", ignoreCase = true) && !it.expired }
            ?: artifactsList.firstOrNull { it.name.contains("APK", ignoreCase = true) && !it.expired }
            ?: artifactsList.firstOrNull { !it.expired }

        val finalArtifactName = targetArtifact?.name ?: "WhatIsRPG-Debug-APK"

        if (token.trim().isNotEmpty() && targetArtifact != null) {
            Log.d("GitHubUpdateService", "Resolved API ZIP URL with custom PAT for artifact ID: ${targetArtifact.id}")
            return@withContext Pair(targetArtifact.archiveDownloadUrl, finalArtifactName)
        }

        // Standard fallback mode: nightly.link
        val nightlyLink = "https://nightly.link/$repoOwner/$repoName/workflows/android/main/${finalArtifactName}.zip"
        Log.d("GitHubUpdateService", "Resolved Fallback artifact ZIP URL from nightly.link: $nightlyLink")
        return@withContext Pair(nightlyLink, finalArtifactName)
    }

    /**
     * Downloads the zip file from specified URL, optionally including a PAT header.
     */
    suspend fun downloadZipFile(
        url: String, 
        destZipFile: File, 
        token: String = "",
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (destZipFile.exists()) {
                destZipFile.delete()
            }
            val reqBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", "DungeonMasterTV")
            
            if (token.trim().isNotEmpty() && url.contains("api.github.com")) {
                reqBuilder.header("Authorization", "Bearer ${token.trim()}")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("GitHubUpdateService", "Download zip failed, HTTP code: ${response.code}")
                    return@withContext false
                }
                val body = response.body ?: return@withContext false
                val totalBytes = body.contentLength()
                
                body.byteStream().use { inputStream ->
                    FileOutputStream(destZipFile).use { outputStream ->
                        val buffer = ByteArray(1024 * 16)
                        var bytesRead: Int
                        var totalRead = 0L
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (totalBytes > 0) {
                                val progress = totalRead.toFloat() / totalBytes.toFloat()
                                onProgress(progress * 0.9f) // Leave remaining 10% for ZIP extraction
                            } else {
                                onProgress(-1f)
                            }
                        }
                    }
                }
                Log.d("GitHubUpdateService", "Successfully downloaded ZIP to: ${destZipFile.absolutePath}")
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e("GitHubUpdateService", "Error downloading ZIP file: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Extracts the first file with .apk extension from the zip file.
     */
    fun extractApkFromZip(zipFile: File, destApkFile: File): Boolean {
        Log.d("GitHubUpdateService", "Extracting APK from downloaded ZIP...")
        try {
            if (destApkFile.exists()) {
                destApkFile.delete()
            }
            java.io.FileInputStream(zipFile).use { fis ->
                ZipInputStream(fis).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name.endsWith(".apk", ignoreCase = true)) {
                            Log.d("GitHubUpdateService", "Found APK entry in ZIP: ${entry.name}")
                            FileOutputStream(destApkFile).use { fos ->
                                val buffer = ByteArray(1024 * 16)
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                            zis.closeEntry()
                            Log.d("GitHubUpdateService", "Successfully extracted APK to: ${destApkFile.absolutePath}")
                            return true
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            Log.e("GitHubUpdateService", "No APK file found inside the retrieved ZIP file.")
        } catch (e: Exception) {
            Log.e("GitHubUpdateService", "Error extracting zip: ${e.message}", e)
        }
        return false
    }

    /**
     * Extracts all contents of the ZIP archive into the specified target directory
     * (e.g., /sdcard/WhatIsRPG na raiz dos arquivos da Android TV).
     */
    fun extractZipToFolder(zipFile: File, targetDir: File): Boolean {
        Log.d("GitHubUpdateService", "Extracting full ZIP to directory: ${targetDir.absolutePath}")
        try {
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            java.io.FileInputStream(zipFile).use { fis ->
                ZipInputStream(fis).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val outFile = File(targetDir, entry.name)
                        
                        // Path traversal vulnerability safeguard
                        val canonicalPath = outFile.canonicalPath
                        val canonicalTarget = targetDir.canonicalPath
                        if (!canonicalPath.startsWith(canonicalTarget)) {
                            throw SecurityException("Caminho inseguro de ZIP bloqueado: ${entry.name}")
                        }

                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos ->
                                val buffer = ByteArray(1024 * 16)
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            Log.d("GitHubUpdateService", "Extraction of ZIP succeeded into: ${targetDir.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e("GitHubUpdateService", "Detailed error extracting ZIP contents: ${e.message}", e)
            return false
        }
    }

    /**
     * Recursively traverses target directory to find the extracted .apk file.
     */
    fun findApkInDirectory(dir: File): File? {
        try {
            if (!dir.exists()) return null
            val files = dir.listFiles() ?: return null
            for (file in files) {
                if (file.isDirectory) {
                    val subFind = findApkInDirectory(file)
                    if (subFind != null) return subFind
                } else if (file.name.endsWith(".apk", ignoreCase = true)) {
                    return file
                }
            }
        } catch (e: Exception) {
            Log.e("GitHubUpdateService", "Error finding APK file inside directories: ${e.message}", e)
        }
        return null
    }

    /**
     * Saves a permanent copy of the APK to the TV's root public folders.
     */
    fun copyApkToPublicFolders(sourceApk: File, targetVersion: Double) {
        try {
            val fileName = "WhatIsRPG_v${targetVersion}.apk"
            val possibleDirs = listOf(
                File(android.os.Environment.getExternalStorageDirectory(), "RPG_TV"),
                File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "RPG_TV"),
                File("/sdcard/RPG_TV")
            )
            for (dir in possibleDirs) {
                try {
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    val destFile = File(dir, fileName)
                    sourceApk.inputStream().use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d("GitHubUpdateService", "Saved master copy of layout update to: ${destFile.absolutePath}")
                } catch (e: Exception) {
                    Log.w("GitHubUpdateService", "Failed to write copy to ${dir.absolutePath}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("GitHubUpdateService", "Global error copying APK clone: ${e.message}")
        }
    }

    /**
     * Custom JSON tokenizer to extract artifacts reliably from the response payload.
     */
    fun parseArtifactsList(json: String): List<GitHubArtifact> {
        val list = mutableListOf<GitHubArtifact>()
        try {
            val startIdx = json.indexOf("\"artifacts\"")
            if (startIdx == -1) return list
            val listStart = json.indexOf("[", startIdx)
            val listEnd = json.indexOf("]", listStart)
            if (listStart == -1 || listEnd == -1) return list
            val listContent = json.substring(listStart + 1, listEnd)
            
            val blocks = listContent.split("{")
            for (block in blocks) {
                val trimmedBlock = block.trim()
                if (trimmedBlock.isEmpty()) continue
                
                val idPattern = Pattern.compile("\"id\"\\s*:\\s*(\\d+)")
                val namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"")
                val archivePattern = Pattern.compile("\"archive_download_url\"\\s*:\\s*\"([^\"]+)\"")
                val expiredPattern = Pattern.compile("\"expired\"\\s*:\\s*(true|false)")
                
                val idMatcher = idPattern.matcher(trimmedBlock)
                val nameMatcher = namePattern.matcher(trimmedBlock)
                val archiveMatcher = archivePattern.matcher(trimmedBlock)
                val expiredMatcher = expiredPattern.matcher(trimmedBlock)
                
                if (idMatcher.find() && nameMatcher.find() && archiveMatcher.find()) {
                    val id = idMatcher.group(1)?.toLongOrNull() ?: continue
                    val name = nameMatcher.group(2) ?: ""
                    val url = archiveMatcher.group(1) ?: ""
                    val expired = if (expiredMatcher.find()) expiredMatcher.group(1).toBoolean() else false
                    list.add(GitHubArtifact(id, name, url, expired))
                }
            }
        } catch (e: Exception) {
            Log.e("GitHubUpdateService", "JSON tokenizer parsing exception: ${e.message}", e)
        }
        return list
    }

    /**
     * Parses the markdown file to extract the first/latest release block.
     */
    fun parseMarkdownNotes(markdown: String, currentVersion: Double): GitHubUpdateInfo? {
        try {
            val pattern = Pattern.compile(
                "^##\\s*\\[(Build\\s+v[\\w.]+)\\]\\s*-\\s*Versão do Aplicativo:\\s*([\\d.]+)",
                Pattern.CASE_INSENSITIVE or Pattern.MULTILINE
            )
            val matcher = pattern.matcher(markdown)

            if (matcher.find()) {
                val buildName = matcher.group(1) ?: "Build Desconhecida"
                val versionStr = matcher.group(2) ?: "1.0"
                val appVersion = versionStr.toDoubleOrNull() ?: 1.0

                val startIdx = matcher.end()
                
                val endIdx = markdown.indexOf("## ", startIdx).let {
                    if (it != -1) it else markdown.length
                }

                val notesBlock = markdown.substring(startIdx, endIdx).trim()
                
                val datePattern = Pattern.compile("\\*\\*Data e Hora do Envio:\\*\\*\\s*(.*)", Pattern.CASE_INSENSITIVE)
                val dateMatcher = datePattern.matcher(notesBlock)
                val dateAndHour = if (dateMatcher.find()) {
                    dateMatcher.group(1)?.trim() ?: "Desconhecido"
                } else {
                    "Disponibilizado recentemente"
                }

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

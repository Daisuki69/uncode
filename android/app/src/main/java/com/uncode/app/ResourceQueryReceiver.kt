package com.uncode.app

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import org.json.JSONArray
import java.io.File

/**
 * ResourceQueryReceiver:
 * Broadcast receiver processing inline direct-reply (RemoteInput) resource searches
 * from the always-active persistent notification shade card.
 *
 * Scans saved resources in studom_resources line-by-line using QIEZKA's Master Syntax:
 * [Term] | [Definition]
 *
 * Behavior:
 * 1. Instantly collapses ONLY the Main Notification (ID = 1006) upon receiving a query.
 *    Any previously dispatched auxiliary notifications RETAIN in the shade.
 * 2. Scans across all saved resources for any line containing the query (case-insensitive).
 * 3. Notification dispatch:
 *    - Match 0: Dispatched to Main Notification (ID = 1006) with RemoteInput "Reply" action attached.
 *    - Matches 1..N: Dispatched as Auxiliary Notifications (IDs = 1100, 1101, ...) without Reply actions.
 * 4. If no match found: Main Notification (ID = 1006) displays "<query> does not exist" without title.
 */
class ResourceQueryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        // 1. Automatic Revival on Swipe / Dismiss
        if (intent.action == QiezkaNotificationHelper.ACTION_REVIVE_RESOURCE_QUERY) {
            QiezkaNotificationHelper.postAlwaysActiveResourceQueryNotification(context)
            return
        }

        if (intent.action != QiezkaNotificationHelper.ACTION_RESOURCE_QUERY) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        // 2. Immediately collapse ONLY the Main Notification (ID = 1006). Auxiliary cards retain!
        nm?.cancel(QiezkaNotificationHelper.NOTIF_ID_RESOURCE_QUERY)

        val bundle = RemoteInput.getResultsFromIntent(intent) ?: return
        val rawQuery = bundle.getCharSequence(QiezkaNotificationHelper.KEY_TEXT_REPLY)?.toString() ?: ""
        val query = rawQuery.trim()

        if (query.isEmpty()) {
            val notif = QiezkaNotificationHelper.buildResourceQueryNotification(
                context,
                null,
                ""
            )
            nm?.notify(QiezkaNotificationHelper.NOTIF_ID_RESOURCE_QUERY, notif)
            return
        }

        // Search studom_resources for all matches
        val matches = searchAllResourceMatches(context, query)

        if (matches.isNotEmpty()) {
            // Main Notification (ID = 1006) gets the primary match with the Reply action
            val mainMatch = matches[0]
            val mainNotif = QiezkaNotificationHelper.buildResourceQueryNotification(
                context,
                mainMatch.first,
                mainMatch.second
            )
            nm?.notify(QiezkaNotificationHelper.NOTIF_ID_RESOURCE_QUERY, mainNotif)

            // Additional matches are posted as auxiliary notifications (IDs = 1100, 1101, ...)
            val maxAux = minOf(matches.size, 10)
            for (i in 1 until maxAux) {
                val auxMatch = matches[i]
                val auxNotifId = QiezkaNotificationHelper.BASE_NOTIF_ID_AUX + (i - 1)
                val auxNotif = QiezkaNotificationHelper.buildAuxiliaryNotification(
                    context,
                    auxMatch.first,
                    auxMatch.second,
                    auxNotifId
                )
                nm?.notify(auxNotifId, auxNotif)
            }
        } else {
            // When no match is found: NO TITLE, Text: "$query does not exist"
            val notif = QiezkaNotificationHelper.buildResourceQueryNotification(
                context,
                null,
                QiezkaNotificationHelper.formatNotFoundText(query)
            )
            nm?.notify(QiezkaNotificationHelper.NOTIF_ID_RESOURCE_QUERY, notif)
        }
    }

    companion object {
        /**
         * Scans saved resources for all lines matching [query] (case-insensitive contains).
         * Returns List of Pair(resourceSetTitle, matchedLine).
         */
        @JvmStatic
        fun searchAllResourceMatches(context: Context, query: String): List<Pair<String, String>> {
            val candidateFiles = listOf(
                File(context.filesDir, "studom_resources"),
                File(context.filesDir, "Documents/studom_resources"),
                File(context.getExternalFilesDir(null), "studom_resources")
            )
            val file = candidateFiles.firstOrNull { it.exists() } ?: return emptyList()
            return try {
                val json = file.readText(Charsets.UTF_8)
                searchAllInJsonString(json, query)
            } catch (ignore: Exception) {
                emptyList()
            }
        }

        @JvmStatic
        fun searchResourceMatch(context: Context, query: String): Pair<String, String>? {
            return searchAllResourceMatches(context, query).firstOrNull()
        }

        @JvmStatic
        fun parseResources(json: String): List<Pair<String, String>> {
            val list = mutableListOf<Pair<String, String>>()
            try {
                val arr = JSONArray(json)
                if (arr.length() > 0) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val title = obj.optString("title", "").trim()
                        val content = obj.optString("content", "")
                        list.add(Pair(title, content))
                    }
                    return list
                }
            } catch (ignore: Throwable) {}

            // Fallback regex search for JVM unit tests where returnDefaultValues produces empty JSONArray
            val contentPattern = Regex("\"content\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"", RegexOption.DOT_MATCHES_ALL)
            val titlePattern = Regex("\"title\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"", RegexOption.DOT_MATCHES_ALL)

            val contentMatches = contentPattern.findAll(json).toList()
            val titleMatches = titlePattern.findAll(json).toList()

            for (i in contentMatches.indices) {
                val rawContent = contentMatches[i].groupValues[1]
                val content = rawContent.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")
                val title = if (i < titleMatches.size) {
                    titleMatches[i].groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\").trim()
                } else ""
                list.add(Pair(title, content))
            }
            return list
        }

        @JvmStatic
        fun searchAllInResources(resources: List<Pair<String, String>>, cleanQuery: String): List<Pair<String, String>> {
            if (cleanQuery.isEmpty()) return emptyList()

            val matches = mutableListOf<Pair<String, String>>()
            val seen = mutableSetOf<String>()

            // Scan all lines across all resources for substring contains match
            for (resource in resources) {
                val title = resource.first
                val content = resource.second
                val lines = content.lines()
                for (line in lines) {
                    val trimmedLine = line.trim()
                    if (trimmedLine.isEmpty()) continue
                    val displayLine = if (trimmedLine.contains("|")) {
                        val parts = trimmedLine.split("|", limit = 2)
                        val term = parts[0].trim()
                        val def = if (parts.size > 1) parts[1].trim() else ""
                        if (def.isNotEmpty()) "$term | $def" else term
                    } else {
                        trimmedLine
                    }

                    if (displayLine.contains(cleanQuery, ignoreCase = true)) {
                        val key = "$title::$displayLine"
                        if (seen.add(key)) {
                            matches.add(Pair(title, displayLine))
                        }
                    }
                }
            }

            // Fallback: If no lines matched, check if resource set title contains query
            if (matches.isEmpty()) {
                for (resource in resources) {
                    val title = resource.first
                    val content = resource.second
                    if (title.isNotEmpty() && title.contains(cleanQuery, ignoreCase = true)) {
                        val firstLine = content.lines().firstOrNull { it.isNotBlank() }?.trim() ?: title
                        val key = "$title::$firstLine"
                        if (seen.add(key)) {
                            matches.add(Pair(title, firstLine))
                        }
                    }
                }
            }

            return matches
        }

        @JvmStatic
        fun searchAllInJsonString(json: String, query: String): List<Pair<String, String>> {
            val cleanQuery = query.trim()
            if (cleanQuery.isEmpty()) return emptyList()
            val resources = parseResources(json)
            return searchAllInResources(resources, cleanQuery)
        }

        @JvmStatic
        fun searchInJsonString(json: String, query: String): Pair<String, String>? {
            return searchAllInJsonString(json, query).firstOrNull()
        }
    }
}

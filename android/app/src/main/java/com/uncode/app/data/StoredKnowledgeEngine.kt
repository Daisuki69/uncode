package com.uncode.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

object StoredKnowledgeEngine {

    @Volatile
    private var database: SQLiteDatabase? = null

    @Volatile
    private var databasePath: String? = null

    private val commonStopwords = setOf(
        "with", "from", "that", "this", "have", "some", "used", "which",
        "their", "other", "more", "when", "into", "also", "such", "what",
        "where", "describe", "term", "card", "deck", "general", "about"
    )

    private val knownAcronyms = setOf(
        "RAM", "ROM", "EEPROM", "EPROM", "SRAM", "DRAM", "SIMM", "DIMM",
        "LSIC", "VLSIC", "ULSIC", "VLSI", "LSI", "MSI", "SSI", "IC",
        "CPU", "GPU", "ALU", "CU", "MS-DOS", "PC-DOS", "DOS", "OS",
        "AI", "GUI", "CLI", "BPO", "IT", "NTC", "HDD", "SSD", "CD-ROM", "DVD-ROM",
        "USB", "LAN", "WAN", "MAN", "PAN", "WLAN", "TCP", "IP", "UDP",
        "HTTP", "HTTPS", "FTP", "SSH", "DNS", "DHCP", "MAC", "HTML", "CSS",
        "SQL", "DBMS", "RDBMS", "BIOS", "CMOS", "IDE", "SATA", "PCI", "AGP", "ISA"
    )

    private fun toOrdinal(n: Int): String {
        val mod100 = n % 100
        val mod10 = n % 10
        val suffix = when {
            mod100 in 11..13 -> "th"
            mod10 == 1 -> "st"
            mod10 == 2 -> "nd"
            mod10 == 3 -> "rd"
            else -> "th"
        }
        return "$n$suffix"
    }

    fun init(context: Context) {
        SemanticEmbeddingEngine.init(context)
        val path = context.getDatabasePath("words.db").path
        databasePath = path

        if (database != null && database?.isOpen == true) return

        synchronized(this) {
            if (database != null && database?.isOpen == true) return
            try {
                val dbFile = File(path)
                var needsCopy = !dbFile.exists() || dbFile.length() == 0L

                if (!needsCopy) {
                    try {
                        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { testDb ->
                            testDb.rawQuery("SELECT COUNT(*) FROM knowledge_clusters", null).use { cursor ->
                                if (!cursor.moveToFirst() || cursor.getInt(0) < 1000) {
                                    needsCopy = true
                                }

                            }
                        }
                    } catch (e: Exception) {
                        needsCopy = true
                    }
                }

                if (needsCopy) {
                    dbFile.parentFile?.mkdirs()
                    context.assets.open("databases/words.db").use { input ->
                        FileOutputStream(dbFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                if (dbFile.exists() && dbFile.length() > 0L) {
                    database = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
                }
            } catch (e: Exception) {
                database = null
            }
        }
    }

    private fun ensureDatabaseOpen(): SQLiteDatabase? {
        val existing = database
        if (existing != null && existing.isOpen) return existing

        val path = databasePath ?: return null
        synchronized(this) {
            val secondCheck = database
            if (secondCheck != null && secondCheck.isOpen) return secondCheck

            return try {
                val file = File(path)
                if (file.exists() && file.length() > 0L) {
                    val db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
                    database = db
                    db
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    fun extractAttributeSlot(term: String): String {
        val t = term.trim()
        if (t.isBlank()) return ""

        val relMatch = Regex("(?i)^([a-zA-Z\\s-]+?)\\s+(?:of|for|in)\\s+([a-zA-Z0-9\\s-]+)$").find(t)
        if (relMatch != null) {
            val attr = relMatch.groupValues[1].trim().lowercase()
            if (attr.length >= 3 && !attr.contains("year") && !attr.contains("time")) {
                return attr
            }
        }

        val slotRules = listOf(
            Regex("(?i)\\b(birth and death years?|design year|release year|timeframe|time period|years?)\\b") to "years",
            Regex("(?i)\\b(main innovation|major innovation|key innovation|invention)\\b") to "innovation",
            Regex("(?i)\\b(input\\s*/?\\s*output devices?)\\b") to "input output devices",
            Regex("(?i)\\b(main memory|primary memory|semiconductor memory|volatile memory|memory)\\b") to "memory",
            Regex("(?i)\\b(external storage|secondary storage|storage media|storage)\\b") to "storage",
            Regex("(?i)\\b(operating systems?|os)\\b") to "operating system",
            Regex("(?i)\\b(programming languages?|machine languages?|languages?)\\b") to "languages",
            Regex("(?i)\\b(physical size|computer size|size)\\b") to "size",
            Regex("(?i)\\b(clock speed|processor speed|speed|rate)\\b") to "speed",
            Regex("(?i)\\b(co-?founded companies|companies|company|corporation)\\b") to "companies",
            Regex("(?i)\\b(dosages?|dose)\\b") to "dosage",
            Regex("(?i)\\b(mechanism of action|mechanism|mode of action)\\b") to "mechanism",
            Regex("(?i)\\b(contraindications?)\\b") to "contraindication",
            Regex("(?i)\\b(symptoms?|signs and symptoms?|clinical presentation)\\b") to "symptom",
            Regex("(?i)\\b(diagnostic tests?|diagnostic procedures?|diagnosis)\\b") to "diagnostic test",
            Regex("(?i)\\b(adverse effects?|side effects?|toxicity)\\b") to "adverse effect",
            Regex("(?i)\\b(anatomical structures?|anatomy|organs?|bones?|muscles?|nerves?)\\b") to "anatomy",
            Regex("(?i)\\b(elements?|requisites?)\\b") to "elements",
            Regex("(?i)\\b(penalties?|punishment|sentencing)\\b") to "penalty",
            Regex("(?i)\\b(prescription period|statute of limitations?)\\b") to "prescription",
            Regex("(?i)\\b(legal remedies?|remedies?|judicial remedy)\\b") to "legal remedy",
            Regex("(?i)\\b(burden of proof|standard of proof|quantum of proof)\\b") to "standard of proof",
            Regex("(?i)\\b(legal doctrines?|principles?|doctrines?)\\b") to "doctrine",
            Regex("(?i)\\b(ethical theory|normative theory)\\b") to "ethical theory",
            Regex("(?i)\\b(ethical principles?|moral principles?)\\b") to "ethical principle",
            Regex("(?i)\\b(view on morality|moral view)\\b") to "view on morality",
            Regex("(?i)\\b(workplace principles?|workplace guideline)\\b") to "workplace principle",
            Regex("(?i)\\b(core values?|cultural values?)\\b") to "core values",
            Regex("(?i)\\b(core characteristics?|characteristics?|traits?|qualities?|nuance)\\b") to "characteristics",
            Regex("(?i)\\b(determinants(?:\\s+of\\s+[a-zA-Z\\s]+)?)\\b") to "determinants",
            Regex("(?i)\\b(definitions?)\\b") to "definition",
            Regex("(?i)\\b(examples?|real-world context|context example)\\b") to "examples",
            Regex("(?i)\\b(professions?|titles?|nicknames?|roles?|significance|purpose)\\b") to "profession",
            Regex("(?i)\\b(rank|ranking)\\b") to "rank",
            Regex("(?i)\\b(shift(?:\\s+from\\s+[a-zA-Z\\s]+)?)\\b") to "shift",
            Regex("(?i)\\b(scope|acronym meaning|components?)\\b") to "components"
        )

        for ((pattern, canonical) in slotRules) {
            if (pattern.containsMatchIn(t)) {
                return canonical
            }
        }

        val words = t.split(Regex("\\s+")).filter { it.isNotBlank() }
        val suffixWord = words.firstOrNull { it.matches(Regex("(?i)^[a-zA-Z]+(tion|ment|ity|ence|ance|ics)$")) }
        if (suffixWord != null) {
            return suffixWord.lowercase()
        }

        return when {
            words.size >= 3 -> "${words[words.size - 2]} ${words[words.size - 1]}".lowercase()
            words.isNotEmpty() -> words.last().lowercase()
            else -> ""
        }
    }

    fun generateDistractors(
        targetTerm: String,
        targetAnswer: String,
        deckCategory: String,
        existingCardAnswers: List<String>,
        count: Int
    ): List<String> = generateDistractors(
        targetTerm = targetTerm,
        targetAnswer = targetAnswer,
        deckCategory = deckCategory,
        existingCards = emptyList(),
        existingCardAnswers = existingCardAnswers,
        count = count
    )

    fun generateDistractors(
        targetTerm: String,
        targetAnswer: String,
        deckCategory: String = "",
        existingCards: List<Pair<String, String>> = emptyList(),
        existingCardAnswers: List<String> = emptyList(),
        count: Int = 3
    ): List<String> {
        val cleanAnswer = targetAnswer.trim()
        val cleanTerm = targetTerm.trim()
        val db = ensureDatabaseOpen()

        val adminRegex = Regex("(?i)\\b(course code|department|school of|general education program|national teachers college|syllabus|instructor)\\b")
        val effectiveCards = (if (existingCards.isNotEmpty()) {
            existingCards
        } else {
            existingCardAnswers.map { "" to it }
        }).filter { (term, ans) -> !adminRegex.containsMatchIn(term) && !adminRegex.containsMatchIn(ans) }
        val effectiveAnswers = effectiveCards.map { it.second }

        val boolLower = cleanAnswer.lowercase()
        if (boolLower in setOf("true", "false", "yes", "no")) {
            val complement = when (boolLower) {
                "true" -> "False"
                "false" -> "True"
                "yes" -> "No"
                "no" -> "Yes"
                else -> "False"
            }
            return listOf(matchCasingStyle(complement, cleanAnswer))
        }

        val yearOnwardMatch = Regex("^(\\d{4})\\s*-\\s*onward$", RegexOption.IGNORE_CASE).find(cleanAnswer)
        if (yearOnwardMatch != null) {
            val y = yearOnwardMatch.groupValues[1].toInt()
            val offsets = listOf(-20, -10, 10, 20, -15, 15).shuffled()
            val pool = offsets.map { "${y + it}-onward" }.filter { it != cleanAnswer }.take(count)
            if (pool.size >= count) {
                return pool.shuffled()
            }
        }

        val decadeMatch = Regex("^(\\d{4})s$", RegexOption.IGNORE_CASE).find(cleanAnswer)
        if (decadeMatch != null) {
            val d = decadeMatch.groupValues[1].toInt()
            val offsets = listOf(-20, -10, 10, 20, 30, -30).shuffled()
            val pool = offsets.map { "${d + it}s" }.filter { it != cleanAnswer }.take(count)
            if (pool.size >= count) {
                return pool.shuffled()
            }
        }

        val yearRangeMatch = Regex("^(\\d{4})\\s*-\\s*(\\d{4})$").find(cleanAnswer)
        if (yearRangeMatch != null) {
            val y1 = yearRangeMatch.groupValues[1].toInt()
            val y2 = yearRangeMatch.groupValues[2].toInt()
            val diff = y2 - y1

            val intraYearRanges = effectiveCards
                .map { it.second.trim() }
                .filter { it.isNotBlank() && !it.equals(cleanAnswer, ignoreCase = true) && Regex("^\\d{4}\\s*-\\s*\\d{4}$").matches(it) }
                .distinctBy { it.lowercase() }
                .shuffled()

            val rangePool = mutableListOf<String>()
            rangePool.addAll(intraYearRanges)

            if (rangePool.size < count) {
                val offsets = listOf(-15, -10, -5, 5, 10, 15, 20, -20).shuffled()
                for (off in offsets) {
                    val sy = (y1 + off).coerceAtLeast(1700)
                    val ey = sy + diff
                    val cand = "$sy-$ey"
                    if (!rangePool.contains(cand) && cand != cleanAnswer) {
                        rangePool.add(cand)
                    }
                    if (rangePool.size >= count) break
                }
            }

            if (rangePool.size >= count) {
                return rangePool.take(count).map { matchCasingStyle(it, cleanAnswer) }.shuffled()
            }
        }

        val ordinalRankMatch = Regex("^(\\d+)(?:st|nd|rd|th)?\\s+(out of|of)\\s+(\\d+)(.*)$", RegexOption.IGNORE_CASE).find(cleanAnswer)
        if (ordinalRankMatch != null) {
            val rank = ordinalRankMatch.groupValues[1].toIntOrNull()
            val prep = ordinalRankMatch.groupValues[2]
            val total = ordinalRankMatch.groupValues[3].toIntOrNull()
            val suffix = ordinalRankMatch.groupValues[4]
            if (rank != null && total != null && total > 1) {
                val hadOrdinal = cleanAnswer.matches(Regex("^\\d+(?:st|nd|rd|th).*", RegexOption.IGNORE_CASE))
                val offsets = listOf(-18, -12, -6, -3, 3, 6, 12, 18, 25, -25).shuffled()
                val rankPool = mutableListOf<String>()
                for (off in offsets) {
                    val perturbed = (rank + off).coerceIn(1, total)
                    if (perturbed != rank) {
                        val formattedRank = if (hadOrdinal) toOrdinal(perturbed) else perturbed.toString()
                        val cand = "$formattedRank $prep $total$suffix".trim()
                        if (!rankPool.contains(cand) && !cand.equals(cleanAnswer, ignoreCase = true)) {
                            rankPool.add(cand)
                        }
                    }
                    if (rankPool.size >= count) break
                }
                if (rankPool.size >= count) {
                    return rankPool.take(count).map { matchCasingStyle(it, cleanAnswer) }.shuffled()
                }
            }
        }

        val singleOrdinalMatch = Regex("^(\\d+)(st|nd|rd|th)$", RegexOption.IGNORE_CASE).find(cleanAnswer)
        if (singleOrdinalMatch != null) {
            val r = singleOrdinalMatch.groupValues[1].toIntOrNull()
            if (r != null && r > 0) {
                val offsets = listOf(-6, -4, -2, -1, 1, 2, 4, 6, 8, 10).shuffled()
                val pool = offsets.map { (r + it).coerceAtLeast(1) }
                    .filter { it != r }
                    .distinct()
                    .take(count)
                    .map { toOrdinal(it) }
                if (pool.size >= count) {
                    return pool.map { matchCasingStyle(it, cleanAnswer) }.shuffled()
                }
            }
        }

        val targetSlot = extractAttributeSlot(cleanTerm)
        val harvestedSiblings = mutableListOf<String>()
        if (targetSlot.isNotBlank() && effectiveCards.isNotEmpty()) {
            val siblingMatches = effectiveCards
                .filter { (cTerm, cAns) ->
                    val cleanCAns = cAns.trim()
                    cleanCAns.isNotBlank() &&
                            !cleanCAns.equals(cleanAnswer, ignoreCase = true) &&
                            extractAttributeSlot(cTerm).equals(targetSlot, ignoreCase = true)
                }
                .map { it.second.trim() }
                .distinctBy { it.lowercase() }
                .shuffled()

            harvestedSiblings.addAll(siblingMatches)
            if (harvestedSiblings.size >= count) {
                return harvestedSiblings.take(count).map { matchCasingStyle(it, cleanAnswer) }
            }
        }

        val quantityRegex = Regex("^([\\$\\\u20AC\\\u00A3]?\\s*[\\d,]+(?:\\.\\d+)?)\\s+([a-zA-Z\u00B0%\u00B5/\\s-]+)$")
        val quantityMatch = quantityRegex.find(cleanAnswer)
        if (quantityMatch != null) {
            val numStr = quantityMatch.groupValues[1]
            val unitStr = quantityMatch.groupValues[2].trim()
            val prefix = if (numStr.startsWith("$") || numStr.startsWith("\u20AC") || numStr.startsWith("\u00A3")) numStr.take(1) else ""
            val cleanNumStr = if (prefix.isNotEmpty()) numStr.substring(1).trim() else numStr
            val rawNum = cleanNumStr.replace(",", "").toDoubleOrNull()
            if (rawNum != null && rawNum > 0.0) {
                val hasComma = cleanNumStr.contains(",")
                val isInt = rawNum % 1.0 == 0.0 && !cleanNumStr.contains(".")
                val multipliers = listOf(0.25, 0.5, 2.0, 4.0, 5.0, 10.0, 0.1, 0.75, 1.5).shuffled()
                val perturbedPool = mutableListOf<String>()
                for (m in multipliers) {
                    val v = rawNum * m
                    val formattedNum = if (isInt) {
                        val longVal = v.toLong()
                        if (hasComma) String.format("%,d", longVal) else longVal.toString()
                    } else {
                        if (hasComma) String.format("%,.2f", v) else String.format("%.2f", v)
                    }
                    val cand = "$prefix$formattedNum $unitStr"
                    if (!cand.equals(cleanAnswer, ignoreCase = true) && !perturbedPool.contains(cand)) {
                        perturbedPool.add(cand)
                    }
                    if (perturbedPool.size >= count) break
                }
                if (perturbedPool.size >= count) {
                    return perturbedPool.shuffled()
                }
            }
        }

        val isListOrTuple = cleanAnswer.contains(Regex("(?<!\\d),(?!\\d)|;"))
        if (isListOrTuple) {
            val listDistractors = generateListDistractors(cleanAnswer, cleanTerm, targetSlot, effectiveCards, effectiveAnswers, count, db)
            if (listDistractors.size >= count) {
                return listDistractors
            }
        }

        val answerWordCount = cleanAnswer.split(Regex("\\s+")).size
        val isTargetShort = answerWordCount <= 3

        val isAnswerYear = Regex("^(1[0-9]{3}|20[0-2][0-9])$").matches(cleanAnswer) ||
                (cleanTerm.contains(Regex("\\b(when|year|date|invented in)\\b", RegexOption.IGNORE_CASE)) && cleanAnswer.matches(Regex("^\\d{4}$")))
        val isAnswerPureNumber = cleanAnswer.matches(Regex("^\\d+$"))
        val isAnswerUnit = Regex("^(\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/%\u00B0]+)$").matches(cleanAnswer)

        val intraDeckMatches = effectiveAnswers
            .map { it.trim() }
            .filter { candidate ->
                candidate.isNotBlank() &&
                        !candidate.equals(cleanAnswer, ignoreCase = true) &&
                        !candidate.contains(Regex("[,;]")) &&
                        when {
                            isAnswerYear -> Regex("^\\d{4}$").matches(candidate)
                            isAnswerPureNumber -> Regex("^\\d+$").matches(candidate)
                            isAnswerUnit -> Regex("^(\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/%\u00B0]+)$").matches(candidate)
                            isTargetShort -> candidate.split(Regex("\\s+")).size in 1..5
                            else -> {
                                val candWords = candidate.split(Regex("\\s+")).size
                                candWords >= 2 && (candWords.toFloat() / answerWordCount.toFloat() in 0.45f..2.25f || effectiveAnswers.size <= 5)
                            }
                        }
            }
            .distinctBy { it.lowercase() }
            .sortedBy { abs(it.length - cleanAnswer.length) }

        if (intraDeckMatches.size >= count && harvestedSiblings.isEmpty()) {
            return intraDeckMatches.take(count).map { matchCasingStyle(it, cleanAnswer) }.shuffled()
        }

        if (db != null && db.isOpen) {
            var clusterSubcategory: String? = null
            val answerLower = cleanAnswer.lowercase()

            db.rawQuery(
                "SELECT subcategory FROM knowledge_clusters WHERE LOWER(term) = ? OR LOWER(aliases) LIKE ? ORDER BY (CASE WHEN LOWER(term) = ? THEN 1 ELSE 2 END) LIMIT 1",
                arrayOf(answerLower, "%$answerLower%", answerLower)
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    clusterSubcategory = cursor.getString(0)
                }
            }

            if (clusterSubcategory == null) {
                val termLower = cleanTerm.lowercase()
                db.rawQuery(
                    "SELECT subcategory FROM knowledge_clusters WHERE LOWER(term) = ? OR LOWER(aliases) LIKE ? ORDER BY (CASE WHEN LOWER(term) = ? THEN 1 ELSE 2 END) LIMIT 1",
                    arrayOf(termLower, "%$termLower%", termLower)
                ).use { cursor ->
                    if (cursor.moveToFirst()) {
                        clusterSubcategory = cursor.getString(0)
                    }
                }
            }

            if (clusterSubcategory == null) {
                val keywords = extractKeywords("$cleanTerm $cleanAnswer")
                for (kw in keywords) {
                    db.rawQuery(
                        "SELECT subcategory FROM knowledge_clusters WHERE LOWER(term) = ? OR LOWER(aliases) LIKE ? LIMIT 1",
                        arrayOf(kw, "%$kw%")
                    ).use { cursor ->
                        if (cursor.moveToFirst()) {
                            clusterSubcategory = cursor.getString(0)
                        }
                    }
                    if (clusterSubcategory != null) break
                }
            }

            if (clusterSubcategory != null) {
                val clusterSiblings = mutableListOf<String>()
                db.rawQuery(
                    "SELECT term FROM knowledge_clusters WHERE subcategory = ? AND LOWER(term) != ? ORDER BY RANDOM() LIMIT 8",
                    arrayOf(clusterSubcategory, answerLower)
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val sib = cursor.getString(0)
                        if (!sib.equals(cleanAnswer, ignoreCase = true) && !harvestedSiblings.any { it.equals(sib, ignoreCase = true) }) {
                            val lenRatio = sib.length.toFloat() / cleanAnswer.length.toFloat()
                            val sim = SemanticEmbeddingEngine.computeSimilarity(cleanAnswer, sib)
                            val isLengthCompatible = lenRatio in 0.35f..2.85f || (cleanAnswer.length <= 15 && sib.length <= 15)
                            val isSemanticallyCompatible = sim == -1f || sim in 0.35f..0.92f
                            if (isLengthCompatible && isSemanticallyCompatible) {
                                clusterSiblings.add(sib)
                            }
                        }
                    }
                }
                if (clusterSiblings.isNotEmpty()) {
                    val combined = (harvestedSiblings + clusterSiblings).distinctBy { it.lowercase() }
                    if (combined.size >= count) {
                        return combined.take(count).map { matchCasingStyle(it, cleanAnswer) }.shuffled()
                    }
                }
            }
        }

        if (isAnswerYear) {
            val y = cleanAnswer.toIntOrNull()
            if (y != null) {
                val offsets = listOf(-10, -8, -6, -5, -4, -3, -2, 2, 3, 4, 5, 6, 8, 10).shuffled()
                val yearDistractors = offsets.map { (y + it).coerceIn(1700, 2025).toString() }
                    .distinct()
                    .filter { it != cleanAnswer }
                    .take(count)
                if (yearDistractors.size >= count) {
                    return yearDistractors.map { matchCasingStyle(it, cleanAnswer) }.shuffled()
                }
            }
        }

        val unitMatch = Regex("^(\\d+(?:\\.\\d+)?)\\s*([a-zA-Z/%\u00B0]+)$").find(cleanAnswer)
        if (unitMatch != null) {
            val numStr = unitMatch.groupValues[1]
            val unitStr = unitMatch.groupValues[2]
            val value = numStr.toDoubleOrNull()
            if (value != null) {
                val powerOfTwoValues = setOf(1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0, 128.0, 256.0, 512.0, 1024.0)
                val unitDistractors = if (value in powerOfTwoValues) {
                    val pool = powerOfTwoValues.filter { it != value && abs(it - value) <= (value * 4) }.shuffled()
                    pool.take(count).map { "${if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()} $unitStr" }
                } else {
                    val factors = listOf(0.5, 0.75, 1.25, 1.5, 2.0).shuffled()
                    factors.take(count).map { f ->
                        val scaled = value * f
                        val formatted = if (scaled % 1.0 == 0.0) scaled.toInt().toString() else String.format("%.1f", scaled)
                        "$formatted $unitStr"
                    }
                }
                if (unitDistractors.size >= count) {
                    return unitDistractors.map { matchCasingStyle(it, cleanAnswer) }.shuffled()
                }
            }
        }

        if (isAnswerPureNumber) {
            val ival = cleanAnswer.toLongOrNull()
            if (ival != null) {
                val powerOfTwoLongs = listOf(1L, 2L, 4L, 8L, 16L, 32L, 64L, 128L, 256L, 512L, 1024L)
                val numDistractors = if (ival in powerOfTwoLongs) {
                    powerOfTwoLongs.filter { it != ival && abs(it - ival) <= (ival * 4) }.shuffled().take(count).map { it.toString() }
                } else {
                    val offsets = listOf(-4L, -3L, -2L, -1L, 1L, 2L, 3L, 4L, 5L).shuffled()
                    offsets.map { (ival + it).coerceAtLeast(0L).toString() }.distinct().filter { it != cleanAnswer }.take(count)
                }
                if (numDistractors.size >= count) {
                    return numDistractors.map { matchCasingStyle(it, cleanAnswer) }.shuffled()
                }
            }
        }

        if (db != null && db.isOpen) {
            val isAcronymCandidate = cleanAnswer.length in 2..7 && cleanAnswer.all { it.isUpperCase() || it.isDigit() || it == '-' || it == '/' }
            if (isAcronymCandidate) {
                val cat = when {
                    deckCategory.contains("hardware", true) || cleanTerm.contains(Regex("\\b(hardware|disk|drive|memory|cpu|gpu|chip|storage|bus)\\b", RegexOption.IGNORE_CASE)) -> "hardware"
                    deckCategory.contains("network", true) || cleanTerm.contains(Regex("\\b(network|protocol|port|packet|ip|lan|wan)\\b", RegexOption.IGNORE_CASE)) -> "networking"
                    deckCategory.contains("os", true) || cleanTerm.contains(Regex("\\b(os|kernel|process|system|thread)\\b", RegexOption.IGNORE_CASE)) -> "operating_systems"
                    deckCategory.contains("database", true) || cleanTerm.contains(Regex("\\b(sql|database|query|table)\\b", RegexOption.IGNORE_CASE)) -> "database"
                    deckCategory.contains("security", true) || cleanTerm.contains(Regex("\\b(security|attack|cipher|auth)\\b", RegexOption.IGNORE_CASE)) -> "cybersecurity"
                    deckCategory.contains("ai", true) || cleanTerm.contains(Regex("\\b(neural|learning|model|intelligence)\\b", RegexOption.IGNORE_CASE)) -> "artificial_intelligence"
                    else -> null
                }

                val acronymList = mutableListOf<String>()
                if (cat != null) {
                    db.rawQuery("SELECT acronym FROM acronyms WHERE category = ? AND acronym != ? ORDER BY RANDOM() LIMIT 8", arrayOf(cat, cleanAnswer)).use { c ->
                        while (c.moveToNext()) {
                            acronymList.add(c.getString(0))
                        }
                    }
                }
                if (acronymList.size < count) {
                    db.rawQuery("SELECT acronym FROM acronyms WHERE acronym != ? ORDER BY RANDOM() LIMIT 8", arrayOf(cleanAnswer)).use { c ->
                        while (c.moveToNext()) {
                            val a = c.getString(0)
                            if (!acronymList.contains(a)) acronymList.add(a)
                        }
                    }
                }
                if (acronymList.size >= count) {
                    return acronymList.shuffled().take(count).map { matchCasingStyle(it, cleanAnswer) }
                }
            }

            val isPersonQuery = cleanTerm.contains(Regex("\\b(who|inventor|invented|father|pioneer|founded|designed|creator)\\b", RegexOption.IGNORE_CASE)) ||
                    (cleanAnswer.split(" ").size in 2..3 && cleanAnswer.split(" ").all { it.isNotEmpty() && it.first().isUpperCase() } && cleanTerm.contains(Regex("\\b(who|inventor|author)\\b", RegexOption.IGNORE_CASE)))

            if (isPersonQuery) {
                val pioneerList = mutableListOf<String>()
                db.rawQuery("SELECT name FROM pioneers WHERE name != ? ORDER BY RANDOM() LIMIT 8", arrayOf(cleanAnswer)).use { c ->
                    while (c.moveToNext()) {
                        pioneerList.add(c.getString(0))
                    }
                }
                if (pioneerList.size >= count) {
                    return pioneerList.shuffled().take(count).map { matchCasingStyle(it, cleanAnswer) }
                }
            }
        }

        val candidates = mutableListOf<String>()
        candidates.addAll(intraDeckMatches)

        if (db != null && db.isOpen) {
            try {
                val domainKey = when {
                    deckCategory.contains("algorithm", true) || deckCategory.contains("data structure", true) || cleanTerm.contains(Regex("\\b(algorithm|tree|graph|sort|search|stack|queue|hash)\\b", RegexOption.IGNORE_CASE)) -> "algorithms_data_structures"
                    deckCategory.contains("hardware", true) || cleanTerm.contains(Regex("\\b(hardware|device|cpu|gpu|memory|disk|drive|chip|bus)\\b", RegexOption.IGNORE_CASE)) -> "computer_hardware"
                    deckCategory.contains("os", true) || cleanTerm.contains(Regex("\\b(os|kernel|process|thread|memory management|file system)\\b", RegexOption.IGNORE_CASE)) -> "operating_systems"
                    deckCategory.contains("network", true) || cleanTerm.contains(Regex("\\b(network|protocol|packet|router|switch|port)\\b", RegexOption.IGNORE_CASE)) -> "networking"
                    else -> null
                }

                if (domainKey != null) {
                    db.rawQuery("SELECT term FROM domain_concepts WHERE domain = ? AND term != ? ORDER BY RANDOM() LIMIT 8", arrayOf(domainKey, cleanAnswer)).use { c ->
                        while (c.moveToNext()) {
                            val term = c.getString(0)
                            if (term.isNotBlank() && !term.equals(cleanAnswer, ignoreCase = true) && !term.equals(cleanTerm, ignoreCase = true)) {
                                candidates.add(term)
                            }
                        }
                    }
                }

                val keywords = extractKeywords("$cleanTerm $cleanAnswer $deckCategory")
                var detectedPos: String? = null

                db.rawQuery("SELECT type FROM words WHERE word = ? LIMIT 1", arrayOf(cleanTerm.lowercase())).use { cursor ->
                    if (cursor.moveToFirst()) {
                        detectedPos = cursor.getString(0)
                    }
                }

                val minLen = (cleanAnswer.length * 0.75).toInt().coerceAtLeast(3)
                val maxLen = (cleanAnswer.length * 1.25).toInt().coerceAtLeast(minLen + 20)

                for (kw in keywords.take(4)) {
                    if (isTargetShort) {
                        val query = if (detectedPos != null) {
                            "SELECT word FROM words WHERE type = ? AND definitions LIKE ? AND word != ? LIMIT 12"
                        } else {
                            "SELECT word FROM words WHERE definitions LIKE ? AND word != ? LIMIT 12"
                        }
                        val args = if (detectedPos != null) {
                            arrayOf(detectedPos, "%$kw%", cleanTerm.lowercase())
                        } else {
                            arrayOf("%$kw%", cleanTerm.lowercase())
                        }

                        db.rawQuery(query, args).use { cursor ->
                            while (cursor.moveToNext()) {
                                val w = cursor.getString(0)
                                if (w.isNotBlank() && !w.equals(cleanTerm, ignoreCase = true) && !w.equals(cleanAnswer, ignoreCase = true)) {
                                    candidates.add(w)
                                }
                            }
                        }
                    } else {
                        val query = "SELECT definitions FROM words WHERE definitions LIKE ? AND LENGTH(definitions) BETWEEN $minLen AND $maxLen LIMIT 8"
                        db.rawQuery(query, arrayOf("%$kw%")).use { cursor ->
                            while (cursor.moveToNext()) {
                                val rawDef = cursor.getString(0)
                                val def = rawDef.replace(Regex("^\\([A-Za-z\\s-]+\\)\\s*"), "").trim()
                                val stemWords = cleanTerm.lowercase().split(Regex("\\s+")).filter { it.length >= 4 }
                                val defLower = def.lowercase()
                                val repeatsStemNoun = stemWords.any { defLower.contains(it) }
                                if (def.isNotBlank() && !def.equals(cleanAnswer, ignoreCase = true) && !repeatsStemNoun) {
                                    candidates.add(def)
                                }
                            }
                        }
                    }
                }

                if (candidates.size < count && isTargetShort) {
                    val targetLen = cleanAnswer.length
                    val minL = (targetLen * 0.75).toInt().coerceAtLeast(3)
                    val maxL = (targetLen * 1.25).toInt().coerceAtLeast(minL + 10)
                    val fallbackQuery = "SELECT word FROM words WHERE LENGTH(word) BETWEEN $minL AND $maxL ORDER BY RANDOM() LIMIT 20"

                    db.rawQuery(fallbackQuery, null).use { cursor ->
                        while (cursor.moveToNext()) {
                            val text = cursor.getString(0)
                            if (text.isNotBlank() && !text.equals(cleanAnswer, ignoreCase = true) && !text.equals(cleanTerm, ignoreCase = true)) {
                                candidates.add(text)
                            }
                        }
                    }
                }

                if (candidates.size < count && isTargetShort) {
                    val broadQuery = "SELECT word FROM words ORDER BY RANDOM() LIMIT 20"
                    db.rawQuery(broadQuery, null).use { cursor ->
                        while (cursor.moveToNext()) {
                            val text = cursor.getString(0)
                            if (text.isNotBlank() && !text.equals(cleanAnswer, ignoreCase = true) && !text.equals(cleanTerm, ignoreCase = true)) {
                                candidates.add(text)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
            }

        }

        if (candidates.size < count) {
            synthesizeStructuralFallbacks(cleanAnswer, cleanTerm, isTargetShort).forEach {
                candidates.add(it)
            }
        }

        val targetLength = cleanAnswer.length
        val normalizedList = candidates
            .filter { it.isNotBlank() && !it.equals(cleanAnswer, ignoreCase = true) && !harvestedSiblings.any { s -> s.equals(it, ignoreCase = true) } }
            .distinctBy { it.lowercase() }
            .filter { cand ->
                val sim = SemanticEmbeddingEngine.computeSimilarity(cleanAnswer, cand)
                val lenRatio = cand.length.toFloat() / targetLength.toFloat()
                (sim == -1f || sim in 0.30f..0.92f) && (lenRatio in 0.25f..3.5f || targetLength <= 10)
            }
            .sortedByDescending { cand ->
                val sim = SemanticEmbeddingEngine.computeSimilarity(cleanAnswer, cand)
                if (sim >= 0f) sim else (1000f - abs(cand.length - targetLength).toFloat())
            }
            .map { matchCasingStyle(it, cleanAnswer) }

        val result = (harvestedSiblings.map { matchCasingStyle(it, cleanAnswer) } + normalizedList)
            .distinctBy { it.lowercase() }
            .take(count)
            .toMutableList()

        if (result.size < count) {
            val extraTerms = effectiveAnswers
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.equals(cleanAnswer, ignoreCase = true) && !result.any { r -> r.equals(it, ignoreCase = true) } }
            for (t in extraTerms) {
                result.add(matchCasingStyle(t, cleanAnswer))
                if (result.size >= count) break
            }
        }

        return result.shuffled()
    }

    private fun generateListDistractors(
        cleanAnswer: String,
        cleanTerm: String,
        targetSlot: String,
        effectiveCards: List<Pair<String, String>>,
        existingCardAnswers: List<String>,
        count: Int,
        db: SQLiteDatabase?
    ): List<String> {
        val delimiter = if (cleanAnswer.contains(";")) "; " else ", "
        val rawElements = cleanAnswer.split(Regex("[,;]\\s*")).filter { it.isNotBlank() }
        if (rawElements.size < 2) return emptyList()

        val pool = mutableListOf<String>()

        if (targetSlot.isNotBlank() && effectiveCards.isNotEmpty()) {
            val sameSlotLists = effectiveCards
                .filter { (cTerm, cAns) ->
                    val cClean = cAns.trim()
                    cClean.isNotBlank() &&
                            !cClean.equals(cleanAnswer, ignoreCase = true) &&
                            cClean.contains(Regex("[,;]")) &&
                            extractAttributeSlot(cTerm).equals(targetSlot, ignoreCase = true)
                }
                .map { it.second.trim() }
                .distinctBy { it.lowercase() }
            pool.addAll(sameSlotLists)
        }

        if (pool.size < count) {
            val replacementPool = mutableListOf<String>()

            if (db != null && db.isOpen) {
                val subcatVotes = mutableMapOf<String, Int>()
                for (elem in rawElements) {
                    val elemLower = elem.lowercase()
                    db.rawQuery(
                        "SELECT subcategory FROM knowledge_clusters WHERE LOWER(term) = ? OR LOWER(aliases) LIKE ? ORDER BY (CASE WHEN LOWER(term) = ? THEN 1 ELSE 2 END) LIMIT 2",
                        arrayOf(elemLower, "%$elemLower%", elemLower)
                    ).use { c ->
                        while (c.moveToNext()) {
                            val subcat = c.getString(0)
                            subcatVotes[subcat] = (subcatVotes[subcat] ?: 0) + 1
                        }
                    }
                }

                val dominantSubcats = subcatVotes.entries.sortedByDescending { it.value }.map { it.key }.take(2)
                for (cat in dominantSubcats) {
                    db.rawQuery(
                        "SELECT term FROM knowledge_clusters WHERE subcategory = ? ORDER BY RANDOM() LIMIT 8",
                        arrayOf(cat)
                    ).use { c ->
                        while (c.moveToNext()) {
                            val t = c.getString(0)
                            if (!replacementPool.contains(t) && rawElements.none { it.equals(t, ignoreCase = true) }) {
                                replacementPool.add(t)
                            }
                        }
                    }
                }
            }

            if (replacementPool.isNotEmpty()) {
                var attempt = 0
                while (pool.size < count && attempt < 16) {
                    val newElements = rawElements.toMutableList()
                    val replaceIdx = attempt % newElements.size
                    val rep = replacementPool.shuffled().firstOrNull { cand ->
                        newElements.none { it.equals(cand, ignoreCase = true) }
                    }
                    if (rep != null) {
                        newElements[replaceIdx] = matchCasingStyle(rep, newElements[replaceIdx])

                        if (attempt >= rawElements.size && newElements.size >= 3) {
                            val secondIdx = (replaceIdx + 1) % newElements.size
                            val rep2 = replacementPool.shuffled().firstOrNull { cand ->
                                newElements.none { it.equals(cand, ignoreCase = true) }
                            }
                            if (rep2 != null) {
                                newElements[secondIdx] = matchCasingStyle(rep2, newElements[secondIdx])
                            }
                        }

                        val candidateList = newElements.joinToString(delimiter)
                        if (!pool.any { it.equals(candidateList, ignoreCase = true) } && !candidateList.equals(cleanAnswer, ignoreCase = true)) {
                            pool.add(candidateList)
                        }
                    }
                    attempt++
                }
            }
        }

        if (pool.size < count) {
            val otherListsInDeck = effectiveCards
                .map { it.second.trim() }
                .filter { it.isNotBlank() && !it.equals(cleanAnswer, ignoreCase = true) && it.contains(Regex("[,;]")) }
                .distinctBy { it.lowercase() }
            for (ol in otherListsInDeck) {
                if (!pool.any { it.equals(ol, ignoreCase = true) }) {
                    val sim = SemanticEmbeddingEngine.computeSimilarity(cleanAnswer, ol)
                    val lenRatio = ol.length.toFloat() / cleanAnswer.length.toFloat()
                    if (sim in 0.40f..0.92f && lenRatio in 0.4f..2.5f) {
                        pool.add(ol)
                    }
                }
                if (pool.size >= count) break
            }
        }

        if (pool.size < count) {
            val permutations = listOf(
                rawElements.reversed(),
                listOf(rawElements.last()) + rawElements.dropLast(1),
                rawElements.drop(1) + listOf(rawElements.first())
            )
            for (p in permutations) {
                val cand = p.joinToString(delimiter)
                if (!pool.contains(cand) && cand != cleanAnswer) {
                    pool.add(cand)
                }
                if (pool.size >= count) break
            }
        }

        return pool.take(count).map { matchCasingStyle(it, cleanAnswer) }.shuffled()

    }

    private fun extractKeywords(text: String): List<String> {
        val matches = Regex("\\b[a-zA-Z]{4,}\\b").findAll(text)
        return matches.map { it.value.lowercase() }
            .filter { it !in commonStopwords }
            .distinct()
            .toList()
    }

    private fun matchCasingStyle(candidate: String, reference: String): String {
        if (candidate.isBlank() || reference.isBlank()) return candidate

        val refTrim = reference.trim()
        val isRefAllUpper = refTrim.length in 2..6 && refTrim.all { it.isUpperCase() || !it.isLetter() }
        val isCandAcronym = candidate.trim().uppercase() in knownAcronyms ||
                (candidate.length in 2..5 && candidate.all { it.isLetter() || it == '-' })

        if (isRefAllUpper && isCandAcronym) {
            return candidate.uppercase()
        }

        val refStartsUpper = refTrim.first().isUpperCase()

        val restored = candidate.split(Regex("(?<=[\\s,;])|(?=[\\s,;])")).joinToString("") { token ->
            val cleanToken = token.trim()
            val matchAcronym = knownAcronyms.firstOrNull { it.equals(cleanToken, ignoreCase = true) }
            if (matchAcronym != null) {
                matchAcronym
            } else {
                token
            }
        }

        return if (refStartsUpper && restored.isNotEmpty() && restored.first().isLowerCase()) {
            restored.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            restored
        }
    }

    private fun synthesizeStructuralFallbacks(answer: String, term: String, isShort: Boolean): List<String> {
        val results = mutableListOf<String>()
        val words = answer.split(Regex("\\s+"))

        if (isShort && words.size in 1..3) {
            val headNoun = words.last().lowercase()
            val techSubstitutes = listOf("controller", "processor", "hardware", "peripheral", "module", "system", "component")
            techSubstitutes.forEach { sub ->
                if (!sub.equals(headNoun, ignoreCase = true)) {
                    if (words.size > 1) {
                        results.add("${words.dropLast(1).joinToString(" ")} $sub")
                    } else {
                        results.add(sub)
                    }
                }
            }
        }
        return results
    }
}


package com.uncode.app

import java.util.Locale

/**
 * Authoritative Single Source of Truth for Digital Web Safe-Havens (Stage 2 Web Gate).
 *
 * Centralizes:
 * 1. General Reference, Dictionaries & Encyclopedias (Wikipedia, Britannica, Merriam-Webster)
 * 2. Learning Management Systems (LMS) & School Portals (Canvas, Blackboard, Classroom, Moodle)
 * 3. Science, Mathematics, STEM & Computational Tools (Desmos, GeoGebra, WolframAlpha, Symbolab)
 * 4. Scholarly Research, Journals & Scientific Repositories (arXiv, PubMed, JSTOR, Nature, IEEE)
 * 5. Online Learning Platforms, Study Guides & Open Courseware (Coursera, edX, Quizlet, DeepL)
 * 6. Computer Science, Programming & Developer Docs (MDN, W3Schools, StackOverflow, GitHub)
 * 7. Student Productivity & Document Workspaces (Google Docs, Drive, Sheets, Notion, Obsidian)
 * 8. Worldwide Educational (.edu, .ac.*) and Governmental (.gov, .mil) domains
 * 9. Browser Internal Safe Schemes (about:blank, newtab)
 * 10. Dynamic Unified Services (YouTube, AI assistants when active in policy) and Custom Allowed Domains
 */
object KnownSafeWeb {

    @JvmField
    val ACADEMIC_EXEMPT_DOMAINS: Set<String> = hashSetOf(
        // General Reference, Dictionaries & Encyclopedias
        "wikipedia.org", "wikimedia.org", "wiktionary.org", "wikibooks.org", "wikisource.org",
        "wikiversity.org", "wikidata.org", "britannica.com", "worldbook.com",
        "merriam-webster.com", "dictionary.com", "thesaurus.com", "oxfordlearnersdictionaries.com",
        "cambridge.org", "collinsdictionary.com", "vocabulary.com", "etymonline.com",
        "plato.stanford.edu", "iep.utm.edu",

        // Learning Management Systems (LMS) & School Portals
        "canvaslms.com", "instructure.com", "blackboard.com", "schoology.com",
        "moodle.org", "moodle.com", "edmodo.com", "classroom.google.com",
        "powerschool.com", "infinitecampus.com", "seesaw.me", "d2l.com", "brightspace.com",
        "edgenuity.com", "collegeboard.org", "apcentral.collegeboard.org", "act.org",

        // Science, Mathematics, STEM & Computational Tools
        "desmos.com", "geogebra.org", "wolframalpha.com", "wolfram.com",
        "symbolab.com", "mathway.com", "cymath.com", "integral-calculator.com",
        "derivative-calculator.net", "quickmath.com", "mathsisfun.com", "mathisfun.com", "purplemath.com",
        "khanacademy.org", "brilliant.org", "physicsclassroom.com", "phet.colorado.edu",
        "chemguide.co.uk", "ptable.com", "biomanbio.com", "cellsalive.com",
        "hyperphysics.phy-astr.gsu.edu", "projecteuler.net", "oeis.org",

        // Scholarly Research, Journals & Scientific Repositories
        "arxiv.org", "biorxiv.org", "medrxiv.org", "researchgate.net", "jstor.org",
        "nature.com", "science.org", "sciencedirect.com", "springer.com", "wiley.com",
        "cell.com", "pnas.org", "ieee.org", "ieeexplore.ieee.org", "acm.org", "dl.acm.org",
        "ncbi.nlm.nih.gov", "nih.gov", "pubmed.ncbi.nlm.nih.gov", "scholar.google.com",
        "semanticscholar.org", "scopus.com", "frontiersin.org", "plos.org", "plosone.org",
        "cern.ch", "nasa.gov", "noaa.gov", "usgs.gov",

        // Online Learning Platforms, Study Guides & Open Courseware
        "coursera.org", "edx.org", "udemy.com", "futurelearn.com", "openlearning.com",
        "quizlet.com", "brainly.com", "chegg.com", "coursehero.com", "studocu.com", "scribd.com",
        "academia.edu", "gutenberg.org", "archive.org", "openlibrary.org",
        "sparknotes.com", "cliffsnotes.com", "litcharts.com",

        // Language Learning
        "duolingo.com", "babbel.com", "memrise.com", "busuu.com", "lingvist.com", "ankiweb.net",
        "reverso.net", "wordreference.com", "linguee.com", "deepl.com", "translate.google.com",

        // Computer Science, Programming & Developer Docs
        "developer.mozilla.org", "w3schools.com", "geeksforgeeks.org",
        "stackoverflow.com", "stackexchange.com", "superuser.com", "serverfault.com", "askubuntu.com",
        "github.com", "gitlab.com", "bitbucket.org",
        "leetcode.com", "hackerrank.com", "codewars.com", "neetcode.io",
        "freecodecamp.org", "codecademy.com", "tutorialspoint.com", "javatpoint.com", "baeldung.com",
        "docs.oracle.com", "docs.python.org", "nodejs.org", "typescriptlang.org",
        "react.dev", "angular.dev", "vuejs.org", "nextjs.org", "svelte.dev",
        "tailwindcss.com", "getbootstrap.com",
        "developer.android.com", "developer.apple.com", "learn.microsoft.com",
        "aws.amazon.com", "cloud.google.com", "kubernetes.io", "docker.com",
        "docs.unity3d.com", "docs.godotengine.org", "unrealengine.com",
        "kaggle.com", "huggingface.co", "paperswithcode.com",
        "overleaf.com", "latex-project.org", "ctan.org",

        // Student Productivity & Document Workspaces
        "docs.google.com", "drive.google.com", "sheets.google.com", "slides.google.com",
        "forms.google.com", "sites.google.com", "keep.google.com", "calendar.google.com",
        "notion.so", "notion.site", "obsidian.md", "trello.com", "miro.com", "figma.com", "canva.com",
        "lucidchart.com", "coggle.it", "draw.io", "diagrams.net",
        "office.com", "onedrive.live.com", "onenote.com", "sharepoint.com"
    )

    private val ACADEMIC_KEYWORDS: Array<String> = arrayOf(
        "wikipedia", "wikimedia", "wiktionary", "wikibooks", "wikiversity", "wikidata",
        "britannica", "worldbook", "merriam-webster", "dictionary.com", "thesaurus.com",
        "canvaslms", "instructure", "blackboard", "schoology", "moodle", "classroom.google",
        "powerschool", "infinitecampus", "collegeboard", "khanacademy", "brilliant.org",
        "desmos", "geogebra", "wolfram", "symbolab", "mathway", "physicsclassroom",
        "arxiv", "biorxiv", "medrxiv", "jstor", "researchgate", "nature.com", "sciencedirect",
        "pubmed", "scholar.google", "semanticscholar", "nasa.gov", "cern.ch",
        "coursera", "edx", "udemy", "quizlet", "brainly", "chegg", "coursehero",
        "duolingo", "babbel", "memrise", "deepl", "translate.google",
        "developer.mozilla", "w3schools", "geeksforgeeks", "stackoverflow", "stackexchange",
        "github", "gitlab", "leetcode", "hackerrank", "freecodecamp",
        "docs.oracle", "docs.python", "developer.android", "learn.microsoft",
        "google.com/search"
    )

    /**
     * Stage 2 Web Gate: Evaluates whether a destination URL or domain is KnownSafe.
     * Combines institutional academic immunity with active Unified Policy services and custom allowed domains.
     */
    @JvmStatic
    fun isKnownSafeWeb(urlOrDomain: String?, allowedDomains: Set<String>?): Boolean {
        if (urlOrDomain == null || urlOrDomain.trim().isEmpty()) return false
        val clean = urlOrDomain.trim().lowercase(Locale.US)

        // 1. Tier 1 Academic & Institutional Immunity
        if (isAcademicExempt(clean)) {
            return true
        }

        // 2. Active Unified Policy Services & Custom Allowed Domains
        if (!allowedDomains.isNullOrEmpty()) {
            val host = WebBlocklistConstants.extractHost(clean)
            for (domain in allowedDomains) {
                val cleanDomain = domain.trim().lowercase(Locale.US)
                if (host == cleanDomain || host.endsWith(".$cleanDomain") || clean.contains(cleanDomain)) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Checks if a domain or URL is immune under the academic whitelist.
     * Enforces worldwide educational (.edu, .ac.*) and governmental (.gov, .mil) domain immunity.
     */
    @JvmStatic
    fun isAcademicExempt(lowerUrl: String?): Boolean {
        if (lowerUrl == null) return false
        val clean = lowerUrl.trim().lowercase(Locale.US)

        // Internal browser schemes
        if (clean == "about:blank" || clean.startsWith("about:") ||
            clean == "chrome://newtab" || clean == "edge://newtab" ||
            clean.startsWith("chrome-native://") || clean == "new tab") {
            return true
        }

        for (kw in ACADEMIC_KEYWORDS) {
            if (clean.contains(kw)) return true
        }

        for (exempt in ACADEMIC_EXEMPT_DOMAINS) {
            if (clean.contains(exempt)) return true
        }

        return clean.contains(".edu/") || clean.endsWith(".edu") ||
               clean.contains(".edu.") || clean.contains(".ac.uk") ||
               clean.contains(".ac.jp") || clean.contains(".ac.in") ||
               clean.contains(".edu.au") || clean.contains(".edu.sg") ||
               clean.contains(".edu.ph") || clean.contains(".edu.cn") ||
               clean.contains(".edu.br") || clean.contains(".edu.mx") ||
               clean.contains(".edu.ng") || clean.contains(".gov/") ||
               clean.endsWith(".gov") || clean.contains(".gov.") ||
               clean.contains(".gov.uk") || clean.contains(".gov.au") ||
               clean.contains(".gov.ph") || clean.contains(".mil/") ||
               clean.endsWith(".mil")
    }
}

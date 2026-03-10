package com.zachvlat.lootify.network

import com.zachvlat.lootify.data.FreeGame
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class RssParser {

    private val gameSources = listOf(
        "steam", "epic", "gog", "itch.io", "microsoft", "xbox", "fanatical"
    )

    fun parse(xml: String): List<FreeGame> {
        val games = mutableListOf<FreeGame>()
        
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var currentEntry: MutableMap<String, String>? = null
        var inAuthor = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "entry" -> {
                            currentEntry = mutableMapOf()
                        }
                        "author" -> {
                            inAuthor = true
                        }
                        "title" -> {
                            if (!inAuthor) {
                                currentEntry?.put("title", parser.nextText() ?: "")
                            }
                        }
                        "link" -> {
                            val href = parser.getAttributeValue(null, "href")
                            if (href != null && currentEntry?.containsKey("link") != true) {
                                currentEntry?.put("link", href)
                            }
                        }
                        "id" -> {
                            if (!inAuthor) {
                                currentEntry?.put("id", parser.nextText() ?: "")
                            }
                        }
                        "published" -> {
                            if (!inAuthor) {
                                currentEntry?.put("published", parser.nextText() ?: "")
                            }
                        }
                        "name" -> {
                            if (inAuthor) {
                                currentEntry?.put("author", parser.nextText() ?: "")
                            }
                        }
                        "content" -> {
                            val content = parser.nextText() ?: ""
                            val gameLink = extractGameLink(content)
                            if (gameLink != null) {
                                currentEntry?.put("gameLink", gameLink)
                            }
                            currentEntry?.put("content", content)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "author") {
                        inAuthor = false
                    }
                    if (parser.name == "entry" && currentEntry != null) {
                        val title = currentEntry["title"] ?: ""
                        val gameLink = currentEntry["gameLink"] ?: currentEntry["link"] ?: ""
                        
                        if (title.isNotBlank() && gameLink.isNotBlank()) {
                            val source = detectSource(title, gameLink)
                            val cleanTitle = cleanTitle(title)
                            val rawDate = currentEntry["published"] ?: ""
                            val formattedDate = try {
                                OffsetDateTime.parse(rawDate).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            } catch (e: Exception) {
                                rawDate.take(10)
                            }
                            games.add(
                                FreeGame(
                                    id = currentEntry["id"] ?: "",
                                    title = cleanTitle,
                                    gameLink = gameLink,
                                    postLink = currentEntry["link"] ?: "",
                                    author = currentEntry["author"] ?: "",
                                    publishedDate = formattedDate,
                                    source = source
                                )
                            )
                        }
                        currentEntry = null
                    }
                }
            }
            eventType = parser.next()
        }
        
        return games
    }

    private fun extractGameLink(content: String): String? {
        val patterns = listOf(
            """href="([^"]+)"""".toRegex(),
            """href=&quot;([^&]+)&quot;""".toRegex()
        )
        
        for (pattern in patterns) {
            val matches = pattern.findAll(content)
            for (match in matches) {
                val link = match.groupValues[1]
                if (isGameLink(link)) {
                    return link
                }
            }
        }
        
        return null
    }

    private fun isGameLink(link: String): Boolean {
        val lowerLink = link.lowercase()
        return gameSources.any { lowerLink.contains(it) }
    }

    private fun detectSource(title: String, gameLink: String): String {
        val combined = (title + gameLink).lowercase()
        return when {
            combined.contains("steam") -> "Steam"
            combined.contains("epic") -> "Epic Games"
            combined.contains("gog") -> "GOG"
            combined.contains("itch.io") -> "Itch.io"
            combined.contains("microsoft") || combined.contains("xbox") -> "Microsoft"
            combined.contains("fanatical") -> "Fanatical"
            else -> "Other"
        }
    }

    private fun cleanTitle(title: String): String {
        var cleaned = title
        val patterns = listOf(
            """\[.*?]""".toRegex(),
            """\(.*?\)""".toRegex()
        )
        for (pattern in patterns) {
            cleaned = pattern.replace(cleaned, "")
        }
        return cleaned.trim()
    }
}

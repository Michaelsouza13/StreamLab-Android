package com.streamlab.tv.utils

import com.streamlab.tv.data.local.ChannelEntity
import java.io.InputStream

object M3uParser {

    fun parse(inputStream: InputStream): List<ChannelEntity> {
        val channels = mutableListOf<ChannelEntity>()
        
        var currentName = "Canal"
        var currentLogo = ""
        var currentGroup = "Geral"

        val tvgLogoRegex = Regex("""(?:tvg-logo|logo)=["']([^"']*)["']""", RegexOption.IGNORE_CASE)
        val groupTitleRegex = Regex("""(?:group-title|tvg-group)=["']([^"']*)["']""", RegexOption.IGNORE_CASE)
        val tvgNameRegex = Regex("""tvg-name=["']([^"']*)["']""", RegexOption.IGNORE_CASE)

        inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (rawLine in lines) {
                val line = rawLine.trim().removePrefix("\uFEFF")
                if (line.isEmpty()) continue

                if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                    val tvgLogoMatch = tvgLogoRegex.find(line)
                    val groupTitleMatch = groupTitleRegex.find(line)
                    val tvgNameMatch = tvgNameRegex.find(line)

                    currentLogo = tvgLogoMatch?.groupValues?.get(1)?.trim() ?: ""
                    currentGroup = groupTitleMatch?.groupValues?.get(1)?.trim()?.ifEmpty { "Geral" } ?: "Geral"

                    val commaIndex = line.lastIndexOf(',')
                    if (commaIndex != -1 && commaIndex + 1 < line.length) {
                        val parsedName = line.substring(commaIndex + 1).trim()
                        currentName = parsedName.ifEmpty { tvgNameMatch?.groupValues?.get(1)?.trim() ?: "Canal" }
                    } else {
                        currentName = tvgNameMatch?.groupValues?.get(1)?.trim() ?: "Canal"
                    }
                } else if (line.startsWith("#EXTGRP:", ignoreCase = true)) {
                    val grp = line.substringAfter(":").trim()
                    if (grp.isNotEmpty()) {
                        currentGroup = grp
                    }
                } else if (!line.startsWith("#")) {
                    val cleanUrl = line.split("|").firstOrNull()?.trim() ?: line
                    if (cleanUrl.startsWith("http://", ignoreCase = true) || 
                        cleanUrl.startsWith("https://", ignoreCase = true) ||
                        cleanUrl.startsWith("rtmp://", ignoreCase = true) ||
                        cleanUrl.startsWith("rtsp://", ignoreCase = true)) {
                        channels.add(
                            ChannelEntity(
                                name = currentName,
                                logo = currentLogo,
                                url = cleanUrl,
                                group = currentGroup
                            )
                        )
                    }
                    // Reset for next entry
                    currentName = "Canal"
                    currentLogo = ""
                    currentGroup = "Geral"
                }
            }
        }
        return channels
    }
}

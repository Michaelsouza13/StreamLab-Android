package com.streamlab.tv.utils

import com.streamlab.tv.data.local.ChannelEntity
import java.io.InputStream

object M3uParser {

    fun parse(inputStream: InputStream): List<ChannelEntity> {
        val channels = mutableListOf<ChannelEntity>()
        val lines = inputStream.bufferedReader().readLines()
        
        var currentName = "Unknown Channel"
        var currentLogo = ""
        var currentGroup = "All"

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("#EXTINF:")) {
                // Parse attributes
                val tvgLogoMatch = Regex("""tvg-logo="([^"]*)"""").find(trimmedLine)
                val groupTitleMatch = Regex("""group-title="([^"]*)"""").find(trimmedLine)
                
                currentLogo = tvgLogoMatch?.groupValues?.get(1) ?: ""
                currentGroup = groupTitleMatch?.groupValues?.get(1) ?: "All"

                // Parse name (usually after the last comma)
                val commaIndex = trimmedLine.lastIndexOf(',')
                if (commaIndex != -1) {
                    currentName = trimmedLine.substring(commaIndex + 1).trim()
                }
            } else if (!trimmedLine.startsWith("#") && trimmedLine.isNotEmpty()) {
                // It's a URL
                channels.add(
                    ChannelEntity(
                        name = currentName,
                        logo = currentLogo,
                        url = trimmedLine,
                        group = currentGroup
                    )
                )
                // Reset for next entry
                currentName = "Unknown Channel"
                currentLogo = ""
                currentGroup = "All"
            }
        }
        return channels
    }
}

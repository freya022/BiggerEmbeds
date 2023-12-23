package io.github.freya022.bot.link

val urlRegex = Regex("""https://.+?\.(?:com|net)\S*""")

typealias SourceMessageID = Long
typealias RepostedMessageID = Long
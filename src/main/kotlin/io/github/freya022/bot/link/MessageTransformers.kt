package io.github.freya022.bot.link

import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData

object MessageTransformers {
    /** `null` if unmodified */
    suspend fun transformMessage(baseBuilder: MessageCreateBuilder, originalAttachments: List<Attachment>, transformers: List<MessageTransformer>): MessageCreateData? {
        val data = TransformData(baseBuilder, originalAttachments.toMutableList())
        try {
            transformers.forEach { it.processMessage(data) }
            return when {
                data.hasChanged -> data.build()
                else -> null
            }
        } finally {
            data.close()
        }
    }
}

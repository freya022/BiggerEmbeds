package io.github.freya022.bot.link

import io.github.freya022.bot.utils.SuspendAutoCloseable
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData

typealias SuccessCallback = suspend () -> Unit

class TransformData private constructor(
    private val originalMessageData: DataObject?,
    val builder: MessageCreateBuilder,
    private val originalAttachments: MutableList<Attachment>,
) : SuspendAutoCloseable {
    enum class AttachmentResult {
        KEEP,
        REMOVE
    }

    constructor(builder: MessageCreateBuilder, originalAttachments: MutableList<Attachment>) : this(builder.tryBuild()?.toData(), builder, originalAttachments)
    constructor(message: Message) : this(MessageCreateBuilder.fromMessage(message), message.attachments.toMutableList())
    constructor(content: String) : this(MessageCreateBuilder().setContent(content), mutableListOf())

    private val leftOverAttachments: List<Attachment> get() = originalAttachments
    private val onSuccessCallbacks: MutableList<SuccessCallback> = arrayListOf()

    val hasChanged: Boolean get() = originalMessageData != builder.tryBuild()?.toData()

    fun addCallback(callback: SuccessCallback) {
        onSuccessCallbacks += callback
    }

    internal suspend fun forEachRemainingAttachment(block: suspend (Attachment) -> AttachmentResult): Boolean {
        var modified = false
        val iterator = originalAttachments.listIterator()
        while (iterator.hasNext()) {
            val attachment = iterator.next()
            val result = block(attachment)
            if (result == AttachmentResult.REMOVE) {
                iterator.remove()
                modified = true
            }
        }

        return modified
    }

    fun buildMessage(): MessageCreateData {
        // Disabled, re-uploading attachments is pointless as they can't be suppressed from someone's message
//        val leftOverAttachments = leftOverAttachments.map { FileUpload.fromData(it.proxy.download().await(), it.fileName) }
//        builder.setFiles(leftOverAttachments + builder.attachments)
        return builder.build()
    }

    override suspend fun close() {
        onSuccessCallbacks.forEach {
            it.invoke()
        }
    }
}

private fun MessageCreateBuilder.tryBuild(): MessageCreateData? = when {
    isValid -> build()
    else -> null
}
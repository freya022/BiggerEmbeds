package io.github.freya022.bot.link

import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData

class TransformData(
    private val builder: MessageCreateBuilder,
    private val originalAttachments: MutableList<Attachment>,
) {
    private typealias SuccessCallback = suspend () -> Unit

    enum class AttachmentResult {
        KEEP,
        REMOVE
    }

    private val onSuccessCallbacks: MutableList<SuccessCallback> = arrayListOf()

    var hasChanged: Boolean = false
        private set

    val content = builder.content
    fun setContent(content: String) {
        hasChanged = true
        builder.setContent(content)
    }

    fun addComponents(vararg components: MessageTopLevelComponent) {
        hasChanged = true
        builder.addComponents(*components)
    }

    fun addComponents(components: Collection<MessageTopLevelComponent>) {
        hasChanged = true
        builder.addComponents(components)
    }

    fun addFiles(vararg files: FileUpload) {
        hasChanged = true
        builder.addFiles(*files)
    }

    fun addFiles(files: Collection<FileUpload>) {
        hasChanged = true
        builder.addFiles(files)
    }

    fun addCallback(callback: SuccessCallback) {
        onSuccessCallbacks += callback
    }

    internal suspend fun forEachRemainingAttachment(block: suspend (Attachment) -> AttachmentResult) {
        val iterator = originalAttachments.listIterator()
        while (iterator.hasNext()) {
            val attachment = iterator.next()
            val result = block(attachment)
            if (result == AttachmentResult.REMOVE) {
                iterator.remove()
                hasChanged = true
            }
        }
    }

    fun build(): MessageCreateData {
        return builder.build()
    }

    suspend fun close() {
        onSuccessCallbacks.forEach { it.invoke() }
    }
}

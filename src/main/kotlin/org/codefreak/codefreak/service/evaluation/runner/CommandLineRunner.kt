package org.codefreak.codefreak.service.evaluation.runner

import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.Feedback
import org.codefreak.codefreak.service.ContainerService
import org.codefreak.codefreak.service.ExecResult
import org.codefreak.codefreak.service.evaluation.EvaluationRunner
import org.fusesource.jansi.HtmlAnsiOutputStream
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*
import kotlin.text.Charsets.UTF_8

@Component
class CommandLineRunner : EvaluationRunner {

  data class Execution(val command: String, val result: ExecResult)

  @Autowired
  private lateinit var containerService: ContainerService

  override fun getName() = "commandline"

  override fun getDefaultTitle() = "Command Line"

  override fun getDocumentationUrl() = "https://docs.codefreak.org/codefreak/for-teachers/definitions.html#commandline"

  override fun getOptionsSchema() = ClassPathResource("evaluation/commandline.schema.json").inputStream.use { String(it.readBytes()) }

  override fun run(answer: Answer, options: Map<String, Any>): List<Feedback> {
    return executeCommands(answer, options, null).map(this::executionToFeedback)
  }

  protected fun executionToFeedback(execution: Execution): Feedback {
    return Feedback(execution.command).apply {
      longDescription = if (execution.result.output.isNotBlank()) {
        wrapInMarkdownHTMLCodeBlock(processColorCodes(convertBase64(execution.result.output.trim())))
      } else null
      status = if (execution.result.exitCode == 0L) Feedback.Status.SUCCESS else Feedback.Status.FAILED
    }
  }

  override fun summarize(feedbackList: List<Feedback>): String {
    return if (feedbackList.any { feedback -> feedback.isFailed }) {
      "FAILED"
    } else {
      "OK"
    }
  }

  protected fun executeCommands(answer: Answer, options: Map<String, Any>, processFiles: ((InputStream) -> Unit)?): List<Execution> {
    val image = options.getRequired("image", String::class)
    val projectPath = options.getRequired("project-path", String::class)
    val commands = options.getList("commands", String::class, true)!!
    val stopOnFail = options.get("stop-on-fail", Boolean::class) ?: true

    return containerService.runCommandsForEvaluation(answer, image, projectPath, commands.toList(), stopOnFail, processFiles)
        .mapIndexed { index, result -> Execution(commands[index], result) }
  }

  protected fun wrapInMarkdownCodeBlock(value: String) = "```\n$value\n```"
  protected fun wrapInMarkdownHTMLCodeBlock(value: String) = "<virtualHtml>\n${value.replace("\n", "<br>")}\n</virtualHtml>"
  protected fun processColorCodes(text: String): String {
    val os = ByteArrayOutputStream()
    val hos = HtmlAnsiOutputStream(os)
    hos.write(text.toByteArray(UTF_8))
    hos.close()
    return String(os.toByteArray(), UTF_8)
  }
  protected fun convertBase64(txt: String): String {
    if (!txt.startsWith("base64:", ignoreCase = false)){
      return txt
    }
    val decodedBytes: ByteArray = Base64.getDecoder().decode(txt.substring(7))
    return String(decodedBytes)
  }
}

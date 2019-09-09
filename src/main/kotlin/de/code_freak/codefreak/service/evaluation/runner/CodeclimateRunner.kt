package de.code_freak.codefreak.service.evaluation.runner

import de.code_freak.codefreak.entity.Answer
import de.code_freak.codefreak.service.ContainerService
import de.code_freak.codefreak.service.evaluation.EvaluationRunner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CodeclimateRunner : EvaluationRunner {

  @Autowired
  private lateinit var containerService: ContainerService

  override fun getName(): String {
    return "codeclimate"
  }

  override fun run(answer: Answer, options: Map<String, Any>): String {
    return containerService.runCodeclimate(answer)
  }

  override fun parseResultContent(content: ByteArray): Any {
    return String(content)
  }
}

package org.codefreak.codefreak.service.file

import org.codefreak.codefreak.config.AppConfiguration
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.Task
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
class PathResolver
{
  @Autowired
  lateinit var config: AppConfiguration

  constructor()
  constructor(config: AppConfiguration) {
    this.config = config
  }

  var root: Path? = null
  var location: Path? = null
    set(value){
      field = if (root != null)
        value
      else
        null
    }

  val path: Path?
    get(){
      return (root?.resolve(location!!)?: null)
    }

  private fun batchReplaceInString(str: String, replacements: List<Pair<String, String>>): String {
    var tmpStr: String = str
    replacements.forEach {
      tmpStr = tmpStr.replace(it.first, it.second)
    }
    return tmpStr
  }

  fun resolveAnswerPath(answer: Answer): PathResolver {
    val answerTitle = answer.task.title
    val assignmentTitle = answer.task.assignment?.title ?: "Unknown_Assignment"
    val username = answer.submission.user.username
    val valueMappings =
        listOf(
            Pair("{answer_title}", answerTitle),
            Pair("{assignment_title}", assignmentTitle),
            Pair("{username}", username),
        )
    val resolver = PathResolver(config)
    if (config.files.hddUsage.contains(AppConfiguration.Files.HDDStorageUsage.Answers)){
      resolver.root = Path.of(config.files.userRoot)
      resolver.location = Path.of(batchReplaceInString(config.files.userAnswerPath, valueMappings))
    }
    return resolver
  }

  fun resolveTasksPath(task:Task): PathResolver {
    val assignmentTitle = task.assignment?.title?: "Unknown_Assignment"
    val assignmentUuid = task.assignment?.id.toString()
    val taskTitle = task.title
    val taskUuid = task.id.toString()
    val valueMappings =
        listOf(
            Pair("{assignment_title}", assignmentTitle),
            Pair("{assignment_uuid}", assignmentUuid),
            Pair("{task_title}", taskTitle),
            Pair("{task_uuid}", taskUuid)
            )
    val resolver = PathResolver(config)
    if (config.files.hddUsage.contains(AppConfiguration.Files.HDDStorageUsage.Assignments)){
      resolver.root = Path.of(config.files.systemRoot)
      resolver.location = Path.of(batchReplaceInString(config.files.taskPath, valueMappings))
    }
    return resolver
  }
}

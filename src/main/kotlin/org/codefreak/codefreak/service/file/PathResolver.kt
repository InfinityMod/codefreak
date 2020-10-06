package org.codefreak.codefreak.service.file

import org.codefreak.codefreak.config.AppConfiguration
import org.codefreak.codefreak.entity.Answer
import org.codefreak.codefreak.entity.Task
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
@Configurable
class PathResolver()
{
  @Autowired
  lateinit var config: AppConfiguration

  var root: Path? = null
  var location: Path? = null
    set(value){
      if (root != null)
        field = value
    }

  val path: Path?
    get(){
      return (root?.resolve(location?: Path.of("")) ?: null)
    }

  private fun batchReplaceInString(str: String, replacements: List<Pair<String, String>>): String {
    var _str: String = str
    replacements.forEach {
      _str = _str.replace(it.first, it.second)
    }
    return _str
  }

  fun resolveAnswerPath(answer: Answer): PathResolver {
    val answer_title = answer.task.title
    val assignment_title = answer.task.assignment?.title ?: "Unknown_Assignment"
    val username = answer.submission.user.username
    val value_mappings =
        listOf<Pair<String, String>>(
            Pair("{answer_title}", answer_title),
            Pair("{assignment_title}", assignment_title),
            Pair("{username}", username),

        )
    val resolver = PathResolver()
    if (config.files.hddUsage.contains(AppConfiguration.Files.HDDStorageUsage.Answers)){
      resolver.root = Path.of(config.files.userRoot)
      resolver.location = Path.of(batchReplaceInString(config.files.userAnswerPath, value_mappings))
    }
    return resolver
  }

  fun resolveTasksPath(task:Task): PathResolver {
    val assignment_title = task.assignment?.title?: "Unknown_Assignment"
    val assignment_uuid = task.assignment?.id.toString()
    val task_title = task.title
    val task_uuid = task.id.toString()
    val value_mappings =
        listOf<Pair<String, String>>(
            Pair("{assignment_title}", assignment_title),
            Pair("{assignment_uuid}", assignment_uuid),
            Pair("{task_title}", task_title),
            Pair("{task_uuid}", task_uuid)
            )
    val resolver = PathResolver()
    if (config.files.hddUsage.contains(AppConfiguration.Files.HDDStorageUsage.Assignments)){
      resolver.root = Path.of(config.files.systemRoot)
      resolver.location = Path.of(batchReplaceInString(config.files.taskPath, value_mappings))
    }
    return resolver
  }
}

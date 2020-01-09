package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import de.code_freak.codefreak.entity.Submission
import de.code_freak.codefreak.graphql.BaseDto
import de.code_freak.codefreak.graphql.ResolverContext

@GraphQLName("Submission")
class SubmissionDto(@GraphQLIgnore val entity: Submission, ctx: ResolverContext) : BaseDto(ctx) {

  @GraphQLID
  val id = entity.id
  val user by lazy { UserDto(entity.user, ctx) }
  val assignment by lazy { AssignmentDto(entity.assignment, ctx) }
  val answers by lazy { entity.answers.map { AnswerDto(it, ctx) } }
}

package org.codefreak.codefreak.graphql

import com.expediagroup.graphql.annotations.GraphQLIgnore
import graphql.kickstart.servlet.context.GraphQLWebSocketContext
import graphql.schema.DataFetchingEnvironment
import org.codefreak.codefreak.auth.Authorization
import org.codefreak.codefreak.auth.NotAuthenticatedException
import org.codefreak.codefreak.entity.Oidc_User
import org.codefreak.codefreak.entity.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import kotlin.reflect.full.isSubclassOf

open class BaseResolver {

  @Autowired
  @GraphQLIgnore
  private lateinit var serviceAccess: ServiceAccess

  @GraphQLIgnore
  fun <T> context(block: ResolverContext.() -> T): T {
    return ResolverContext(serviceAccess, Authorization()).block()
  }

  @GraphQLIgnore
  fun <T> context(env: DataFetchingEnvironment, block: ResolverContext.() -> T): T {
    val ctx = env.getContext<Any>()
    val authorization = if (ctx is GraphQLWebSocketContext) {
      val userPrincipal = ctx.session.userPrincipal
      if (userPrincipal !is Authentication) {
        throw NotAuthenticatedException()
      }
      var user = userPrincipal.principal
      if((user!!::class != User::class) && user!!::class.isSubclassOf(User::class)) {
        user = (user as Oidc_User).user
      }else if (user !is User) {
        throw NotAuthenticatedException()
      }
      Authorization(user)
    } else {
      Authorization()
    }
    return ResolverContext(serviceAccess, authorization).block()
  }
}

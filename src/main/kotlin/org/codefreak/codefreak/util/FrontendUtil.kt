package org.codefreak.codefreak.util

import org.codefreak.codefreak.auth.NotAuthenticatedException
import org.codefreak.codefreak.entity.Oidc_User
import org.codefreak.codefreak.entity.User
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import kotlin.reflect.full.isSubclassOf

object FrontendUtil {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getRequest() = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request

  fun getCurrentUser(): User {
    val auth = SecurityContextHolder.getContext().authentication
    val principal = auth?.principal
    if ((principal!!::class != User::class) && principal!!::class.isSubclassOf(User::class)) {
      return (principal as Oidc_User).user
    }else if (principal is User) {
      return principal
    } else if (auth !is AnonymousAuthenticationToken) {
      log.warn("Unexpected authentication principal: $principal")
    }
    throw NotAuthenticatedException()
  }
  fun getUriBuilder() = ServletUriComponentsBuilder.fromCurrentContextPath()
  //fun getUriBuilder() = ServletUriComponentsBuilder.fromCurrentRequestUri().replacePath(null)
}

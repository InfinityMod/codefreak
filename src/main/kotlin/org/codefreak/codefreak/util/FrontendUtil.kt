package org.codefreak.codefreak.util

import org.codefreak.codefreak.auth.NotAuthenticatedException
import org.codefreak.codefreak.entity.User
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

object FrontendUtil {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getRequest() = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request

  fun getCurrentUser(): User {
    val auth = SecurityContextHolder.getContext().authentication
    val principal = auth?.principal
    if (principal is User) {
      return principal
    } else if (auth !is AnonymousAuthenticationToken) {
      log.warn("Unexpected authentication principal: $principal")
    }
    throw NotAuthenticatedException()
  }

  fun getUriBuilder() = ServletUriComponentsBuilder.fromCurrentRequestUri().replacePath(null)
}

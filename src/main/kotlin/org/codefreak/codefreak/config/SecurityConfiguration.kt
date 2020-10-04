package org.codefreak.codefreak.config

import org.codefreak.codefreak.auth.AuthenticationMethod
import org.codefreak.codefreak.auth.LdapUserDetailsContextMapper
import org.codefreak.codefreak.auth.Role
import org.codefreak.codefreak.auth.SimpleUserDetailsService
import org.codefreak.codefreak.entity.OAuth_User
import org.codefreak.codefreak.entity.Oidc_User
import org.codefreak.codefreak.service.UserService
import org.codefreak.codefreak.util.withTrailingSlash
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.PathRequest
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.BeanIds
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.session.HttpSessionEventPublisher

@Configuration
class SecurityConfiguration : WebSecurityConfigurerAdapter() {

  @Autowired
  lateinit var config: AppConfiguration

  @Autowired
  lateinit var userService: UserService

  @Autowired(required = false)
  var ldapUserDetailsContextMapper: LdapUserDetailsContextMapper? = null

  @Bean(BeanIds.AUTHENTICATION_MANAGER)
  override fun authenticationManagerBean(): AuthenticationManager = super.authenticationManagerBean()

  override fun configure(web: WebSecurity?) {
    // disable web security for all static files
    web?.ignoring()
        ?.requestMatchers(PathRequest.toStaticResources().atCommonLocations())
        ?.antMatchers("/assets/**")
        // all static files served from React – this is far from being perfect.
        // There should be something like "all files from /static directory are allowed" but security is based
        // on the HTTP Request and not on actual files
        ?.antMatchers("/*.*", "/static/**")
  }

  override fun configure(http: HttpSecurity?) {
    http
        ?.authorizeRequests()
            // use /api/... for non-GraphQL API resources that require authentication
            ?.antMatchers("/api/**")?.authenticated()
            ?.anyRequest()?.permitAll()
        ?.and()
            ?.formLogin()
                // force redirect to React's login page
                ?.loginPage("/login")
        ?.and()
            ?.csrf()?.ignoringAntMatchers("/graphql")
        ?.and()
          ?.oauth2Login()?.userInfoEndpoint()
          ?.oidcUserService(CustomOidcUserServiceImpl(config, userService))
          ?.userService(CustomOAuthUserServiceImpl(config, userService))

    http?.sessionManagement()
        ?.maximumSessions(1)
        ?.sessionRegistry(sessionRegistry())
  }

  class CustomOidcUserServiceImpl(config: AppConfiguration, userService: UserService) : OAuth2UserService<OidcUserRequest, OidcUser> {
    private val simpleUserService = userService
    private val oidcUserService: OidcUserService = OidcUserService()
    private val oAuthAuthoritySelector: Map<String, Role> = config.authorities.oAuth2.withDefault { Role.STUDENT }

    @Throws(OAuth2AuthenticationException::class)
    override fun loadUser(userRequest: OidcUserRequest): OidcUser {
      val oidcUser: OidcUser = oidcUserService.loadUser(userRequest)
      val user: Oidc_User = Oidc_User(oidcUser, simpleUserService)
      user.roles.add(oAuthAuthoritySelector.getValue(user.getUsername()))
      return user
    }
  }
  class CustomOAuthUserServiceImpl(config: AppConfiguration, userService: UserService) : OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private val simpleUserService = userService
    private val oAuthService: DefaultOAuth2UserService = DefaultOAuth2UserService()
    private val oAuthAuthoritySelector: Map<String, Role> = config.authorities.oAuth2.withDefault { Role.STUDENT }

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
      val oauthUser: OAuth2User = oAuthService.loadUser(userRequest)
      val user: OAuth_User = OAuth_User(oauthUser, simpleUserService)
      user.roles.add(oAuthAuthoritySelector.getValue(user.getUsername()))
      return user
    }
  }

  @Bean
  override fun userDetailsService(): UserDetailsService {
    return when (config.authenticationMethod) {
      AuthenticationMethod.SIMPLE -> SimpleUserDetailsService(userService)
      else -> super.userDetailsService()
    }
  }

  @Autowired
  @Throws(Exception::class)
  fun configAuthentication(auth: AuthenticationManagerBuilder) {
    auth.userDetailsService(this.userDetailsService())
  }

  @Bean
  fun sessionRegistry(): SessionRegistry {
    return SessionRegistryImpl()
  }

  @Bean
  fun httpSessionEventPublisher(): ServletListenerRegistrationBean<HttpSessionEventPublisher> {
    return ServletListenerRegistrationBean(HttpSessionEventPublisher())
  }

  override fun configure(auth: AuthenticationManagerBuilder?) {
    when (config.authenticationMethod) {
      AuthenticationMethod.LDAP -> configureLdapAuthentication(auth)
      else -> super.configure(auth)
    }
  }

  private fun configureLdapAuthentication(auth: AuthenticationManagerBuilder?) {
    val url = config.ldap.url ?: throw IllegalStateException("LDAP URL has not been configured")
    if (config.ldap.activeDirectory) {
      val authenticationProvider = ActiveDirectoryLdapAuthenticationProvider(null, url, config.ldap.rootDn)
      authenticationProvider.setUserDetailsContextMapper(ldapUserDetailsContextMapper)
      auth?.authenticationProvider(authenticationProvider)
      return
    }
    auth?.ldapAuthentication()
        ?.userDetailsContextMapper(ldapUserDetailsContextMapper)
        ?.userSearchBase(config.ldap.userSearchBase)
        ?.userSearchFilter(config.ldap.userSearchFilter)
        ?.groupSearchBase(config.ldap.groupSearchBase)
        ?.groupSearchFilter(config.ldap.groupSearchFilter)
        ?.contextSource()
            ?.url(url.withTrailingSlash() + config.ldap.rootDn)
  }
}

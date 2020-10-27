package org.codefreak.codefreak.entity

import java.util.Optional
import javax.persistence.CollectionTable
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.Inheritance
import javax.persistence.Table
import kotlin.jvm.Transient
import org.codefreak.codefreak.auth.AuthenticationMethod
import org.codefreak.codefreak.auth.Role
import org.codefreak.codefreak.service.UserService
import org.springframework.beans.BeanUtils
import org.springframework.data.annotation.Immutable
import org.springframework.security.core.CredentialsContainer
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User

@Entity
@Table(name = "user")
open class User(private var username: String) : BaseEntity(), UserDetails, CredentialsContainer {

  @Column(unique = true)
  val usernameCanonical = username.toLowerCase()

  @ElementCollection(targetClass = Role::class, fetch = FetchType.EAGER)
  @CollectionTable
  @Enumerated(EnumType.STRING)
  @Column(name = "role")
  var roles: MutableSet<Role> = mutableSetOf()

  var firstName: String? = null

  var lastName: String? = null

  var password: String? = null
    @JvmName("_getPassword") get

  var authMethod: String? = AuthenticationMethod.SIMPLE.name

  fun getDisplayName() = listOfNotNull(firstName, lastName).ifEmpty { listOf(username) }.joinToString(" ")
  override fun getUsername() = username
  override fun getPassword() = password

  override fun getAuthorities() = roles.flatMap { it.allGrantedAuthorities }.toMutableList()
  override fun isEnabled() = true
  override fun isCredentialsNonExpired() = true
  override fun isAccountNonExpired() = true
  override fun isAccountNonLocked() = true
  override fun eraseCredentials() {
    password = null
  }
}

@Immutable
@Inheritance
@Table(name = "user")
class Oidc_User : User, OidcUser {
  @Transient
  private var oidcPrincipal: OidcUser
  @Transient
  public var user: User

  constructor(oidcPrincipal: OidcUser, userService: UserService) : super(Optional.ofNullable(oidcPrincipal.email).orElse(oidcPrincipal.name)) {
    this.oidcPrincipal = oidcPrincipal
    this.user = userService.getOrCreateUser(super.getUsername()) {
      val firstNames: MutableList<String?> = mutableListOf<String?>()
      val lastNames: MutableList<String?>  = mutableListOf<String?>()


      if (oidcPrincipal.fullName != null) {
        if (oidcPrincipal.fullName!!.isNotEmpty()) {
          val nameSplit = oidcPrincipal.fullName.split(",");
          if (nameSplit.size > 1) {
            firstNames.add(nameSplit.subList(1, nameSplit.size).joinToString(" "))
            lastNames.add(nameSplit.getOrNull(0))
          } else if (nameSplit.size == 1) {
            firstNames.add(nameSplit.getOrNull(0))
          }
        }
      }

      firstNames.add(oidcPrincipal?.givenName)
      lastNames.add(oidcPrincipal?.familyName)

      firstNames.add(oidcPrincipal?.name)
      firstNames.add("unknown")
      lastNames.add("unknown")

      this.firstName = firstNames.filterNotNull()[0]
      this.lastName = lastNames.filterNotNull()[0]
      this.authMethod = AuthenticationMethod.OAUTH.name
    }
    BeanUtils.copyProperties(this.user, this )
  }


  // OidcUser support
  override fun getName(): String {
    return this.oidcPrincipal.getName()
  }
  override fun getAttributes(): MutableMap<String, Any> {
    return this.oidcPrincipal.getAttributes()
  }
  override fun getClaims(): MutableMap<String, Any> {
    return this.oidcPrincipal.getClaims()
  }
  override fun getUserInfo(): OidcUserInfo {
    return this.oidcPrincipal.getUserInfo()
  }
  override fun getIdToken(): OidcIdToken {
    return this.oidcPrincipal.getIdToken()
  }
}

@Immutable
@Inheritance
@Table(name = "user")
class OAuth_User : User, OAuth2User {
  @Transient
  private var oauthPrincipal: OAuth2User
  @Transient
  public var user: User

  constructor(oauthPrincipal: OAuth2User, userService: UserService) : super(Optional.ofNullable(oauthPrincipal.getAttribute<String>("email")).orElse(oauthPrincipal.getAttribute<String>("nickname"))) {
    this.oauthPrincipal = oauthPrincipal
    val names = oauthPrincipal.getAttribute<String>("name")?.split(", ")

    this.user = userService.getOrCreateUser(super.getUsername()) {
      this.firstName  = names?.getOrNull(1) ?: "Unknown"
      this.lastName = names?.getOrNull(0) ?: "Unknown"
      this.authMethod = AuthenticationMethod.OAUTH.name
    }
    BeanUtils.copyProperties(this.user, this)
  }

  // OidcUser support
  override fun getName(): String {
    return this.oauthPrincipal.getName()
  }
  override fun getAttributes(): MutableMap<String, Any> {
    return this.oauthPrincipal.getAttributes()
  }

}

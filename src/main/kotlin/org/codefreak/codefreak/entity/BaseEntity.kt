package org.codefreak.codefreak.entity

import com.vladmihalcea.hibernate.type.json.JsonStringType
import java.util.UUID
import javax.persistence.Id
import javax.persistence.MappedSuperclass
import org.hibernate.annotations.TypeDef
import kotlin.reflect.full.isSubclassOf

@MappedSuperclass
@TypeDef(name = "json", typeClass = JsonStringType::class)
abstract class BaseEntity(
  @Id
  var id: UUID = UUID.randomUUID()
) {

  override fun equals(other: Any?): Boolean {
    other ?: return false

    if (this === other) return true

    if (this::class != other::class && !(other::class.isSubclassOf(this::class) || this::class.isSubclassOf(other::class))) return false

    other as BaseEntity
    return this.id == other.id
  }

  override fun hashCode(): Int {
    return this.id.hashCode()
  }
}

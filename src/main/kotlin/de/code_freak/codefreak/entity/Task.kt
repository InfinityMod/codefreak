package de.code_freak.codefreak.entity

import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Type
import java.time.Instant
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

@Entity
class Task(
  /**
   * Related assignment this task belongs to
   */
  @ManyToOne
  var assignment: Assignment?,

  /**
   * The teacher who created this assignment
   */
  @ManyToOne
  var owner: User,

  /**
   * Position/Index in the assignment (zero-based)
   */
  @Column(nullable = false)
  var position: Long,

  /**
   * Title of the task
   */
  @Column(nullable = false)
  var title: String,

  /**
   * The task body/description of what to do
   */
  @Type( type = "text" )
  var body: String?,

  /**
   * A weight >=0, <=100 how the task is weighted
   * The total weight of all tasks should not be > 100
   */
  var weight: Int?
) : BaseEntity(), Comparable<Task> {
  @OneToMany(mappedBy = "task", cascade = [CascadeType.REMOVE])
  var answers: MutableSet<Answer> = mutableSetOf()

  @CreationTimestamp
  var createdAt: Instant = Instant.now()

  override fun compareTo(other: Task) = position.compareTo(other.position)
}
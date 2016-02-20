package ru.pavkin.todoist.api.core.model

import org.joda.time.DateTime

case class Task(id: TaskId,
                userId: UserId,
                projectId: ProjectId,
                content: String,
                date: Option[TaskDate],
                priority: Priority,
                indent: Indent,
                order: Int,
                dayOrder: Int,
                isCollapsed: Boolean,
                labels: List[LabelId],
                assignedBy: Option[UserId],
                responsible: Option[UserId],
                isCompleted: Boolean,
                isInHistory: Boolean,
                isDeleted: Boolean,
                isArchived: Boolean,
                addedAt: DateTime)

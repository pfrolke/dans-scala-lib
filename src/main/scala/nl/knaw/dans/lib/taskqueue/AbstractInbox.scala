/**
 * Copyright (C) 2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.lib.taskqueue

import java.nio.file.Path

import better.files.{ File, FileMonitor }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.Try

/**
 * An inbox is a directory that contains files (or directories) that generate tasks. The
 * inbox is responsible for converting those files into task objects. The nature of the task
 * is determined by each concrete subclass of `AbstractInbox` by implementing [[AbstractInbox#createTask]].
 *
 * Converting files into tasks can be done synchronously, by calling [[AbstractInbox#enqueue]], or asynchronously
 * by creating a  `FileMonitor` with [[AbstractInbox#createFileMonitor]] and starting it in an execution context of
 * choice. A common scenario is that these two are both needed in that order. This scenario is handled by [[InboxWatcher]].
 *
 * @param dir the directory serving as inbox
 * @tparam T the type of target for the tasks
 */
abstract class AbstractInbox[T](dir: File) extends DebugEnhancedLogging {
  def this(path: Path) = this(File(path))

  private val files = dir.list(f => f != dir, maxDepth = 1).toList

  private val identitySorter = new TaskSorter[T] {
    override def sort(tasks: List[Task[T]]): List[Task[T]] = {
      tasks
    }
  }

  /**
   * Immediately converts the currently available files into tasks and puts them on the provided [[TaskQueue]].
   * The order of enqueueing is determined by a [[TaskSorter]] implementation, if provided.
   *
   * @param q the TaskQueue to which to add the tasks
   * @param s a task sorter
   *
   */
  final def enqueue(q: TaskQueue[T], s: Option[TaskSorter[T]]): Try[Unit] = Try {
    s.getOrElse(identitySorter)
      .sort(files.map(createTask)
        .filter(_.isDefined)
        .map(_.head))
      .foreach(q.add)
  }

  /**
   * Creates and returns a FileMonitor that enqueues new files and directories as they appear in
   * the inbox directory. Note that the caller is responsible for starting the FileMonitor
   * in the ExecutionContext of its choice.
   *
   * @param q the TaskQueue to which to add the tasks
   * @return the FileMonitor
   */
  final def createFileMonitor(q: TaskQueue[T]): FileMonitor = {
    new FileMonitor(dir, maxDepth = 1) {
      override def onCreate(f: File, count: Int): Unit = {
        trace(f, count)
        createTask(f).foreach(q.add)
      }
    }
  }

  /**
   * Converts a file (or directory) into a task or returns `None`, if the file is
   * not actionable, in which case it will simply be ignored.
   *
   * @param f the file to convert
   * @return the task
   */
  def createTask(f: File): Option[Task[T]]
}

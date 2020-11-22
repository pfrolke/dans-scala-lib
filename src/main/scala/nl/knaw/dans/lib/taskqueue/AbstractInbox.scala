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
import java.util.Comparator

import better.files.{ File, FileMonitor }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.collection.JavaConverters._

/**
 * An inbox is a directory that contains files (or directories) that represent tasks. The
 * inbox is responsible for converting those files into task objects. The nature of the task
 * is determined by each concrete subclass of `AbstractInbox` by implementing [[AbstractInbox#createTask]].
 *
 * Converting files into tasks can be done synchronously, by calling [[AbstractInbox#enqueue]], or asynchronously
 * by creating a  `FileMonitor` with [[AbstractInbox#createFileMonitor]] and start it in an execution context of
 * choice.
 *
 * @param dir the directory serving as inbox
 */
abstract class AbstractInbox(dir: File) extends DebugEnhancedLogging {
  private val files = dir.list(_ => true, maxDepth = 1).toList

  /**
   * Immediately convert the currently available files into tasks and put them on the provided [[TaskQueue]].
   * The order of enqueueing is determined by the java.util.Comparator, if provided.
   *
   * @param q the TaskQueue to which to add the tasks
   * @param c the comparator for ordering the available files
   *
   */
  def enqueue(q: TaskQueue, c: Option[Comparator[File]] = None): Unit = {
    val sortedFiles = c.map { c =>
      val mutableFiles = files.asJava
      mutableFiles.sort(c)
      mutableFiles.asScala.toList
    }.getOrElse(files)
    for (f <- sortedFiles) {
      debug(s"Adding $f")
      createTask(f).foreach(q.add)
    }
  }

  /**
   * Creates and returns a FileMonitor that enqueues new files and directories as they appear in
   * the inbox directory. Note that the caller is responsible for starting the FileMonitor
   * in the ExecutionContext of its choice.
   *
   * @param q the TaskQueue to which to add the tasks
   * @return the FileMonitor
   */
  def createFileMonitor(q: TaskQueue): FileMonitor = {
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
  protected def createTask(f: File): Option[Task]
}

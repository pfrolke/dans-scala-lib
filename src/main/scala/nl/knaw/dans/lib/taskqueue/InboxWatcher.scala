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

import java.util.concurrent.Executors

import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
 * Active object that, after it is started, watches an inbox for new files appearing, using the FileMonitor provided
 * by the inbox. Before receiving new files it will first process the files that were available at startup time.
 *
 * @param inbox the inbox to watch
 * @tparam T the type of target for the tasks
 */
class InboxWatcher[T](inbox: AbstractInbox[T]) extends DebugEnhancedLogging {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
  private val tasks: ActiveTaskQueue[T] = new ActiveTaskQueue()
  private val monitor = inbox.createFileMonitor(tasks)

  /**
   * Enqueues files currently present and the starts watching for new ones. A background thread processes
   * the tasks derived from the enqueued files.
   *
   * @param s the task sort
   */
  def start(s: Option[TaskSorter[T]] = None): Try[Unit] = {
    trace(())
    logger.info("Enqueuing files found in inbox...")
    for {
      _ <- inbox.enqueue(tasks, s)
      _ = logger.info("Start processing deposits...")
      _ = tasks.start()
      _ = logger.info("Starting inbox monitor...")
      _ = monitor.start()
    } yield ()
  }

  /**
   * Cancels all tasks except the one currently being executed, then terminates the processing thread.
   */
  def stop(): Try[Unit] = Try {
    trace(())
    logger.info("Sending stop item to queue...")
    tasks.stop()
  }
}

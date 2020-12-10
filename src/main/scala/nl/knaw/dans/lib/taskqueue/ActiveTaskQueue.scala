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

import java.util.concurrent.{ Executors, LinkedBlockingDeque }

import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
 * TaskQueue that processes its [[Task]]s on a dedicated thread.
 *
 * @param capacity the maximum capacity of the queue
 */
class ActiveTaskQueue[T](capacity: Int = 100000) extends TaskQueue[T] with DebugEnhancedLogging {
  private val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
  private val tasks = new LinkedBlockingDeque[Option[Task[T]]](capacity)

  /**
   * Adds a new task to the queue.
   *
   * @param t the task to add
   */
  def add(t: Task[T]): Try [Unit]  = Try {
    trace(t)
    tasks.put(Some(t))
    debug("Task added to queue")
  }

  /**
   * Starts the queue's processing thread.
   */
  def start(): Try[Unit] = Try {
    executionContext.execute(runnable = () => {
      logger.info("Processing thread ready for running tasks")
      while (runTask(tasks.take())) {}
      logger.info("Finished processing tasks.")
    })
  }

  private def runTask(t: Option[Task[T]]): Boolean = {
    t.map(_.run().recover {
      case e: Throwable => logger.warn(s"Task $t failed", e);
    }).isDefined
  }

  /**
   * Cancels pending tasks and lets the currently running task finish. Then lets the
   * processing thread terminate.
   */
  def stop(): Try[Unit] = Try {
    tasks.clear()
    tasks.put(Option.empty[Task[T]])
  }
}

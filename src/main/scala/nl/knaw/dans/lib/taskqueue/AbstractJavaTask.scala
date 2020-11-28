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

import scala.util.Try

/**
 * An alternative for [[Task]] that is easier to implement in Java. Instead of implementing [[Task#run]]
 * implement [[AbstractJavaTask#runTask]] and throw an exception when the task fails.
 *
 * @tparam T the type of the target of this task
 */
abstract class AbstractJavaTask[T] extends Task[T] {
  final override def run(): Try[Unit] = Try {
    runTask()
  }

  /**
   * Runs the tasks and throws an exception to signal that the task failed.
   */
  def runTask(): Unit
}

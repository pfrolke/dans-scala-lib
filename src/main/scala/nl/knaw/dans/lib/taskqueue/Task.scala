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
 * A task that can succeed or fail. This must be implemented by the application. The task probably has a type
 * that represents the unit of work that it works on. For example, if your inbox processes orders, you will probably
 * encapsulate all data about an order in an `Order` class. The target type of your task would then be `Order`.
 *
 * @tparam T the type of the target of this task
 */
trait Task[T] {

  /**
   * Runs the task.
   *
   * @return success or failure
   */
  def run(): Try[Unit]

  /**
   * Returns the object that is the target of the task
   *
   * @return the file
   */
  def getTarget: T
}

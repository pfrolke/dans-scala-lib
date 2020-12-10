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

import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.Try

/**
 *
 * @param shouldFail if this is set to true the task will throw an exception in the run method
 */
case class TriggerTask(shouldFail: Boolean = false) extends Task[Any] with DebugEnhancedLogging {
  var triggered = false

  override def run(): Try[Unit] = Try {
    trace(())
    if (shouldFail) throw new Exception("Task failed")
    triggered = true
  }

  override def getTarget: Any = {}
}

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
package nl.knaw.dans.lib

/**
 * Classes that provide facilities to generate tasks from files in an inbox and execute those tasks. The extension points
 * for the user of the library are:
 *
 *   - [[Task]]: extend this to implement the application specific work to be done; when using the library from Java use [[AbstractJavaTask]] instead, as
 *     it lets you throw an exception to signal failure of the task.
 *   - [[AbstractInbox]]: extend this implementing [[AbstractInbox#createTask]] to generate the correct type of task for the appropriate files.
 *   - [[TaskSorter]]: optional object that lets you specify the order of processing for tasks that are enqueued en masse; when using this
 *     library from Java use [[AbstractJavaTaskSorter]] instead, as it lets you work with a `java.util.List`.
 *
 * Typical use would roughly look like this:
 *
 * @example
 * {{{
 *  case class Order(id: Int /* , ... more fields */)
 *
 *  class OrderProcessingTask(Order: Order) extends Task[Order] {
 *    def run(): Try[Unit] = {
 *      // Process the order
 *    }
 *
 *    def getTarget: Order = order
 *  }
 *
 *  class OrderInbox extends AbstractInbox[Order] {
 *    def createTask(f: File): Option[Task[T]] = {
 *      // Determine if f is a file or directory that represents an order
 *    }
 *  }
 *
 *   // At service start-up
 *   val myOrderInboxWatcher = new InboxWatcher(new OrderInbox(File("/path/to/inbox")))
 *   myOrderInboxWatcher.start()
 *
 *   // At service shut-down
 *   myOrderInboxWatcher.stop()
 * }}}
 */
package object taskqueue {
}

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

import java.util

import scala.collection.mutable.ListBuffer

/**
 * A Java-compatible TaskSorter. The sort implementation task a `java.util.List` which, unlike the Scala `List`,
 * may be modified by the sort algorithm.
 *
 * @tparam T the type of target for the tasks
 */
abstract class AbstractJavaTaskSorter[T] extends TaskSorter[T] {
  final override def sort(tasks: List[Task[T]]): List[Task[T]] = {
    import scala.collection.JavaConverters._
    val mutableTasks = ListBuffer[Task[T]]()
    mutableTasks.appendAll(tasks)
    sort(mutableTasks.asJava).asScala.toList
  }

  /**
   * Sorts a list of tasks returning a sorted version of the list.
   *
   * @param tasks the tasks to be sorted
   * @return the sorted list
   */
  def sort(tasks: util.List[Task[T]]): util.List[Task[T]]
}

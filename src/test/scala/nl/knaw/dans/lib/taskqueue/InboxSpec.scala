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

import better.files.File
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{ Eventually, IntegrationPatience }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable.ListBuffer
import scala.util.Success

class InboxSpec extends AnyFlatSpec with Matchers with BeforeAndAfterEach with Eventually with IntegrationPatience with MockFactory {

  val testDir: File = File.currentWorkingDirectory / "target" / "test" / getClass.getSimpleName
  val inboxDir: File = testDir / "inbox"

  // Inbox implementation for testing purposes which creates tasks for .txt files and failing tasks for .jpg files
  case class TestInbox() extends AbstractInbox[File](inboxDir) {
    val createdTasks: ListBuffer[Task[File]] = ListBuffer[Task[File]]() // keep track of tasks created

    override def createTask(f: File): Option[Task[File]] = f.extension match {
      case Some(".txt") => {
        // mocking a Task instance to test sorting tasks more easily
        val fileTask = stub[Task[File]]
        (fileTask.getTarget _).when().returns(f)
        (fileTask.run _).when().returns(Success(()))

        createdTasks += fileTask
        Some(fileTask)
      }

      case Some(".jpg") => {
        val fileTask = stub[Task[File]]
        (fileTask.getTarget _).when().returns(f)
        (fileTask.run _).when().throws(new Exception("Task failed"))

        createdTasks += fileTask
        Some(fileTask)
      }

      case _ => None
    }
  }

  // Test task sorter that sorts the tasks on target file name alphabetically
  val testTaskSorter = new TaskSorter[File] {
    override def sort(tasks: List[Task[File]]): List[Task[File]] = {
      tasks.sortBy(_.getTarget.name)
    }
  }

  override def beforeEach(): Unit = {
    inboxDir.createDirectories()
    inboxDir.clear()
  }

  "start" should "process files already in inbox" in {
    val file1: File = (inboxDir / "file1.txt").createFile()
    val file2: File = (inboxDir / "file2.txt").createFile()

    val testInbox = TestInbox()
    val inboxWatcher = new InboxWatcher[File](testInbox)

    inboxWatcher.start()

    eventually {
      testInbox.createdTasks.size shouldBe 2

      // verify the tasks target the correct files
      testInbox.createdTasks.exists(_.getTarget == file1) shouldBe true
      testInbox.createdTasks.exists(_.getTarget == file2) shouldBe true

      // verify all tasks' run method has been called
      (testInbox.createdTasks(0).run _).verify()
      (testInbox.createdTasks(1).run _).verify()
    }

    inboxWatcher.stop()
  }

  it should "process incoming files" in {
    val testInbox = TestInbox()
    val inboxWatcher = new InboxWatcher[File](testInbox)

    inboxWatcher.start()

    // create files after starting watcher
    val file1: File = (inboxDir / "file1.txt").createFile()
    val file2: File = (inboxDir / "file2.txt").createFile()

    eventually {
      testInbox.createdTasks.size shouldBe 2

      // verify the tasks target the correct files
      testInbox.createdTasks.exists(_.getTarget == file1) shouldBe true
      testInbox.createdTasks.exists(_.getTarget == file2) shouldBe true

      // verify all tasks' run method has been called
      (testInbox.createdTasks(0).run _).verify()
      (testInbox.createdTasks(1).run _).verify()
    }

    inboxWatcher.stop()
  }

  it should "process only the actionable files" in {
    // TestInbox only creates tasks for .txt files
    val file1: File = (inboxDir / "file1.txt").createFile()
    val file2: File = (inboxDir / "file2.png").createFile()
    val file3: File = (inboxDir / "file3.txt").createFile()

    val testInbox = TestInbox()
    val inboxWatcher = new InboxWatcher[File](testInbox)

    inboxWatcher.start()

    eventually {
      testInbox.createdTasks.size shouldBe 2

      // verify the tasks target the correct files
      testInbox.createdTasks.exists(_.getTarget == file1) shouldBe true
      testInbox.createdTasks.exists(_.getTarget == file3) shouldBe true
      testInbox.createdTasks.exists(_.getTarget == file2) shouldBe false

      // verify all tasks' run method has been called
      (testInbox.createdTasks(0).run _).verify()
      (testInbox.createdTasks(1).run _).verify()
    }

    inboxWatcher.stop()
  }

  it should "use a defined TaskSorter to process tasks in the correct order" in {
    val file1: File = (inboxDir / "d-file1.txt").createFile()
    val file2: File = (inboxDir / "c-file2.txt").createFile()
    val file3: File = (inboxDir / "b-file3.txt").createFile()
    val file4: File = (inboxDir / "a-file4.txt").createFile()

    val testInbox = TestInbox()
    val inboxWatcher = new InboxWatcher[File](testInbox)

    inboxWatcher.start(Some(testTaskSorter))

    eventually {
      testInbox.createdTasks.size shouldBe 4

      // verify the tasks have been ran in the correct order
      val task1 = testInbox.createdTasks.find(_.getTarget == file4).get
      val task2 = testInbox.createdTasks.find(_.getTarget == file3).get
      val task3 = testInbox.createdTasks.find(_.getTarget == file2).get
      val task4 = testInbox.createdTasks.find(_.getTarget == file1).get

      inSequence {
        (task1.run _).verify()
        (task2.run _).verify()
        (task3.run _).verify()
        (task4.run _).verify()
      }
    }

    inboxWatcher.stop()
  }

  it should "use a defined TaskSorter to process a single task" in {
    val file1: File = (inboxDir / "file1.txt").createFile()

    val testInbox = TestInbox()
    val inboxWatcher = new InboxWatcher[File](testInbox)

    inboxWatcher.start(Some(testTaskSorter))

    eventually {
      testInbox.createdTasks.size shouldBe 1

      val task = testInbox.createdTasks.head

      (task.run _).verify()
    }

    inboxWatcher.stop()
  }

  it should "process other tasks if a task fails" in {
    // the TestInbox defined above creates a failing task for .jpg extensions
    val file1: File = (inboxDir / "file1.txt").createFile()
    val file2: File = (inboxDir / "file2.jpg").createFile()
    val file3: File = (inboxDir / "file3.txt").createFile()

    val testInbox = TestInbox()
    val inboxWatcher = new InboxWatcher[File](testInbox)

    inboxWatcher.start()

    eventually {
      testInbox.createdTasks.size shouldBe 3

      // verify the tasks target the correct files
      testInbox.createdTasks.exists(_.getTarget == file1) shouldBe true
      testInbox.createdTasks.exists(_.getTarget == file2) shouldBe true
      testInbox.createdTasks.exists(_.getTarget == file3) shouldBe true

      // verify all tasks' run method has been called
      (testInbox.createdTasks(0).run _).verify()
      (testInbox.createdTasks(1).run _).verify()
      (testInbox.createdTasks(2).run _).verify()
    }

    inboxWatcher.stop()
  }
}

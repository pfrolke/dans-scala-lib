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

import org.scalamock.scalatest.MockFactory
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.{ Eventually, IntegrationPatience }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Success

class ActiveTaskQueueSpec extends AnyFlatSpec with Matchers with OneInstancePerTest with Eventually with IntegrationPatience with MockFactory {

  "start" should "process previously queued tasks" in {
    val taskQueue = new ActiveTaskQueue[Any]()
    val triggeredTasks = List(
      TriggerTask(),
      TriggerTask(),
      TriggerTask(),
    )

    triggeredTasks.foreach(taskQueue.add)
    taskQueue.start()

    eventually {
      triggeredTasks(0).triggered shouldBe true
      triggeredTasks(1).triggered shouldBe true
      triggeredTasks(2).triggered shouldBe true
    }

    taskQueue.stop()
  }

  it should "process other tasks if one task fails" in {
    val taskQueue = new ActiveTaskQueue[Any]()
    val triggeredTasks = List(
      TriggerTask(),
      TriggerTask(shouldFail = true),
      TriggerTask(),
    )

    triggeredTasks.foreach(taskQueue.add)
    taskQueue.start()

    eventually {
      triggeredTasks(0).triggered shouldBe true
      triggeredTasks(2).triggered shouldBe true
    }

    taskQueue.stop()
  }

  it should "process incoming tasks" in {
    val taskQueue = new ActiveTaskQueue[Any]()
    val triggeredTasks = List(
      TriggerTask(),
      TriggerTask(),
      TriggerTask(),
    )

    // starting the taskQueue before adding tasks
    taskQueue.start()

    triggeredTasks.foreach(taskQueue.add)

    eventually {
      triggeredTasks(0).triggered shouldBe true
      triggeredTasks(1).triggered shouldBe true
      triggeredTasks(2).triggered shouldBe true
    }

    taskQueue.stop()
  }

  it should "not fail on an empty task queue" in {
    val taskQueue = new ActiveTaskQueue[Any]()
    taskQueue.start()
    taskQueue.stop()
  }

  "stop" should "cancel all pending tasks" in {
    val taskQueue = new ActiveTaskQueue[Any]()

    // this mocked task will stop the ActiveTaskQueue when it gets processed
    // therefore all tasks following it will get skipped
    val stopTask = mock[Task[Any]]
    (stopTask.run _)
      .expects()
      .onCall(() => {
        taskQueue.stop()
        Success(())
      })

    val triggeredTasks = List(
      TriggerTask(),
      stopTask,
      TriggerTask(),
      TriggerTask(),
    )

    triggeredTasks.foreach(taskQueue.add)
    taskQueue.start()

    eventually {
      triggeredTasks(0).asInstanceOf[TriggerTask].triggered shouldBe true

      // these tasks came after the stopTask and should be skipped
      triggeredTasks(2).asInstanceOf[TriggerTask].triggered shouldBe false
      triggeredTasks(3).asInstanceOf[TriggerTask].triggered shouldBe false
    }
  }
}

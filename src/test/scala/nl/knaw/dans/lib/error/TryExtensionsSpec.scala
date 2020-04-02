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
package nl.knaw.dans.lib.error

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger }

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ FlatSpec, Inside }

import scala.util.{ Failure, Success, Try }

class TryExtensionsSpec extends AnyFlatSpec with Matchers with Inside {

  "doIfSuccess" should "perform a side effect if the Try is a Success" in {
    val value = 42
    val t: Try[Int] = Success(value)
    val sideEffectingInteger = new AtomicInteger()

    sideEffectingInteger.get() should not be value

    t.doIfSuccess(sideEffectingInteger.set)

    sideEffectingInteger.get() shouldBe value
  }

  it should "not perform a side effect if the Try is a Failure" in {
    val t: Try[Int] = Failure(new IllegalArgumentException("foobar"))
    val sideEffectingBoolean = new AtomicBoolean(false)

    t.doIfSuccess(_ => sideEffectingBoolean.set(true))

    sideEffectingBoolean.get() shouldBe false
  }

  "doIfFailure" should "perform a side effect only if the Throwable in the Try is defined in the PartialFunction" in {
    val t: Try[Int] = Failure(new IllegalArgumentException("foobar"))
    val sideEffectingBoolean = new AtomicBoolean(false)

    t.doIfFailure {
      case _: IllegalArgumentException => sideEffectingBoolean.set(true)
    }

    sideEffectingBoolean.get() shouldBe true
  }

  it should "not perform the side effect if the Throwable in the Try is not defined in the PartialFunction" in {
    val t: Try[Int] = Failure(new NoSuchElementException("foobar"))
    val sideEffectingBoolean = new AtomicBoolean(false)

    t.doIfFailure {
      case _: IllegalArgumentException => sideEffectingBoolean.set(true)
    }

    sideEffectingBoolean.get() shouldBe false
  }

  it should "not perform the side effect if the Try is actually a Success" in {
    val t: Try[Int] = Success(42)
    val sideEffectingBoolean = new AtomicBoolean(false)

    t.doIfFailure {
      case _: IllegalArgumentException => sideEffectingBoolean.set(true)
    }

    sideEffectingBoolean.get() shouldBe false
  }

  private case class OnErrorHelperException(defaultValue: Int) extends Exception("onError helper exception")

  "getOrRecover" should "return the actual value when the Try is actually a Success" in {
    val t: Try[Int] = Success(42)

    val result = t.getOrRecover {
      case OnErrorHelperException(default) => default
      case _ => -99
    }

    result shouldBe 42
  }

  it should "be able to distinguish between various exception types using pattern matching on the lambda" in {
    val t: Try[Int] = Failure(OnErrorHelperException(-42))

    val result = t.getOrRecover {
      case OnErrorHelperException(default) => default
      case _ => -99
    }

    result shouldBe -42
  }

  it should "return the correct value when the Try is actually a Failure" in {
    val t: Try[Int] = Failure(new NoSuchElementException())

    val result = t.getOrRecover {
      case OnErrorHelperException(default) => default
      case _ => -99
    }

    result shouldBe -99
  }

  // this function is used in the `combine` tests below.
  // note that this will throw an exception if `j == 0`
  private def divide(i: Int)(j: Int): Int = i / j

  "combine" should "apply the function to the value if both are Success" in {
    val f = Try { divide _ }
    val x = Try(12)
    val y = Try(2)

    f.combine(x).combine(y) should matchPattern { case Success(6) => }
  }

  it should "return a failure if applying the function causes a failure" in {
    val f = Try { divide _ }
    val x = Try(12)
    val y = Try(0)

    inside(f.combine(x).combine(y)) {
      case Failure(e: ArithmeticException) => e should have message "/ by zero"
    }
  }

  it should "return a failure if the function is a failure" in {
    val f: Try[(Int) => (Int) => Int] = Failure(new Exception("foo"))
    val x = Try(12)
    val y = Try(0)

    inside(f.combine(x).combine(y)) {
      case Failure(e) => e should have message "foo"
    }
  }

  it should "return a failure if the value is a failure" in {
    val f = Try { divide _ }
    val x = Failure(new Exception("foo"))
    val y = Try(0)

    inside(f.combine(x).combine(y)) {
      case Failure(e) => e should have message "foo"
    }
  }

  it should "return a failure with a CompositeException if both the function and the value are a failure" in {
    val f: Try[(Int) => (Int) => Int] = Failure(new Exception("foo"))
    val x = Failure(new Exception("bar"))
    val y = Failure(new Exception("baz"))

    inside(f.combine(x).combine(y)) {
      case Failure(CompositeException(e1 :: e2 :: e3 :: Nil)) =>
        e1 should have message "foo"
        e2 should have message "bar"
        e3 should have message "baz"
    }
  }

  "unsafeGetOrThrow" should "return the value inside a Success" in {
    Success(5).unsafeGetOrThrow shouldBe 5
  }

  it should "throw the exception in a Failure" in {
    val e = new Exception("err msg")

    the[Exception] thrownBy Failure(e).unsafeGetOrThrow shouldBe e
  }
}

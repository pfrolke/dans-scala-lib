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
package nl.knaw.dans.lib.logging.servlet

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MapExtensionsSpec extends AnyFlatSpec with Matchers {

  "makeString" should "format a Map[String, String] properly" in {
    val input = Map(
      "a" -> "foo",
      "b" -> "bar",
      "c" -> "baz",
      "d" -> "qux",
    )

    input.makeString shouldBe "[a -> foo, b -> bar, c -> baz, d -> qux]"
  }

  it should "take special care of values that are a Seq" in {
    val input = Map(
      "a" -> "foo".toSeq,
      "b" -> "bar".toSeq,
      "c" -> "baz".toSeq,
      "d" -> "qux".toSeq,
    )

    input.makeString shouldBe "[a -> [f, o, o], b -> [b, a, r], c -> [b, a, z], d -> [q, u, x]]"
  }

  it should "take special care of nested Seq structures" in {
    val input = Map(
      "a" -> List(1 to 3, 4 to 6).map(_.map(i => s"foo$i")),
      "b" -> List(1 to 3, 4 to 6).map(_.map(i => s"bar$i")),
    )

    input.makeString shouldBe "[a -> [[foo1, foo2, foo3], [foo4, foo5, foo6]], b -> [[bar1, bar2, bar3], [bar4, bar5, bar6]]]"
  }

  it should "properly format nested Map structures" in {
    val input = Map(
      "a" -> Map(
        "foo" -> "oof",
        "bar" -> "rab",
        "baz" -> "zab",
      ),
      "b" -> Map(
        "foo" -> "oof",
        "bar" -> "rab",
        "baz" -> "zab",
      ),
    )

    input.makeString shouldBe "[a -> [foo -> oof, bar -> rab, baz -> zab], b -> [foo -> oof, bar -> rab, baz -> zab]]"
  }

  it should "format a Map with various types of values" in {
    val input = Map(
      "a" -> Map(
        "foo" -> "oof",
        "bar" -> "rab",
      ),
      "b" -> Seq("foo", "bar"),
      "c" -> "foobar",
    )

    input.makeString shouldBe "[a -> [foo -> oof, bar -> rab], b -> [foo, bar], c -> foobar]"
  }
}

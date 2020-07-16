/* Copyright 2020 EPFL, Lausanne
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package silex

/** Represents a sequence of characters mutably traversed over.
  *
  * The sequence can be traversed only once, but arbitrary long lookaheads are supported.
  *
  * Sources conceptually contain two mutable pointers:
  * - The base pointer, which points past the fully traversed portion of the sequence.
  * - The lookahead pointer, which can point arbitrarily past the base pointer.
  *
  * The base pointer can be advanced all the way through the lookahead pointer using `consume()`.
  * The sequence of characters advanced over are returned.
  *
  * The lookahead pointer can be advanced by one character at the time using `ahead()`,
  * and can be reset to the base pointer by a call to `back()`.
  *
  * @group source
  */
trait Source[Character, Position] {

  /** Checks if the lookahead pointer is at the end of the sequence. */
  def atEnd: Boolean

  /** Advances the lookahead pointer by one character in the sequence.
    *
    * @return The character that was advanced over.
    */
  def ahead(): Character

  /** Consumes all characters that are currently looked ahead.
    *
    * @return The sequence of characters.
    */
  def consume(): Seq[Character]

  /** Resets the lookahead pointer. */
  def back(): Unit

  /** Resets the lookahead pointer and returns the content looked-ahead. */
  def backContent(): Seq[Character]

  /** Current position of the lookahead pointer in the source. */
  def currentPosition: Position
}

/** Builds sources.
  *
  * @group source
  */
object Source {

  /** Builds a source from a `file`. */
  def fromFile(file: String): Source[Char, StringPosition] = {
    fromFile(file, StringPositioner)
  }

  /** Builds a source from a `file` and a `positioner`. */
  def fromFile[Position](
      file: String,
      positioner: Positioner[Char, Position]): Source[Char, Position] = {

    val iterator = io.Source.fromFile(file)

    new IteratorSource[Char, Position](iterator, positioner)
  }

  /** Builds a source from a `file`. */
  def fromFile(file: java.io.File): Source[Char, StringPosition] = {
    fromFile(file, StringPositioner)
  }

  /** Builds a source from a `file` and a `positioner`. */
  def fromFile[Position](
      file: java.io.File,
      positioner: Positioner[Char, Position]): Source[Char, Position] = {

    val iterator = io.Source.fromFile(file)

    new IteratorSource[Char, Position](iterator, positioner)
  }

  /** Builds a source from a `string`. */
  def fromString(string: String): Source[Char, StringPosition] = {
    fromString(string, StringPositioner)
  }

  /** Builds a source from a `string` and a `positioner`. */
  def fromString[Position](
      string: String,
      positioner: Positioner[Char, Position]): Source[Char, Position] = {

    val iterator = string.iterator

    new IteratorSource[Char, Position](iterator, positioner)
  }

  /** Builds a source from an `iterator` and a `positioner`. */
  def fromIterator[Character, Position](
      iterator: Iterator[Character],
      positioner: Positioner[Character, Position]): Source[Character, Position] = {

    new IteratorSource[Character, Position](iterator, positioner)
  }
}

/** Source over an iterator.
  *
  * @group source
  */
class IteratorSource[Character, Position](
      iterator: Iterator[Character],
      positioner: Positioner[Character, Position])
    extends Source[Character, Position] {

  private var buffer: Vector[Character] = Vector()
  private var index: Int = 0
  private var basePos: Position = positioner.start
  private var aheadPos: Position = positioner.start

  /** Checks if the lookahead pointer is at the end of the sequence. */
  def atEnd: Boolean = !iterator.hasNext && index >= buffer.size

  /** Advances the lookahead pointer by one character in the sequence.
    *
    * @return The character that was advanced over.
    */
  def ahead(): Character = {
    if (index >= buffer.size) {
      val res = iterator.next()
      buffer :+= res
      index += 1
      aheadPos = positioner.increment(aheadPos, res)
      res
    }
    else {
      val res = buffer(index)
      index += 1
      aheadPos = positioner.increment(aheadPos, res)
      res
    }
  }

  /** Consumes all characters that are currently looked ahead.
    *
    * @return The sequence of characters.
    */
  def consume(): Seq[Character] = {
    val (res, newBuffer) = buffer.splitAt(index)
    buffer = newBuffer
    basePos = aheadPos
    index = 0
    res
  }

  /** Resets the lookahead pointer. */
  def back(): Unit = {
    aheadPos = basePos
    index = 0
  }

  /** Resets the lookahead pointer. */
  def backContent(): Seq[Character] = {
    val res = buffer.take(index)
    aheadPos = basePos
    index = 0
    res
  }

  /** Current position of the lookahead pointer in the source. */
  def currentPosition: Position = aheadPos
}

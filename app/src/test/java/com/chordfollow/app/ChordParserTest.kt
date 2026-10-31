package com.chordfollow.app

import org.junit.Assert.*
import org.junit.Test

class ChordParserTest {
 @Test fun parsesMajorMinorAndSeventh() {
  assertEquals(setOf(0,4,7), Chords.parse("C")!!.full)
  assertEquals(setOf(2,5,9), Chords.parse("Dm")!!.full)
  assertEquals(setOf(7,11,2,5), Chords.parse("G7")!!.full)
 }
 @Test fun extractsChordSymbols() {
  assertEquals(7, Chords.extract("C G Am F | Dm G7 C").size)
 }
 @Test fun rejectsLyrics() { assertNull(Chords.parse("hello")) }
}

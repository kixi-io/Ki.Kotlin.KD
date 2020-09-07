package io.kixi.kd.schema

import io.kixi.TypeDef
import io.kixi.kd.KD
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TagDefTest {

    @Test
    fun basic() {
        val tagDef = TagDef(
            TagDef.EMPTY_ANNOS,
            StringMatcher.EMPTY,
            StringMatcher.ID("Foo"),
            listOf<ValueDef>(
                ValueDef(TypeDef.String),
                ValueDef(TypeDef.Int, 5),
                ValueDef(TypeDef.Bool, true)
            )
            // varValueDef:ValueDef? = null
        )

        var tag = KD.read("""
            Foo "HelloKDS"
        """.trimIndent())
        tagDef.apply(tag)
        assertEquals("Foo \"HelloKDS\" 5 true", tag.toString())

        tag = KD.read("""
            Foo "HelloKDS" 12
        """.trimIndent())
        tagDef.apply(tag)
        assertEquals("Foo \"HelloKDS\" 12 true", tag.toString())

        tag = KD.read("""
            Foo "HelloKDS" 12 false
        """.trimIndent())
        tagDef.apply(tag)
        assertEquals("Foo \"HelloKDS\" 12 false", tag.toString())

        // Fails because the second value is a Long rather then an Int
        assertThrows(KDSException::class.java) {
            tag = KD.read("""
                Foo "HelloKDS" 12L false
            """.trimIndent())
            tagDef.apply(tag)
        }
    }

    @Test
    fun varValues() {
        val tagDef = TagDef(
                TagDef.EMPTY_ANNOS,
                StringMatcher.EMPTY,
                StringMatcher.ID("Foo"),
                listOf<ValueDef>(
                        ValueDef(TypeDef.String)
                ),
                ValueDef(TypeDef.Int)
        )

        val tag = KD.read("""
            Foo "HelloKDS" 1 2 3
        """.trimIndent())
        tagDef.apply(tag)
        assertEquals("Foo \"HelloKDS\" 1 2 3", tag.toString())
    }

    @Test
    fun stringMatcher() {
        val tagDef = TagDef(
                TagDef.EMPTY_ANNOS,
                StringMatcher.EMPTY,
                StringMatcher.ID("gift"),
                listOf<ValueDef>(
                        StringDef(TypeDef.String, StringListMatcher("flowers", "ring", "hat"))
                )
        )

        var tag = KD.read("""
                gift "flowers"
            """.trimIndent())
        tagDef.apply(tag)

        tag = KD.read("""
                gift "hat"
            """.trimIndent())
        tagDef.apply(tag)

        assertThrows<KDSException>("gift: Value poodle doesn't match String[flowers, ring, hat]") {
            tag = KD.read("""
                    gift "poodle"
                """.trimIndent())
            tagDef.apply(tag)
        }
    }
}
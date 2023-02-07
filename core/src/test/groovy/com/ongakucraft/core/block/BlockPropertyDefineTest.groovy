package com.ongakucraft.core.block

import com.ongakucraft.core.block.define.BlockPropertyDefine
import spock.lang.Specification

class BlockPropertyDefineTest extends Specification {
    def "validate constructor input"() {
        when:
        new BlockPropertyDefine(id, key, values)

        then:
        thrown(expectedException)

        where:
        id   | key   | values     | expectedException
        null | "key" | ["a", "b"] | NullPointerException
        "id" | null  | ["a", "b"] | NullPointerException
        "id" | "key" | null       | NullPointerException
    }

    def "validate contains input"() {
        when:
        def actual = new BlockPropertyDefine("id", "key", ["a", "b"]).contains(value)

        then:
        thrown(expectedException)

        where:
        value | expectedException
        null  | NullPointerException
    }

    def "check contains logic"() {
        when:
        def actual = new BlockPropertyDefine("id", "key", values).contains(value)

        then:
        expecteds == actual

        where:
        values     | value | expecteds
        ["a", "b"] | "a"   | true
        ["a", "b"] | "b"   | true
        ["a", "b"] | "c"   | false
    }
}

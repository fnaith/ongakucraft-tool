package com.ongakucraft.core.block

import com.ongakucraft.core.block.define.BlockPropertyDefine
import spock.lang.Specification

class BlockPropertyDefineTest extends Specification {
    def "should throw exception when null is passed to constructor"() {
        when:
        BlockPropertyDefine.of(id, key, values)

        then:
        thrown(expectedException)

        where:
        id   | key   | values     | expectedException
        null | "key" | ["a", "b"] | NullPointerException
        "id" | null  | ["a", "b"] | NullPointerException
        "id" | "key" | null       | NullPointerException
    }

    def "should throw exception when null is passed to contains"() {
        when:
        BlockPropertyDefine.of("id", "key", ["a", "b"]).contains(value)

        then:
        thrown(expectedException)

        where:
        value | expectedException
        null  | NullPointerException
    }

    def "should return the value exist or not when it is passed to contains()"() {
        when:
        def actual = BlockPropertyDefine.of("id", "key", values).contains(value)

        then:
        expected == actual

        where:
        values     | value | expected
        ["a", "b"] | "a"   | true
        ["a", "b"] | "b"   | true
        ["a", "b"] | "c"   | false
    }
}

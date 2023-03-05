package com.ongakucraft.core.block

import com.ongakucraft.core.block.define.BlockDefine
import spock.lang.Specification

class BlockTest extends Specification {
    def "should throw exception when null is passed to constructor"() {
        when:
        Block.of(blockDefine)

        then:
        thrown(expectedException)

        where:
        blockDefine | expectedException
        null        | NullPointerException
    }

    def "should return default facing when block isn't rotated"() {
        setup:
        def blockId = BlockId.of("note_block")
        def blockDefine = BlockDefine.of(blockId, [], false)

        when:
        def block = Block.of(blockDefine)

        then:
        Block.DEFAULT_FACING == block.facing
    }

    def "should throw exception when null is passed to get()"() {
        when:
        def blockId = BlockId.of("note_block")
        def blockDefine = BlockDefine.of(blockId, [], false)
        def block = Block.of(blockDefine)
        block.get(property)

        then:
        thrown(expectedException)

        where:
        property | expectedException
        null     | NullPointerException
    }

    def "should throw exception when null is passed to put()"() {
        when:
        def blockId = BlockId.of("note_block")
        def blockDefine = BlockDefine.of(blockId, [], false)
        def block = Block.of(blockDefine)
        block.put(property, value)

        then:
        thrown(expectedException)

        where:
        property | value   | expectedException
        null     | false   | NullPointerException
        null     | 0       | NullPointerException
        null     | "south" | NullPointerException
        "facing" | null    | NullPointerException
    }

    def "should throw exception when null is passed to remove()"() {
        when:
        def blockId = BlockId.of("note_block")
        def blockDefine = BlockDefine.of(blockId, [], false)
        def block = Block.of(blockDefine)
        block.remove(property)

        then:
        thrown(expectedException)

        where:
        property | expectedException
        null     | NullPointerException
    }
}

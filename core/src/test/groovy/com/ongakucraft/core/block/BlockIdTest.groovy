package com.ongakucraft.core.block

import spock.lang.Specification

class BlockIdTest extends Specification {
    def "validate constructor input"() {
        when:
        BlockId.of(namespace, path)

        then:
        thrown(expectedException)

        where:
        namespace   | path         | expectedException
        null        | "note_block" | NullPointerException
        "minecraft" | null         | NullPointerException
    }

    def "check id format"() {
        setup:
        def namespace = "minecraft"
        def path = "note_block"

        when:
        def blockId = BlockId.of(namespace, path)

        then:
        "minecraft:note_block" == blockId.id
    }
}

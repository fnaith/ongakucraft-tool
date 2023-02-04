package com.ongakucraft.core.block

import spock.lang.Specification

class BlockIdTest extends Specification {
    def "validate constructor input"() {
        when:
        new BlockId(namespace, path)

        then:
        thrown(expectedException)

        where:
        namespace   | path        | expectedException
        null        | "noteblock" | NullPointerException
        "minecraft" | null        | NullPointerException
    }

    def "check id format"() {
        setup:
        def namespace = "minecraft"
        def path = "noteblock"

        when:
        def blockId = new BlockId(namespace, path)

        then:
        "minecraft:noteblock" == blockId.id
    }
}

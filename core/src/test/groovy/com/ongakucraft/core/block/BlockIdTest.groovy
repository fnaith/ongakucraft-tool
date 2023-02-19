package com.ongakucraft.core.block

import spock.lang.Specification

class BlockIdTest extends Specification {
    def "should throw exception when null is passed to constructor"() {
        when:
        BlockId.of(namespace, path)

        then:
        thrown(expectedException)

        where:
        namespace   | path         | expectedException
        null        | "note_block" | NullPointerException
        "minecraft" | null         | NullPointerException
    }

    def "should get default namespace when no one is passed to constructor()"() {
        setup:
        def path = "note_block"

        when:
        def blockId = BlockId.of(path)

        then:
        BlockId.DEFAULT_NAMESPACE == blockId.namespace
    }

    def "should throw exception when null is passed to parse()"() {
        when:
        BlockId.parse(id)

        then:
        thrown(expectedException)

        where:
        id   | expectedException
        null | NullPointerException
    }

    def "should return empty result when invalid id format is passed to parse()"() {
        when:
        def blockId = BlockId.parse(id)

        then:
        isEmpty == blockId.isEmpty()

        where:
        id   | isEmpty
        ""   | true
        "::" | true
        ":"  | false
    }

    def "should return expected namespace and path when valid id format is passed to parse()"() {
        when:
        def blockId = BlockId.parse(id).get()

        then:
        namespace == blockId.namespace
        path == blockId.path

        where:
        namespace   | path         | id
        ""          | ""           | ":"
        "minecraft" | ""           | "minecraft:"
        ""          | "note_block" | ":note_block"
        "minecraft" | "note_block" | "minecraft:note_block"
    }

    def "should return expected format of id"() {
        when:
        def blockId = BlockId.of(namespace, path)

        then:
        id == blockId.id

        where:
        namespace   | path         | id
        ""          | ""           | ":"
        "minecraft" | ""           | "minecraft:"
        ""          | "note_block" | ":note_block"
        "minecraft" | "note_block" | "minecraft:note_block"
    }
}

package com.keyboard.prediction

class Trie {
    private val root = TrieNode()

    fun insert(word: String, frequency: Int = 1) {
        if (word.isBlank()) return
        var node = root
        word.lowercase().forEach { char ->
            node = node.children.getOrPut(char) { TrieNode() }
        }
        node.isWord = true
        node.frequency += frequency
    }

    fun findByPrefix(prefix: String, limit: Int = 8): List<Pair<String, Int>> {
        if (prefix.isBlank()) return emptyList()
        var node = root
        for (char in prefix.lowercase()) {
            node = node.children[char] ?: return emptyList()
        }
        val words = mutableListOf<Pair<String, Int>>()
        collect(node, prefix.lowercase(), words)
        return words.sortedByDescending { it.second }.take(limit)
    }

    private fun collect(node: TrieNode, current: String, out: MutableList<Pair<String, Int>>) {
        if (node.isWord) {
            out += current to node.frequency
        }
        node.children.forEach { (char, next) ->
            collect(next, current + char, out)
        }
    }

    private data class TrieNode(
        val children: MutableMap<Char, TrieNode> = mutableMapOf(),
        var isWord: Boolean = false,
        var frequency: Int = 0
    )
}

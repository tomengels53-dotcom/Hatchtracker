package com.example.hatchtracker.common.util

import com.example.hatchtracker.model.Bird

/**
 * Service for tracing bird lineage and detecting potential inbreeding.
 */
object AncestryService {

    data class AncestryNode(
        val birdId: Long,
        val motherId: Long?,
        val fatherId: Long?,
        val motherNode: AncestryNode? = null,
        val fatherNode: AncestryNode? = null
    )

    /**
     * Checks if two birds share any immediate parents or grandparents.
     * Returns true if a conflict is found.
     */
    fun hasConflict(birdA: Bird, birdB: Bird, birdMap: Map<Long, Bird>): Boolean {
        // 1. Immediate Parent Check (Siblings or Half-Siblings)
        if (areSiblings(birdA, birdB)) return true
        
        // 2. Parent-Child Check
        if (isParentChild(birdA, birdB)) return true
        
        // 3. Grandparent Check (Cousins)
        // This requires traversing up one more level if data is available
        val treeA = buildTree(birdA, birdMap, depth = 2)
        val treeB = buildTree(birdB, birdMap, depth = 2)
        
        return hasSharedAncestors(treeA, treeB)
    }

    private fun areSiblings(a: Bird, b: Bird): Boolean {
        val sameMother = a.motherId != null && a.motherId == b.motherId
        val sameFather = a.fatherId != null && a.fatherId == b.fatherId
        return sameMother || sameFather
    }

    private fun isParentChild(a: Bird, b: Bird): Boolean {
        return a.localId == b.motherId || a.localId == b.fatherId ||
               b.localId == a.motherId || b.localId == a.fatherId
    }

    private fun buildTree(bird: Bird, birdMap: Map<Long, Bird>, depth: Int): AncestryNode {
        if (depth == 0) return AncestryNode(bird.localId, bird.motherId, bird.fatherId)

        val mother = bird.motherId?.let { birdMap[it] }
        val father = bird.fatherId?.let { birdMap[it] }

        return AncestryNode(
            birdId = bird.localId,
            motherId = bird.motherId,
            fatherId = bird.fatherId,
            motherNode = mother?.let { buildTree(it, birdMap, depth - 1) },
            fatherNode = father?.let { buildTree(it, birdMap, depth - 1) }
        )
    }

    private fun hasSharedAncestors(nodeA: AncestryNode, nodeB: AncestryNode): Boolean {
        val ancestorsA = collectAncestorIds(nodeA)
        val ancestorsB = collectAncestorIds(nodeB)
        
        // Intersect sets to find common IDs (excluding nulls)
        val common = ancestorsA.intersect(ancestorsB)
        return common.isNotEmpty()
    }

    private fun collectAncestorIds(node: AncestryNode): Set<Long> {
        val ids = mutableSetOf<Long>()
        node.motherId?.let { ids.add(it) }
        node.fatherId?.let { ids.add(it) }
        
        node.motherNode?.let { ids.addAll(collectAncestorIds(it)) }
        node.fatherNode?.let { ids.addAll(collectAncestorIds(it)) }
        
        return ids
    }
}


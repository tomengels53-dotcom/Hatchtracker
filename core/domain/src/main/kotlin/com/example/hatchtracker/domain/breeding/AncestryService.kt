package com.example.hatchtracker.domain.breeding

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
     * Calculates the Coefficient of Inbreeding (COI) for potential offspring.
     * Uses Wright's Path Coefficient method.
     */
    fun calculateCOI(sire: Bird, dam: Bird, birdMap: Map<Long, Bird>, maxDepth: Int = 5): Float {
        val ancestorsSire = getAncestorPaths(sire, birdMap, maxDepth)
        val ancestorsDam = getAncestorPaths(dam, birdMap, maxDepth)

        val commonAncestors = ancestorsSire.keys.intersect(ancestorsDam.keys)
        if (commonAncestors.isEmpty()) return 0.0f

        var coi = 0.0f
        commonAncestors.forEach { commonId ->
            val pathsSire = ancestorsSire[commonId] ?: return@forEach
            val pathsDam = ancestorsDam[commonId] ?: return@forEach

            pathsSire.forEach { pathSire ->
                pathsDam.forEach { pathDam ->
                    // F_x = sum [ (1/2)^(n1 + n2 + 1) * (1 + F_a) ]
                    // Simplified: (0.5)^(generation_steps_total + 1)
                    // n1 = steps from sire to common, n2 = steps from dam to common
                    val totalSteps = pathSire + pathDam
                    coi += Math.pow(0.5, (totalSteps + 1).toDouble()).toFloat()
                }
            }
        }

        return coi
    }

    private fun getAncestorPaths(
        bird: Bird, 
        birdMap: Map<Long, Bird>, 
        maxDepth: Int, 
        currentDepth: Int = 0
    ): Map<Long, List<Int>> {
        if (currentDepth >= maxDepth) return emptyMap()

        val results = mutableMapOf<Long, MutableList<Int>>()
        
        fun recordPath(id: Long, depth: Int) {
            results.getOrPut(id) { mutableListOf() }.add(depth)
        }

        bird.motherId?.let { id ->
            recordPath(id, currentDepth)
            birdMap[id]?.let { mother ->
                getAncestorPaths(mother, birdMap, maxDepth, currentDepth + 1).forEach { (ancId, depths) ->
                    depths.forEach { d -> recordPath(ancId, d) }
                }
            }
        }

        bird.fatherId?.let { id ->
            recordPath(id, currentDepth)
            birdMap[id]?.let { father ->
                getAncestorPaths(father, birdMap, maxDepth, currentDepth + 1).forEach { (ancId, depths) ->
                    depths.forEach { d -> recordPath(ancId, d) }
                }
            }
        }

        return results
    }

    /**
     * Checks if two birds share any immediate parents or grandparents.
     * Returns true if a conflict is found.
     */
    fun hasConflict(birdA: Bird, birdB: Bird, birdMap: Map<Long, Bird>): Boolean {
        // 1. Immediate Parent Check (Siblings or Half-Siblings)
        if (areSiblings(birdA, birdB)) return true
        
        // 2. Parent-Child Check
        if (isParentChild(birdA, birdB)) return true
        
        // 3. COI Threshold Check (Strategic)
        val coi = calculateCOI(birdA, birdB, birdMap)
        return coi > 0.25f // Block extremely high inbreeding (equivalent to parent-child or full sibling)
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


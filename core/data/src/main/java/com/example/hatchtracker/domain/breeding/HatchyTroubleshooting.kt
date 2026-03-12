package com.example.hatchtracker.domain.breeding

/**
 * Deterministic decision trees for troubleshooting common issues.
 */
object HatchyTroubleshooting {

    fun getIncubationTroubleshooting(issue: String): String {
        return when {
            issue.contains("late") -> """
                Slow hatch usually means the temp was a bit low throughout. 
                1. Check your thermometer accuracy.
                2. Check for drafts near the incubator. 
                3. Let 'em have an extra day or two; stay patient.
            """.trimIndent()
            issue.contains("early") -> "Early hatching usually means your temp was running a bit hot. Try lowerin' it by 0.5 degrees for the next batch."
            issue.contains("sticky") -> "Chicks stuck in the shell usually indicates humidity was too low during incubation or pips were exposed to dry air too long (opening the lid)."
            issue.contains("hatch rate") -> """
                Low hatch rate has many causes:
                - Fertility: Check eggs at day 7.
                - Temperature: Ensure no spikes or drops.
                - Turning: Was the rotator working?
                - Shipping: Shipped eggs often have damaged air cells.
            """.trimIndent()
            else -> "Trouble in the incubator? Tell me if they're hatchin' early, late, or if you're seein' low rates."
        }
    }

    fun getBrooderTroubleshooting(issue: String): String {
        return when {
            issue.contains("huddl") -> "If they're huddl'in tight under the lamp, they're cold. Drop the lamp an inch or two. If they're in the corners, it's too hot!"
            issue.contains("pasty") -> "Pasty butt is caused by stress or incorrect temps. Clean it gently with warm water, dry 'em off, and check your brooder temp."
            else -> "Brooder issues can be tricky. Look for signs of stress or temp extremes in their behavior."
        }
    }
}

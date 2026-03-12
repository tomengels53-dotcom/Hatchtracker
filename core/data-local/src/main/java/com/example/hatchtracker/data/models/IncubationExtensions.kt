package com.example.hatchtracker.data.models

fun Incubation.hatchRate(): Float {
    return if (eggsCount > 0) (hatchedCount.toFloat() / eggsCount) * 100f else 0f
}

fun Incubation.infertilityRate(): Float {
    return if (eggsCount > 0) (infertileCount.toFloat() / eggsCount) * 100f else 0f
}


package com.example.hatchtracker.data.models

enum class FinancialCategory(val value: String) {
    FEED("feed"),
    WATER("water"),
    HEALTHCARE("healthcare"),
    SHELTER("shelter"),
    PURCHASE_ADULT("purchase_adult"),
    PURCHASE_EGGS("purchase_eggs"),
    PURCHASE_CHICKS("purchase_chicks"),
    SALE_ADULT("sale_adult"),
    SALE_EGGS("sale_eggs"),
    SALE_CHICKS("sale_chicks"),
    OTHER("other");

    companion object {
        fun fromString(value: String): FinancialCategory {
            return entries.find { it.value == value.lowercase() } ?: OTHER
        }
    }
}


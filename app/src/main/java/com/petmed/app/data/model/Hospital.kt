package com.petmed.app.data.model

data class Hospital(
    val id: Int,
    val name: String,
    val phone: String,
    val address: String = "",
    val district: String = ""
)

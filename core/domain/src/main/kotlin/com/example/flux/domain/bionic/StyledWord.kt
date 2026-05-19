package com.example.flux.domain.bionic

data class StyledWord(
    val boldPart: String,
    val tail: String,
    val isSkipped: Boolean = false,
)

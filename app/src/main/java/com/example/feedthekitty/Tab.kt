package com.example.feedthekitty

data class Tab(
    val owner: String = "",
    val users: String = "",
    val paidUsers: String = "",
    val totalRequested: Int = 0,
    val balance: Int = 0,
    val open: Boolean = true
)
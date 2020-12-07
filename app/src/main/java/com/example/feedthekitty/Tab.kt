package com.example.feedthekitty

data class Tab(
    val eventName : String = "",
    val owner: String = "",
    val users: String = "",
    val paidUsers: String = "",
    val totalRequested: String  = "0.00",
    val balance: String = "0.00",
    val open: Boolean = true,
    val description: String = ""
)
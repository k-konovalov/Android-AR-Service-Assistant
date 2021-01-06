package com.konovalovk.serviceassistant

data class Port(
        val number: String = "0",
        val isEnabled: Boolean = false,
        val client: Client?
)

data class Client(
        val name: String = "Test",
        val ip: String = "",
        val numContract: Int = 0
)
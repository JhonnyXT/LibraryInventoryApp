package com.example.libraryinventoryapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello ${platform.name}! ¡Migrando a KMP exitosamente!"
    }
}

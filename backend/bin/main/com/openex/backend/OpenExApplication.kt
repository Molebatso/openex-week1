package com.openex.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OpenExApplication

fun main(args: Array<String>) {
    runApplication<OpenExApplication>(*args)
}

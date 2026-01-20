package io.github.cachij.vaultrefinery

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class VaultRefineryApplication

fun main(args: Array<String>) {
	runApplication<VaultRefineryApplication>(*args)
}

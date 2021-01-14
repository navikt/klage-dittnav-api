package no.nav.klage.domain.exception

class FullmaktNotFoundException(override val message: String = "Fullmakt not found"): RuntimeException()
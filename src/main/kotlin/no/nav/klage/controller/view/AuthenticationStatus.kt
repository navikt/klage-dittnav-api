package no.nav.klage.controller.view

data class AuthenticationStatus(
    val authenticated: Boolean,
    val tokenx: Boolean,
    val selvbetjening: Boolean,
)
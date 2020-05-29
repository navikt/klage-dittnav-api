package no.nav.klage.filter

import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono


@Component
class CorsWebFilter : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void?>? {
        exchange.response
            .headers
            .add("Access-Control-Allow-Origin", "*")

        return chain.filter(exchange)
    }
}





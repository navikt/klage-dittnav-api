package no.nav.klage.config

import com.natpryce.konfig.*

private val config = ConfigurationProperties.systemProperties() overriding
        EnvironmentVariables overriding
        ConfigurationProperties.fromResource("default.properties")

private fun String.configProperty(): String = config[Key(this, stringType)]

data class Configuration(
    val db: Db = Db()
) {
    data class Db(
        val dbUrl: String = "jdbc:postgresql://" + "DB_HOST".configProperty() + "/klage",
        val dbUsername: String = "DB_USERNAME".configProperty(),
        val dbPassword: String = "DB_PASSWORD".configProperty(),
        val dbMaximumPoolSize: Int = "db.maximumPoolSize".configProperty().toInt(),
        val dbMinimumIdle: Int = "db.minimumIdle".configProperty().toInt(),
        val dbConnectionTimeout: Long = "db.connectionTimeout".configProperty().toLong()
    )
}
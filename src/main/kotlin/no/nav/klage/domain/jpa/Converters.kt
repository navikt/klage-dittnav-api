package no.nav.klage.domain.jpa

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import no.nav.klage.domain.klage.CheckboxEnum

@Converter
class CheckboxEnumConverter : AttributeConverter<List<CheckboxEnum>, String?> {

    override fun convertToDatabaseColumn(entity: List<CheckboxEnum>?): String? =
        entity?.joinToString(",")

    override fun convertToEntityAttribute(commaSeparatedString: String?): List<CheckboxEnum>? =
        commaSeparatedString?.let { s ->
            s.split(",").map {
                CheckboxEnum.valueOf(it)
            }
        }
}
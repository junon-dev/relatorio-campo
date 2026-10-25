package br.app.relatoriocampo.domain

/** Registro de tempo de um dia. kind = "field" (campo) ou "credit" (crédito). */
data class Entry(
    val id: String,
    val date: String, // yyyy-MM-dd
    val min: Int,
    val kind: String,
    val note: String = "",
)

/** Estudo bíblico ("study") ou revisita ("rv"). */
data class Contact(
    val id: String,
    val name: String,
    val kind: String,
    val notes: String = "",
    val active: Boolean = true,
)

/** Dados lançados para um mês (yyyy-MM). Campos nulos usam o padrão das configurações. */
data class MonthData(
    val month: String,
    val cat: String? = null,
    val participated: Boolean? = null,
    val studies: Int? = null,
    val notes: String? = null,
    val mode: String? = null, // "daily" | "total"
    val totField: Int? = null, // minutos (modo total)
    val totCredit: Int? = null, // minutos (modo total)
    val auxGoal: Int? = null,
    val sent: Boolean = false,
)

data class Settings(
    val name: String = "",
    val group: String = "",
    val category: String = "pub", // pub | aux | reg
    val annualGoal: Int = 600,
    val creditLimit: Int = 55,
    val startMonth: String = "", // yyyy-MM
    val carry: Boolean = true,
    val lang: String = "pt", // pt | es
    val theme: String = "auto", // auto | light | dark
    val entryMode: String = "daily",
    val remEnabled: Boolean = true,
    val remEnd: Int = 5,
    val remDaily: Boolean = false,
    val remSendDay: Int = 1,
    val remHour: Int = 9,
    val remMinute: Int = 0,
    val timerStart: Long? = null,
    val askedNotif: Boolean = false,
    val widgetOpacity: Int = 100, // % de opacidade do fundo dos widgets
)

/** Resultado calculado de um mês. Tempos em minutos. */
data class MonthCalc(
    val month: String,
    val field: Int,
    val credit: Int,
    val creditUsed: Int,
    val carryIn: Int,
    val total: Int,
    val reported: Int,
    val carryOut: Int,
    val isPio: Boolean,
    val cat: String,
    val studies: Int,
    val participated: Boolean,
    val entries: List<Entry>,
    val mode: String,
    val sent: Boolean,
) {
    val hasData: Boolean get() = participated || this.field + credit > 0 || studies > 0
}

package io.heimdall.core

enum class HealthStatus {
    OK,
    WARNING,
    CRITICAL,
    UNAVAILABLE,
}

enum class MetricConfidence {
    MEASURED,
    UNAVAILABLE,
}

data class HealthMetric(
    val key: String,
    val label: String,
    val value: Double?,
    val displayValue: String,
    val unit: String,
    val status: HealthStatus,
    val confidence: MetricConfidence,
)

data class HealthRule(
    val key: String,
    val label: String,
    val warningAt: Double,
    val criticalAt: Double,
    val unit: String,
)

/** A pure function of its arguments, not a capture \u2014 kept identical to the real module rather
 * than made a no-op, since there is nothing here to disable. Always returns metrics as
 * [MetricConfidence.MEASURED] for whatever lists it's given, which are always empty in a release
 * build since nothing here records into them. */
object HealthRules {
    val fatalCrashes = HealthRule("fatal_crashes", "Fatal crashes", 1.0, 1.0, "count")
    val networkErrors = HealthRule("network_errors", "Network errors", 1.0, 3.0, "count")
    val slowOperations = HealthRule("slow_operations", "Slow operations", 1.0, 3.0, "count")

    fun current(
        network: List<NetworkRecord>,
        crashes: List<CrashRecord>,
        performance: List<PerformanceRecord>,
    ): List<HealthMetric> = listOf(
        measured(fatalCrashes, crashes.count { it.isFatal }.toDouble()),
        measured(networkErrors, network.count { it.isError }.toDouble()),
        measured(slowOperations, performance.count { it.durationMillis >= 200 }.toDouble()),
    )

    private fun measured(rule: HealthRule, value: Double): HealthMetric = HealthMetric(
        key = rule.key,
        label = rule.label,
        value = value,
        displayValue = value.toInt().toString(),
        unit = rule.unit,
        status = when {
            value >= rule.criticalAt -> HealthStatus.CRITICAL
            value >= rule.warningAt -> HealthStatus.WARNING
            else -> HealthStatus.OK
        },
        confidence = MetricConfidence.MEASURED,
    )
}

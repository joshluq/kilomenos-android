package es.joshluq.kmsafe.feature.widget.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import es.joshluq.kmsafe.core.ui.util.NumberFormatter
import es.joshluq.kmsafe.domain.model.WidgetSummary
import es.joshluq.kmsafe.domain.usecase.GetWidgetSummaryUseCase
import es.joshluq.kmsafe.feature.widget.R
import kotlin.math.absoluteValue

/**
 * Android Home Screen Widget implemented with Jetpack Glance following the 4-Layer Clean UI Pattern.
 */
class KmSafeGlanceWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun getWidgetSummaryUseCase(): GetWidgetSummaryUseCase
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            WidgetEntryPoint::class.java
        )
        val getWidgetSummaryUseCase = entryPoint.getWidgetSummaryUseCase()
        val result = getWidgetSummaryUseCase(GetWidgetSummaryUseCase.Input).getOrNull()

        provideContent {
            val launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://kmsafe.app/dashboard")).apply {
                setPackage(context.packageName)
                component = ComponentName(context.packageName, "es.joshluq.kmsafe.MainActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val quickAddIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://kmsafe.app/dashboard?action=quick_add")).apply {
                setPackage(context.packageName)
                component = ComponentName(context.packageName, "es.joshluq.kmsafe.MainActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(KmSafeGlanceTheme.Surface)
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity(launchIntent))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                when (result) {
                    is GetWidgetSummaryUseCase.Output.Success -> {
                        WidgetContent(
                            summary = result.summary,
                            onQuickAdd = quickAddIntent
                        )
                    }
                    else -> {
                        WidgetEmptyContent(
                            onAction = launchIntent
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun WidgetContent(
        summary: WidgetSummary,
        onQuickAdd: Intent
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Header: Vehicle Name & Sync Tag
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_car),
                    contentDescription = null,
                    modifier = GlanceModifier.size(14.dp)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = summary.vehicleName,
                    style = TextStyle(
                        color = KmSafeGlanceTheme.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                if (summary.isSyncPending) {
                    Box(
                        modifier = GlanceModifier
                            .background(KmSafeGlanceTheme.SurfaceHighlight)
                            .cornerRadius(4.dp)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PENDING",
                            style = TextStyle(
                                color = KmSafeGlanceTheme.BrandAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Layer 1 (The Pulse): Balance & Layer 3 (Quick Action Button)
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    val balanceSign = if (summary.balance >= 0) "+" else "-"
                    val balanceFormatted = NumberFormatter.formatDistance(summary.balance.absoluteValue)
                    val balanceColor = if (summary.isSafe) KmSafeGlanceTheme.Success else KmSafeGlanceTheme.Error

                    Text(
                        text = "$balanceSign$balanceFormatted km",
                        style = TextStyle(
                            color = balanceColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (summary.isSafe) "Margen a favor" else "Exceso penalizable",
                        style = TextStyle(
                            color = balanceColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }

                // Layer 3 (Zero-Friction Action): 1-Tap Quick Register
                Box(
                    modifier = GlanceModifier
                        .size(44.dp)
                        .background(KmSafeGlanceTheme.ActionBackground)
                        .cornerRadius(22.dp)
                        .clickable(actionStartActivity(onQuickAdd)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_add),
                        contentDescription = "Registrar odómetro",
                        modifier = GlanceModifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Layer 2 (Contextual Decision Radar): Daily allowance & Odometer
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dailyAllowance = NumberFormatter.formatRate(summary.dailyBudget)
                val currentOdometer = NumberFormatter.formatDistance(summary.currentOdometer)

                Text(
                    text = "Hoy: $dailyAllowance km",
                    style = TextStyle(
                        color = KmSafeGlanceTheme.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = "•",
                    style = TextStyle(
                        color = KmSafeGlanceTheme.TextMuted,
                        fontSize = 11.sp
                    )
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = "$currentOdometer km tot.",
                    style = TextStyle(
                        color = KmSafeGlanceTheme.TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }

    @Composable
    private fun WidgetEmptyContent(
        onAction: Intent
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "KiloMenos",
                style = TextStyle(
                    color = KmSafeGlanceTheme.BrandAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Sin vehículo activo configurado",
                style = TextStyle(
                    color = KmSafeGlanceTheme.TextSecondary,
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Box(
                modifier = GlanceModifier
                    .background(KmSafeGlanceTheme.SurfaceHighlight)
                    .cornerRadius(12.dp)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .clickable(actionStartActivity(onAction))
            ) {
                Text(
                    text = "Abrir KiloMenos",
                    style = TextStyle(
                        color = KmSafeGlanceTheme.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

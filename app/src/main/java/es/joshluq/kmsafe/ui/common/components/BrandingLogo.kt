package es.joshluq.kmsafe.ui.common.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.joshluq.canvaskit.foundations.theme.CanvasKitTheme
import es.joshluq.kmsafe.R

/**
 * Reusable branding component that combines the Animated "K" logo with the brand text.
 *
 * @param modifier Root modifier.
 * @param logoSize The size of the "K" logo.
 * @param textStyle Style for the "iloMenos" text.
 */
@Composable
fun BrandingLogo(
    modifier: Modifier = Modifier,
    logoSize: Dp = 32.dp,
    textColor: androidx.compose.ui.graphics.Color = CanvasKitTheme.colors.textPrimary,
    textStyle: TextStyle = CanvasKitTheme.typography.headingLarge,
    animationDuration: Int = 1000
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        AnimatedLogo(
            logoSize = logoSize,
            pathColor = textColor,
            animationDuration = animationDuration
        )
        
        Text(
            modifier = Modifier.offset(x = (-(logoSize * 0.25f))),
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = textColor)) {
                    append("ilo")
                }
                withStyle(style = SpanStyle(color = CanvasKitTheme.colors.brandAccent)) {
                    append(stringResource(R.string.common_brand_menos))
                }
            },
            style = textStyle,
            letterSpacing = 2.sp
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
fun BrandingLogoPreview() {
    CanvasKitTheme {
        Surface(color = CanvasKitTheme.colors.backgroundPrimary) {
            BrandingLogo(modifier = Modifier.padding(16.dp))
        }
    }
}

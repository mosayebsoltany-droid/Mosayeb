package ir.elat.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ElatBlue = Color(0xFF063B66)
val ElatBlue2 = Color(0xFF0B62A4)
val ElatGreen = Color(0xFF22A447)
val ElatRed = Color(0xFFE6222E)
val ElatBg = Color(0xFFF5F8FB)

private val Scheme = lightColorScheme(
    primary = ElatBlue2,
    secondary = ElatGreen,
    error = ElatRed,
    background = ElatBg,
    surface = Color.White
)

@Composable
fun ElatTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}

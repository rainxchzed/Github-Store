package zed.rainxch.githubstore.core.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import githubstore.composeapp.generated.resources.JetBrainsMono_Bold
import githubstore.composeapp.generated.resources.JetBrainsMono_Light
import githubstore.composeapp.generated.resources.JetBrainsMono_Medium
import githubstore.composeapp.generated.resources.JetBrainsMono_Regular
import githubstore.composeapp.generated.resources.JetBrainsMono_SemiBold
import githubstore.composeapp.generated.resources.Poppins_Black
import githubstore.composeapp.generated.resources.Poppins_Bold
import githubstore.composeapp.generated.resources.Poppins_Light
import githubstore.composeapp.generated.resources.Poppins_Medium
import githubstore.composeapp.generated.resources.Poppins_Regular
import githubstore.composeapp.generated.resources.Poppins_SemiBold
import githubstore.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.Font

val jetbrainsMonoFontFamily
    @Composable get() = FontFamily(
        Font(Res.font.JetBrainsMono_Light, FontWeight.Light),
        Font(Res.font.JetBrainsMono_Regular, FontWeight.Normal),
        Font(Res.font.JetBrainsMono_Medium, FontWeight.Medium),
        Font(Res.font.JetBrainsMono_SemiBold, FontWeight.SemiBold),
        Font(Res.font.JetBrainsMono_Bold, FontWeight.Bold),
    )

val poppinsFontFamily
    @Composable get() = FontFamily(
        Font(Res.font.Poppins_Light, FontWeight.Light),
        Font(Res.font.Poppins_Regular, FontWeight.Normal),
        Font(Res.font.Poppins_Medium, FontWeight.Medium),
        Font(Res.font.Poppins_SemiBold, FontWeight.SemiBold),
        Font(Res.font.Poppins_Bold, FontWeight.Bold),
        Font(Res.font.Poppins_Black, FontWeight.Black),
    )

val baseline = Typography()

val AppTypography
    @Composable get() = Typography(
        displayLarge = baseline.displayLarge.copy(fontFamily = jetbrainsMonoFontFamily),
        displayMedium = baseline.displayMedium.copy(fontFamily = jetbrainsMonoFontFamily),
        displaySmall = baseline.displaySmall.copy(fontFamily = jetbrainsMonoFontFamily),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = jetbrainsMonoFontFamily),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = jetbrainsMonoFontFamily),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = jetbrainsMonoFontFamily),
        titleLarge = baseline.titleLarge.copy(fontFamily = jetbrainsMonoFontFamily),
        titleMedium = baseline.titleMedium.copy(fontFamily = jetbrainsMonoFontFamily),
        titleSmall = baseline.titleSmall.copy(fontFamily = jetbrainsMonoFontFamily),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = poppinsFontFamily),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = poppinsFontFamily),
        bodySmall = baseline.bodySmall.copy(fontFamily = poppinsFontFamily),
        labelLarge = baseline.labelLarge.copy(fontFamily = poppinsFontFamily),
        labelMedium = baseline.labelMedium.copy(fontFamily = poppinsFontFamily),
        labelSmall = baseline.labelSmall.copy(fontFamily = poppinsFontFamily),
    )


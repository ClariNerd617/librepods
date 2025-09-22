package me.kavishdevar.librepods.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorFilter
import com.kyant.backdrop.effects.refraction
import me.kavishdevar.librepods.R

@Composable
fun ConfirmationDialog(
    showDialog: MutableState<Boolean>,
    title: String,
    message: String,
    confirmText: String = "Enable",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = { showDialog.value = false },
    backdrop: Backdrop,
) {
    if (showDialog.value) {
        val isLightTheme = !isSystemInDarkTheme()
        val contentColor = if (isLightTheme) Color.Black else Color.White
        val accentColor = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
        val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(0.6f) else Color(0xFF121212).copy(0.4f)
        val dimColor = if (isLightTheme) Color(0xFF29293A).copy(0.23f) else Color(0xFF121212).copy(0.56f)

        Box(
            Modifier
                .background(dimColor)
                .fillMaxSize()
                .clickable(onClick = onDismiss)
        ) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .drawBackdrop(
                        backdrop,
                        { RoundedCornerShape(48f.dp) },
//                        highlight = { Highlight { HighlightStyle.Solid } },
                        onDrawSurface = { drawRect(containerColor) }
                    ) {
                        colorFilter(
                            brightness = if (isLightTheme) 0.2f else 0.1f,
                            saturation = 1.5f
                        )
                        blur(if (isLightTheme) 16f.dp.toPx() else 8f.dp.toPx())
                        refraction(24f.dp.toPx(), 48f.dp.toPx(), true)
                    }
                    .fillMaxWidth(0.75f)
                    .requiredWidthIn(min = 200.dp, max = 360.dp)
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        title,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            fontFamily = FontFamily(Font(R.font.sf_pro))
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        message,
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = contentColor.copy(alpha = 0.8f),
                            fontFamily = FontFamily(Font(R.font.sf_pro))
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        Modifier
                            .padding(24.dp, 12.dp, 24.dp, 24.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(containerColor.copy(0.2f))
                                .clickable(onClick = onDismiss)
                                .height(48.dp)
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                dismissText,
                                style = TextStyle(contentColor, 16.sp)
                            )
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(accentColor)
                                .clickable(onClick = onConfirm)
                                .height(48.dp)
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                confirmText,
                                style = TextStyle(Color.White, 16.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/*
 * LibrePods - AirPods liberated from Apple’s ecosystem
 *
 * Copyright (C) 2025 LibrePods contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package me.kavishdevar.librepods.screens

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.CupertinoMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.composables.AccessibilitySlider
import me.kavishdevar.librepods.composables.IndependentToggle
import me.kavishdevar.librepods.composables.LoudSoundReductionSwitch
import me.kavishdevar.librepods.composables.SinglePodANCSwitch
import me.kavishdevar.librepods.composables.StyledSwitch
import me.kavishdevar.librepods.composables.ToneVolumeSlider
import me.kavishdevar.librepods.composables.VolumeControlSwitch
import me.kavishdevar.librepods.services.ServiceManager
import me.kavishdevar.librepods.utils.ATTManager
import me.kavishdevar.librepods.utils.ATTHandles
import me.kavishdevar.librepods.utils.AACPManager
import me.kavishdevar.librepods.utils.RadareOffsetFinder
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.io.encoding.ExperimentalEncodingApi

private var debounceJob: Job? = null
private var phoneMediaDebounceJob: Job? = null
private const val TAG = "HearingAidAdjustments"

@SuppressLint("DefaultLocale")
@ExperimentalHazeMaterialsApi
@OptIn(ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class)
@Composable
fun HearingAidAdjustmentsScreen(navController: NavController) {
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val verticalScrollState = rememberScrollState()
    val hazeState = remember { HazeState() }
    val snackbarHostState = remember { SnackbarHostState() }
    val attManager = ServiceManager.getService()?.attManager ?: throw IllegalStateException("ATTManager not available")

    val aacpManager = remember { ServiceManager.getService()?.aacpManager }
    val context = LocalContext.current
    val radareOffsetFinder = remember { RadareOffsetFinder(context) }
    val isSdpOffsetAvailable = remember { mutableStateOf(RadareOffsetFinder.isSdpOffsetAvailable()) }
    val service = ServiceManager.getService()

    val trackColor = if (isDarkTheme) Color(0xFFB3B3B3) else Color(0xFF929491)
    val activeTrackColor = if (isDarkTheme) Color(0xFF007AFF) else Color(0xFF3C6DF5)
    val thumbColor = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFFFFFFFF)
    val labelTextColor = if (isDarkTheme) Color.White else Color.Black

    Scaffold(
        containerColor = if (isSystemInDarkTheme()) Color(0xFF000000) else Color(0xFFF2F2F7),
        topBar = {
            val darkMode = isSystemInDarkTheme()
            val mDensity = remember { mutableFloatStateOf(1f) }

            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.adjustments),
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (darkMode) Color.White else Color.Black,
                            fontFamily = FontFamily(Font(R.font.sf_pro))
                        )
                    )
                },
                modifier = Modifier
                    .hazeEffect(
                        state = hazeState,
                        style = CupertinoMaterials.thick(),
                        block = fun HazeEffectScope.() {
                            alpha = if (verticalScrollState.value > 60.dp.value * mDensity.floatValue) 1f else 0f
                        })
                    .drawBehind {
                        mDensity.floatValue = density
                        val strokeWidth = 0.7.dp.value * density
                        val y = size.height - strokeWidth / 2
                        if (verticalScrollState.value > 60.dp.value * density) {
                            drawLine(
                                if (darkMode) Color.DarkGray else Color.LightGray,
                                Offset(0f, y),
                                Offset(size.width, y),
                                strokeWidth
                            )
                        }
                    },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .hazeSource(hazeState)
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(verticalScrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val backgroundColor = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)

            val enabled = remember { mutableStateOf(false) }
            val amplificationSliderValue = remember { mutableFloatStateOf(0.5f) }
            val balanceSliderValue = remember { mutableFloatStateOf(0.5f) }
            val toneSliderValue = remember { mutableFloatStateOf(0.5f) }
            val ambientNoiseReductionSliderValue = remember { mutableFloatStateOf(0.0f) }
            val conversationBoostEnabled = remember { mutableStateOf(false) }
            val eq = remember { mutableStateOf(FloatArray(8)) }
            val ownVoiceAmplification = remember { mutableFloatStateOf(0.5f) }

            val phoneMediaEQ = remember { mutableStateOf(FloatArray(8) { 0.5f }) }
            val phoneEQEnabled = remember { mutableStateOf(false) }
            val mediaEQEnabled = remember { mutableStateOf(false) }

            val initialLoadComplete = remember { mutableStateOf(false) }

            val initialReadSucceeded = remember { mutableStateOf(false) }
            val initialReadAttempts = remember { mutableStateOf(0) }

            val HearingAidSettings = remember {
                mutableStateOf(
                    HearingAidSettings(
                        leftEQ = eq.value,
                        rightEQ = eq.value,
                        leftAmplification = amplificationSliderValue.floatValue + (0.5f - balanceSliderValue.floatValue) * amplificationSliderValue.floatValue * 2,
                        rightAmplification = amplificationSliderValue.floatValue + (balanceSliderValue.floatValue - 0.5f) * amplificationSliderValue.floatValue * 2,
                        leftTone = toneSliderValue.floatValue,
                        rightTone = toneSliderValue.floatValue,
                        leftConversationBoost = conversationBoostEnabled.value,
                        rightConversationBoost = conversationBoostEnabled.value,
                        leftAmbientNoiseReduction = ambientNoiseReductionSliderValue.floatValue,
                        rightAmbientNoiseReduction = ambientNoiseReductionSliderValue.floatValue,
                        netAmplification = amplificationSliderValue.floatValue,
                        balance = balanceSliderValue.floatValue,
                        ownVoiceAmplification = ownVoiceAmplification.floatValue
                    )
                )
            }

            val hearingAidEnabled = remember {
                val aidStatus = aacpManager?.controlCommandStatusList?.find { it.identifier == AACPManager.Companion.ControlCommandIdentifiers.HEARING_AID }
                val assistStatus = aacpManager?.controlCommandStatusList?.find { it.identifier == AACPManager.Companion.ControlCommandIdentifiers.HEARING_ASSIST_CONFIG }
                mutableStateOf((aidStatus?.value?.getOrNull(1) == 0x01.toByte()) && (assistStatus?.value?.getOrNull(0) == 0x01.toByte()))
            }

            val hearingAidListener = remember {
                object : AACPManager.ControlCommandListener {
                    override fun onControlCommandReceived(controlCommand: AACPManager.ControlCommand) {
                        if (controlCommand.identifier == AACPManager.Companion.ControlCommandIdentifiers.HEARING_AID.value ||
                            controlCommand.identifier == AACPManager.Companion.ControlCommandIdentifiers.HEARING_ASSIST_CONFIG.value) {
                            val aidStatus = aacpManager?.controlCommandStatusList?.find { it.identifier == AACPManager.Companion.ControlCommandIdentifiers.HEARING_AID }
                            val assistStatus = aacpManager?.controlCommandStatusList?.find { it.identifier == AACPManager.Companion.ControlCommandIdentifiers.HEARING_ASSIST_CONFIG }
                            hearingAidEnabled.value = (aidStatus?.value?.getOrNull(1) == 0x01.toByte()) && (assistStatus?.value?.getOrNull(0) == 0x01.toByte())
                        }
                    }
                }
            }

            val hearingAidATTListener = remember {
                object : (ByteArray) -> Unit {
                    override fun invoke(value: ByteArray) {
                        val parsed = parseHearingAidSettingsResponse(value)
                        if (parsed != null) {
                            amplificationSliderValue.floatValue = parsed.netAmplification
                            balanceSliderValue.floatValue = parsed.balance
                            toneSliderValue.floatValue = parsed.leftTone
                            ambientNoiseReductionSliderValue.floatValue = parsed.leftAmbientNoiseReduction
                            conversationBoostEnabled.value = parsed.leftConversationBoost
                            eq.value = parsed.leftEQ.copyOf()
                            ownVoiceAmplification.floatValue = parsed.ownVoiceAmplification
                            Log.d(TAG, "Updated hearing aid settings from notification")
                        } else {
                            Log.w(TAG, "Failed to parse hearing aid settings from notification")
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                aacpManager?.registerControlCommandListener(AACPManager.Companion.ControlCommandIdentifiers.HEARING_AID, hearingAidListener)
                aacpManager?.registerControlCommandListener(AACPManager.Companion.ControlCommandIdentifiers.HEARING_ASSIST_CONFIG, hearingAidListener)
            }

            DisposableEffect(Unit) {
                onDispose {
                    aacpManager?.unregisterControlCommandListener(AACPManager.Companion.ControlCommandIdentifiers.HEARING_AID, hearingAidListener)
                    aacpManager?.unregisterControlCommandListener(AACPManager.Companion.ControlCommandIdentifiers.HEARING_ASSIST_CONFIG, hearingAidListener)
                    attManager.unregisterListener(ATTHandles.HEARING_AID, hearingAidATTListener)
                }
            }

            LaunchedEffect(amplificationSliderValue.floatValue, balanceSliderValue.floatValue, toneSliderValue.floatValue, conversationBoostEnabled.value, ambientNoiseReductionSliderValue.floatValue, ownVoiceAmplification.floatValue, initialLoadComplete.value, initialReadSucceeded.value) {
                if (!initialLoadComplete.value) {
                    Log.d(TAG, "Initial device load not complete - skipping send")
                    return@LaunchedEffect
                }

                if (!initialReadSucceeded.value) {
                    Log.d(TAG, "Initial device read not successful yet - skipping send until read succeeds")
                    return@LaunchedEffect
                }

                HearingAidSettings.value = HearingAidSettings(
                    leftEQ = eq.value,
                    rightEQ = eq.value,
                    leftAmplification = amplificationSliderValue.floatValue + if (balanceSliderValue.floatValue < 0) -balanceSliderValue.floatValue else 0f,
                    rightAmplification = amplificationSliderValue.floatValue + if (balanceSliderValue.floatValue > 0) balanceSliderValue.floatValue else 0f,
                    leftTone = toneSliderValue.floatValue,
                    rightTone = toneSliderValue.floatValue,
                    leftConversationBoost = conversationBoostEnabled.value,
                    rightConversationBoost = conversationBoostEnabled.value,
                    leftAmbientNoiseReduction = ambientNoiseReductionSliderValue.floatValue,
                    rightAmbientNoiseReduction = ambientNoiseReductionSliderValue.floatValue,
                    netAmplification = amplificationSliderValue.floatValue,
                    balance = balanceSliderValue.floatValue,
                    ownVoiceAmplification = ownVoiceAmplification.floatValue
                )
                Log.d(TAG, "Updated settings: ${HearingAidSettings.value}")
                sendHearingAidSettings(attManager, HearingAidSettings.value)
            }

            LaunchedEffect(Unit) {
                Log.d(TAG, "Connecting to ATT...")
                try {
                    attManager.enableNotifications(ATTHandles.HEARING_AID)
                    attManager.registerListener(ATTHandles.HEARING_AID, hearingAidATTListener)

                    try {
                        if (aacpManager != null) {
                            Log.d(TAG, "Found AACPManager, reading cached EQ data")
                            val aacpEQ = aacpManager.eqData
                            if (aacpEQ.isNotEmpty()) {
                                eq.value = aacpEQ.copyOf()
                                phoneMediaEQ.value = aacpEQ.copyOf()
                                phoneEQEnabled.value = aacpManager.eqOnPhone
                                mediaEQEnabled.value = aacpManager.eqOnMedia
                                Log.d(TAG, "Populated EQ from AACPManager: ${aacpEQ.toList()}")
                            } else {
                                Log.d(TAG, "AACPManager EQ data empty")
                            }
                        } else {
                            Log.d(TAG, "No AACPManager available")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error reading EQ from AACPManager: ${e.message}")
                    }

                    var parsedSettings: HearingAidSettings? = null
                    for (attempt in 1..3) {
                        initialReadAttempts.value = attempt
                        try {
                            val data = attManager.read(ATTHandles.HEARING_AID)
                            parsedSettings = parseHearingAidSettingsResponse(data = data)
                            if (parsedSettings != null) {
                                Log.d(TAG, "Parsed settings on attempt $attempt")
                                break
                            } else {
                                Log.d(TAG, "Parsing returned null on attempt $attempt")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Read attempt $attempt failed: ${e.message}")
                        }
                        delay(200)
                    }

                    if (parsedSettings != null) {
                        Log.d(TAG, "Initial hearing aid settings: $parsedSettings")
                        amplificationSliderValue.floatValue = parsedSettings.netAmplification
                        balanceSliderValue.floatValue = parsedSettings.balance
                        toneSliderValue.floatValue = parsedSettings.leftTone
                        ambientNoiseReductionSliderValue.floatValue = parsedSettings.leftAmbientNoiseReduction
                        conversationBoostEnabled.value = parsedSettings.leftConversationBoost
                        eq.value = parsedSettings.leftEQ.copyOf()
                        ownVoiceAmplification.floatValue = parsedSettings.ownVoiceAmplification
                        initialReadSucceeded.value = true
                    } else {
                        Log.d(TAG, "Failed to read/parse initial hearing aid settings after ${initialReadAttempts.value} attempts")
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    initialLoadComplete.value = true
                }
            }

            val isDarkThemeLocal = isSystemInDarkTheme()
            var backgroundColorHA by remember { mutableStateOf(if (isDarkThemeLocal) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)) }
            val animatedBackgroundColorHA by animateColorAsState(targetValue = backgroundColorHA, animationSpec = tween(durationMillis = 500))

            Text(
                text = stringResource(R.string.amplification).uppercase(),
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = textColor.copy(alpha = 0.6f),
                    fontFamily = FontFamily(Font(R.font.sf_pro))
                ),
                modifier = Modifier.padding(8.dp, bottom = 0.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp, vertical = 0.dp)
                    .height(55.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "􀊥",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = labelTextColor,
                            fontFamily = FontFamily(Font(R.font.sf_pro))
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    AccessibilitySlider(
                        valueRange = -1f..1f,
                        value = amplificationSliderValue.floatValue,
                        onValueChange = {
                            amplificationSliderValue.floatValue = snapIfClose(it, listOf(-0.5f, -0.25f, 0f, 0.25f, 0.5f))
                        },
                        widthFrac = 0.90f
                    )
                    Text(
                        text = "􀊩",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = labelTextColor,
                            fontFamily = FontFamily(Font(R.font.sf_pro))
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }

            val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

            IndependentToggle(
                name = stringResource(R.string.swipe_to_control_amplification),
                service = service,
                sharedPreferences = sharedPreferences,
                controlCommandIdentifier = AACPManager.Companion.ControlCommandIdentifiers.HPS_GAIN_SWIPE,
                description = stringResource(R.string.swipe_amplification_description)
            )

            Text(
                text = stringResource(R.string.balance).uppercase(),
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = textColor.copy(alpha = 0.6f),
                    fontFamily = FontFamily(Font(R.font.sf_pro))
                ),
                modifier = Modifier.padding(8.dp, bottom = 0.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.left),
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = labelTextColor,
                                fontFamily = FontFamily(Font(R.font.sf_pro))
                            )
                        )
                        Text(
                            text = stringResource(R.string.right),
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = labelTextColor,
                                fontFamily = FontFamily(Font(R.font.sf_pro))
                            )
                        )
                    }
                    AccessibilitySlider(
                        valueRange = -1f..1f,
                        value = balanceSliderValue.floatValue,
                        onValueChange = {
                            balanceSliderValue.floatValue = snapIfClose(it, listOf(0f))
                        },
                    )
                }
            }

            Text(
                text = stringResource(R.string.tone).uppercase(),
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = textColor.copy(alpha = 0.6f),
                    fontFamily = FontFamily(Font(R.font.sf_pro))
                ),
                modifier = Modifier.padding(8.dp, bottom = 0.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.darker),
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = labelTextColor,
                                fontFamily = FontFamily(Font(R.font.sf_pro))
                            )
                        )
                        Text(
                            text = stringResource(R.string.brighter),
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = labelTextColor,
                                fontFamily = FontFamily(Font(R.font.sf_pro))
                            )
                        )
                    }
                    AccessibilitySlider(
                        valueRange = -1f..1f,
                        value = toneSliderValue.floatValue,
                        onValueChange = {
                            toneSliderValue.floatValue = snapIfClose(it, listOf(0f))
                        },
                    )
                }
            }

            Text(
                text = stringResource(R.string.ambient_noise_reduction).uppercase(),
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    color = textColor.copy(alpha = 0.6f),
                    fontFamily = FontFamily(Font(R.font.sf_pro))
                ),
                modifier = Modifier.padding(8.dp, bottom = 0.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.less),
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = labelTextColor,
                                fontFamily = FontFamily(Font(R.font.sf_pro))
                            )
                        )
                        Text(
                            text = stringResource(R.string.more),
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = labelTextColor,
                                fontFamily = FontFamily(Font(R.font.sf_pro))
                            )
                        )
                    }
                    AccessibilitySlider(
                        valueRange = 0f..1f,
                        value = ambientNoiseReductionSliderValue.floatValue,
                        onValueChange = {
                            ambientNoiseReductionSliderValue.floatValue = snapIfClose(it, listOf(0.1f, 0.3f, 0.5f, 0.7f, 0.9f))
                        },
                    )
                }
            }

            AccessibilityToggle(
                text = stringResource(R.string.conversation_boost),
                mutableState = conversationBoostEnabled,
                independent = true,
                description = stringResource(R.string.conversation_boost_description)
            )
        }
    }
}

private data class HearingAidSettings(
    val leftEQ: FloatArray,
    val rightEQ: FloatArray,
    val leftAmplification: Float,
    val rightAmplification: Float,
    val leftTone: Float,
    val rightTone: Float,
    val leftConversationBoost: Boolean,
    val rightConversationBoost: Boolean,
    val leftAmbientNoiseReduction: Float,
    val rightAmbientNoiseReduction: Float,
    val netAmplification: Float,
    val balance: Float,
    val ownVoiceAmplification: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HearingAidSettings

        if (leftAmplification != other.leftAmplification) return false
        if (rightAmplification != other.rightAmplification) return false
        if (leftTone != other.leftTone) return false
        if (rightTone != other.rightTone) return false
        if (leftConversationBoost != other.leftConversationBoost) return false
        if (rightConversationBoost != other.rightConversationBoost) return false
        if (leftAmbientNoiseReduction != other.leftAmbientNoiseReduction) return false
        if (rightAmbientNoiseReduction != other.rightAmbientNoiseReduction) return false
        if (!leftEQ.contentEquals(other.leftEQ)) return false
        if (!rightEQ.contentEquals(other.rightEQ)) return false
        if (ownVoiceAmplification != other.ownVoiceAmplification) return false

        return true
    }

    override fun hashCode(): Int {
        var result = leftAmplification.hashCode()
        result = 31 * result + rightAmplification.hashCode()
        result = 31 * result + leftTone.hashCode()
        result = 31 * result + rightTone.hashCode()
        result = 31 * result + leftConversationBoost.hashCode()
        result = 31 * result + rightConversationBoost.hashCode()
        result = 31 * result + leftAmbientNoiseReduction.hashCode()
        result = 31 * result + rightAmbientNoiseReduction.hashCode()
        result = 31 * result + leftEQ.contentHashCode()
        result = 31 * result + rightEQ.contentHashCode()
        result = 31 * result + ownVoiceAmplification.hashCode()
        return result
    }
}

private fun parseHearingAidSettingsResponse(data: ByteArray): HearingAidSettings? {
    if (data.size < 104) return null
    val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

    val phoneEnabled = buffer.get() == 0x01.toByte()
    val mediaEnabled = buffer.get() == 0x01.toByte()
    buffer.getShort() // skip 0x60 0x00

    val leftEQ = FloatArray(8)
    for (i in 0..7) {
        leftEQ[i] = buffer.float
    }
    val leftAmplification = buffer.float
    val leftTone = buffer.float
    val leftConvFloat = buffer.float
    val leftConversationBoost = leftConvFloat > 0.5f
    val leftAmbientNoiseReduction = buffer.float

    val rightEQ = FloatArray(8)
    for (i in 0..7) {
        rightEQ[i] = buffer.float
    }
    val rightAmplification = buffer.float
    val rightTone = buffer.float
    val rightConvFloat = buffer.float
    val rightConversationBoost = rightConvFloat > 0.5f
    val rightAmbientNoiseReduction = buffer.float

    val ownVoiceAmplification = buffer.float

    val avg = (leftAmplification + rightAmplification) / 2
    val amplification = avg.coerceIn(-1f, 1f)
    val diff = rightAmplification - leftAmplification
    val balance = diff.coerceIn(-1f, 1f)

    return HearingAidSettings(
        leftEQ = leftEQ,
        rightEQ = rightEQ,
        leftAmplification = leftAmplification,
        rightAmplification = rightAmplification,
        leftTone = leftTone,
        rightTone = rightTone,
        leftConversationBoost = leftConversationBoost,
        rightConversationBoost = rightConversationBoost,
        leftAmbientNoiseReduction = leftAmbientNoiseReduction,
        rightAmbientNoiseReduction = rightAmbientNoiseReduction,
        netAmplification = amplification,
        balance = balance,
        ownVoiceAmplification = ownVoiceAmplification
    )
}

private fun sendHearingAidSettings(
    attManager: ATTManager,
    HearingAidSettings: HearingAidSettings
) {
    debounceJob?.cancel()
    debounceJob = CoroutineScope(Dispatchers.IO).launch {
        delay(100)
        try {
            val currentData = attManager.read(ATTHandles.HEARING_AID)
            Log.d(TAG, "Current data before update: ${currentData.joinToString(" ") { String.format("%02X", it) }}")
            if (currentData.size < 104) {
                Log.w(TAG, "Current data size ${currentData.size} too small, cannot send settings")
                return@launch
            }
            val buffer = ByteBuffer.wrap(currentData).order(ByteOrder.LITTLE_ENDIAN)

            // for some reason
            buffer.put(2, 0x64)

            // Left ear adjustments
            buffer.putFloat(36, HearingAidSettings.leftAmplification)
            buffer.putFloat(40, HearingAidSettings.leftTone)
            buffer.putFloat(44, if (HearingAidSettings.leftConversationBoost) 1.0f else 0.0f)
            buffer.putFloat(48, HearingAidSettings.leftAmbientNoiseReduction)

            // Right ear adjustments
            buffer.putFloat(84, HearingAidSettings.rightAmplification)
            buffer.putFloat(88, HearingAidSettings.rightTone)
            buffer.putFloat(92, if (HearingAidSettings.rightConversationBoost) 1.0f else 0.0f)
            buffer.putFloat(96, HearingAidSettings.rightAmbientNoiseReduction)

            // Own voice amplification
            buffer.putFloat(100, HearingAidSettings.ownVoiceAmplification)

            Log.d(TAG, "Sending updated settings: ${currentData.joinToString(" ") { String.format("%02X", it) }}")

            attManager.write(ATTHandles.HEARING_AID, currentData)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

private fun snapIfClose(value: Float, points: List<Float>, threshold: Float = 0.05f): Float {
    val nearest = points.minByOrNull { kotlin.math.abs(it - value) } ?: value
    return if (kotlin.math.abs(nearest - value) <= threshold) nearest else value
}

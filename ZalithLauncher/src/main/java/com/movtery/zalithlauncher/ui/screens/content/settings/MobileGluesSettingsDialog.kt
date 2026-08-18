package com.movtery.zalithlauncher.ui.screens.content.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.utils.settings.MobileGluesConfig
import com.movtery.zalithlauncher.utils.settings.MobileGluesConfig.Companion.load

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileGluesSettingsDialog(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val config = remember {
        MobileGluesConfig.load() ?: MobileGluesConfig()
    }

    val knownKeys = remember {
        setOf(
            "enableANGLE", "enableNoError", "enableExtTimerQuery",
            "enableExtComputeShader", "enableExtDirectStateAccess",
            "maxGlslCacheSize", "multidrawMode", "angleDepthClearFixMode",
            "maxGlslCacheSize", "angleDepthClearFixMode",
            "customGLVersion", "fsr1"
        )
    }

    var enableANGLE by remember { mutableIntStateOf(config.get("enableANGLE", 1)) }
    var enableNoError by remember { mutableIntStateOf(config.get("enableNoError", 0)) }
    var enableExtTimerQuery by remember { mutableStateOf(config.get("enableExtTimerQuery", 0) == 1) }
    var enableExtComputeShader by remember { mutableStateOf(config.get("enableExtComputeShader", 0) == 1) }
    var enableExtDirectStateAccess by remember { mutableStateOf(config.get("enableExtDirectStateAccess", 0) == 1) }
    var maxGlslCacheSize by remember { mutableStateOf(config.get("maxGlslCacheSize", 32).toString()) }
    var angleDepthClearFixMode by remember { mutableIntStateOf(config.get("angleDepthClearFixMode", 0)) }
    var customGLVersion by remember { mutableIntStateOf(config.get("customGLVersion", 0)) }
    var fsr1 by remember { mutableStateOf(config.get("fsr1", 0) == 1) }

    val unknownKeys = remember(config) {
        val known = setOf(
            "enableANGLE", "enableNoError", "enableExtTimerQuery",
            "enableExtComputeShader", "enableExtDirectStateAccess",
            "maxGlslCacheSize", "angleDepthClearFixMode",
            "customGLVersion", "fsr1"
        )
        config.allKeys.filter { it !in known }
    }

    var unknownValues by remember(unknownKeys) {
        mutableStateOf(unknownKeys.associateWith { config.get(it) })
    }

    val disabledLabel = stringResource(R.string.mobileglues_gl_disabled)
    val glVersions = remember(disabledLabel) {
        listOf(
            0 to disabledLabel,
            32 to "OpenGL 3.2", 33 to "OpenGL 3.3",
            40 to "OpenGL 4.0", 41 to "OpenGL 4.1", 42 to "OpenGL 4.2",
            43 to "OpenGL 4.3", 44 to "OpenGL 4.4", 45 to "OpenGL 4.5",
            46 to "OpenGL 4.6"
        )
    }

    var showResetConfirm by remember { mutableStateOf(false) }

    val mgVersion = remember {
        runCatching {
            val jsonStr = context.assets.open("mobileglues/metadata.json").bufferedReader().use { it.readText() }
            val obj = com.google.gson.JsonParser.parseString(jsonStr).asJsonObject
            obj.get("version")?.asString
        }.getOrNull() ?: "v2.0.0"
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = cardColor(false),
            contentColor = onCardColor(),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.mobileglues_settings_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = mgVersion,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            config.set("enableANGLE", enableANGLE)
                            config.set("enableNoError", enableNoError)
                            config.set("enableExtTimerQuery", if (enableExtTimerQuery) 1 else 0)
                            config.set("enableExtComputeShader", if (enableExtComputeShader) 1 else 0)
                            config.set("enableExtDirectStateAccess", if (enableExtDirectStateAccess) 1 else 0)
                            config.set("maxGlslCacheSize", maxGlslCacheSize.toIntOrNull() ?: 32)
                            config.set("angleDepthClearFixMode", angleDepthClearFixMode)
                            config.set("customGLVersion", customGLVersion)
                            config.set("fsr1", if (fsr1) 1 else 0)
                            unknownValues.forEach { (k, v) -> config.set(k, v) }
                            config.save()
                            Toast.makeText(context, context.getString(R.string.mobileglues_saved_toast), Toast.LENGTH_SHORT).show()
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(R.string.generic_save))
                    }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { showResetConfirm = true }
                    ) {
                        Text(stringResource(R.string.generic_reset))
                    }
                }

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HorizontalDivider()

                    SectionHeader(stringResource(R.string.mobileglues_section_rendering))

                    ANGLEPicker(
                        value = enableANGLE,
                        onValueChange = { enableANGLE = it }
                    )

                    AngleClearPicker(
                        value = angleDepthClearFixMode,
                        onValueChange = { angleDepthClearFixMode = it }
                    )

                    GLVersionDropdown(
                        value = customGLVersion,
                        options = glVersions,
                        onValueChange = { customGLVersion = it }
                    )

                    SettingsSwitchRow(
                        title = stringResource(R.string.mobileglues_fsr1),
                        summary = stringResource(R.string.mobileglues_fsr1_summary),
                        checked = fsr1,
                        onCheckedChange = { fsr1 = it }
                    )

                    HorizontalDivider()

                    SectionHeader(stringResource(R.string.mobileglues_section_advanced))

                    NoErrorPicker(
                        value = enableNoError,
                        onValueChange = { enableNoError = it }
                    )

                    HorizontalDivider()

                    SectionHeader(stringResource(R.string.mobileglues_section_extensions))

                    SettingsSwitchRow(
                        title = stringResource(R.string.mobileglues_compute_shader),
                        summary = stringResource(R.string.mobileglues_compute_shader_summary),
                        checked = enableExtComputeShader,
                        onCheckedChange = { enableExtComputeShader = it }
                    )

                    SettingsSwitchRow(
                        title = stringResource(R.string.mobileglues_timer_query),
                        summary = stringResource(R.string.mobileglues_timer_query_summary),
                        checked = enableExtTimerQuery,
                        onCheckedChange = { enableExtTimerQuery = it }
                    )

                    SettingsSwitchRow(
                        title = stringResource(R.string.mobileglues_direct_state_access),
                        summary = stringResource(R.string.mobileglues_direct_state_access_summary),
                        checked = enableExtDirectStateAccess,
                        onCheckedChange = { enableExtDirectStateAccess = it }
                    )

                    HorizontalDivider()
                    SectionHeader(stringResource(R.string.mobileglues_section_cache))

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = maxGlslCacheSize,
                        onValueChange = { maxGlslCacheSize = it },
                        label = { Text(stringResource(R.string.mobileglues_cache_size_label)) },
                        supportingText = { Text(stringResource(R.string.mobileglues_cache_size_supporting)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    if (unknownKeys.isNotEmpty()) {
                        HorizontalDivider()
                        SectionHeader(stringResource(R.string.mobileglues_section_other))

                        unknownKeys.forEach { key ->
                            UnknownSettingRow(
                                key = key,
                                value = unknownValues[key] ?: 0,
                                onValueChange = { unknownValues = unknownValues + (key to it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.generic_reset)) },
            text = { Text(stringResource(R.string.mobileglues_reset_confirm_message)) },
            confirmButton = {
                Button(onClick = {
                    enableANGLE = 1
                    enableNoError = 0
                    enableExtTimerQuery = false
                    enableExtComputeShader = false
                    enableExtDirectStateAccess = false
                    maxGlslCacheSize = "32"
                    angleDepthClearFixMode = 0
                    customGLVersion = 0
                    fsr1 = false
                    unknownValues = unknownKeys.associateWith { 0 }
                    showResetConfirm = false
                }) {
                    Text(stringResource(R.string.generic_reset))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.generic_cancel))
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = summary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun UnknownSettingRow(key: String, value: Int, onValueChange: (Int) -> Unit) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = textValue,
        onValueChange = {
            textValue = it
            it.toIntOrNull()?.let { onValueChange(it) }
        },
        label = { Text(key) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

@Composable
private fun ANGLEPicker(value: Int, onValueChange: (Int) -> Unit) {
    val disable = stringResource(R.string.mobileglues_angle_disable)
    val auto = stringResource(R.string.mobileglues_angle_auto)
    val force = stringResource(R.string.mobileglues_angle_force)
    val disableIfPossible = stringResource(R.string.mobileglues_angle_disable_if_possible)
    val options = remember(disable, auto, force, disableIfPossible) {
        listOf(disable to 0, auto to 1, force to 2, disableIfPossible to 3)
    }
    DropdownSettingRow(
        label = stringResource(R.string.mobileglues_angle_mode),
        options = options,
        selectedValue = value,
        onValueChange = onValueChange
    )
}

@Composable
private fun NoErrorPicker(value: Int, onValueChange: (Int) -> Unit) {
    val auto = stringResource(R.string.mobileglues_no_error_auto)
    val enable = stringResource(R.string.mobileglues_no_error_enable)
    val disablePri = stringResource(R.string.mobileglues_no_error_disable_pri)
    val disableSec = stringResource(R.string.mobileglues_no_error_disable_sec)
    val options = remember(auto, enable, disablePri, disableSec) {
        listOf(auto to 0, enable to 1, disablePri to 2, disableSec to 3)
    }
    DropdownSettingRow(
        label = stringResource(R.string.mobileglues_no_error_mode),
        options = options,
        selectedValue = value,
        onValueChange = onValueChange
    )
}

@Composable
private fun AngleClearPicker(value: Int, onValueChange: (Int) -> Unit) {
    val off = stringResource(R.string.mobileglues_angle_clear_off)
    val mode1 = stringResource(R.string.mobileglues_angle_clear_mode1)
    val options = remember(off, mode1) {
        listOf(off to 0, mode1 to 1)
    }
    DropdownSettingRow(
        label = stringResource(R.string.mobileglues_angle_depth_clear_fix),
        options = options,
        selectedValue = value,
        onValueChange = onValueChange
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GLVersionDropdown(
    value: Int,
    options: List<Pair<Int, String>>,
    onValueChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == value }?.second ?: stringResource(R.string.mobileglues_gl_disabled)

    Text(stringResource(R.string.mobileglues_custom_gl_version), style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(4.dp))

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (v, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onValueChange(v)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSettingRow(
    label: String,
    options: List<Pair<String, Int>>,
    selectedValue: Int,
    onValueChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.second == selectedValue }?.first ?: options.first().first

    Text(label, style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(4.dp))

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (label, v) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onValueChange(v)
                        expanded = false
                    }
                )
            }
        }
    }
}

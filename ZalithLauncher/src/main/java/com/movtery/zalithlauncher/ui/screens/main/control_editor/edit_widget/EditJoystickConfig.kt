package com.movtery.zalithlauncher.ui.screens.main.control_editor.edit_widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.layer_controller.data.JOYSTICK_DEAD_ZONE_RANGE
import com.movtery.layer_controller.data.JOYSTICK_LOCK_THRESHOLD_RANGE
import com.movtery.layer_controller.data.JoystickTriggerMode
import com.movtery.layer_controller.observable.ObservableJoystickData
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.verticalScrollWithBar
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.main.control_editor.InfoLayoutListItem
import com.movtery.zalithlauncher.ui.screens.main.control_editor.InfoLayoutSliderItem
import com.movtery.zalithlauncher.ui.screens.main.control_editor.InfoLayoutSwitchItem
import com.movtery.zalithlauncher.ui.screens.main.control_editor.getTriggerModeText

@Composable
fun EditJoystickConfig(
    screenKey: TitledNavKey,
    currentKey: TitledNavKey?,
    data: ObservableJoystickData
) {
    BaseScreen(
        screenKey = screenKey,
        currentKey = currentKey
    ) {
        Column(
            modifier = Modifier
                .padding(start = 4.dp, end = 8.dp)
                .fillMaxSize()
                .verticalScrollWithBar(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier)

            // 死区比例
            InfoLayoutSliderItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.control_editor_edit_joystick_dead_zone),
                value = data.deadZoneRatio,
                onValueChange = { data.deadZoneRatio = it },
                valueRange = JOYSTICK_DEAD_ZONE_RANGE,
                decimalFormat = "#0.00",
                fineTuningStep = 0.1f,
            )

            // 操控方式
            InfoLayoutListItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.control_editor_edit_joystick_trigger_mode),
                items = JoystickTriggerMode.entries,
                selectedItem = data.triggerMode,
                onItemSelected = { data.triggerMode = it },
                getItemText = { it.getTriggerModeText() }
            )

            // 自由模式配置
            if (data.triggerMode == JoystickTriggerMode.FREE) {
                // 感应区域 X 坐标
                InfoLayoutSliderItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.control_editor_edit_joystick_free_pos_x),
                    value = data.freeAreaPosition.x / 100f,
                    onValueChange = { data.freeAreaPosition = data.freeAreaPosition.copy(x = (it * 100).toInt()) },
                    valueRange = 0f..100f,
                    decimalFormat = "#0.00",
                    suffix = "%",
                    fineTuningStep = 1.0f
                )

                // 感应区域 Y 坐标
                InfoLayoutSliderItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.control_editor_edit_joystick_free_pos_y),
                    value = data.freeAreaPosition.y / 100f,
                    onValueChange = { data.freeAreaPosition = data.freeAreaPosition.copy(y = (it * 100).toInt()) },
                    valueRange = 0f..100f,
                    decimalFormat = "#0.00",
                    suffix = "%",
                    fineTuningStep = 1.0f
                )

                // 感应区域 尺寸类型
                val sizeTypes = listOf(com.movtery.layer_controller.data.ButtonSize.Type.Dp, com.movtery.layer_controller.data.ButtonSize.Type.Percentage)
                InfoLayoutListItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.control_editor_edit_joystick_free_size_type),
                    items = sizeTypes,
                    selectedItem = data.freeAreaSize.type,
                    onItemSelected = { data.freeAreaSize = data.freeAreaSize.copy(type = it) },
                    getItemText = { type ->
                        val textRes = when (type) {
                            com.movtery.layer_controller.data.ButtonSize.Type.Dp -> R.string.control_editor_edit_size_type_dp
                            com.movtery.layer_controller.data.ButtonSize.Type.Percentage -> R.string.control_editor_edit_size_type_percentage
                            com.movtery.layer_controller.data.ButtonSize.Type.WrapContent -> R.string.control_editor_edit_size_type_wrap_content
                        }
                        stringResource(textRes)
                    }
                )

                when (data.freeAreaSize.type) {
                    com.movtery.layer_controller.data.ButtonSize.Type.Dp -> {
                        InfoLayoutSliderItem(
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.control_editor_edit_joystick_free_size_width),
                            value = data.freeAreaSize.widthDp,
                            onValueChange = { data.freeAreaSize = data.freeAreaSize.copy(widthDp = it) },
                            valueRange = 20f..1000f,
                            decimalFormat = "#0.0",
                            suffix = "Dp",
                            fineTuningStep = 5f
                        )
                        InfoLayoutSliderItem(
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.control_editor_edit_joystick_free_size_height),
                            value = data.freeAreaSize.heightDp,
                            onValueChange = { data.freeAreaSize = data.freeAreaSize.copy(heightDp = it) },
                            valueRange = 20f..1000f,
                            decimalFormat = "#0.0",
                            suffix = "Dp",
                            fineTuningStep = 5f
                        )
                    }
                    com.movtery.layer_controller.data.ButtonSize.Type.Percentage -> {
                        InfoLayoutSliderItem(
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.control_editor_edit_joystick_free_size_width),
                            value = data.freeAreaSize.widthPercentage / 100f,
                            onValueChange = { data.freeAreaSize = data.freeAreaSize.copy(widthPercentage = (it * 100).toInt()) },
                            valueRange = 1f..100f,
                            decimalFormat = "#0.00",
                            suffix = "%",
                            fineTuningStep = 1.0f
                        )
                        InfoLayoutSliderItem(
                            modifier = Modifier.fillMaxWidth(),
                            title = stringResource(R.string.control_editor_edit_joystick_free_size_height),
                            value = data.freeAreaSize.heightPercentage / 100f,
                            onValueChange = { data.freeAreaSize = data.freeAreaSize.copy(heightPercentage = (it * 100).toInt()) },
                            valueRange = 1f..100f,
                            decimalFormat = "#0.00",
                            suffix = "%",
                            fineTuningStep = 1.0f
                        )
                    }
                    else -> {}
                }

                // 静止时不透明度
                InfoLayoutSliderItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.control_editor_edit_joystick_free_resting_alpha),
                    value = data.freeRestingAlpha,
                    onValueChange = { data.freeRestingAlpha = it },
                    valueRange = com.movtery.layer_controller.data.JOYSTICK_FREE_RESTING_ALPHA_RANGE,
                    decimalFormat = "#0.00",
                    fineTuningStep = 0.05f
                )

                // 动画过渡时长 (ms)
                InfoLayoutSliderItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.control_editor_edit_joystick_free_anim_duration),
                    value = data.freeAnimationDurationMs.toFloat(),
                    onValueChange = { data.freeAnimationDurationMs = it.toInt() },
                    valueRange = com.movtery.layer_controller.data.JOYSTICK_FREE_ANIMATION_DURATION_RANGE,
                    decimalFormat = "#0",
                    suffix = " ms",
                    fineTuningStep = 25f
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 前进锁
            InfoLayoutSwitchItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.control_editor_edit_joystick_can_lock),
                value = data.canLock,
                onValueChange = { data.canLock = it }
            )

            // 锁定阈值
            if (data.canLock) {
                InfoLayoutSliderItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.control_editor_edit_joystick_lock_threshold),
                    value = data.lockThreshold,
                    onValueChange = { data.lockThreshold = it },
                    valueRange = JOYSTICK_LOCK_THRESHOLD_RANGE,
                    decimalFormat = "#0.00",
                    fineTuningStep = 0.1f
                )
            }

            Spacer(Modifier)
        }
    }
}

/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.layer_controller.observable

import android.graphics.Rect
import android.graphics.Region
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.movtery.layer_controller.data.ButtonPosition
import com.movtery.layer_controller.data.ButtonSize
import com.movtery.layer_controller.data.JOYSTICK_MIN_SIZE_DP
import com.movtery.layer_controller.data.JOYSTICK_MIN_SIZE_PERCENTAGE
import com.movtery.layer_controller.data.JoystickData
import com.movtery.layer_controller.data.JoystickDirection
import com.movtery.layer_controller.data.JoystickTriggerMode
import com.movtery.layer_controller.data.VisibilityType
import com.movtery.layer_controller.data.cloneNew
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.event.EventHandler
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 可观察的摇杆控件包装类
 */
class ObservableJoystickData(data: JoystickData) : ObservableWidget() {
    val uuid: String = data.uuid
    var position by mutableStateOf(data.position)
    var sizeType by mutableStateOf(data.sizeType)
    private val _sizeDp = mutableFloatStateOf(data.sizeDp.coerceAtLeast(JOYSTICK_MIN_SIZE_DP))
    var sizeDp: Float
        get() = _sizeDp.floatValue
        set(value) {
            _sizeDp.floatValue = value.coerceAtLeast(JOYSTICK_MIN_SIZE_DP)
        }

    private val _sizePercentage =
        mutableIntStateOf(data.sizePercentage.coerceAtLeast(JOYSTICK_MIN_SIZE_PERCENTAGE))
    var sizePercentage: Int
        get() = _sizePercentage.intValue
        set(value) {
            _sizePercentage.intValue = value.coerceAtLeast(JOYSTICK_MIN_SIZE_PERCENTAGE)
        }

    var visibilityType by mutableStateOf(data.visibilityType)
    var joystickStyleId by mutableStateOf(data.joystickStyleId)
    var deadZoneRatio by mutableFloatStateOf(data.deadZoneRatio)
    var lockThreshold by mutableFloatStateOf(data.lockThreshold)
    var canLock by mutableStateOf(data.canLock)
    var triggerMode by mutableStateOf(data.triggerMode)
    var freeRadiusRatio by mutableFloatStateOf(data.freeRadiusRatio)
    var freeOffsetX by mutableIntStateOf(data.freeOffsetX)
    var freeOffsetY by mutableIntStateOf(data.freeOffsetY)
    var freeRestingAlpha by mutableFloatStateOf(data.freeRestingAlpha)
    var freeAnimationDurationMs by mutableIntStateOf(data.freeAnimationDurationMs)
    var directionEvents by mutableStateOf(data.directionEvents)
    var lockEvents by mutableStateOf(data.lockEvents)

    /**
     * 运行时的方向状态
     */
    var currentDirection by mutableStateOf(JoystickDirection.None)
        private set

    /**
     * 自由模式下摇杆基座的中心偏移（相对于控件中心）
     */
    var baseOffset by mutableStateOf(Offset.Zero)

    /**
     * 是否正在触摸交互
     */
    var isInteracting by mutableStateOf(false)

    /**
     * 摇杆头在背景层内的偏移位置（相对于当前背景层中心）
     */
    var knobOffset by mutableStateOf(Offset.Zero)

    /**
     * 是否处于前进锁定状态
     */
    var isLocked by mutableStateOf(false)
        private set

    /**
     * 是否可以进行前进锁定
     */
    var canLockState by mutableStateOf(false)
        private set

    /**
     * 当前占用的指针ID
     */
    internal var activePointer: PointerId? = null

    /**
     * 上一次拖动的位置
     */
    private var lastDragPosition = Offset.Zero

    override val behavior: InteractionBehavior
        get() = InteractionBehavior.Press

    override val internalRenderPosition: ButtonPosition
        get() = position

    override fun putRenderPosition(position: ButtonPosition) {
        this.position = position
    }

    override fun putWidgetSize(size: ButtonSize) {
        this.sizeDp = size.widthDp
        this.sizePercentage = size.widthPercentage
        this.sizeType = size.type
    }

    override val styleId: String?
        get() = joystickStyleId

    override val widgetSize: ButtonSize
        get() = JoystickData(
            uuid = uuid,
            position = position,
            sizeType = sizeType,
            sizeDp = sizeDp,
            sizePercentage = sizePercentage
        ).toButtonSize()

    override fun onCompositionStart(eventHandler: EventHandler?) {}

    override fun onCompositionDispose(eventHandler: EventHandler?) {
        // 释放时取消所有方向按键
        if (currentDirection != JoystickDirection.None) {
            val events = directionEvents[currentDirection] ?: emptyList()
            eventHandler?.onKeyPressed(events, false)
            currentDirection = JoystickDirection.None
        }
        // 释放锁定事件
        if (canLockState || isLocked) {
            eventHandler?.onKeyPressed(lockEvents, false)
            isLocked = false
        }
        canLockState = false
        isInteracting = false
        baseOffset = Offset.Zero
        knobOffset = Offset.Zero
        lastDragPosition = Offset.Zero
    }

    override fun onCheckVisibilityType(): VisibilityType = visibilityType

    override fun supportsDeepTouchDetection(): Boolean = false

    override fun canProcess(): Boolean = false

    override fun onTouchEvent(
        eventHandler: EventHandler,
        allLayers: List<ObservableControlLayer>,
        activeWidgets: List<ObservableWidget>,
        addThis: () -> Unit,
        consumeEvent: (Boolean) -> Unit
    ) {}

    override fun isReleaseOnOutOfBounds(): Boolean = false

    override fun onPointerBackInBounds(
        eventHandler: EventHandler,
        allLayers: List<ObservableControlLayer>
    ) {}

    override fun onReleaseEvent(
        eventHandler: EventHandler,
        allLayers: List<ObservableControlLayer>
    ) {}

    /**
     * 为锁定状态添加触发事件
     */
    fun addLockEvent(event: ClickEvent) {
        lockEvents += event
    }

    /**
     * 为锁定状态移除触发事件
     */
    fun removeLockEvent(filterNot: (ClickEvent) -> Boolean) {
        lockEvents = lockEvents.filterNot(filterNot)
    }

    /**
     * 为指定方向添加触发事件
     */
    fun addDirectionEvent(
        direction: JoystickDirection?,
        event: ClickEvent
    ) {
        if (direction != null) {
            val current = directionEvents[direction] ?: emptyList()
            if (current.none { it.type == event.type && it.key == event.key }) {
                directionEvents += (direction to current + event)
            }
        }
    }

    /**
     * 为指定方向移除触发事件
     */
    fun removeDirectionEvent(
        direction: JoystickDirection?,
        filterNot: (ClickEvent) -> Boolean
    ) {
        if (direction != null) {
            val current = directionEvents[direction] ?: emptyList()
            directionEvents += (direction to current.filterNot(filterNot))
        }
    }

    /**
     * 计算方向并触发事件
     */
    private fun updateDirection(
        newDirection: JoystickDirection,
        eventHandler: EventHandler
    ) {
        if (newDirection == currentDirection) return

        // 释放旧方向的事件
        if (currentDirection != JoystickDirection.None) {
            val oldEvents = directionEvents[currentDirection] ?: emptyList()
            eventHandler.onKeyPressed(oldEvents, false)
        }

        // 按下新方向的事件
        if (newDirection != JoystickDirection.None) {
            val newEvents = directionEvents[newDirection] ?: emptyList()
            eventHandler.onKeyPressed(newEvents, true)
        }

        currentDirection = newDirection
    }

    /**
     * 更新摇杆状态（方向 + 锁定判断）
     */
    private fun updateJoystickState(
        position: Offset,
        centerPoint: Offset,
        deadZoneRadius: Float,
        lockThresholdPx: Float,
        eventHandler: EventHandler
    ) {
        val effectivePosition = if (triggerMode == JoystickTriggerMode.FREE) {
            position - baseOffset
        } else {
            position
        }

        val clampedPosition = effectivePosition.clampToRegion(
            region = backgroundRegion,
            center = centerPoint
        )

        knobOffset = clampedPosition - centerPoint

        val direction = calculateDirection(
            joystickPosition = clampedPosition,
            backgroundCenter = centerPoint,
            deadZoneRadius = deadZoneRadius
        )
        updateDirection(direction, eventHandler)

        val newCanLockState =
            canLock &&
                    direction == JoystickDirection.North &&
                    lastDragPosition.y < -lockThresholdPx

        if (newCanLockState && !canLockState) {
            eventHandler.onKeyPressed(lockEvents, true)
        } else if (!newCanLockState && canLockState) {
            eventHandler.onKeyPressed(lockEvents, false)
        }

        canLockState = newCanLockState
    }

    /**
     * 背景区域缓存，在 touchModifier 中使用，由 JoystickWidgetRenderer 设置
     */
    internal var backgroundRegion: Region = Region()

    override fun Modifier.touchModifier(
        pointerEventBus: PointerEventBus,
        eventHandler: EventHandler,
        allLayers: List<ObservableControlLayer>,
        screenSize: IntSize,
        onOccupiedPointer: (PointerId) -> Unit,
        onReleasePointer: (PointerId) -> Unit
    ): Modifier = this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()

                // 按下事件
                event.changes
                    .filter { it.changedToDown() }
                    .forEach { change ->
                        val pointerId = change.id

                        if (activePointer == null) {
                            if (!pointerEventBus.checkOccupiedPointers(pointerId)) {
                                val pos = change.position
                                val defaultCenter = Offset(
                                    internalRenderSize.width / 2f,
                                    internalRenderSize.height / 2f
                                )
                                val bgRadius = minOf(
                                    internalRenderSize.width,
                                    internalRenderSize.height
                                ) / 2f

                                val isHit = if (triggerMode == JoystickTriggerMode.FREE) {
                                    val freeRadiusPx = bgRadius * freeRadiusRatio
                                    val activeAreaCenter = Offset(
                                        defaultCenter.x + (freeOffsetX / 10000f) * internalRenderSize.width,
                                        defaultCenter.y + (freeOffsetY / 10000f) * internalRenderSize.height
                                    )
                                    val dx = pos.x - activeAreaCenter.x
                                    val dy = pos.y - activeAreaCenter.y
                                    (dx * dx + dy * dy) <= (freeRadiusPx * freeRadiusPx)
                                } else {
                                    backgroundRegion.contains(pos.x.toInt(), pos.y.toInt())
                                }

                                // 命中检测
                                if (isHit) {
                                    change.consume()
                                    activePointer = pointerId
                                    isInteracting = true
                                    onOccupiedPointer(pointerId)
                                    lastDragPosition = pos

                                    // 如果当前锁定中，解锁
                                    if (isLocked) {
                                        isLocked = false
                                    }

                                    if (triggerMode == JoystickTriggerMode.FREE) {
                                        // 自由模式：将基座中心移动到手指触摸点
                                        val offsetFromDefault = pos - defaultCenter
                                        val maxMoveDistance = bgRadius * (freeRadiusRatio - 1f).coerceAtLeast(0f)
                                        val currentDistance = sqrt(offsetFromDefault.x * offsetFromDefault.x + offsetFromDefault.y * offsetFromDefault.y)
                                        baseOffset = if (currentDistance > maxMoveDistance && currentDistance > 0f) {
                                            offsetFromDefault * (maxMoveDistance / currentDistance)
                                        } else {
                                            offsetFromDefault
                                        }
                                        knobOffset = Offset.Zero
                                    } else if (triggerMode == JoystickTriggerMode.TOUCH) {
                                        // 触碰触发时立即更新摇杆状态
                                        updateJoystickState(
                                            position = pos,
                                            centerPoint = defaultCenter,
                                            deadZoneRadius = bgRadius * deadZoneRatio,
                                            lockThresholdPx = bgRadius * lockThreshold,
                                            eventHandler = eventHandler
                                        )
                                    }
                                }
                            }
                        }
                    }

                // 移动事件
                activePointer?.let { pointerId ->
                    event.changes
                        .firstOrNull { it.id == pointerId && it.positionChanged() && !it.isConsumed }
                        ?.let { moveChange ->
                            val localPos = moveChange.position
                            val defaultCenter = Offset(
                                internalRenderSize.width / 2f,
                                internalRenderSize.height / 2f
                            )
                            val bgRadius = minOf(internalRenderSize.width, internalRenderSize.height) / 2f
                            val deadZoneRadius = bgRadius * deadZoneRatio
                            val lockThresholdPx = bgRadius * lockThreshold

                            lastDragPosition = localPos

                            if (isLocked) isLocked = false

                            updateJoystickState(
                                position = localPos,
                                centerPoint = defaultCenter,
                                deadZoneRadius = deadZoneRadius,
                                lockThresholdPx = lockThresholdPx,
                                eventHandler = eventHandler
                            )

                            moveChange.consume()
                        }
                }

                // 释放事件
                event.changes
                    .filter { it.changedToUpIgnoreConsumed() }
                    .forEach { change ->
                        val pointerId = change.id
                        if (pointerId == activePointer) {
                            val defaultCenter = Offset(
                                internalRenderSize.width / 2f,
                                internalRenderSize.height / 2f
                            )
                            val bgRadius = minOf(internalRenderSize.width, internalRenderSize.height) / 2f
                            val deadZoneRadius = bgRadius * deadZoneRatio
                            val lockThresholdPx = bgRadius * lockThreshold

                            isInteracting = false

                            if (canLockState) {
                                isLocked = true
                                canLockState = false
                                val lockPosition = Offset(defaultCenter.x, 0f)
                                updateJoystickState(
                                    position = lockPosition,
                                    centerPoint = defaultCenter,
                                    deadZoneRadius = deadZoneRadius,
                                    lockThresholdPx = lockThresholdPx,
                                    eventHandler = eventHandler
                                )
                            } else {
                                canLockState = false
                                isLocked = false
                                baseOffset = Offset.Zero
                                updateJoystickState(
                                    position = defaultCenter,
                                    centerPoint = defaultCenter,
                                    deadZoneRadius = deadZoneRadius,
                                    lockThresholdPx = lockThresholdPx,
                                    eventHandler = eventHandler
                                )
                            }

                            activePointer = null
                            onReleasePointer(pointerId)
                        }
                    }
            }
        }
    }

    fun packJoystick(): JoystickData {
        return JoystickData(
            uuid = uuid,
            position = position,
            sizeType = sizeType,
            sizeDp = sizeDp,
            sizePercentage = sizePercentage,
            visibilityType = visibilityType,
            joystickStyleId = joystickStyleId,
            deadZoneRatio = deadZoneRatio,
            lockThreshold = lockThreshold,
            canLock = canLock,
            triggerMode = triggerMode,
            freeRadiusRatio = freeRadiusRatio,
            freeOffsetX = freeOffsetX,
            freeOffsetY = freeOffsetY,
            freeRestingAlpha = freeRestingAlpha,
            freeAnimationDurationMs = freeAnimationDurationMs,
            directionEvents = directionEvents,
            lockEvents = lockEvents
        )
    }

    companion object {
        fun Shape.toRegion(size: Size, density: Density, layoutDirection: LayoutDirection): Region {
            val outline: Outline = this.createOutline(size, layoutDirection, density)

            val composePath: Path = when (outline) {
                is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
                is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                is Outline.Generic -> outline.path
            }
            val androidPath = composePath.asAndroidPath()

            val region = Region()
            val rect = Rect(0, 0, size.width.toInt(), size.height.toInt())
            region.setPath(androidPath, Region(rect))
            return region
        }

        fun Offset.clampToRegion(region: Region, center: Offset): Offset {
            if (region.contains(x.toInt(), y.toInt())) return this

            var low = 0f
            var high = 1f
            var result = center
            repeat(10) {
                val mid = (low + high) / 2
                val testPoint = center + (this - center) * mid
                if (region.contains(testPoint.x.toInt(), testPoint.y.toInt())) {
                    result = testPoint
                    low = mid
                } else {
                    high = mid
                }
            }
            return result
        }

        fun calculateDirection(
            joystickPosition: Offset,
            backgroundCenter: Offset,
            deadZoneRadius: Float
        ): JoystickDirection {
            if (joystickPosition == backgroundCenter) {
                return JoystickDirection.None
            }

            val vector = joystickPosition - backgroundCenter
            val distance = sqrt(vector.x * vector.x + vector.y * vector.y)

            //如果距离小于死区半径，认为是无方向
            if (distance < deadZoneRadius) {
                return JoystickDirection.None
            }

            val angle = Math.toDegrees(atan2(vector.y.toDouble(), vector.x.toDouble())).toFloat()

            return when {
                angle >= -22.5f && angle < 22.5f -> JoystickDirection.East
                angle in 22.5f..<67.5f -> JoystickDirection.SouthEast
                angle in 67.5f..<112.5f -> JoystickDirection.South
                angle in 112.5f..<157.5f -> JoystickDirection.SouthWest
                angle >= 157.5f || angle < -157.5f -> JoystickDirection.West
                angle >= -157.5f && angle < -112.5f -> JoystickDirection.NorthWest
                angle >= -112.5f && angle < -67.5f -> JoystickDirection.North
                angle >= -67.5f && angle < -22.5f -> JoystickDirection.NorthEast
                else -> JoystickDirection.None
            }
        }
    }
}

fun ObservableJoystickData.cloneJoystick(): ObservableJoystickData {
    return ObservableJoystickData(packJoystick().cloneNew())
}

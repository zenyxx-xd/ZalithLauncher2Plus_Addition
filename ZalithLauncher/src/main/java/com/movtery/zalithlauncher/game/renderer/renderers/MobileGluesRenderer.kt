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

package com.movtery.zalithlauncher.game.renderer.renderers

import com.movtery.zalithlauncher.game.renderer.RendererInterface

object MobileGluesRenderer : RendererInterface {
    override fun getRendererId(): String = "mobileglues"

    override fun getUniqueIdentifier(): String = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"

    override fun getRendererName(): String {
        val version = runCatching {
            val jsonStr = com.movtery.zalithlauncher.context.GlobalContext.assets.open("mobileglues/metadata.json").bufferedReader().use { it.readText() }
            val obj = com.google.gson.JsonParser.parseString(jsonStr).asJsonObject
            obj.get("version")?.asString
        }.getOrNull() ?: "v2.0.0"

        return "MobileGlues $version"
    }

    override fun getMinMCVersion(): String = "1.17"

    override fun getRendererSummary(): String? = null

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy { emptyMap() }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libmobileglues.so"
}

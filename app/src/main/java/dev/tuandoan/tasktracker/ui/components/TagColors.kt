package dev.tuandoan.tasktracker.ui.components

import androidx.compose.ui.graphics.Color

object TagColors {

    val palette = listOf(
        TagColor("red", Color(0xFFEF5350), Color(0xFFFFEBEE), Color(0xFFB71C1C)),
        TagColor("orange", Color(0xFFFF7043), Color(0xFFFBE9E7), Color(0xFFBF360C)),
        TagColor("amber", Color(0xFFFFCA28), Color(0xFFFFF8E1), Color(0xFFF57F17)),
        TagColor("green", Color(0xFF66BB6A), Color(0xFFE8F5E9), Color(0xFF1B5E20)),
        TagColor("teal", Color(0xFF26A69A), Color(0xFFE0F2F1), Color(0xFF004D40)),
        TagColor("blue", Color(0xFF42A5F5), Color(0xFFE3F2FD), Color(0xFF0D47A1)),
        TagColor("indigo", Color(0xFF5C6BC0), Color(0xFFE8EAF6), Color(0xFF1A237E)),
        TagColor("purple", Color(0xFFAB47BC), Color(0xFFF3E5F5), Color(0xFF4A148C)),
        TagColor("pink", Color(0xFFEC407A), Color(0xFFFCE4EC), Color(0xFF880E4F)),
        TagColor("brown", Color(0xFF8D6E63), Color(0xFFEFEBE9), Color(0xFF3E2723)),
    )

    fun fromKey(key: String?): TagColor? = key?.let { k -> palette.find { it.key == k } }
}

data class TagColor(val key: String, val primary: Color, val container: Color, val onContainer: Color)

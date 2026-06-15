package com.example.ecozaschitnik.ai

import com.example.ecozaschitnik.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiRepository {

    suspend fun createReport(
        description: String,
        lat: Double,
        lon: Double
    ): String = withContext(Dispatchers.IO) {

        val prompt = """
На основе полученных данных создай краткий экологический отчёт.

📌 Описание места: $description
📍 Координаты: широта $lat, долгота $lon

Формат и стиль:
- Один абзац на 5–7 предложений
- Живой, человеческий язык без лишних подробностей
- Без обращений к читателю и без призывов к действию
- Без фраз "давайте", "мы должны", "я считаю", "нужно", "следует"
- Передай проблему и её последствия, завершив выводом о серьёзности ситуации
- Только цельный текст, без списков и маркеров
""".trimIndent()

        val response = OpenRouterClient.api.generateReport(
            auth = "Bearer ${BuildConfig.OPENROUTER_API_KEY}",
            body = ChatRequest(
                model = BuildConfig.LLM_MODEL,
                messages = listOf(Message("user", prompt)),
                max_tokens = 280,
            )
        )

        response.choices.firstOrNull()?.message?.content
            ?: "⚠️ ИИ не вернул ответ"
    }
}

package com.lolik612.luminagpt

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ChatMessage(val role: String, val content: String)

data class Settings(
    val serverUrl: String,
    val apiKey: String,
    val systemPrompt: String,
    val temperature: Float,
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("lumina", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        // 0 = без таймаута чтения: на слабом сервере первый токен может идти долго
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    val messages = mutableStateListOf<ChatMessage>()

    var generating by mutableStateOf(false)
        private set

    var settings by mutableStateOf(loadSettings())
        private set

    @Volatile
    private var currentCall: Call? = null

    @Volatile
    private var stopped = false
    private var job: Job? = null

    init {
        loadHistory()
    }

    // ---------- настройки ----------

    private fun loadSettings() = Settings(
        serverUrl = prefs.getString("url", "http://127.0.0.1:8080") ?: "",
        apiKey = prefs.getString("key", "") ?: "",
        systemPrompt = prefs.getString(
            "system",
            "Ты — LuminaGPT, дружелюбный и умный ИИ-помощник. Отвечай на языке пользователя, понятно и по делу."
        ) ?: "",
        temperature = prefs.getFloat("temp", 0.7f),
    )

    fun saveSettings(s: Settings) {
        settings = s
        prefs.edit()
            .putString("url", s.serverUrl.trim())
            .putString("key", s.apiKey.trim())
            .putString("system", s.systemPrompt)
            .putFloat("temp", s.temperature)
            .apply()
    }

    // ---------- история ----------

    private fun loadHistory() {
        val raw = prefs.getString("history", null) ?: return
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val role = o.getString("role")
                val content = o.getString("content")
                if (content.isNotBlank()) messages.add(ChatMessage(role, content))
            }
        } catch (_: Exception) {
        }
    }

    private fun saveHistory() {
        val arr = JSONArray()
        messages.forEach {
            if (it.content.isNotBlank()) {
                arr.put(JSONObject().put("role", it.role).put("content", it.content))
            }
        }
        prefs.edit().putString("history", arr.toString()).apply()
    }

    fun clear() {
        stop()
        messages.clear()
        saveHistory()
    }

    // ---------- отправка ----------

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || generating) return

        messages.add(ChatMessage("user", t))
        messages.add(ChatMessage("assistant", ""))
        generating = true
        stopped = false

        val history = messages.dropLast(1).toList()
        val s = settings

        job = viewModelScope.launch(Dispatchers.IO) {
            var error: String? = null
            try {
                stream(history, s)
            } catch (e: Exception) {
                if (!stopped) error = e.message ?: e.javaClass.simpleName
            }
            withContext(Dispatchers.Main) {
                if (error != null) {
                    val last = messages.lastOrNull()
                    val note = "⚠ Ошибка: $error"
                    if (last != null && last.role == "assistant") {
                        val text2 = if (last.content.isBlank()) note else last.content + "\n\n" + note
                        messages[messages.lastIndex] = ChatMessage("assistant", text2)
                    }
                }
                generating = false
                currentCall = null
                saveHistory()
            }
        }
    }

    fun stop() {
        stopped = true
        currentCall?.cancel()
    }

    private suspend fun stream(history: List<ChatMessage>, s: Settings) {
        val msgs = JSONArray()
        if (s.systemPrompt.isNotBlank()) {
            msgs.put(JSONObject().put("role", "system").put("content", s.systemPrompt))
        }
        history.forEach { msgs.put(JSONObject().put("role", it.role).put("content", it.content)) }

        val body = JSONObject()
            .put("model", "lumina")
            .put("messages", msgs)
            .put("stream", true)
            .put("temperature", s.temperature.toDouble())

        val base = s.serverUrl.trim().trimEnd('/')
        if (base.isEmpty()) throw IOException("не указан адрес сервера (настройки ⚙)")

        val rb = Request.Builder()
            .url("$base/v1/chat/completions")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
        if (s.apiKey.isNotBlank()) rb.header("Authorization", "Bearer ${s.apiKey.trim()}")

        val call = client.newCall(rb.build())
        currentCall = call

        call.execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code} ${resp.body?.string()?.take(300) ?: ""}")
            }
            val source = resp.body?.source() ?: throw IOException("пустой ответ сервера")
            val acc = StringBuilder()
            while (true) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") break
                val delta = try {
                    val d = JSONObject(data).getJSONArray("choices").getJSONObject(0).optJSONObject("delta")
                    if (d == null || d.isNull("content")) "" else d.getString("content")
                } catch (_: Exception) {
                    ""
                }
                if (delta.isNotEmpty()) {
                    acc.append(delta)
                    val snapshot = acc.toString()
                    withContext(Dispatchers.Main) {
                        if (messages.isNotEmpty()) {
                            messages[messages.lastIndex] = ChatMessage("assistant", snapshot)
                        }
                    }
                }
            }
        }
    }
}

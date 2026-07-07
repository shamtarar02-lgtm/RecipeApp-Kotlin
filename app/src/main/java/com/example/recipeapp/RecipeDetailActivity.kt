package com.example.recipeapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Locale

class RecipeDetailActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var recipe: Recipe? = null
    private var countDownTimer: CountDownTimer? = null
    private var servings = 1
    private var originalIngredients = ""
    private lateinit var recipeRepository: RecipeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        recipeRepository = RecipeRepository(this)
        recipe = intent.getSerializableExtra("RECIPE") as? Recipe

        val currentRecipe = recipe ?: run { finish(); return }

        originalIngredients = currentRecipe.ingredients

        // Toolbar back navigation
        val toolbar = findViewById<MaterialToolbar>(R.id.detailToolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // TTS
        tts = TextToSpeech(this, this)

        // Populate UI
        val detailImage = findViewById<ImageView>(R.id.detailImage)
        Glide.with(this)
            .load(currentRecipe.imageUrl)
            .transition(DrawableTransitionOptions.withCrossFade(400))
            .placeholder(R.drawable.ic_placeholder)
            .centerCrop()
            .into(detailImage)

        findViewById<TextView>(R.id.detailTitle).text = currentRecipe.title
        findViewById<TextView>(R.id.detailDesc).text = currentRecipe.desc
        findViewById<TextView>(R.id.detailCategory).text = currentRecipe.mainCategory
        findViewById<TextView>(R.id.detailCalories).text = "🔥 ${currentRecipe.calories}"
        findViewById<TextView>(R.id.detailIngredients).text = currentRecipe.ingredients
        findViewById<TextView>(R.id.detailSteps).text = currentRecipe.steps

        // Favorite FAB
        val fabFavorite = findViewById<FloatingActionButton>(R.id.fabFavorite)
        updateFavoriteIcon(fabFavorite, currentRecipe.isFavorite)

        fabFavorite.setOnClickListener {
            currentRecipe.isFavorite = !currentRecipe.isFavorite
            updateFavoriteIcon(fabFavorite, currentRecipe.isFavorite)
            recipeRepository.saveFavorite(currentRecipe.id, currentRecipe.isFavorite)
            val msg = if (currentRecipe.isFavorite) "Added to Favorites ❤️" else "Removed from Favorites"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        // Share
        findViewById<Button>(R.id.btnShare).setOnClickListener {
            val shareText = buildString {
                append("🍽️ ${currentRecipe.title}\n")
                append("🔥 Calories: ${currentRecipe.calories}\n\n")
                append("📝 Ingredients:\n${currentRecipe.ingredients}\n\n")
                append("👨‍🍳 Steps:\n${currentRecipe.steps}\n\n")
                append("Shared via Recipe App 📱")
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, currentRecipe.title)
            }
            startActivity(Intent.createChooser(intent, "Share Recipe via"))
        }

        // TTS Listen
        findViewById<Button>(R.id.btnListen).setOnClickListener {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            } else {
                val textToRead = "Recipe: ${currentRecipe.title}. " +
                        "Calories: ${currentRecipe.calories}. " +
                        "Ingredients: ${currentRecipe.ingredients.replace("•", "")}. " +
                        "Steps: ${currentRecipe.steps}"
                tts?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "recipe_tts")
            }
        }

        // Servings Calculator
        val tvServings = findViewById<TextView>(R.id.tvServings)
        tvServings.text = servings.toString()

        findViewById<Button>(R.id.btnPlusServing).setOnClickListener {
            servings++
            tvServings.text = servings.toString()
            updateIngredients()
        }
        findViewById<Button>(R.id.btnMinusServing).setOnClickListener {
            if (servings > 1) {
                servings--
                tvServings.text = servings.toString()
                updateIngredients()
            }
        }

        // Timer
        val etTimerMinutes = findViewById<EditText>(R.id.etTimerMinutes)
        val tvTimerDisplay = findViewById<TextView>(R.id.tvTimerDisplay)

        findViewById<Button>(R.id.btnStartTimer).setOnClickListener {
            val minsStr = etTimerMinutes.text.toString()
            if (minsStr.isNotEmpty()) {
                val mins = minsStr.toLongOrNull() ?: return@setOnClickListener
                if (mins > 0) startTimer(mins, tvTimerDisplay)
            } else {
                Toast.makeText(this, "Please enter minutes", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnStopTimer).setOnClickListener {
            countDownTimer?.cancel()
            tvTimerDisplay.text = "00:00"
            Toast.makeText(this, "Timer Stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFavoriteIcon(fab: FloatingActionButton, isFavorite: Boolean) {
        val iconRes = if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline
        fab.setImageResource(iconRes)
        fab.backgroundTintList = if (isFavorite)
            getColorStateList(R.color.red_favorite)
        else
            getColorStateList(R.color.white)
    }

    private fun updateIngredients() {
        if (originalIngredients.isEmpty()) return
        val regex = "(\\d+([./]\\d+)?)".toRegex()
        val updatedText = originalIngredients.lines().joinToString("\n") { line ->
            line.replace(regex) { match ->
                val num = match.value.toDoubleOrNull()
                if (num != null) {
                    val total = num * servings
                    if (total == total.toInt().toDouble()) total.toInt().toString()
                    else "%.1f".format(total)
                } else match.value
            }
        }
        findViewById<TextView>(R.id.detailIngredients).text = updatedText
    }

    private fun startTimer(minutes: Long, display: TextView) {
        countDownTimer?.cancel()
        Toast.makeText(this, "Timer started for $minutes minutes!", Toast.LENGTH_SHORT).show()
        countDownTimer = object : CountDownTimer(minutes * 60_000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val mins = millisUntilFinished / 60_000
                val secs = (millisUntilFinished % 60_000) / 1000
                display.text = String.format(Locale.US, "%02d:%02d", mins, secs)
            }
            override fun onFinish() {
                display.text = "00:00"
                Toast.makeText(this@RecipeDetailActivity, "⏰ Timer Done! Enjoy your meal!", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        countDownTimer?.cancel()
        super.onDestroy()
    }
}
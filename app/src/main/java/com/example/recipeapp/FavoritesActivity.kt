package com.example.recipeapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar

class FavoritesActivity : AppCompatActivity() {

    private lateinit var adapter: RecipeAdapter
    private lateinit var recipeRepository: RecipeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        recipeRepository = RecipeRepository(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.favoritesToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val recyclerView = findViewById<RecyclerView>(R.id.rvFavorites)
        val tvEmpty = findViewById<TextView>(R.id.tvNoFavorites)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        adapter = RecipeAdapter { recipe ->
            recipeRepository.saveFavorite(recipe.id, recipe.isFavorite)
        }
        recyclerView.adapter = adapter

        recipeRepository.getRemoteRecipes { recipes ->
            runOnUiThread {
                val favoriteIds = recipeRepository.getFavorites()
                val favorites = recipes.filter { it.id in favoriteIds }
                    .onEach { it.isFavorite = true }

                adapter.submitList(favorites)
                tvEmpty.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}
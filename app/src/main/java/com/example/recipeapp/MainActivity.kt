package com.example.recipeapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: RecipeAdapter
    private lateinit var recipeRepository: RecipeRepository
    private var fullRecipeList: List<Recipe> = emptyList()

    private var selectedCategory = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.rvRecipes)
        val searchBar = findViewById<TextInputEditText>(R.id.searchBar)
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroup)
        val loadingBar = findViewById<ProgressBar>(R.id.loadingBar)
        val fab = findViewById<ExtendedFloatingActionButton>(R.id.fabSurprise)

        recyclerView.layoutManager = GridLayoutManager(this, 2)

        recipeRepository = RecipeRepository(this)

        adapter = RecipeAdapter { recipe ->
            recipeRepository.saveFavorite(recipe.id, recipe.isFavorite)
        }
        recyclerView.adapter = adapter

        // ✅ Load categories dynamically from API
        loadCategoriesFromApi(chipGroup, searchBar, loadingBar)

        // 🔥 Load all recipes on start
        loadAllRecipes(loadingBar)

        // 🔍 Search listener
        searchBar.addTextChangedListener { text ->
            filterAndUpdate(text.toString())
        }

        // 🎲 Surprise Me
        fab.setOnClickListener {
            val filtered = getCurrentFilteredList(searchBar.text.toString())
            if (filtered.isNotEmpty()) {
                val random = filtered.random()
                val intent = Intent(this, RecipeDetailActivity::class.java)
                intent.putExtra("RECIPE", random)
                startActivity(intent)
            }
        }

        // ❤️ Favorites screen
        findViewById<View>(R.id.btnFavoritesScreen)?.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
    }

    // =========================================
    // ✅ DYNAMIC CATEGORIES FROM API
    // =========================================
    private fun loadCategoriesFromApi(
        chipGroup: ChipGroup,
        searchBar: TextInputEditText,
        loadingBar: ProgressBar
    ) {
        recipeRepository.getCategories { categories ->
            runOnUiThread {
                // XML mein jo bhi chips hain — sab clear karo
                chipGroup.removeAllViews()

                // "All" chip — pehle add karo, checked by default
                val allChip = createChip("All", isChecked = true)
                chipGroup.addView(allChip)

                // API se aayi categories add karo
                categories.forEach { categoryName ->
                    val chip = createChip(categoryName, isChecked = false)
                    chipGroup.addView(chip)
                }

                // Category selection listener
                chipGroup.setOnCheckedChangeListener { group, checkedId ->
                    val chip = group.findViewById<Chip>(checkedId)
                    selectedCategory = chip?.text?.toString()?.trim() ?: "All"

                    loadingBar.visibility = View.VISIBLE

                    if (selectedCategory.equals("All", ignoreCase = true)) {
                        loadAllRecipes(loadingBar)
                    } else {
                        recipeRepository.getFullRecipesByCategory(selectedCategory) { recipes ->
                            runOnUiThread {
                                loadingBar.visibility = View.GONE
                                fullRecipeList = recipes
                                filterAndUpdate(searchBar.text.toString())
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================
    // 🔧 CHIP FACTORY — Consistent styling
    // =========================================
    private fun createChip(label: String, isChecked: Boolean): Chip {
        return Chip(this).apply {
            text = label
            isCheckable = true
            isCheckedIconVisible = false
            isClickable = true
            isFocusable = true
            this.isChecked = isChecked
        }
    }

    // =========================================
    // 🔥 LOAD ALL RECIPES
    // =========================================
    private fun loadAllRecipes(loadingBar: ProgressBar) {
        loadingBar.visibility = View.VISIBLE
        recipeRepository.getRemoteRecipes { recipes ->
            runOnUiThread {
                loadingBar.visibility = View.GONE
                fullRecipeList = recipes
                filterAndUpdate("")
            }
        }
    }

    // =========================================
    // 🔍 FILTER + UPDATE UI
    // =========================================
    private fun filterAndUpdate(query: String) {
        val filtered = getCurrentFilteredList(query)
        adapter.submitList(filtered)

        val emptyStateContainer = findViewById<View>(R.id.tvEmpty)
        emptyStateContainer.visibility =
            if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    // =========================================
    // 🔎 SEARCH + CATEGORY FILTER
    // =========================================
    private fun getCurrentFilteredList(query: String): List<Recipe> {
        val q = query.lowercase().trim()
        val cat = selectedCategory.lowercase().trim()

        return fullRecipeList.filter { recipe ->
            val searchMatch =
                q.isEmpty() ||
                        recipe.title.lowercase().contains(q) ||
                        recipe.desc.lowercase().contains(q) ||
                        recipe.ingredients.lowercase().contains(q)

            val categoryMatch =
                cat == "all" ||
                        recipe.mainCategory.trim().lowercase() == cat

            searchMatch && categoryMatch
        }
    }

    override fun onResume() {
        super.onResume()
        val searchBar = findViewById<TextInputEditText>(R.id.searchBar)
        filterAndUpdate(searchBar.text.toString())
    }
}
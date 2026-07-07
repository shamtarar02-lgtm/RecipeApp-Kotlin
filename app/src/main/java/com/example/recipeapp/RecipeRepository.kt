package com.example.recipeapp

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RecipeRepository(private val context: Context) {

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("RECIPE_PREFS", Context.MODE_PRIVATE)

    // =========================================
    // 🍽️ GET RECIPES BY CATEGORY (FIXED)
    // filter.php sirf 3 fields deta hai (id, name, image)
    // isliye har meal ki full detail lookup.php se fetch karo
    // =========================================
    fun getFullRecipesByCategory(category: String, onResult: (List<Recipe>) -> Unit) {

        Thread {
            try {
                val cleanCategory = category.trim()

                // Step 1: Category ke meals ki list lo (sirf IDs milti hain)
                val filterUrl = URL("https://www.themealdb.com/api/json/v1/1/filter.php?c=$cleanCategory")
                val filterConn = filterUrl.openConnection() as HttpURLConnection
                filterConn.connectTimeout = 10000
                filterConn.readTimeout = 10000
                val filterResponse = filterConn.inputStream.bufferedReader().readText()

                val filterJson = JSONObject(filterResponse)
                val mealsArray = filterJson.optJSONArray("meals")

                if (mealsArray == null) {
                    onResult(emptyList())
                    return@Thread
                }

                // Step 2: Har meal ID ke liye lookup.php se full detail fetch karo
                val list = mutableListOf<Recipe>()

                for (i in 0 until mealsArray.length()) {
                    val mealId = mealsArray.getJSONObject(i).optString("idMeal")

                    try {
                        val detailUrl = URL("https://www.themealdb.com/api/json/v1/1/lookup.php?i=$mealId")
                        val detailConn = detailUrl.openConnection() as HttpURLConnection
                        detailConn.connectTimeout = 8000
                        detailConn.readTimeout = 8000
                        val detailResponse = detailConn.inputStream.bufferedReader().readText()

                        val detailJson = JSONObject(detailResponse)
                        val detailMeals = detailJson.optJSONArray("meals") ?: continue
                        val meal = detailMeals.getJSONObject(0)

                        val ingredients = mutableListOf<String>()
                        for (j in 1..20) {
                            val ing = meal.optString("strIngredient$j")
                            val measure = meal.optString("strMeasure$j")
                            if (!ing.isNullOrBlank() && ing != "null") {
                                val entry = if (!measure.isNullOrBlank() && measure != "null")
                                    "$measure $ing".trim() else ing
                                ingredients.add(entry)
                            }
                        }

                        val instructions = meal.optString("strInstructions")
                        val stepsList = instructions
                            .split("\r\n", "\n")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }

                        val recipe = Recipe(
                            id = meal.optString("idMeal"),
                            title = meal.optString("strMeal"),
                            mainCategory = meal.optString("strCategory"),
                            imageUrl = meal.optString("strMealThumb"),
                            desc = instructions,
                            ingredientsList = ingredients,
                            instructionsList = stepsList
                        )

                        recipe.isFavorite = sharedPrefs.getBoolean(recipe.id, false)
                        list.add(recipe)

                    } catch (e: Exception) {
                        e.printStackTrace()
                        // ek meal fail ho to baaki continue karo
                    }
                }

                onResult(list)

            } catch (e: Exception) {
                e.printStackTrace()
                onResult(emptyList())
            }
        }.start()
    }

    // =========================================
    // 🌐 GET ALL RECIPES
    // =========================================
    fun getRemoteRecipes(onResult: (List<Recipe>) -> Unit) {

        Thread {
            try {
                val url = URL("https://www.themealdb.com/api/json/v1/1/search.php?s=")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val response = connection.inputStream.bufferedReader().readText()

                val jsonObject = JSONObject(response)
                val mealsArray = jsonObject.optJSONArray("meals")

                if (mealsArray == null) {
                    onResult(emptyList())
                    return@Thread
                }

                val list = mutableListOf<Recipe>()

                for (i in 0 until mealsArray.length()) {
                    val item = mealsArray.getJSONObject(i)

                    val ingredients = mutableListOf<String>()
                    for (j in 1..20) {
                        val ing = item.optString("strIngredient$j")
                        val measure = item.optString("strMeasure$j")
                        if (!ing.isNullOrBlank() && ing != "null") {
                            val entry = if (!measure.isNullOrBlank() && measure != "null")
                                "$measure $ing".trim() else ing
                            ingredients.add(entry)
                        }
                    }

                    val instructions = item.optString("strInstructions")
                    val stepsList = instructions
                        .split("\r\n", "\n")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    val recipe = Recipe(
                        id = item.optString("idMeal"),
                        title = item.optString("strMeal"),
                        mainCategory = item.optString("strCategory"),
                        imageUrl = item.optString("strMealThumb"),
                        desc = instructions,
                        ingredientsList = ingredients,
                        instructionsList = stepsList
                    )

                    recipe.isFavorite = sharedPrefs.getBoolean(recipe.id, false)
                    list.add(recipe)
                }

                onResult(list)

            } catch (e: Exception) {
                e.printStackTrace()
                onResult(emptyList())
            }
        }.start()
    }

    // =========================================
    // 📂 GET CATEGORIES FROM API
    // =========================================
    fun getCategories(onResult: (List<String>) -> Unit) {

        Thread {
            try {
                val url = URL("https://www.themealdb.com/api/json/v1/1/list.php?c=list")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val response = connection.inputStream.bufferedReader().readText()

                val jsonObject = JSONObject(response)
                val array = jsonObject.getJSONArray("meals")

                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getJSONObject(i).getString("strCategory"))
                }

                onResult(list)

            } catch (e: Exception) {
                e.printStackTrace()
                onResult(emptyList())
            }
        }.start()
    }

    // =========================================
    // ❤️ FAVORITES
    // =========================================
    fun saveFavorite(recipeId: String, isFavorite: Boolean) {
        sharedPrefs.edit().putBoolean(recipeId, isFavorite).apply()
    }

    fun getFavorites(): List<String> {
        return sharedPrefs.all
            .filter { it.value == true }
            .map { it.key }
    }
}
package com.example.recipeapp

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Recipe(
    @SerializedName("id")
    var id: String = "",

    @SerializedName("name")
    var title: String = "",

    @SerializedName("category")
    var mainCategory: String = "All",

    @SerializedName("image")
    var imageUrl: String = "",

    @SerializedName("time")
    var desc: String = "",

    @SerializedName("ingredients")
    var ingredientsList: List<String> = emptyList(),

    @SerializedName("instructions")
    var instructionsList: List<String> = emptyList(),

    var subCategory: String = "All",
    var calories: String = "350 kcal", // Default value
    var isFavorite: Boolean = false
) : Serializable {
    // UI ke liye formatted strings
    val ingredients: String
        get() = ingredientsList.joinToString("\n") { "• $it" }

    val steps: String
        get() = instructionsList.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n\n")
}
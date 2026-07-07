package com.example.recipeapp

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class RecipeAdapter(
    private val onFavoriteToggle: (Recipe) -> Unit
) : ListAdapter<Recipe, RecipeAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

    inner class RecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.recipeCard)
        val titleText: TextView = view.findViewById(R.id.recipeTitle)
        val descText: TextView = view.findViewById(R.id.recipeDesc)
        val categoryBadge: TextView = view.findViewById(R.id.recipeCategoryBadge)
        val recipeImage: ImageView = view.findViewById(R.id.recipeImage)
        val btnFavorite: MaterialButton = view.findViewById(R.id.btnFavorite)

        fun bind(recipe: Recipe) {
            titleText.text = recipe.title
            descText.text = recipe.desc
            categoryBadge.text = recipe.mainCategory

            Glide.with(itemView.context)
                .load(recipe.imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade(300))
                .placeholder(R.drawable.ic_placeholder)
                .error(android.R.drawable.ic_menu_gallery)
                .error(R.drawable.ic_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(recipeImage)

            updateFavoriteIcon(recipe.isFavorite)

            btnFavorite.setOnClickListener {
                recipe.isFavorite = !recipe.isFavorite
                updateFavoriteIcon(recipe.isFavorite)
                onFavoriteToggle(recipe)
            }

            card.setOnClickListener {
                val context: Context = itemView.context
                val intent = Intent(context, RecipeDetailActivity::class.java)
                intent.putExtra("RECIPE", recipe)
                context.startActivity(intent)
            }
        }

        private fun updateFavoriteIcon(isFavorite: Boolean) {
            if (isFavorite) {
                btnFavorite.setIconResource(R.drawable.ic_favorite_filled)
                btnFavorite.setIconTintResource(R.color.red_favorite)
            } else {
                btnFavorite.setIconResource(R.drawable.ic_favorite_outline)
                btnFavorite.setIconTintResource(R.color.gray_icon)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // DiffUtil - only redraws items that actually changed (better performance)
    class RecipeDiffCallback : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe) =
            oldItem.title == newItem.title &&
                    oldItem.isFavorite == newItem.isFavorite &&
                    oldItem.imageUrl == newItem.imageUrl
    }
}
package com.equipo.atrapame.presentation.score

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.equipo.atrapame.data.models.Score
import com.equipo.atrapame.databinding.ItemScoreBinding

class ScoreAdapter : ListAdapter<Score, ScoreAdapter.ScoreViewHolder>(ScoreDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val binding = ItemScoreBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return ScoreViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }
    
    class ScoreViewHolder(private val binding: ItemScoreBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(score: Score, position: Int) {
            binding.tvPosition.text = "#$position"
            binding.tvPlayerName.text = score.playerName
            binding.tvMoves.text = "${score.moves} movimientos"
            binding.tvTime.text = score.getFormattedTime()
            binding.tvDifficulty.text = score.difficulty.displayName
        }
    }
    
    private class ScoreDiffCallback : DiffUtil.ItemCallback<Score>() {
        override fun areItemsTheSame(oldItem: Score, newItem: Score): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Score, newItem: Score): Boolean {
            return oldItem == newItem
        }
    }
}
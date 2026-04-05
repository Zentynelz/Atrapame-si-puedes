package com.equipo.atrapame.presentation.score

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.equipo.atrapame.data.models.Score
import com.equipo.atrapame.databinding.ItemScoreBinding

class ScoreAdapter(
    private val isInteractive: Boolean = false,
    private val onSurveyClick: (Score) -> Unit = {}
) : ListAdapter<Score, ScoreAdapter.ScoreViewHolder>(ScoreDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val binding = ItemScoreBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return ScoreViewHolder(binding, isInteractive, onSurveyClick)
    }
    
    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }
    
    class ScoreViewHolder(
        private val binding: ItemScoreBinding,
        private val isInteractive: Boolean,
        private val onSurveyClick: (Score) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(score: Score, position: Int) {
            binding.tvPosition.text = "#$position"
            binding.tvPlayerName.text = score.playerName
            binding.tvMoves.text = "${score.moves} movimientos"
            binding.tvTime.text = score.getFormattedTime()
            binding.tvDifficulty.text = score.difficulty.displayName

            // New Telemetry Binding
            val smilePct = (score.avgSmilingProb * 100).toInt()
            val eyePct = (score.avgRightEyeOpenProb * 100).toInt()
            binding.tvTelemetry.text = "Ojos: $eyePct% | Sonrisa: $smilePct% | Audio: ${score.maxAudioAmplitude}"

            if (score.perceivedStressScore > 0) {
                binding.tvSurvey.text = "Estrés Percibido: ${score.perceivedStressScore}/10"
                binding.btnSurvey.visibility = android.view.View.GONE
            } else {
                binding.tvSurvey.text = "Estrés Percibido: N/A"
                if (isInteractive) {
                    binding.btnSurvey.visibility = android.view.View.VISIBLE
                    binding.btnSurvey.setOnClickListener {
                        onSurveyClick(score)
                    }
                } else {
                    binding.btnSurvey.visibility = android.view.View.GONE
                }
            }
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
package com.pal.ussd.ui.progress

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pal.ussd.R
import com.pal.ussd.data.model.StepState
import com.pal.ussd.data.model.StepStatus
import com.pal.ussd.databinding.ItemStepBinding

class StepsAdapter : ListAdapter<StepStatus, StepsAdapter.StepVH>(DiffCb()) {

    inner class StepVH(private val b: ItemStepBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: StepStatus) {
            b.tvLabel.text = item.label

            when (item.state) {
                StepState.PENDING -> {
                    b.tvIcon.text = "○"
                    b.tvIcon.setTextColor(ContextCompat.getColor(b.root.context, R.color.step_pending))
                    b.tvState.text = "في الانتظار"
                    b.tvState.setTextColor(ContextCompat.getColor(b.root.context, R.color.step_pending))
                }
                StepState.RUNNING -> {
                    b.tvIcon.text = "⏳"
                    b.tvIcon.setTextColor(ContextCompat.getColor(b.root.context, R.color.step_running))
                    b.tvState.text = "جاري التنفيذ…"
                    b.tvState.setTextColor(ContextCompat.getColor(b.root.context, R.color.step_running))
                }
                StepState.DONE -> {
                    b.tvIcon.text = "✅"
                    b.tvIcon.setTextColor(ContextCompat.getColor(b.root.context, R.color.step_done))
                    b.tvState.text = "تم"
                    b.tvState.setTextColor(ContextCompat.getColor(b.root.context, R.color.step_done))
                }
                StepState.FAILED -> {
                    b.tvIcon.text = "❌"
                    b.tvIcon.setTextColor(ContextCompat.getColor(b.root.context, R.color.step_failed))
                    b.tvState.text = "فشل"
                    b.tvState.setTextColor(ContextCompat.getColor(b.root.context, R.color.step_failed))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepVH {
        val binding = ItemStepBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StepVH(binding)
    }

    override fun onBindViewHolder(holder: StepVH, position: Int) = holder.bind(getItem(position))

    class DiffCb : DiffUtil.ItemCallback<StepStatus>() {
        override fun areItemsTheSame(o: StepStatus, n: StepStatus) = o.index == n.index
        override fun areContentsTheSame(o: StepStatus, n: StepStatus) = o == n
    }
}

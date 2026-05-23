package com.runner.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.runner.databinding.FragmentSecondBinding

class HistoryFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mockRuns = listOf(
            RunActivity("1", "May 14, 2026", "42:17", "6.2 km", "6:49", emptyList()),
            RunActivity("2", "May 12, 2026", "31:04", "4.8 km", "6:28", emptyList()),
            RunActivity("3", "May 10, 2026", "58:33", "9.1 km", "6:26", emptyList()),
            RunActivity("4", "May 7, 2026",  "22:45", "3.5 km", "6:30", emptyList()),
            RunActivity("5", "May 5, 2026",  "45:12", "7.0 km", "6:27", emptyList()),
            RunActivity("6", "May 3, 2026",  "35:50", "5.5 km", "6:31", emptyList())
        )

        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = HistoryAdapter(mockRuns)
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

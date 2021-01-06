package com.konovalovk.serviceassistant

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.random.Random

class TestFragment: Fragment(R.layout.fragment_test) {
    val rvAdapter = RecyclerAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<RecyclerView>(R.id.rvMainArUI).run {
            layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.HORIZONTAL, false)
            adapter = rvAdapter
        }
    }

    override fun onStart() {
        super.onStart()
        val ports = mutableListOf<Port>()
        for(x in 1..24){
            ports.add(Port(x.toString(), Random.nextInt(0,2) == 0))
        }
        rvAdapter.ports = ports
    }
}
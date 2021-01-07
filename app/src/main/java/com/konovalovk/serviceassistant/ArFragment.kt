package com.konovalovk.serviceassistant

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.Observer
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.ux.ArFragment


class ArFragment: ArFragment() {
    private val viewModel = ArFragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.initAr(this)
        initListeners()
    }

    private fun initListeners() {
        val onUpdateListener = Scene.OnUpdateListener {
            viewModel.findBarcode(this@ArFragment, planeDiscoveryController)
            viewModel.checkArImage(arSceneView?.arFrame)
        }

        arSceneView?.scene?.addOnUpdateListener(onUpdateListener)

        setOnTapArPlaneListener { hitResult: HitResult, _: Plane?, motionEvent: MotionEvent? ->
            if (motionEvent?.action == MotionEvent.ACTION_UP)
                viewModel.onPlaneTap(hitResult.createAnchor())
        }

        //arSceneView?.scene?.removeOnUpdateListener()
        viewModel.rvAdapter.portClicked.observe(viewLifecycleOwner,
                Observer {
                    viewModel.initAndShowDetailARView(it ?: return@Observer, requireContext())
                }
        )
    }

    override fun getSessionConfiguration(session: Session?) = Config(session).apply {
        instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP // Set the Instant Placement mode.
        focusMode = Config.FocusMode.AUTO
        augmentedImageDatabase = viewModel.createArImageDB(session, requireActivity().assets)
    }
}
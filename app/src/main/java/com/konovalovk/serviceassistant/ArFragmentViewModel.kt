package com.konovalovk.serviceassistant

import android.app.AlertDialog
import android.content.Context
import android.graphics.PointF
import android.media.Image
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.Surface
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.lifecycle.ViewModel
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.TransformableNode
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class ArFragmentViewModel: ViewModel() {
    val TAG = "ArFragmentViewModel"
    var parentNode = AnchorNode()
    var viewRenderable: ViewRenderable? = null
    var hitAnchor: Anchor? = null
    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    val scanner = BarcodeScanning.getClient(options)

    fun initRenderable(context: Context) {

        initArrow(context)
    }

    private fun initArrow(context: Context) {
        val arrowViewSize = 200
        val arrowRedDownLinearLayout = ScrollView(context).apply {
            val ll = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            addView(ll)
            layoutParams = ViewGroup.LayoutParams(200, 200)

            for (x in 0..10) {
                ll.addView(
                    ImageView(context).apply { setImageResource(R.drawable.ic_launcher_background) },
                    arrowViewSize,
                    arrowViewSize
                )
            }
        }

        ViewRenderable
            .builder()
            .setView(context, ImageView(context).apply { setImageResource(R.drawable.ic_launcher_background) })
            .build()
            .thenAccept { renderable ->
                viewRenderable = renderable
                viewRenderable?.apply {
                    isShadowCaster = false
                    isShadowReceiver = false
                }
            }
            .exceptionally {
                AlertDialog.Builder(context).run {
                    setMessage(it.message)
                    setTitle("Error")
                    create()
                    show()
                }
                return@exceptionally null
            }
    }

    fun onTap(hitResult: HitResult, arFragment: ArFragment) {
        hitAnchor = hitAnchor?.apply { return } ?: hitResult.createAnchor()
        val transformableNode = initTransformableNode(arFragment)

        parentNode = AnchorNode(hitAnchor).apply {
            setParent(arFragment.arSceneView?.scene)
            addChild(transformableNode)
        }
    }

    private fun initTransformableNode(arFragment: ArFragment) = TransformableNode(arFragment.transformationSystem).apply {
        renderable = viewRenderable
        scaleController.isEnabled = true
        scaleController.minScale = 0.01f
        scaleController.maxScale = 0.2f
        translationController.isEnabled = false
        rotationController.isEnabled = true

        //Fix rotation
        /*val parallelToPlane = Quaternion.axisAngle(Vector3(1f,0f,0f), 90f)
        val fixTopAndBottom = Quaternion.axisAngle(Vector3(0f,0f,1f), 180f)
        val finalTransform = Quaternion.multiply(parallelToPlane, fixTopAndBottom)
        localRotation = finalTransform*/
    }

    fun findBarcode(image: Image, arFragment: ArFragment, context: Context) {
        hitAnchor?.run { return }
        val inputImage = InputImage.fromByteBuffer(
            image.planes[0].buffer,
            image.width,
            image.height,
            Surface.ROTATION_0,
            InputImage.IMAGE_FORMAT_NV21
        )
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    barcodes.forEach {
                        val bb = it.boundingBox
                        Log.d(TAG, "Barcodes: ${it.rawValue}")
                    }
                    val dispayMetrics = DisplayMetrics()
                    context.display?.getRealMetrics(dispayMetrics)
                    val centerPoint = PointF(dispayMetrics.widthPixels / 2f, dispayMetrics.heightPixels / 2f)
                    arFragment.arSceneView?.arFrame?.hitTestInstantPlacement(centerPoint.x, centerPoint.y, 2f)?.run {
                        if (isNotEmpty()) {
                            onTap(get(0), arFragment)
                            //Disable Instant Placement mode
                            //arFragment.arSceneView.session?.configure(Config(arFragment.arSceneView.session).apply { instantPlacementMode = Config.InstantPlacementMode.DISABLED })
                            //disableInstantPlacement()
                        }
                    }
                }


                // Task completed successfully
                // ...
            }
            .addOnFailureListener {
                Log.e(TAG,"Error during process frame", it)
                // Task failed with an exception
                // ...
            }

        image.close()
    }

}
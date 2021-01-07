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
import android.widget.TextView
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.PlaneDiscoveryController
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlin.random.Random

class ArFragmentViewModel: ViewModel() {
    private val TAG = "ArFragmentViewModel"

    private var hitAnchor: Anchor? = null
    private var parentNode = AnchorNode()
    var mainUINode: TransformableNode? = null
    var detailsNode: TransformableNode? = null
    var transformationSystem: TransformationSystem? = null
    var mainUIRenderable: ViewRenderable? = null
    var detailsRenderable: ViewRenderable? = null

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    private val scanner = BarcodeScanning.getClient(options)

    val rvAdapter = RecyclerAdapter()

    fun initRenderable(context: Context) {
        initArUI(context)
    }

    private fun initArUI(context: Context) {
        ViewRenderable
                .builder()
                .setView(context, initMainUI(context))
                .build()
                .thenAccept { renderable ->
                    mainUIRenderable = renderable
                    mainUIRenderable?.apply {
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

    fun initMainUI(context: Context) = RecyclerView(context).apply {
        layoutManager = GridLayoutManager(context, 2, GridLayoutManager.HORIZONTAL, false)
        adapter = rvAdapter
        shuffleRVwithFakeData()
    }

    private fun shuffleRVwithFakeData(){
        rvAdapter.ports = mutableListOf<Port>().apply {
            for (x in 1..24) {
                val isEnabled = Random.nextInt(0, 2) == 0
                add(Port(
                        number = x.toString(),
                        isEnabled = isEnabled,
                        client = if (isEnabled) Client(ip = generateRandomIP()) else null))
            }
        }.toList()
    }

    private fun generateRandomIP() = "${Random.nextInt(0, 255)}.${Random.nextInt(0, 255)}.${Random.nextInt(0, 255)}.${Random.nextInt(0, 255)}"

    fun initAndShowDetailARView(port: Port, context: Context){
        ViewRenderable
                .builder()
                .setView(context, initDetailView(context, port))
                .build()
                .thenAccept { renderable ->
                    detailsRenderable = renderable
                    detailsRenderable?.apply {
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

        detailsNode = initTransformableNode(transformationSystem ?: return, detailsRenderable ?: return).apply {
            localPosition = mainUINode?.localPosition?.let { Vector3(it.x, it.y, it.z + 0.1f) } //z + 10cm
            scaleController.minScale = 0.2f
            scaleController.maxScale = 0.5f
            rotationController.isEnabled = false
        }

        parentNode.run {
            addChild(detailsNode)
        }
    }

    private fun initDetailView(context: Context, port: Port, viewSize: Int = 200) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        background = context.resources.getDrawable(R.color.white)
        gravity = Gravity.CENTER
        setPadding(8, 8, 8, 8)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, viewSize)

        var clientInfo = ""
        clientInfo += "Port: ${port.number}"
        clientInfo += port.client?.let { "\nIP: ${it.ip}\nName: ${it.name}\nNumContract:${it.numContract}" }

        addView(TextView(context).apply { text = clientInfo })
        addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_close)
            layoutParams = ViewGroup.LayoutParams(50, 50)
            setOnClickListener {
                shuffleRVwithFakeData()

                parentNode.removeChild(detailsNode)
            }
        })
    }

    fun findBarcode(arFragment: ArFragment, planeDiscoveryController: PlaneDiscoveryController) {
        hitAnchor?.run { return } //if anchor already placed drop fun

        val image: Image = try { arFragment.arSceneView?.arFrame?.acquireCameraImage() ?: return }
        catch (e: Exception) { return }

        val inputImage = InputImage.fromByteBuffer(image.planes[0].buffer, image.width, image.height, Surface.ROTATION_0, InputImage.IMAGE_FORMAT_NV21)
        scanner.process(inputImage)
            .addOnSuccessListener { processBarcodes(it, arFragment, planeDiscoveryController) }
            .addOnFailureListener { Log.e(TAG,"Error during process frame", it) }

        image.close()
    }

    private fun processBarcodes(barcodes: MutableList<Barcode>, arFragment: ArFragment, planeDiscoveryController: PlaneDiscoveryController) {
        if (barcodes.isNotEmpty()) {
            barcodes.forEach {
                //val bb = it.boundingBox
                Log.d(TAG, "Barcodes: ${it.rawValue}")
            }
            placeAnchorInCenterOfDisplay(arFragment, planeDiscoveryController)
        }
    }

    private fun placeAnchorInCenterOfDisplay(arFragment: ArFragment, planeDiscoveryController: PlaneDiscoveryController){
        val dispayMetrics = DisplayMetrics()
        arFragment.context?.display?.getRealMetrics(dispayMetrics)
        val centerPoint = PointF(dispayMetrics.widthPixels / 2f, dispayMetrics.heightPixels / 2f)
        arFragment.arSceneView?.arFrame?.hitTestInstantPlacement(centerPoint.x, centerPoint.y, 2f)?.run {
            if (isNotEmpty()) {
                onPlaneTap(get(0), arFragment)
                planeDiscoveryController.hide()
                //Disable Instant Placement mode
                //arFragment.arSceneView.session?.configure(Config(arFragment.arSceneView.session).apply { instantPlacementMode = Config.InstantPlacementMode.DISABLED })
                //disableInstantPlacement()
            }
        }
    }

    fun onPlaneTap(hitResult: HitResult, arFragment: ArFragment) {
        hitAnchor = hitAnchor?.apply { return } ?: hitResult.createAnchor()
        transformationSystem = transformationSystem ?: arFragment.transformationSystem
        mainUINode = initTransformableNode(transformationSystem ?: return, mainUIRenderable ?: return)

        parentNode = AnchorNode(hitAnchor).apply {
            setParent(arFragment.arSceneView?.scene)
            addChild(mainUINode)
        }
    }

    private fun initTransformableNode(transformationSystem: TransformationSystem, viewRenderable: ViewRenderable) = TransformableNode(transformationSystem).apply {
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

}
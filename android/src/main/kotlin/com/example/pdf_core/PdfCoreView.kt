package com.example.pdf_core

import android.R.attr.x
import android.R.attr.y
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer.Page.*
import android.os.ParcelFileDescriptor
import android.provider.Telephony.Mms.Part.FILENAME
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.shockwave.pdfium.PdfiumCore
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.platform.PlatformView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class PdfCoreView internal constructor(context: Context, messenger: BinaryMessenger, id: Int, args: Any?) : PlatformView, MethodCallHandler, OnLoadCompleteListener, OnPageChangeListener {

    //private val imageView: ImageView
    private val methodChannel: MethodChannel

    private var emptyView: View = View(context)
    private var numberOfPages: Int = 0
    private lateinit var pdfView: PDFView

    override fun getView(): View {
        return emptyView
    }

    init {

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.image_layout, null)
        pdfView = view.findViewById<PDFView>(R.id.pdfView)


        // Set client so that you can interact within WebView
        methodChannel = MethodChannel(messenger, "plugins.maticla/pdf_core_$id")
        // Init methodCall Listener
        methodChannel.setMethodCallHandler(this)

        val pdfBytes = args as ByteArray

//        val file: File = File(context.cacheDir, FILENAME)
//        if (!file.exists()) {
//            var asset: InputStream? = null
//            try {
//                asset = context.resources.openRawResource(R.raw.ilovepdf_merged)
//                var output: FileOutputStream? = null
//                output = FileOutputStream(file)
//                val buffer = ByteArray(1024)
//                var size: Int
//                while (asset.read(buffer).also { size = it } != -1) {
//                    output.write(buffer, 0, size)
//                }
//                asset.close()
//                output.close()
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }
//        val fdx = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

        val core = PdfiumCore(context)
        val doc = core.newDocument(pdfBytes)

        pdfView.useBestQuality(true)
        pdfView.maxZoom = 5.0F
        com.github.barteksc.pdfviewer.util.Constants.THUMBNAIL_RATIO = 1f
        com.github.barteksc.pdfviewer.util.Constants.PART_SIZE = 600f
        pdfView.fromBytes(pdfBytes)
                .onLoad(this)
                .onPageChange(this)
                .pageFitPolicy(FitPolicy.BOTH)
                .swipeHorizontal(true)
                .pageSnap(true)
                .pageFling(true)
                .fitEachPage(true)
                .enableSwipe(pdfView.zoom <= 1.0)
                .enableDoubletap(true)
                .enableAntialiasing(true)
                .onTap { e ->
                    // View to Document conversion
                    // https://github.com/barteksc/AndroidPdfViewer/issues/829#issuecomment-538114262

                    val offsetX: Float = pdfView.currentXOffset
                    val offsetY: Float = pdfView.currentYOffset
                    val zoom = pdfView.zoom

                    val zoomX = (e.x - offsetX) / zoom
                    var zoomY = (e.y - offsetY) / zoom
                    val spacing: Int = pdfView.spacingPx

                    var page = 0
                    for (i in 0 until pdfView.pageCount) {
                        if (zoomY < pdfView.getPageSize(i).height) {
                            page = i
                            break
                        } else {
                            zoomY -= pdfView.getPageSize(i).height + spacing
                        }
                    }

                    core.openPage(doc, pdfView.currentPage)

                    val pageWidth = core.getPageWidthPoint(doc, pdfView.currentPage)
                    val pageHeight = core.getPageHeightPoint(doc, pdfView.currentPage)

                    val viewWidth: Float = pdfView.getPageSize(page).width
                    val viewHeight: Float = pdfView.getPageSize(page).height

                    val resultX = (zoomX / viewWidth * pageWidth) - (pdfView.currentPage * pageWidth)
                    val resultY = zoomY / viewHeight * pageHeight

                    Log.d("COREPDFREADER", "X: $resultX, Y: $resultY")

                    methodChannel.invokeMethod("tap", floatArrayOf(resultX, resultY))

                    true
                }
                .load()

        emptyView = view
    }

    override fun onMethodCall(methodCall: MethodCall, result: MethodChannel.Result) {
        when (methodCall.method) {
            //"setUrl" -> setText(methodCall, result)
            "getPageCount" -> result.success(numberOfPages)
            "jumpToPage" -> jumpToPage(methodCall, result)
            else -> result.notImplemented()
        }
    }

    private fun jumpToPage(methodCall: MethodCall, result: MethodChannel.Result) {
        val page = methodCall.arguments as Int
        pdfView.jumpTo(page)
    }

//    // set and load new Url
//    private fun setText(methodCall: MethodCall, result: MethodChannel.Result ) {
//        val url = methodCall.arguments as String
//        webView.loadUrl(url)
//        result.success(null)
//    }

    // Destroy WebView when PlatformView is destroyed
    override fun dispose() {
    }

    override fun loadComplete(nbPages: Int) {
        Log.d("COREPDF", "Pdf loading loadComplete()")
        numberOfPages = nbPages
        methodChannel.invokeMethod("isReady", true)
    }

    override fun onPageChanged(page: Int, pageCount: Int) {
        Log.d("COREPDF", "Pdf onPageChanged()")
        methodChannel.invokeMethod("pageChanged", page)
    }
}
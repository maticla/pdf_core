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


fun getScreenWidth(): Int {
    return Resources.getSystem().displayMetrics.widthPixels
}

fun getScreenHeight(): Int {
    return Resources.getSystem().displayMetrics.heightPixels
}

class PdfCoreView internal constructor(context: Context, messenger: BinaryMessenger, id: Int) : PlatformView, MethodCallHandler {

    private val webView: WebView
    //private val imageView: ImageView
    private val methodChannel: MethodChannel

    private var emptyView: View = View(context)

    override fun getView(): View {
        return emptyView
    }

    init {
        webView = WebView(context)

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.image_layout, null)
        val pdfView = view.findViewById<PDFView>(R.id.pdfView)

        // Set client so that you can interact within WebView
        webView.webViewClient = WebViewClient()
        methodChannel = MethodChannel(messenger, "plugins.maticla/pdf_core_$id")
        // Init methodCall Listener
        methodChannel.setMethodCallHandler(this)

        val file: File = File(context.cacheDir, FILENAME)
        if (!file.exists()) {
            var asset: InputStream? = null
            try {
                asset = context.resources.openRawResource(R.raw.ilovepdf_merged)
                var output: FileOutputStream? = null
                output = FileOutputStream(file)
                val buffer = ByteArray(1024)
                var size: Int
                while (asset.read(buffer).also { size = it } != -1) {
                    output.write(buffer, 0, size)
                }
                asset.close()
                output.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        val fdx = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

        val core = PdfiumCore(context)
        val doc = core.newDocument(fdx)

        pdfView.useBestQuality(true)
        pdfView.maxZoom = 5.0F
        com.github.barteksc.pdfviewer.util.Constants.THUMBNAIL_RATIO = 1f
        com.github.barteksc.pdfviewer.util.Constants.PART_SIZE = 600f
        pdfView.fromFile(file)
                .pageFitPolicy(FitPolicy.WIDTH)
                .swipeHorizontal(true)
                .pageSnap(true)
                .pageFling(true)
                .fitEachPage(true)
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

                    true
                }
                .load()

        emptyView = view
    }

//    init {
//        val inflater = LayoutInflater.from(context)
//        val view = inflater.inflate(R.layout.image_layout, null)
//        // Init WebView
//        webView = WebView(context)
//        imageView = ImageView(context)
//        //val bmap = generateImageBitmap()
//        //imageView.setImageBitmap(bmap)
//        //emptyView = imageView
//        emptyView = view
//        val pv = PhotoView(context)
//
//        // Set client so that you can interact within WebView
//        webView.webViewClient = WebViewClient()
//        methodChannel = MethodChannel(messenger, "plugins.maticla/pdf_core_$id")
//        // Init methodCall Listener
//        methodChannel.setMethodCallHandler(this)
//
//        Log.d("PARCELFD_BEFORE", "Before executing all fd stuff")
//
//
//        val resources = context.resources
//        val afd = resources.openRawResourceFd(R.raw.ilovepdf_merged)
//        val parcelFileDescriptor = afd.parcelFileDescriptor
//
//        Log.d("PARCELFD", parcelFileDescriptor.toString())
//
//        // this is needed because pdfrenderer is sayign that pdf is corrupted for sm reason
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
//
//
//
//
//        val fdx = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
//        val pdfRenderer: PdfRenderer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            PdfRenderer(fdx)
//        } else {
//            TODO("VERSION.SDK_INT < LOLLIPOP")
//        }
//
//        val pageCount = pdfRenderer.pageCount
//
//        Log.d("PARCELFD", "PDF Page Count: $pageCount")
//
//        val page = pdfRenderer.openPage(0)
//        val pageWidth = page.width
//        val pageHeight = page.height
//        val res = context.resources
//        val densityDpi = res.displayMetrics.densityDpi
//        val DPI = 72
//        val bitmap = newWhiteBitmap(pageWidth, pageHeight)
//        page.render(bitmap, null, null, RENDER_MODE_FOR_DISPLAY)
//        imageView.setImageBitmap(bitmap)
//
//        Log.d("PARCELFD", "Bitmap Rendering done.")
////        // Get screen dimensions
////        val screenWidth = getScreenWidth()
////        val screenHeight = getScreenHeight()
////
////        // Parse the asset File
////        val input = ParcelFileDescriptor.open(File(filePath), ParcelFileDescriptor.MODE_READ_ONLY)
////
////        // Create instance of PdfRenderer
////        val renderer = PdfRenderer(input)
////
////        // Open page 0 and convert to Bitmap
////        val page = renderer.openPage(0)
////        val bitmap = Bitmap.createBitmap(screenWidth)
//
//    }

    fun newWhiteBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        return bitmap
    }

//    fun generateImageBitmap(): Bitmap {
//        val width = 200
//        val height = 200
//        val color = Color.RED // Replace with your desired color
//
//        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(bitmap)
//        canvas.drawColor(color)
//
//        return bitmap
//    }

    override fun onMethodCall(methodCall: MethodCall, result: MethodChannel.Result) {
        when (methodCall.method) {
            "setUrl" -> setText(methodCall, result)
            else -> result.notImplemented()
        }
    }

    // set and load new Url
    private fun setText(methodCall: MethodCall, result: MethodChannel.Result ) {
        val url = methodCall.arguments as String
        webView.loadUrl(url)
        result.success(null)
    }

    // Destroy WebView when PlatformView is destroyed
    override fun dispose() {
        webView.destroy()
    }
}
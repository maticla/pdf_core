package com.example.pdf_core

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.shockwave.pdfium.PdfiumCore
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.platform.PlatformView

import java.io.File
import java.io.FileOutputStream


class PdfCoreView internal constructor(context: Context, messenger: BinaryMessenger, id: Int, args: Any?) : PlatformView, MethodCallHandler, OnLoadCompleteListener, OnPageChangeListener {

    //private val imageView: ImageView
    private val methodChannel: MethodChannel

    private var emptyView: View = View(context)
    private var numberOfPages: Int = 0
    private var pdfView: PDFView
    private val cacheDir: File

    override fun getView(): View {
        return emptyView
    }

    init {

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.image_layout, null)
        pdfView = view.findViewById<PDFView>(R.id.pdfView)

        PDFBoxResourceLoader.init(context)

        // Set client so that you can interact within PdfCore
        methodChannel = MethodChannel(messenger, "plugins.maticla/pdf_core_$id")
        // Init methodCall Listener
        methodChannel.setMethodCallHandler(this)

        val pdfFilePaths = args as List<String>

        cacheDir = context.cacheDir

        val mergedPdfFile = mergePdf(pdfFilePaths)

        val core = PdfiumCore(context)

        val doc = core.newDocument(mergedPdfFile.readBytes())

        pdfView.useBestQuality(true)
        pdfView.maxZoom = 5.0F
        com.github.barteksc.pdfviewer.util.Constants.THUMBNAIL_RATIO = 1f
        com.github.barteksc.pdfviewer.util.Constants.PART_SIZE = 600f
        pdfView.fromFile(mergedPdfFile)
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

                    val topLeftX = (zoomX / viewWidth * pageWidth) - (pdfView.currentPage * pageWidth)
                    val topLeftY = zoomY / viewHeight * pageHeight

                    val bottomRightX = topLeftX + pageWidth
                    val bottomRightY = topLeftY + pageHeight

                    Log.d("COREPDF - COORDINATES 1", "X1: $topLeftX, Y1: $topLeftY")
                    Log.d("COREPDF - COORDINATES 2", "X2: $bottomRightX, Y2: $bottomRightY")

                    methodChannel.invokeMethod("tap", floatArrayOf(topLeftX, topLeftY, bottomRightX, bottomRightY))

                    true
                }
                .load()

        emptyView = view
    }

    private fun mergePdf(paths: List<String>): File {
        val ut = PDFMergerUtility()

        val fileList = mutableListOf<File>()

        paths.forEach {
            val temp = File(it)
            fileList.add(temp)
        }

        fileList.forEach {
            ut.addSource(it)
        }

        val file: File = File(cacheDir.path, "DELO" + ".pdf")
        val fileOutputStream = FileOutputStream(file)


        fileOutputStream.use { fileOutputStream ->
            ut.destinationStream = fileOutputStream
            ut.mergeDocuments(MemoryUsageSetting.setupTempFileOnly())
        }
        fileOutputStream.close()
        Log.d("PDFMERGER", "merging done")
        return file
    }

    override fun onMethodCall(methodCall: MethodCall, result: MethodChannel.Result) {
        when (methodCall.method) {
            "getPageCount" -> result.success(numberOfPages)
            "jumpToPage" -> jumpToPage(methodCall, result)
            else -> result.notImplemented()
        }
    }

    private fun jumpToPage(methodCall: MethodCall, result: MethodChannel.Result) {
        val page = methodCall.arguments as Int
        pdfView.jumpTo(page)
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

    override fun dispose() {
        // TODO: Handle dispose if needed
    }
}
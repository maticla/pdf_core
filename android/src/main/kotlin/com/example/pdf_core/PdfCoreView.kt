package com.example.pdf_core


import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.pdf_core.R.*
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.shockwave.pdfium.PdfiumCore
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.platform.PlatformView
import java.io.File


class PdfCoreView internal constructor(context: Context, messenger: BinaryMessenger, id: Int, args: Any?) : PlatformView, MethodCallHandler/*, OnLoadCompleteListener, OnPageChangeListener*/ {

    //private val imageView: ImageView
    private val methodChannel: MethodChannel

    private var emptyView: View = View(context)
    private var numberOfPages: Int = 0
    private var pdfView: PDFView
    private val cacheDir: File
    private lateinit var adapter: PDFViewPagerAdapter
    private lateinit var pager: ViewPager
    private lateinit var pdfFilePaths: List<String>

    override fun getView(): View {
        return emptyView
    }

    init {

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(layout.image_layout, null)
        pdfView = view.findViewById(R.id.pdfView)

        // Set client so that you can interact within PdfCore
        methodChannel = MethodChannel(messenger, "plugins.maticla/pdf_core_$id")
        // Init methodCall Listener
        methodChannel.setMethodCallHandler(this)

        pdfFilePaths = args as List<String>

        val core = PdfiumCore(context)

        val view2 = inflater.inflate(layout.pager_layout, null)
        pager = view2.findViewById<ViewPager>(R.id.myViewPager)
        val filesFromPaths: MutableList<File> = mutableListOf()
        pdfFilePaths.forEach {
            filesFromPaths.add(File(it))
        }
        val pdfFiles = filesFromPaths // replace with your initial PDF files
        adapter = PDFViewPagerAdapter(pdfFiles, core, methodChannel)
        pager.adapter = adapter

        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                Log.d("Pager listener", "onPageScrolled $position")
                methodChannel.invokeMethod("pageChanged", position)
            }

            override fun onPageSelected(position: Int) {
                Log.d("Pager listener", "onPageSelected")
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        cacheDir = context.cacheDir

        methodChannel.invokeMethod("isReady", true)

        emptyView = view2
    }

    override fun onMethodCall(methodCall: MethodCall, result: MethodChannel.Result) {
        when (methodCall.method) {
            "getPageCount" -> result.success(this.adapter.count)
            "jumpToPage" -> jumpToPage(methodCall, result)
            "appendFiles" -> appendFiles(methodCall, result)
            else -> result.notImplemented()
        }
    }

    private fun jumpToPage(methodCall: MethodCall, result: MethodChannel.Result) {
        Log.d("JUMP", "Jump to page called")
        val page = methodCall.arguments as Int
        pager.setCurrentItem(page, false)
    }

    private fun appendFiles(methodCall: MethodCall, result: MethodChannel.Result) {
        val filePaths = methodCall.arguments as List<String>
        val filesFromPaths: MutableList<File> = mutableListOf()
        filePaths.forEach {
            filesFromPaths.add(File(it))
        }
        adapter.appendFiles(filesFromPaths)
    }

    override fun dispose() {

    }
}

class PDFViewPagerAdapter(private val pdfFiles: MutableList<File>, private val core: PdfiumCore, private val methodChannel: MethodChannel) : PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater = LayoutInflater.from(container.context)
        val layout = inflater.inflate(layout.image_layout, null)
        val pdfView = layout.findViewById<PDFView>(id.pdfView)
        createPDFConfigurator(pdfView, position).load()
        container.addView(layout)
        return layout // layout is actually a View
    }

    override fun getCount(): Int {
        return pdfFiles.count()
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    fun appendFiles(files: MutableList<File>) {
        Log.d("ADD_PDF_FILE", "Added pdf files")
        pdfFiles.addAll(files)
        Log.d("ADD_PDF_FILE", "length is ${pdfFiles.count()}")
        notifyDataSetChanged()
    }

    private fun createPDFConfigurator(pdfView: PDFView, position: Int): PDFView.Configurator {
        val doc = core.newDocument(pdfFiles[position].readBytes())
        pdfView.useBestQuality(false)
        pdfView.minZoom = 1.0F
        pdfView.midZoom = 3.35F

        pdfView.maxZoom = 50.0F
        com.github.barteksc.pdfviewer.util.Constants.THUMBNAIL_RATIO = 0.3f
        com.github.barteksc.pdfviewer.util.Constants.PART_SIZE = 400f
        com.github.barteksc.pdfviewer.util.Constants.PRELOAD_OFFSET = 0

        return pdfView.fromFile(pdfFiles[position])
                .pageFitPolicy(FitPolicy.BOTH)
                .fitEachPage(true)
                .enableDoubletap(true)
                .enableAntialiasing(false)
                .swipeHorizontal(true)
                .onTap { e ->
                    Log.d("PDF", "TAP $e")
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
                .onLoad {
                    // do nothing for now
                }
    }
}
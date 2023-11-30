//
//  PDFCoreView.swift
//  pdf_core
//
//  Created by Matic on 16/08/2023.
//

import Foundation
import Flutter
import UIKit
import PDFKit

class PDFCoreView: NSObject, FlutterPlatformView {
    private var _pdfCoreView: UIView
    private var _methodChannel: FlutterMethodChannel
    
    func view() -> UIView {
        return _pdfCoreView
    }
    
    init(
        frame: CGRect,
        viewIdentifier viewId: Int64,
        arguments args: Any?,
        binaryMessenger messenger: FlutterBinaryMessenger
    ) {
        
        //print("Native view got arguments from Flutter. Value is: \(args)")
        //let bts = args as! FlutterStandardTypedData
        let bts = args as! Array<String>
        
        let pdfCoreViewController = PDFCoreViewController()
        //        pdfCoreViewController.pdfBytes = bts.data
        
        pdfCoreViewController.paths = bts
        _pdfCoreView = pdfCoreViewController.view
        _methodChannel = FlutterMethodChannel(name: "plugins.maticla/pdf_core_\(viewId)", binaryMessenger: messenger)
        pdfCoreViewController.mChannel = _methodChannel
        super.init()
        //_methodChannel.setMethodCallHandler(onMethodCall)
    }
}

class PDFCoreViewController: UIViewController, UIGestureRecognizerDelegate {
    
    var pdfView: CustomPDFViewSubclass = CustomPDFViewSubclass()
    var pdfBytes: Data?
    var paths: Array<String>?
    var isReady: Bool = false
    
    var mChannel: FlutterMethodChannel? {
        didSet {
            mChannel!.setMethodCallHandler(onMethodCall)
            pdfView.mChannel = mChannel!
        }
    }
    
    func onMethodCall(call: FlutterMethodCall, result: FlutterResult) {
        switch(call.method) {
        case "getPlatformVersion":
            result("iOS " + UIDevice.current.systemVersion)
        case "getPageCount":
            result(pdfView.document?.pageCount)
        case "jumpToPage":
            jumpToPage(call: call, result: result)
        default:
            result(FlutterMethodNotImplemented)
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        pdfView.translatesAutoresizingMaskIntoConstraints = false
        self.view.addSubview(pdfView)
        
        pdfView.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor).isActive = true
        pdfView.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor).isActive = true
        pdfView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor).isActive = true
        pdfView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor).isActive = true
        
        pdfView.frame = self.view.bounds
        
        removeScrollIndicators()
        
        DispatchQueue.global().async {
            let finalMergedPdf = self.createPdfFromFilePaths(paths: self.paths!)
            DispatchQueue.main.async {
                self.pdfView.document = finalMergedPdf
                self.pdfView.displayMode = .singlePage
                self.pdfView.displayDirection = .horizontal
                self.pdfView.usePageViewController(true)
                self.pdfView.backgroundColor = .white
                self.pdfView.minScaleFactor = 1.0
                self.pdfView.minScaleFactor = self.pdfView.scaleFactorForSizeToFit
                self.pdfView.autoScales = true
                self.isReady = true
                self.mChannel!.invokeMethod("isReady", arguments: true)
                print("PDFCore - Native - Done Rendering")
                NotificationCenter.default.addObserver(
                    self,
                    selector: #selector(self.handlePageChange(notification:)),
                    name: Notification.Name.PDFViewPageChanged,
                    object: self.pdfView)
            }
        }
    }
    
    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        removeScrollIndicators()
    }
    
    func removeScrollIndicators() {
        for view in pdfView.subviews {
            if let scrollView = findUIScrollView(of: view) {
                // print("Yep")
                scrollView.showsVerticalScrollIndicator = false
                scrollView.showsHorizontalScrollIndicator = false
            }
        }
    }
    
    private func findUIScrollView(of uiView: UIView) -> UIScrollView? {
        if let scrollView = uiView as? UIScrollView {
            return scrollView
        }
        
        for view in uiView.subviews {
            if let scrollView = view as? UIScrollView {
                return scrollView
            }
            
            if !view.subviews.isEmpty {
                return findUIScrollView(of: view)
            }
        }
        return nil
    }
    
    
    @objc private func handlePageChange(notification: Notification) {
        let currentPageIndex = pdfView.document!.index(for: pdfView.currentPage!)
        print("PDFCore - Native - Page changed! Current page index: \(currentPageIndex)")
        self.mChannel!.invokeMethod("pageChanged", arguments: currentPageIndex)
    }
    
    func jumpToPage(call: FlutterMethodCall, result: FlutterResult) {
        let index = call.arguments as! Int
        if let page = self.pdfView.document?.page(at: index) {
            self.pdfView.go(to: page)
        }
    }
    
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        return true
    }
    
    func createPdfFromFilePaths(paths: Array<String>) -> PDFDocument {
        var sourcePDFDocuments: [PDFDocument] = []
        let combinedPDFDocument = PDFDocument()
        
        for path in paths {
            if let pdfDocument = PDFDocument(url: URL(fileURLWithPath: path)) {
                sourcePDFDocuments.append(pdfDocument)
            }
        }
        
        for sourceDocument in sourcePDFDocuments {
            for pageIndex in 0..<sourceDocument.pageCount {
                if let page = sourceDocument.page(at: pageIndex) {
                    combinedPDFDocument.insert(page, at: combinedPDFDocument.pageCount)
                }
            }
        }
        return combinedPDFDocument
    }
    
}


class CustomPDFViewSubclass: PDFView {
    
    private var customTapGestureRecognizer: UITapGestureRecognizer!
    private var doubleTapGestureRecognizer: UITapGestureRecognizer!
    
    var mChannel: FlutterMethodChannel?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupCustomGestureRecognizers()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupCustomGestureRecognizers()
    }
    
    private func setupCustomGestureRecognizers() {
        customTapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(handleCustomTap(_:)))
        doubleTapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(handleDoubleTap(_:)))
        doubleTapGestureRecognizer.numberOfTapsRequired = 2
        customTapGestureRecognizer.require(toFail: doubleTapGestureRecognizer)
        addGestureRecognizer(customTapGestureRecognizer)
        addGestureRecognizer(doubleTapGestureRecognizer)
    }

    @objc private func handleDoubleTap(_ gestureRecognizer: UITapGestureRecognizer) {
        // Handle the double tap gesture here        
        if self.scaleFactor < self.maxScaleFactor {
            self.scaleFactor *= 2.0
        } else {
            self.scaleFactor = self.minScaleFactor
        }
    }
    
    @objc private func handleCustomTap(_ gestureRecognizer: UITapGestureRecognizer) {
        // Handle the custom tap gesture here
        //print("Handling custom tap!")
        if gestureRecognizer.state == .ended {
            let tapLocation = gestureRecognizer.location(in: self)
            let tapLocationInPDF = convert(tapLocation, to: currentPage!)
            
            // Invert the Y-coordinate to match the top-left origin
            let invertedY = currentPage!.bounds(for: .mediaBox).height - tapLocationInPDF.y
            let tapLocationInTopLeftOrigin = CGPoint(x: tapLocationInPDF.x, y: invertedY)
            
//            print(tapLocation)
//            print(tapLocationInPDF)
//            print(tapLocationInTopLeftOrigin)
            
            let topLeftX = tapLocationInTopLeftOrigin.x
            let topLeftY = tapLocationInTopLeftOrigin.y
            
            let width = currentPage!.bounds(for: .mediaBox).width
            let height = currentPage!.bounds(for: .mediaBox).height
            
            let bottomRightX = topLeftX + width
            let bottomRightY = topLeftY + height
            
            print("x1: \(topLeftX), y1: \(topLeftY)")
            print("x2: \(bottomRightX), y2: \(bottomRightY)")
            
            mChannel?.invokeMethod("tap", arguments: [topLeftX, topLeftY, bottomRightX, bottomRightY])
        }
    }
}

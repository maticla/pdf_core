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
    private var _nativeWebView: UIWebView
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
        
        print("Imamo argumente! Vrednost je: \(args)")
        let bts = args as! FlutterStandardTypedData
        
        let x = Data(bts.data)
        
        let pdfCoreViewController = PDFCoreViewController()
        pdfCoreViewController.pdfBytes = bts.data
        
        _pdfCoreView = pdfCoreViewController.view
        _nativeWebView = UIWebView()
        _methodChannel = FlutterMethodChannel(name: "plugins.maticla/pdf_core_\(viewId)", binaryMessenger: messenger)
        pdfCoreViewController.mChannel = _methodChannel
        super.init()
        //_methodChannel.setMethodCallHandler(onMethodCall)
        
    }
    
//    func onMethodCall(call: FlutterMethodCall, result: FlutterResult) {
//        switch(call.method) {
//        case "setUrl":
//            setText(call:call, result:result)
//        case "getPlatformVersion":
//            result("iOS " + UIDevice.current.systemVersion)
//        default:
//            result(FlutterMethodNotImplemented)
//        }
//    }
//
//    func setText(call: FlutterMethodCall, result: FlutterResult){
//        let url = call.arguments as! String
//        _nativeWebView.loadRequest(NSURLRequest(url: NSURL(string: url)! as URL) as URLRequest)
//    }
    
}

class PDFCoreViewController: UIViewController, UIGestureRecognizerDelegate {
    
    var pdfView: CustomPDFViewSubclass = CustomPDFViewSubclass()
    var pdfBytes: Data?
    var isReady: Bool = false
    
    var mChannel: FlutterMethodChannel? {
        didSet {
            mChannel!.setMethodCallHandler(onMethodCall)
            pdfView.mChannel = mChannel!
        }
    }
    
    func onMethodCall(call: FlutterMethodCall, result: FlutterResult) {
        switch(call.method) {
        case "setUrl":
            print("Setting url")
            result(FlutterMethodNotImplemented)
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

        DispatchQueue.global().async {
            //let temp = PDFDocument(url: URL(string: "https://www.cambridgeenglish.org/latinamerica/images/165873-yle-sample-papers-flyers-vol-1.pdf")!)
            let temp = PDFDocument(data: self.pdfBytes!)
            DispatchQueue.main.async {
                self.pdfView.document = temp
                self.pdfView.displayMode = .singlePage
                self.pdfView.displayDirection = .horizontal
                self.pdfView.usePageViewController(true)
                self.pdfView.backgroundColor = .white
                self.pdfView.minScaleFactor = 1.0
                self.pdfView.minScaleFactor = self.pdfView.scaleFactorForSizeToFit
                self.pdfView.autoScales = true
                self.isReady = true
                self.mChannel!.invokeMethod("isReady", arguments: true)
                print("done rendering")
                NotificationCenter.default.addObserver(
                      self,
                      selector: #selector(self.handlePageChange(notification:)),
                      name: Notification.Name.PDFViewPageChanged,
                      object: self.pdfView)
            }
        }
    }
    
    @objc private func handlePageChange(notification: Notification) {
        let currentPageIndex = pdfView.document!.index(for: pdfView.currentPage!)
        print("Page changed! Current page index: \(currentPageIndex)")
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
    
}


class CustomPDFViewSubclass: PDFView {
    
    private var customTapGestureRecognizer: UITapGestureRecognizer!
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
        addGestureRecognizer(customTapGestureRecognizer)
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
            
            print(tapLocation)
            print(tapLocationInPDF)
            print(tapLocationInTopLeftOrigin)
            
            let actualX = tapLocationInTopLeftOrigin.x
            let actualY = tapLocationInTopLeftOrigin.y
            
            mChannel?.invokeMethod("tap", arguments: [actualX, actualY])
        }
    }
}

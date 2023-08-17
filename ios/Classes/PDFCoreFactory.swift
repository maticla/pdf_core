//
//  PDFCoreFactory.swift
//  pdf_core
//
//  Created by Matic on 16/08/2023.
//

import Foundation
import Flutter
import UIKit

class PDFCoreFactory: NSObject, FlutterPlatformViewFactory {
    private var messenger: FlutterBinaryMessenger
    
    init(messenger: FlutterBinaryMessenger) {
        self.messenger = messenger
        super.init()
    }
    
    func create(
        withFrame frame: CGRect,
        viewIdentifier viewId: Int64,
        arguments args: Any?
    ) -> FlutterPlatformView {
        
        

        
        return PDFCoreView(
            frame: frame,
            viewIdentifier: viewId,
            arguments: args,
            binaryMessenger: messenger)
    }
    
    public func createArgsCodec() -> FlutterMessageCodec & NSObjectProtocol {
          return FlutterStandardMessageCodec.sharedInstance()
    }
}

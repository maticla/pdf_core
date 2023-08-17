import Flutter
import UIKit

//public class PdfCorePlugin: NSObject, FlutterPlugin {
//  public static func register(with registrar: FlutterPluginRegistrar) {
//    let channel = FlutterMethodChannel(name: "pdf_core", binaryMessenger: registrar.messenger())
//    let instance = PdfCorePlugin()
//    registrar.addMethodCallDelegate(instance, channel: channel)
//  }
//
//  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
//    switch call.method {
//    case "getPlatformVersion":
//      result("iOS " + UIDevice.current.systemVersion)
//    default:
//      result(FlutterMethodNotImplemented)
//    }
//  }
//}


public class PdfCorePlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        registrar.register(PDFCoreFactory(messenger: registrar.messenger()), withId: "plugins.maticla/pdf_core")
       }
}

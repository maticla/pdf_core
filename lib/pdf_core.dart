// ignore_for_file: avoid_print

import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'pdf_core_platform_interface.dart';

class PDFCore {
  Future<String?> getPlatformVersion() {
    return PdfCorePlatform.instance.getPlatformVersion();
  }
}

typedef FlutterPDFCoreEngineCreatedCallback = void Function(PDFCoreController controller);

class PDFCoreEngine extends StatelessWidget {
  final FlutterPDFCoreEngineCreatedCallback onNativeViewCreated;
  final Uint8List? pdfBytes;
  const PDFCoreEngine({
    Key? key,
    required this.onNativeViewCreated,
    this.pdfBytes,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return AndroidView(
          viewType: 'plugins.maticla/pdf_core',
          onPlatformViewCreated: _onPlatformViewCreated,
          creationParams: pdfBytes,
          creationParamsCodec: const StandardMessageCodec(),
        );
      case TargetPlatform.iOS:
        return UiKitView(
          viewType: 'plugins.maticla/pdf_core',
          onPlatformViewCreated: _onPlatformViewCreated,
          creationParams: pdfBytes,
          creationParamsCodec: const StandardMessageCodec(),
        );
      default:
        return Text('$defaultTargetPlatform is not yet supported by the pdf_core plugin');
    }
  }

  // Callback method when platform view is created
  void _onPlatformViewCreated(int id) => onNativeViewCreated(PDFCoreController._(id));
}

// PDFCoreController class to communicate with native PDF
class PDFCoreController {
  PDFCoreController._(int id) : _channel = MethodChannel('plugins.maticla/pdf_core_$id');

  final MethodChannel _channel;

  Future<String> getPlatformVersion() async {
    return await _channel.invokeMethod('getPlatformVersion');
  }

  Future<void> isReady() async {
    final completer = Completer<void>();

    _channel.setMethodCallHandler((call) async {
      if (call.method == 'isReady') {
        print('Flutter: PDFCoreController: isReady');
        completer.complete();
      }
    });

    await completer.future;
  }

  Future<int> getPageCount() async {
    return await _channel.invokeMethod('getPageCount');
  }

  Future<void> coreListener(dynamic callback) async {
    _channel.setMethodCallHandler((MethodCall call) async {
      callback(call.method, call.arguments);
    });
  }

  Future<void> jumpToPage(int pageNumber) async {
    return _channel.invokeMethod('jumpToPage', pageNumber);
  }
}

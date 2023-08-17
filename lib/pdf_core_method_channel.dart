import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'pdf_core_platform_interface.dart';

/// An implementation of [PdfCorePlatform] that uses method channels.
class MethodChannelPdfCore extends PdfCorePlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('pdf_core');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<int?> getPageCount() async {
    final pageCount = await methodChannel.invokeMethod<int>('getPageCount');
    return pageCount;
  }
}

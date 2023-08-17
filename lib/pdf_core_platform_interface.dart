import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'pdf_core_method_channel.dart';

abstract class PdfCorePlatform extends PlatformInterface {
  /// Constructs a PdfCorePlatform.
  PdfCorePlatform() : super(token: _token);

  static final Object _token = Object();

  static PdfCorePlatform _instance = MethodChannelPdfCore();

  /// The default instance of [PdfCorePlatform] to use.
  ///
  /// Defaults to [MethodChannelPdfCore].
  static PdfCorePlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [PdfCorePlatform] when
  /// they register themselves.
  static set instance(PdfCorePlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() async {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<int?> getPageCount() async {
    throw UnimplementedError('getPageCount() has not been implemented.');
  }
}

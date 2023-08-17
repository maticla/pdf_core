import 'package:flutter_test/flutter_test.dart';
import 'package:pdf_core/pdf_core.dart';
import 'package:pdf_core/pdf_core_platform_interface.dart';
import 'package:pdf_core/pdf_core_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockPdfCorePlatform with MockPlatformInterfaceMixin implements PdfCorePlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');
  
  @override
  Future<int?> getPageCount() => Future.value(42);
}

void main() {
  final PdfCorePlatform initialPlatform = PdfCorePlatform.instance;

  test('$MethodChannelPdfCore is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelPdfCore>());
  });

  test('getPlatformVersion', () async {
    PDFCore pdfCorePlugin = PDFCore();
    MockPdfCorePlatform fakePlatform = MockPdfCorePlatform();
    PdfCorePlatform.instance = fakePlatform;

    expect(await pdfCorePlugin.getPlatformVersion(), '42');
  });
}

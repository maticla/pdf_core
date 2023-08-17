// ignore_for_file: avoid_print

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:pdf_core/pdf_core.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  //String _platformVersion = 'Unknown';

  PDFCoreController? c;
  int pageCount = 0;
  int currentPage = 1;
  bool isLoadingPdf = true;
  bool isLoadingBytes = true;
  late Uint8List pdfUint8List;
  Offset tapPosition = const Offset(0, 0);

  @override
  void initState() {
    super.initState();
    //initPlatformState();
    loadPdfFromAssets();
  }

  // // Platform messages are asynchronous, so we initialize in an async method.
  // Future<void> initPlatformState() async {
  //   String platformVersion;
  //   // Platform messages may fail, so we use a try/catch PlatformException.
  //   // We also handle the message potentially returning null.
  //   try {
  //     platformVersion = await _pdfCorePlugin.getPlatformVersion() ?? 'Unknown platform version';
  //     print('Platform ver: $platformVersion');
  //   } on PlatformException {
  //     platformVersion = 'Failed to get platform version.';
  //   }

  //   // If the widget was removed from the tree while the asynchronous platform
  //   // message was in flight, we want to discard the reply rather than calling
  //   // setState to update our non-existent appearance.
  //   if (!mounted) return;

  //   setState(() {
  //     _platformVersion = platformVersion;
  //   });
  // }

  void handlePageChange(int index) {
    setState(() {
      currentPage = index + 1;
    });
  }

  void handleTap(double x, double y) {
    print('Tapped at: $x, $y');
    setState(() {
      tapPosition = Offset(x, y);
    });
  }

  Future<void> loadPdfFromAssets() async {
    var data = await rootBundle.load('assets/ilovepdf_merged.pdf');
    var bytes = data.buffer.asUint8List();
    setState(() {
      pdfUint8List = bytes;
      isLoadingBytes = false;
    });
    print('Bytes: $bytes');
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        appBar: AppBar(
          leadingWidth: 100.0,
          title: const Text('PDFC'),
          leading: Padding(
            padding: const EdgeInsets.only(left: 8.0, top: 8.0),
            child: Text('X: ${tapPosition.dx.toStringAsFixed(2)}\nY: ${tapPosition.dy.toStringAsFixed(2)}'),
          ),
          actions: [
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: Column(
                children: [
                  Text('Page Count: $pageCount'),
                  Text('Current Page: ${currentPage.toString()}'),
                ],
              ),
            ),
          ],
        ),
        body: isLoadingBytes
            ? const Text('Reading...')
            : Stack(
                children: [
                  isLoadingPdf
                      ? const Center(
                          child: CircularProgressIndicator(),
                        )
                      : Container(),
                  Offstage(
                    offstage: isLoadingPdf,
                    child: PDFCoreEngine(
                      pdfBytes: pdfUint8List,
                      onNativeViewCreated: (PDFCoreController controller) async {
                        await controller.isReady().then(
                          (_) async {
                            setState(() {
                              isLoadingPdf = false;
                              c = controller;
                            });
                            print('PDFCore is ready');
                            final pCount = await controller.getPageCount();
                            print("PAGE COUNT: $pCount");
                            setState(() => pageCount = pCount);

                            controller.coreListener((String method, dynamic arguments) {
                              print('FLUTTER: $method, $arguments');
                              if (method == 'pageChanged') handlePageChange(arguments as int);
                              if (method == 'tap') {
                                final x = arguments[0] as double;
                                final y = arguments[1] as double;
                                handleTap(x, y);
                              }
                            });
                          },
                        );
                      },
                    ),
                  ),
                ],
              ),
        floatingActionButton: FloatingActionButton(
          child: const Icon(Icons.undo),
          onPressed: () {
            c?.jumpToPage(0);
          },
        ),
      ),
    );
  }
}

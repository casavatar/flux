package com.example.flux.data.epub

import android.content.Context
import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object EpubTestFixture {

    fun createWithChapters(context: Context, chapterCount: Int): File {
        val file = File(context.cacheDir, "fixture_${chapterCount}ch.epub")
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            // mimetype must be first and stored uncompressed (EPUB spec §3.3)
            val mimetypeBytes = "application/epub+zip".toByteArray()
            zip.setMethod(ZipOutputStream.STORED)
            zip.putNextEntry(ZipEntry("mimetype").apply {
                size = mimetypeBytes.size.toLong()
                compressedSize = mimetypeBytes.size.toLong()
                crc = CRC32().also { it.update(mimetypeBytes) }.value
            })
            zip.write(mimetypeBytes)
            zip.closeEntry()

            zip.setMethod(ZipOutputStream.DEFLATED)

            zip.entry("META-INF/container.xml", CONTAINER_XML)
            zip.entry("OEBPS/package.opf", buildPackageOpf(chapterCount))
            zip.entry("OEBPS/nav.xhtml", buildNavXhtml(chapterCount))
            repeat(chapterCount) { i ->
                zip.entry("OEBPS/chapter${i + 1}.xhtml", buildChapterXhtml(i + 1))
            }
        }
        return file
    }

    fun createCorrupted(context: Context): File =
        File(context.cacheDir, "fixture_corrupted.epub").also {
            it.writeText("not a valid epub")
        }

    private fun ZipOutputStream.entry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray())
        closeEntry()
    }

    private val CONTAINER_XML = """<?xml version="1.0"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/package.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>"""

    private fun buildPackageOpf(chapterCount: Int): String {
        val manifest = (1..chapterCount).joinToString("\n    ") { i ->
            """<item id="ch$i" href="chapter$i.xhtml" media-type="application/xhtml+xml"/>"""
        }
        val spine = (1..chapterCount).joinToString("\n    ") { i ->
            """<itemref idref="ch$i"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uid">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="uid">urn:uuid:flux-test-fixture</dc:identifier>
    <dc:title>Test EPUB — $chapterCount Chapters</dc:title>
    <dc:language>en</dc:language>
    <dc:creator>Flux Test Fixture</dc:creator>
    <meta property="dcterms:modified">2026-01-01T00:00:00Z</meta>
  </metadata>
  <manifest>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
    $manifest
  </manifest>
  <spine>
    $spine
  </spine>
</package>"""
    }

    private fun buildNavXhtml(chapterCount: Int): String {
        val items = (1..chapterCount).joinToString("\n        ") { i ->
            """<li><a href="chapter$i.xhtml">Chapter $i</a></li>"""
        }
        return """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
<head><title>Navigation</title></head>
<body>
  <nav epub:type="toc">
    <ol>
        $items
    </ol>
  </nav>
</body>
</html>"""
    }

    private fun buildChapterXhtml(number: Int) = """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head><title>Chapter $number</title></head>
<body>
  <h1>Chapter $number</h1>
  <p>Content of chapter $number.</p>
</body>
</html>"""
}

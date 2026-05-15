package com.bhargav.cloudbridge.implementation.compression;

import com.bhargav.cloudbridge.entity.CompressionFormat;
import com.bhargav.cloudbridge.service.compression.CompressionService;
import com.bhargav.cloudbridge.service.compression.CompressionResult;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.*;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;

@Slf4j
@Service
public class CompressionServiceImpl implements CompressionService {

    @Override
    public CompressionResult compress(InputStream inputStream,
                                      String originalFileName,
                                      CompressionFormat format) throws Exception{
        log.info("[Compression] compressing '{}' as {} ", originalFileName, format);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        switch (format) {
            case ZIP     -> compressZip(inputStream, originalFileName, buffer);
            case GZIP    -> compressGzip(inputStream, buffer);
            case TAR     -> compressTar(inputStream, originalFileName, buffer);
            case BZIP2   -> compressBzip2(inputStream, buffer);
            case XZ      -> compressXz(inputStream, buffer);
            case SEVEN_Z -> compressSevenZ(inputStream, originalFileName, buffer);
        }

        byte[] compressedBytes = buffer.toByteArray();
        String compressedFileName = buildCompressedFileName(originalFileName, format);

        log.info("[Compression] Done — compressed size: {} bytes", compressedBytes.length);

        return CompressionResult.builder()
                .compressedStream(new ByteArrayInputStream(compressedBytes))
                .compressedSize(compressedBytes.length)
                .compressedFileName(compressedFileName)
                .format(format)
                .build();
    }

    @Override
    public String buildCompressedFileName(String originalFileName,
                                          CompressionFormat format) {
        return switch (format) {
            case ZIP     -> originalFileName + ".zip";
            case GZIP    -> originalFileName + ".gz";
            case TAR     -> originalFileName + ".tar";
            case BZIP2   -> originalFileName + ".bz2";
            case XZ      -> originalFileName + ".xz";
            case SEVEN_Z -> originalFileName + ".7z";
        };
    }

    @Override
    public InputStream decompress(InputStream compressedStream,
                                  CompressionFormat format) throws Exception {

        log.info("[Compression] Decompressing format: {}", format);

        return switch (format) {
            case ZIP     -> decompressZip(compressedStream);
            case GZIP    -> new GzipCompressorInputStream(compressedStream);
            case TAR     -> decompressTar(compressedStream);
            case BZIP2   -> new BZip2CompressorInputStream(compressedStream);
            case XZ      -> new XZCompressorInputStream(compressedStream);
            case SEVEN_Z -> decompressSevenZ(compressedStream);
        };
    }

    public void compressZip(InputStream in, String fileName, OutputStream out)
        throws Exception{
        try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(out)){
            ZipArchiveEntry entry = new ZipArchiveEntry(fileName);
            zip.putArchiveEntry(entry);
            in.transferTo(out);
            zip.closeArchiveEntry();
        }
    }

    public ZipArchiveInputStream decompressZip(InputStream in) throws Exception {
        ZipArchiveInputStream zip = new ZipArchiveInputStream(in);
        zip.getNextEntry();
        return zip;
    }

    public void compressGzip(InputStream in, OutputStream out) throws Exception{
        try (GzipCompressorOutputStream gzip = new GzipCompressorOutputStream(out)){
            in.transferTo(gzip);
        }
    }

    public void compressTar(InputStream in, String fileName,
                            OutputStream out) throws Exception{
        byte[] bytes = in.readAllBytes();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(out)){
            TarArchiveEntry entry = new TarArchiveEntry(fileName);
            entry.setSize(bytes.length);
            tar.putArchiveEntry(entry);
            tar.write(bytes);
            tar.closeArchiveEntry();
        }
    }

    public InputStream decompressTar(InputStream in) throws Exception{
        TarArchiveInputStream tar = new TarArchiveInputStream(in);
        tar.getNextEntry();
        return tar;
    }

    public void compressBzip2(InputStream in, OutputStream out) throws Exception {
        try (BZip2CompressorOutputStream bzip = new BZip2CompressorOutputStream(out)){
            in.transferTo(bzip);
        }
    }

    public void compressXz(InputStream in, OutputStream out) throws Exception{
        try(XZCompressorOutputStream xz = new XZCompressorOutputStream(out)){
            in.transferTo(xz);
        }
    }

    public void compressSevenZ(InputStream in, String fileName,
                               OutputStream out) throws Exception{
        File temp = File.createTempFile("cb_7z_", ".7z");
        try {
            byte[] bytes = in.readAllBytes();
            try(SevenZOutputFile sevenZ = new SevenZOutputFile(temp)){
                SevenZArchiveEntry entry = new SevenZArchiveEntry();
                entry.setName(fileName);
                entry.setSize(bytes.length);
                sevenZ.putArchiveEntry(entry);
                sevenZ.write(bytes);
                sevenZ.closeArchiveEntry();
            }
            Files.copy(temp.toPath(), out);
        } finally {
            temp.delete();
        }
    }

    private InputStream decompressSevenZ(InputStream in) throws Exception {
        File temp = File.createTempFile("cb_7z_dec_", ".7z");
        try {
            Files.write(temp.toPath(), in.readAllBytes());
            try(SevenZFile sevenZ = new SevenZFile(temp)) {
                sevenZ.getNextEntry();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while((n = sevenZ.read(buf)) != -1) buffer.write(buf, 0, n);
                return new ByteArrayInputStream(buffer.toByteArray());
            }
        }
        finally {
                temp.delete();
        }
    }
}

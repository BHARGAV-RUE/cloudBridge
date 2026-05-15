package com.bhargav.cloudbridge.service.compression;

import com.bhargav.cloudbridge.entity.CompressionFormat;

import java.io.InputStream;
public interface CompressionService {

    CompressionResult compress(InputStream inputStream,
                               String originalFileName,
                               CompressionFormat format) throws Exception;

    InputStream decompress(InputStream compressName,
                           CompressionFormat format) throws Exception;

    String buildCompressedFileName(String originalFileName, CompressionFormat format);
}

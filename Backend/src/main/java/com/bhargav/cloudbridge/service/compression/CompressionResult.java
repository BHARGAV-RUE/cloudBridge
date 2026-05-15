package com.bhargav.cloudbridge.service.compression;

import com.bhargav.cloudbridge.entity.CompressionFormat;
import lombok.Builder;
import lombok.Getter;

import java.io.InputStream;

@Getter
@Builder
public class CompressionResult {

    private final InputStream compressedStream;
    private final long compressedSize;
    private final String compressedFileName;
    private final CompressionFormat format;
}
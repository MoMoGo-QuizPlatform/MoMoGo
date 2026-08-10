package com.momogo.core.common.storage;

import com.momogo.core.common.util.storage.ImageResizeSpec;

import java.io.InputStream;

public interface StorageService {

    String upload(InputStream inputStream, String originalFileName, String contentType, String directory, ImageResizeSpec resizeSpec);

    void delete(String fileUrl);
}

package com.momogo.core.common.storage;

import java.io.InputStream;

public interface StorageService {

    String upload(InputStream inputStream, String originalFileName, String contentType, String directory);

    void delete(String fileUrl);
}

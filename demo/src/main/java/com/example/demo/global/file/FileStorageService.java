package com.example.demo.global.file;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    public String save(MultipartFile image) {
        // 지금 단계에서는 파일명만 반환해도 OK
        // (실제 저장 구현은 다음 단계)
        return image.getOriginalFilename();
    }
}

package com.example.demo.global.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    // 기본 업로드 루트 경로: D:/upload/photoProject (application.yml의 app.file.upload-dir 설정값)
    // - application.yml/properties에서 app.file.upload-dir 로 덮어쓸 수 있음
    @Value("${app.file.upload-dir:D:/upload/photoProject}")
    private String uploadDir;

    // 실제 저장 + 저장된 "상대경로" 반환
    public String save(MultipartFile image) {
        // 방어: null 또는 빈 파일이면 저장 불가
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("업로드된 파일이 비어있습니다.");
        }

        // posts 폴더 경로 만들기
        Path postsDir = Paths.get(uploadDir, "posts");

        // posts 폴더가 없으면 생성
        try {
            Files.createDirectories(postsDir);
        } catch (IOException e) {
            throw new RuntimeException("업로드 디렉토리 생성 실패: " + postsDir, e);
        }

        // 원본 파일명에서 확장자만 추출 (없으면 확장자 없이 저장)
        String originalName = image.getOriginalFilename();
        String ext = extractExtension(originalName);

        // 파일명 정책: uuid + 확장자
        String filename = UUID.randomUUID() + ext;

        // 실제 저장될 전체 경로
        Path target = postsDir.resolve(filename).normalize();

        // 보안/안전: postsDir 밖으로 탈출하는 경로면 차단
        if (!target.startsWith(postsDir)) {
            throw new IllegalArgumentException("잘못된 저장 경로입니다.");
        }

        // 실제 파일 저장
        try {
            Files.copy(image.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패: " + target, e);
        }

        // DB에는 절대경로 대신 "상대경로"로 저장 (추후 URL 매핑/서빙에 유리)
        return "posts/" + filename;
    }

    //  기존 이미지 파일 삭제 (posts/uuid.ext 형태를 기준으로 삭제)
    public void delete(String storedPath) {
        // null/공백이면 삭제할 게 없음
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }

        // uploads 루트 + storedPath 로 실제 파일 경로 만들기
        Path baseDir = Paths.get(uploadDir).normalize();
        Path target = baseDir.resolve(storedPath).normalize();

        // 보안/안전: uploadDir 밖으로 탈출하는 경로면 차단
        if (!target.startsWith(baseDir)) {
            throw new IllegalArgumentException("잘못된 삭제 경로입니다.");
        }

        // 파일이 존재하면 삭제
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new RuntimeException("기존 파일 삭제 실패: " + target, e);
        }
    }

    // 확장자 추출 유틸 (".png" 형태로 반환, 없으면 "")
    private String extractExtension(String filename) {
        if (filename == null) return "";

        int dotIndex = filename.lastIndexOf('.');
        // "."이 없거나 맨 끝이면 확장자 없음
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(dotIndex);
    }
}

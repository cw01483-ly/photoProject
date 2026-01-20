package com.example.demo.global.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

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

    @Value("${app.s3.bucket:}")
    private String bucket;

    private final S3Client s3Client;

    public FileStorageService(S3Client s3Client){
        this.s3Client = s3Client;
    }

    // 실제 저장 + 저장된 "상대경로" 반환
    public String save(MultipartFile image) {
        // 방어: null 또는 빈 파일이면 저장 불가
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("업로드된 파일이 비어있습니다.");
        }

        // S3 버킷 설정, 버킷 비어있으면 로컬 저장 방식으로 풀백
        boolean useS3 = (bucket != null && !bucket.isBlank());

        // 원본 파일명에서 확장자만 추출 (없으면 확장자 없이 저장)
        String originalName = image.getOriginalFilename();
        String ext = extractExtension(originalName);

        // 파일명 정책: uuid + 확장자
        String filename = UUID.randomUUID() + ext;

        // DB엔 절대경로 대신 "상대경로"로 저장, S3에서도 이 값을 "Object Key"로 사용
        String key = "posts/" + filename;

        if (useS3) { // S3업로드(스트림업로드) - 임시 파일 생성 없이 바로 전송
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(image.getContentType())
                    .build();

            try {
                s3Client.putObject(request, RequestBody.fromInputStream(image.getInputStream(), image.getSize()));
            } catch (IOException e) {
                throw new RuntimeException("S3 업로드 실패 : bucket=" + bucket + ", key=" + key, e);
            }
            return key;
        }


        // 로컬 풀백 (기존 디스크 저장 로직)

        // posts 폴더 경로 만들기
        Path postsDir = Paths.get(uploadDir, "posts");

        // posts 폴더 없으면 생성
        try {
            Files.createDirectories(postsDir);
        } catch (IOException e) {
            throw new RuntimeException("업로드 디렉토리 생성 실패: " + postsDir, e);
        }

        // 실제 저장 전체 경로
        Path target = postsDir.resolve(filename).normalize();

        // posts 밖으로 탈출 하는 경로는 차단 (보안)
        if (!target.startsWith(postsDir)) {
            throw new IllegalArgumentException("잘못된 저장 경로입니다.");
        }

        // 실제 파일 저장
        try {
            Files.copy(image.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패 : " + target, e);
        }
        return key;
    }

    // 기존 이미지 파일 삭제 (posts/uuid.ext 기준 삭제)
    public void delete(String storedPath) {
        if (storedPath == null || storedPath.isBlank()){ // null 혹은 공백이면 삭제할 게 없음
            return;
        }

        // bucket 있으면 S3 에서 삭제
        boolean useS3 = (bucket != null && !bucket.isBlank());

        if (useS3) {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(storedPath)
                    .build();

            s3Client.deleteObject(request);
            return;
        }

        // 로컬 풀백 (기존 로컬 파일 삭제 로직)
        // uploads 루트 + storedPath로 실제 파일 경로 생성
        Path baseDir = Paths.get(uploadDir).normalize();
        Path target = baseDir.resolve(storedPath).normalize();

        // 업로드 루트 밖으로 나가면 삭제 차단 (보안)
        if (!target.startsWith(baseDir)) {
            throw new IllegalArgumentException("잘못된 삭제 경로입니다.");
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제 실패 : " + target, e);
        }
    }

    private String extractExtension(String originalName) {
        if (originalName == null) return "";
        int dot = originalName.lastIndexOf(".");
        if (dot == -1) return "";
        return originalName.substring(dot);
    }
}

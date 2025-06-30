package com.tofumaker.controller;

import com.tofumaker.entity.FileEntity;
import com.tofumaker.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File Upload", description = "파일 업로드 및 관리 API")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "파일 업로드", description = "파일을 서버에 업로드합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "파일 업로드 성공",
                    content = @Content(schema = @Schema(implementation = FileEntity.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "413", description = "파일 크기 초과"),
        @ApiResponse(responseCode = "415", description = "지원하지 않는 파일 형식"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> uploadFile(
            @Parameter(description = "업로드할 파일", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "업로드한 사용자 ID")
            @RequestParam(value = "userId", required = false) Long userId,
            @Parameter(description = "파일 설명")
            @RequestParam(value = "description", required = false) String description) {
        
        try {
            FileEntity uploadedFile = fileUploadService.uploadFile(file, userId, description);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File uploaded successfully");
            response.put("file", uploadedFile);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("File upload validation error: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (IOException e) {
            logger.error("File upload IO error: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "File upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/download/{fileId}")
    @Operation(summary = "파일 다운로드", description = "파일 ID로 파일을 다운로드합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "파일 다운로드 성공"),
        @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "파일 ID", required = true)
            @PathVariable Long fileId) {
        
        try {
            Resource resource = fileUploadService.downloadFile(fileId);
            Optional<FileEntity> fileInfo = fileUploadService.getFileInfo(fileId);
            
            if (fileInfo.isPresent()) {
                FileEntity file = fileInfo.get();
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + file.getOriginalFileName() + "\"")
                    .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (RuntimeException e) {
            logger.error("File download error: {}", e.getMessage());
            return ResponseEntity.notFound().build();
            
        } catch (IOException e) {
            logger.error("File download IO error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "파일 정보 조회", description = "파일 ID로 파일 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "파일 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = FileEntity.class))),
        @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
    })
    public ResponseEntity<FileEntity> getFileInfo(
            @Parameter(description = "파일 ID", required = true)
            @PathVariable Long fileId) {
        
        Optional<FileEntity> fileInfo = fileUploadService.getFileInfo(fileId);
        return fileInfo.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "파일 목록 조회", description = "활성 파일 목록을 페이징으로 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "파일 목록 조회 성공")
    })
    public ResponseEntity<Page<FileEntity>> getFiles(
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준 (uploadedAt, originalFileName, fileSize)")
            @RequestParam(defaultValue = "uploadedAt") String sortBy,
            @Parameter(description = "정렬 방향 (asc, desc)")
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<FileEntity> files = fileUploadService.getActiveFiles(pageable);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "사용자별 파일 목록 조회", description = "특정 사용자가 업로드한 파일 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 파일 목록 조회 성공")
    })
    public ResponseEntity<Page<FileEntity>> getUserFiles(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준")
            @RequestParam(defaultValue = "uploadedAt") String sortBy,
            @Parameter(description = "정렬 방향")
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<FileEntity> files = fileUploadService.getUserFiles(userId, pageable);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/search")
    @Operation(summary = "파일명으로 검색", description = "파일명으로 파일을 검색합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "파일 검색 성공")
    })
    public ResponseEntity<Page<FileEntity>> searchFiles(
            @Parameter(description = "검색할 파일명", required = true)
            @RequestParam String fileName,
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준")
            @RequestParam(defaultValue = "uploadedAt") String sortBy,
            @Parameter(description = "정렬 방향")
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<FileEntity> files = fileUploadService.searchFilesByName(fileName, pageable);
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "파일 삭제", description = "파일을 논리적으로 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "파일 삭제 성공"),
        @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
    })
    public ResponseEntity<?> deleteFile(
            @Parameter(description = "파일 ID", required = true)
            @PathVariable Long fileId) {
        
        try {
            fileUploadService.deleteFile(fileId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File deleted successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("File deletion error: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @DeleteMapping("/{fileId}/physical")
    @Operation(summary = "파일 물리적 삭제", description = "파일을 물리적으로 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "파일 물리적 삭제 성공"),
        @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> deleteFilePhysically(
            @Parameter(description = "파일 ID", required = true)
            @PathVariable Long fileId) {
        
        try {
            fileUploadService.deleteFilePhysically(fileId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File deleted physically");
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("Physical file deletion error: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (IOException e) {
            logger.error("Physical file deletion IO error: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "File deletion failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "파일 통계", description = "파일 업로드 통계 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "파일 통계 조회 성공")
    })
    public ResponseEntity<FileUploadService.FileStatistics> getFileStatistics() {
        FileUploadService.FileStatistics statistics = fileUploadService.getFileStatistics();
        return ResponseEntity.ok(statistics);
    }
} 
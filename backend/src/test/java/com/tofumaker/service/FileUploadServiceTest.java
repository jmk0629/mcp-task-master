package com.tofumaker.service;

import com.tofumaker.config.FileUploadConfig;
import com.tofumaker.entity.FileEntity;
import com.tofumaker.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileUploadConfig fileUploadConfig;

    @InjectMocks
    private FileUploadService fileUploadService;

    @TempDir
    Path tempDir;

    private MockMultipartFile testFile;
    private FileEntity testFileEntity;

    @BeforeEach
    void setUp() {
        testFile = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "Test file content".getBytes()
        );

        testFileEntity = new FileEntity(
            "test.txt",
            "20231201_123456_test.txt",
            "2023/12/01/",
            testFile.getSize(),
            "text/plain",
            "txt"
        );
        testFileEntity.setId(1L);
        testFileEntity.setUploadedBy(1L);
        testFileEntity.setDescription("Test file");

        // FileUploadConfig 모킹
        when(fileUploadConfig.getUploadDir()).thenReturn(tempDir.toString() + "/");
        when(fileUploadConfig.getMaxFileSize()).thenReturn(10 * 1024 * 1024L); // 10MB
        when(fileUploadConfig.getAllowedExtensions()).thenReturn(new String[]{"txt", "pdf", "jpg"});
    }

    @Test
    void uploadFile_Success() throws IOException {
        // Given
        when(fileRepository.save(any(FileEntity.class))).thenReturn(testFileEntity);

        // When
        FileEntity result = fileUploadService.uploadFile(testFile, 1L, "Test file");

        // Then
        assertNotNull(result);
        assertEquals("test.txt", result.getOriginalFileName());
        assertEquals("text/plain", result.getContentType());
        assertEquals(1L, result.getUploadedBy());
        assertEquals("Test file", result.getDescription());
        verify(fileRepository).save(any(FileEntity.class));
    }

    @Test
    void uploadFile_EmptyFile_ThrowsException() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", "empty.txt", "text/plain", new byte[0]
        );

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> fileUploadService.uploadFile(emptyFile, 1L, "Empty file"));
        assertEquals("File is empty", exception.getMessage());
    }

    @Test
    void uploadFile_FileTooLarge_ThrowsException() {
        // Given
        when(fileUploadConfig.getMaxFileSize()).thenReturn(1L); // 1 byte limit
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> fileUploadService.uploadFile(testFile, 1L, "Large file"));
        assertTrue(exception.getMessage().contains("File size exceeds maximum allowed size"));
    }

    @Test
    void uploadFile_InvalidExtension_ThrowsException() {
        // Given
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file", "test.exe", "application/octet-stream", "content".getBytes()
        );
        when(fileUploadConfig.getAllowedExtensions()).thenReturn(new String[]{"txt", "pdf"});

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> fileUploadService.uploadFile(invalidFile, 1L, "Invalid file"));
        assertTrue(exception.getMessage().contains("File extension not allowed"));
    }

    @Test
    void downloadFile_Success() throws IOException {
        // Given
        when(fileRepository.findById(1L)).thenReturn(Optional.of(testFileEntity));
        when(fileUploadConfig.getUploadDir()).thenReturn(tempDir.toString() + "/");

        // 테스트 파일 생성
        Path testFilePath = tempDir.resolve("2023/12/01/20231201_123456_test.txt");
        testFilePath.getParent().toFile().mkdirs();
        testFilePath.toFile().createNewFile();

        // When
        Resource result = fileUploadService.downloadFile(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.exists());
    }

    @Test
    void downloadFile_FileNotFound_ThrowsException() {
        // Given
        when(fileRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> fileUploadService.downloadFile(1L));
        assertTrue(exception.getMessage().contains("File not found with id"));
    }

    @Test
    void downloadFile_InactiveFile_ThrowsException() {
        // Given
        testFileEntity.setActive(false);
        when(fileRepository.findById(1L)).thenReturn(Optional.of(testFileEntity));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> fileUploadService.downloadFile(1L));
        assertTrue(exception.getMessage().contains("File is not active"));
    }

    @Test
    void getFileInfo_Success() {
        // Given
        when(fileRepository.findById(1L)).thenReturn(Optional.of(testFileEntity));

        // When
        Optional<FileEntity> result = fileUploadService.getFileInfo(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testFileEntity, result.get());
    }

    @Test
    void getActiveFiles_Success() {
        // Given
        List<FileEntity> files = Arrays.asList(testFileEntity);
        Page<FileEntity> page = new PageImpl<>(files);
        Pageable pageable = PageRequest.of(0, 10);
        when(fileRepository.findByActiveTrue(pageable)).thenReturn(page);

        // When
        Page<FileEntity> result = fileUploadService.getActiveFiles(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testFileEntity, result.getContent().get(0));
    }

    @Test
    void getUserFiles_Success() {
        // Given
        List<FileEntity> files = Arrays.asList(testFileEntity);
        Page<FileEntity> page = new PageImpl<>(files);
        Pageable pageable = PageRequest.of(0, 10);
        when(fileRepository.findByUploadedByAndActiveTrue(1L, pageable)).thenReturn(page);

        // When
        Page<FileEntity> result = fileUploadService.getUserFiles(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testFileEntity, result.getContent().get(0));
    }

    @Test
    void searchFilesByName_Success() {
        // Given
        List<FileEntity> files = Arrays.asList(testFileEntity);
        Page<FileEntity> page = new PageImpl<>(files);
        Pageable pageable = PageRequest.of(0, 10);
        when(fileRepository.findByOriginalFileNameContaining("test", pageable)).thenReturn(page);

        // When
        Page<FileEntity> result = fileUploadService.searchFilesByName("test", pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testFileEntity, result.getContent().get(0));
    }

    @Test
    void deleteFile_Success() {
        // Given
        when(fileRepository.findById(1L)).thenReturn(Optional.of(testFileEntity));
        when(fileRepository.save(any(FileEntity.class))).thenReturn(testFileEntity);

        // When
        fileUploadService.deleteFile(1L);

        // Then
        verify(fileRepository).save(testFileEntity);
        assertFalse(testFileEntity.getActive());
    }

    @Test
    void deleteFile_FileNotFound_ThrowsException() {
        // Given
        when(fileRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> fileUploadService.deleteFile(1L));
        assertTrue(exception.getMessage().contains("File not found with id"));
    }

    @Test
    void deleteFilePhysically_Success() throws IOException {
        // Given
        when(fileRepository.findById(1L)).thenReturn(Optional.of(testFileEntity));
        when(fileUploadConfig.getUploadDir()).thenReturn(tempDir.toString() + "/");

        // 테스트 파일 생성
        Path testFilePath = tempDir.resolve("2023/12/01/20231201_123456_test.txt");
        testFilePath.getParent().toFile().mkdirs();
        testFilePath.toFile().createNewFile();

        // When
        fileUploadService.deleteFilePhysically(1L);

        // Then
        verify(fileRepository).delete(testFileEntity);
        assertFalse(testFilePath.toFile().exists());
    }

    @Test
    void getFileStatistics_Success() {
        // Given
        when(fileRepository.countActiveFiles()).thenReturn(5L);
        when(fileRepository.getTotalFileSize()).thenReturn(1024L);

        // When
        FileUploadService.FileStatistics result = fileUploadService.getFileStatistics();

        // Then
        assertNotNull(result);
        assertEquals(5L, result.getTotalFiles());
        assertEquals(1024L, result.getTotalSize());
        assertEquals("1.00 KB", result.getFormattedSize());
    }

    @Test
    void getFileStatistics_NullValues_HandledCorrectly() {
        // Given
        when(fileRepository.countActiveFiles()).thenReturn(null);
        when(fileRepository.getTotalFileSize()).thenReturn(null);

        // When
        FileUploadService.FileStatistics result = fileUploadService.getFileStatistics();

        // Then
        assertNotNull(result);
        assertEquals(0L, result.getTotalFiles());
        assertEquals(0L, result.getTotalSize());
        assertEquals("0 B", result.getFormattedSize());
    }

    @Test
    void fileStatistics_FormattedSize_VariousUnits() {
        // Test different file size formatting
        FileUploadService.FileStatistics stats1 = new FileUploadService.FileStatistics(1L, 512L);
        assertEquals("512.00 B", stats1.getFormattedSize());

        FileUploadService.FileStatistics stats2 = new FileUploadService.FileStatistics(1L, 1536L);
        assertEquals("1.50 KB", stats2.getFormattedSize());

        FileUploadService.FileStatistics stats3 = new FileUploadService.FileStatistics(1L, 2097152L);
        assertEquals("2.00 MB", stats3.getFormattedSize());

        FileUploadService.FileStatistics stats4 = new FileUploadService.FileStatistics(1L, 3221225472L);
        assertEquals("3.00 GB", stats4.getFormattedSize());
    }
} 
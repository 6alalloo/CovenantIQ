package com.covenantiq.service;

import com.covenantiq.domain.Attachment;
import com.covenantiq.domain.FinancialStatement;
import com.covenantiq.dto.response.AttachmentMetadataResponse;
import com.covenantiq.exception.PayloadTooLargeException;
import com.covenantiq.exception.ResourceNotFoundException;
import com.covenantiq.exception.UnsupportedFileTypeException;
import com.covenantiq.repository.AttachmentRepository;
import com.covenantiq.repository.FinancialStatementRepository;
import com.covenantiq.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class AttachmentService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    private final AttachmentRepository attachmentRepository;
    private final FinancialStatementRepository financialStatementRepository;
    private final CurrentUserService currentUserService;

    public AttachmentService(
            AttachmentRepository attachmentRepository,
            FinancialStatementRepository financialStatementRepository,
            CurrentUserService currentUserService
    ) {
        this.attachmentRepository = attachmentRepository;
        this.financialStatementRepository = financialStatementRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public AttachmentMetadataResponse upload(Long financialStatementId, MultipartFile file) {
        FinancialStatement statement = financialStatementRepository.findById(financialStatementId)
                .orElseThrow(() -> new ResourceNotFoundException("Financial statement " + financialStatementId + " not found"));

        validateFile(file);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new UnsupportedFileTypeException("Unable to read upload file");
        }

        Attachment attachment = new Attachment();
        attachment.setFinancialStatement(statement);
        attachment.setFilename(file.getOriginalFilename() == null ? "attachment.pdf" : file.getOriginalFilename());
        attachment.setFileSize(file.getSize());
        attachment.setContentType("application/pdf");
        attachment.setFileData(bytes);
        attachment.setUploadedBy(currentUserService.usernameOrSystem());
        attachment.setUploadedAt(OffsetDateTime.now(ZoneOffset.UTC));

        Attachment saved = attachmentRepository.save(attachment);
        return toMetadata(saved);
    }

    @Transactional(readOnly = true)
    public List<AttachmentMetadataResponse> list(Long financialStatementId) {
        financialStatementRepository.findById(financialStatementId)
                .orElseThrow(() -> new ResourceNotFoundException("Financial statement " + financialStatementId + " not found"));
        return attachmentRepository.findByFinancialStatementIdOrderByUploadedAtDesc(financialStatementId)
                .stream()
                .map(this::toMetadata)
                .toList();
    }

    @Transactional(readOnly = true)
    public Attachment download(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment " + attachmentId + " not found"));
    }

    @Transactional
    public void delete(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment " + attachmentId + " not found"));
        attachmentRepository.delete(attachment);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UnsupportedFileTypeException("File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new PayloadTooLargeException("File size exceeds 10MB limit");
        }
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!fileName.endsWith(".pdf") && !contentType.equals("application/pdf")) {
            throw new UnsupportedFileTypeException("Only PDF files are supported");
        }
    }

    private AttachmentMetadataResponse toMetadata(Attachment attachment) {
        return new AttachmentMetadataResponse(
                attachment.getId(),
                attachment.getFilename(),
                attachment.getFileSize(),
                attachment.getContentType(),
                attachment.getUploadedBy(),
                attachment.getUploadedAt()
        );
    }
}

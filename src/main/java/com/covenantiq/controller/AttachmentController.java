package com.covenantiq.controller;

import com.covenantiq.domain.Attachment;
import com.covenantiq.dto.response.AttachmentMetadataResponse;
import com.covenantiq.service.AttachmentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(value = "/financial-statements/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public AttachmentMetadataResponse upload(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return attachmentService.upload(id, file);
    }

    @GetMapping("/financial-statements/{id}/attachments")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public List<AttachmentMetadataResponse> list(@PathVariable Long id) {
        return attachmentService.list(id);
    }

    @GetMapping("/attachments/{id}")
    @PreAuthorize("hasAnyRole('ANALYST','RISK_LEAD','ADMIN')")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Attachment attachment = attachmentService.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(attachment.getFileData());
    }

    @DeleteMapping("/attachments/{id}")
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        attachmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

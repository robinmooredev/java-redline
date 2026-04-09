package com.example.demo.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.ContractModification;
import com.example.demo.dto.RedlineOutput;
import com.example.demo.exception.RedlineException;
import com.example.demo.service.RedlineService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * REST controller that receives an original DOCX together with a JSON array of
 * modifications and returns a ZIP that contains:
 * <ul>
 * <li>redline.docx – tracked‑changes version</li>
 * <li>clean.docx – all revisions accepted</li>
 * <li>redline.pdf – PDF view with markup visible</li>
 * </ul>
 */
@RestController
public class RedlineController {
    private static final Logger logger = LoggerFactory.getLogger(RedlineController.class);

    private final RedlineService redlineService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public RedlineController(RedlineService redlineService, ObjectMapper objectMapper) {
        this.redlineService = redlineService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/redline")
    public ResponseEntity<?> redline(
            @RequestParam("file") MultipartFile originalFile,
            @RequestParam("modifications") String modificationsJson) {

        logger.info("Received redline request");
        
        try {
            // Validate file
            if (originalFile == null || originalFile.isEmpty()) {
                logger.warn("Missing or empty file in request");
                return ResponseEntity.badRequest().body("File cannot be empty");
            }
            
            // Check file type
            String contentType = originalFile.getContentType();
            if (contentType == null || !isDocxContentType(contentType)) {
                logger.warn("Invalid file type: {}", contentType);
                return ResponseEntity.badRequest().body("Only DOCX files are supported");
            }
            
            // Parse modifications
            List<ContractModification> mods;
            try {
                mods = objectMapper.readValue(
                        modificationsJson, new TypeReference<List<ContractModification>>() {});
                logger.debug("Parsed {} modifications from request", mods.size());
            } catch (JsonProcessingException e) {
                logger.error("Failed to parse modifications JSON", e);
                return ResponseEntity.badRequest().body("Invalid modifications JSON: " + e.getMessage());
            }
            
            // Generate redline output
            RedlineOutput output = redlineService.generateRedline(originalFile, mods);
            
            // Return ZIP file
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", redlineService.getZipFilename());
            
            logger.info("Successfully processed redline request");
            return ResponseEntity.ok().headers(headers).body(output.getZipContent());

        } catch (Exception ex) {
            logger.error("Unexpected error processing redline request", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + ex.getMessage());
        }
    }
    
    @PostMapping("/accept")
    public ResponseEntity<?> accept(
            @RequestParam("file") MultipartFile file) {

        logger.info("Received accept-all-changes request");

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File cannot be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !isDocxContentType(contentType)) {
            return ResponseEntity.badRequest().body("Only DOCX files are supported");
        }

        try {
            byte[] cleanDoc = redlineService.acceptAllChanges(file);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "accepted.docx");

            logger.info("Successfully accepted all changes");
            return ResponseEntity.ok().headers(headers).body(cleanDoc);
        } catch (Exception ex) {
            logger.error("Unexpected error accepting changes", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + ex.getMessage());
        }
    }

    /**
     * Exception handler for RedlineException
     */
    @ExceptionHandler(RedlineException.class)
    public ResponseEntity<?> handleRedlineException(RedlineException ex) {
        logger.error("Redline exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        HttpStatus status;
        switch (ex.getErrorCode()) {
            case INPUT_VALIDATION_ERROR:
                status = HttpStatus.BAD_REQUEST;
                break;
            case PARAGRAPH_NOT_FOUND:
            case ANCHOR_NOT_FOUND:
                status = HttpStatus.NOT_FOUND;
                break;
            case DOCUMENT_PROCESSING_ERROR:
            default:
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                break;
        }
        
        return ResponseEntity.status(status).body(ex.getMessage());
    }
    
    /**
     * Exception handler for MultipartException
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<?> handleMultipartException(MultipartException ex) {
        logger.error("Multipart exception", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Invalid file upload: " + ex.getMessage());
    }
    
    /**
     * Check if the content type is a DOCX file
     */
    private boolean isDocxContentType(String contentType) {
        return contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || 
               contentType.equals("application/docx") ||
               contentType.equals("application/msword");
    }
}
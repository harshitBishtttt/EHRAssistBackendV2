package ehrAssist.controller;

import ehrAssist.dto.request.BulkUploadRequest;
import ehrAssist.dto.response.BulkUploadResponse;
import ehrAssist.service.BulkUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/baseR4/bulk-upload")
@RequiredArgsConstructor
public class BulkUploadController {

    private final BulkUploadService bulkUploadService;

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping(
            value    = "/workbook",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BulkUploadResponse> uploadWorkbook(
            @RequestPart("file")    MultipartFile     file,
            @RequestPart("request") BulkUploadRequest request) {

        return ResponseEntity.ok(bulkUploadService.uploadWorkbook(file, request));
    }
}

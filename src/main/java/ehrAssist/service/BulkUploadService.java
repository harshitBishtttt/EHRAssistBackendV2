package ehrAssist.service;

import ehrAssist.dto.request.BulkUploadRequest;
import ehrAssist.dto.response.BulkUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface BulkUploadService {

    BulkUploadResponse uploadWorkbook(MultipartFile file, BulkUploadRequest request);
}

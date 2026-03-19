package br.com.assine.fiscal.adapter.out.storage;

import br.com.assine.fiscal.domain.port.out.InvoiceStorageGateway;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class MinIOStorageAdapter implements InvoiceStorageGateway {

    private static final Logger log = LoggerFactory.getLogger(MinIOStorageAdapter.class);

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.endpoint}")
    private String endpoint;

    public MinIOStorageAdapter(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String store(UUID invoiceId, byte[] pdfContent) {
        String key = String.format("invoices/%s.pdf", invoiceId);
        log.info("Storing PDF to MinIO at key: {}", key);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType("application/pdf")
            .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(pdfContent));

        // Construct the URL to access the PDF
        // Using the endpoint and the bucket name
        return String.format("%s/%s/%s", endpoint, bucketName, key);
    }
}

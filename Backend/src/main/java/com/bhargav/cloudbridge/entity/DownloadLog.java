package com.bhargav.cloudbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "download_logs")
@Builder

public class DownloadLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileMetadata file;

    @Column(name = "receiver_email", nullable = false)
    private String receiverEmail;

    @Builder.Default
    private Boolean isUniqueDownloader = false;

    @Column(name = "downloaded_at", nullable = false)
    private LocalDateTime downloadedAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "device_info")
    private String deviceInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "download_status", nullable = false)
    private DownloadStatus downloadStatus;

    @PrePersist
    protected void onCreate(){
        this.downloadedAt = LocalDateTime.now();
    }

}

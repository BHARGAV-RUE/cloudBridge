package com.bhargav.cloudbridge.implementation.email;

import com.bhargav.cloudbridge.entity.FileMetadata;
import com.bhargav.cloudbridge.service.email.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${cloudbridge.email.from-name}")
    private String fromName;

    @Value("${cloudbridge.email.from-address}")
    private String fromAddress;

    @Value("${cloudbridge.frontend-url}")
    private String frontendUrl;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ── RECEIVER EMAIL ─────────────────────────────────────────────────────────

    @Override
    public void sendDownloadLinkToReceiver(FileMetadata file,
                                           String downloadUrl,
                                           boolean hasPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(file.getReceiverEmail());
            helper.setSubject("📁 Someone shared a file with you — CloudBridge");
            helper.setText(buildReceiverEmailBody(file, downloadUrl, hasPassword), true);

            mailSender.send(message);
            log.info("[EmailService] Download link sent to: {}", file.getReceiverEmail());

        } catch (Exception e) {
            log.error("[EmailService] Failed to send receiver email | reason: {}",
                    e.getMessage());
            // Don't throw — email failure should not break the upload flow
        }
    }

    // ── SENDER CONFIRMATION ────────────────────────────────────────────────────

    @Override
    public void sendUploadConfirmationToSender(FileMetadata file,
                                               String downloadUrl) {
        if (file.getSenderEmail() == null || file.getSenderEmail().isBlank()) {
            log.info("[EmailService] No sender email provided — skipping confirmation.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(file.getSenderEmail());
            helper.setSubject("✅ Your file was uploaded successfully — CloudBridge");
            helper.setText(buildSenderEmailBody(file, downloadUrl), true);

            mailSender.send(message);
            log.info("[EmailService] Upload confirmation sent to: {}",
                    file.getSenderEmail());

        } catch (Exception e) {
            log.error("[EmailService] Failed to send sender email | reason: {}",
                    e.getMessage());
        }
    }

    // ── EMAIL BODIES ───────────────────────────────────────────────────────────

    private String buildReceiverEmailBody(FileMetadata file,
                                          String downloadUrl,
                                          boolean hasPassword) {
        String expiryFormatted = file.getExpiryDate().format(DATE_FORMAT);
        String senderInfo = (file.getSenderEmail() != null && !file.getSenderEmail().isBlank())
                ? file.getSenderEmail()
                : "Someone";

        String passwordNote = hasPassword
                ? """
                  <tr>
                    <td style="padding: 12px 24px;">
                      <div style="background:#fff3cd;border-left:4px solid #ffc107;
                                  padding:12px 16px;border-radius:4px;">
                        🔒 <strong>This file is password protected.</strong>
                        The sender will share the password with you separately.
                      </div>
                    </td>
                  </tr>
                  """
                : "";

        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f4f4f4;font-family:Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr>
                      <td align="center" style="padding:40px 0;">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:8px;
                                      box-shadow:0 2px 8px rgba(0,0,0,0.1);">

                          <!-- HEADER -->
                          <tr>
                            <td style="background:#1a73e8;padding:24px;
                                       border-radius:8px 8px 0 0;text-align:center;">
                              <h1 style="color:#ffffff;margin:0;font-size:24px;">
                                ☁️ CloudBridge
                              </h1>
                              <p style="color:#cce0ff;margin:4px 0 0;">
                                Secure File Transfer
                              </p>
                            </td>
                          </tr>

                          <!-- BODY -->
                          <tr>
                            <td style="padding:32px 24px 16px;">
                              <h2 style="color:#333;margin:0 0 8px;">
                                You have a file waiting for you!
                              </h2>
                              <p style="color:#666;margin:0;">
                                <strong>%s</strong> shared a file with you.
                              </p>
                            </td>
                          </tr>

                          <!-- FILE INFO -->
                          <tr>
                            <td style="padding:0 24px;">
                              <table width="100%%" style="background:#f8f9fa;
                                     border-radius:6px;padding:16px;">
                                <tr>
                                  <td style="color:#555;padding:4px 0;">
                                    📄 <strong>File:</strong> %s
                                  </td>
                                </tr>
                                <tr>
                                  <td style="color:#555;padding:4px 0;">
                                    📦 <strong>Size:</strong> %s
                                  </td>
                                </tr>
                                <tr>
                                  <td style="color:#e53935;padding:4px 0;">
                                    ⏰ <strong>Expires:</strong> %s
                                  </td>
                                </tr>
                                <tr>
                                  <td style="color:#555;padding:4px 0;">
                                    ⬇️ <strong>Downloads remaining:</strong> %d
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- PASSWORD NOTE -->
                          %s

                          <!-- DOWNLOAD BUTTON -->
                          <tr>
                            <td style="padding:24px;text-align:center;">
                              <a href="%s"
                                 style="background:#1a73e8;color:#ffffff;
                                        padding:14px 32px;border-radius:6px;
                                        text-decoration:none;font-size:16px;
                                        font-weight:bold;display:inline-block;">
                                ⬇️ Download File
                              </a>
                            </td>
                          </tr>

                          <!-- FOOTER -->
                          <tr>
                            <td style="padding:16px 24px 32px;text-align:center;
                                       color:#999;font-size:12px;">
                              <p style="margin:0;">
                                This link expires on %s and allows up to %d downloads.
                              </p>
                              <p style="margin:8px 0 0;">
                                Powered by <strong>CloudBridge</strong> —
                                Secure Multi-Cloud File Transfer
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                senderInfo,
                file.getOriginalName(),
                formatFileSize(file.getOriginalSize()),
                expiryFormatted,
                file.getDownloadLimit() - file.getDownloadCount(),
                passwordNote,
                downloadUrl,
                expiryFormatted,
                file.getDownloadLimit()
        );
    }

    private String buildSenderEmailBody(FileMetadata file, String downloadUrl) {
        String expiryFormatted = file.getExpiryDate().format(DATE_FORMAT);

        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f4f4f4;font-family:Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr>
                      <td align="center" style="padding:40px 0;">
                        <table width="600" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:8px;
                                      box-shadow:0 2px 8px rgba(0,0,0,0.1);">

                          <!-- HEADER -->
                          <tr>
                            <td style="background:#1a73e8;padding:24px;
                                       border-radius:8px 8px 0 0;text-align:center;">
                              <h1 style="color:#ffffff;margin:0;font-size:24px;">
                                ☁️ CloudBridge
                              </h1>
                              <p style="color:#cce0ff;margin:4px 0 0;">
                                Secure File Transfer
                              </p>
                            </td>
                          </tr>

                          <!-- BODY -->
                          <tr>
                            <td style="padding:32px 24px 16px;">
                              <h2 style="color:#2e7d32;margin:0 0 8px;">
                                ✅ File uploaded successfully!
                              </h2>
                              <p style="color:#666;margin:0;">
                                Your file has been compressed, encrypted, and
                                stored securely in the cloud.
                              </p>
                            </td>
                          </tr>

                          <!-- FILE INFO -->
                          <tr>
                            <td style="padding:0 24px;">
                              <table width="100%%" style="background:#f8f9fa;
                                     border-radius:6px;padding:16px;">
                                <tr>
                                  <td style="color:#555;padding:4px 0;">
                                    📄 <strong>File:</strong> %s
                                  </td>
                                </tr>
                                <tr>
                                  <td style="color:#555;padding:4px 0;">
                                    📦 <strong>Original size:</strong> %s
                                  </td>
                                </tr>
                                <tr>
                                  <td style="color:#555;padding:4px 0;">
                                    🗜️ <strong>Compressed size:</strong> %s
                                  </td>
                                </tr>
                                <tr>
                                  <td style="color:#555;padding:4px 0;">
                                    ☁️ <strong>Stored on:</strong> %s
                                  </td>
                                </tr>
                                <tr>
                                  <td style="color:#555;padding:4px 0;">
                                    📧 <strong>Sent to:</strong> %s
                                  </td>
                                </tr>
                                <tr>
                                  <td style="color:#e53935;padding:4px 0;">
                                    ⏰ <strong>Expires:</strong> %s
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- DOWNLOAD BUTTON -->
                          <tr>
                            <td style="padding:24px;text-align:center;">
                              <a href="%s"
                                 style="background:#1a73e8;color:#ffffff;
                                        padding:14px 32px;border-radius:6px;
                                        text-decoration:none;font-size:16px;
                                        font-weight:bold;display:inline-block;">
                                🔗 View Download Link
                              </a>
                            </td>
                          </tr>

                          <!-- FOOTER -->
                          <tr>
                            <td style="padding:16px 24px 32px;text-align:center;
                                       color:#999;font-size:12px;">
                              <p style="margin:0;">
                                Link expires on %s | Max %d downloads allowed
                              </p>
                              <p style="margin:8px 0 0;">
                                Powered by <strong>CloudBridge</strong> —
                                Secure Multi-Cloud File Transfer
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                file.getOriginalName(),
                formatFileSize(file.getOriginalSize()),
                formatFileSize(file.getCompressedSize()),
                file.getCloudProvider(),
                file.getReceiverEmail(),
                expiryFormatted,
                downloadUrl,
                expiryFormatted,
                file.getDownloadLimit()
        );
    }

    // ── UTILITY ────────────────────────────────────────────────────────────────

    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) return "Unknown";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
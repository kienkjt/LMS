package com.kjt.lms.service.impl;

import com.kjt.lms.model.entity.CertificateEntity;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

final class CertificatePdfGenerator {

    private CertificatePdfGenerator() {}

    static byte[] generate(CertificateEntity certificate) {
        List<String> objects = new ArrayList<>();

        // Catalog
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        // Pages
        objects.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        // Page - A4 landscape
        objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595]" +
                " /Resources << /Font << /F1 4 0 R /F2 5 0 R /F3 6 0 R >> >> /Contents 7 0 R >>");
        // F1 - serif body
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Times-Roman >>");
        // F2 - serif bold (names, titles)
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Times-Bold >>");
        // F3 - serif italic
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Times-Italic >>");

        String studentName  = pdfText(certificate.getStudentName());
        String courseTitle  = pdfText(certificate.getCourseTitle());
        String instructor   = pdfText(certificate.getInstructorName());
        LocalDate issuedDate = certificate.getIssuedAt() != null ? certificate.getIssuedAt() : LocalDate.now();
        String issuedAt     = issuedDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String certCode     = pdfText(certificate.getCertificateCode());

        int nameX   = centerX(certificate.getStudentName(), 28);
        int courseX = centerX(certificate.getCourseTitle(), 20);

        String content = buildContent(studentName, courseTitle, instructor, issuedAt, certCode, nameX, courseX);
        byte[] stream = content.getBytes(StandardCharsets.ISO_8859_1);

        objects.add("<< /Length " + stream.length + " >>\nstream\n" + content + "\nendstream");
        return writePdf(objects);
    }

    private static String buildContent(String studentName, String courseTitle,
                                       String instructor, String issuedAt,
                                       String certCode, int nameX, int courseX) {
        return
                // ── Background ──────────────────────────────────────────────────
                "0.99 0.98 0.96 rg\n"
                        + "0 0 842 595 re f\n"

                        // ── Outer border (gold) ──────────────────────────────────────────
                        + "0.78 0.71 0.38 RG\n"
                        + "2 w\n"
                        + "25 25 792 545 re S\n"

                        // ── Inner border thin ────────────────────────────────────────────
                        + "0.5 w\n"
                        + "35 35 772 525 re S\n"

                        // ── Corner ornaments (L-shapes, 4 corners) ───────────────────────
                        + "1.5 w\n"
                        + "0.60 0.55 0.25 RG\n"
                        // TL
                        + "25 75 m 25 25 l 75 25 l S\n"
                        // TR
                        + "767 25 m 817 25 l 817 75 l S\n"
                        // BL
                        + "25 520 m 25 570 l 75 570 l S\n"
                        // BR
                        + "767 570 m 817 570 l 817 520 l S\n"

                        // ── Seal circle (bottom-left) ────────────────────────────────────
                        + "0.5 w\n"
                        + "0.78 0.71 0.38 RG\n"
                        + "100 120 m\n"  // approximate circle via lines (PDF Type1 has no arc in content stream without operators)
                        // Use rectangle as a stand-in label area
                        + "65 85 70 70 re S\n"
                        + "70 90 60 60 re S\n"
                        + "0.78 0.71 0.38 rg\n"
                        + "0.15 g\n"
                        + "0.78 0.71 0.38 rg\n"
                        + "BT /F2 9 Tf 73 147 Td (CERTIFIED) Tj ET\n"
                        + "BT /F1 8 Tf 79 137 Td (LMS) Tj ET\n"

                        // ── Title ────────────────────────────────────────────────────────
                        + "0.11 0.20 0.35 rg\n"
                        + "BT /F2 11 Tf 251 540 Td (C  E  R  T  I  F  I  C  A  T  E     O  F     C  O  M  P  L  E  T  I  O  N) Tj ET\n"

                        // ── Decorative divider below title ───────────────────────────────
                        + "0.78 0.71 0.38 RG 0.5 w\n"
                        + "200 525 m 642 525 l S\n"
                        + "415 525 m 415 521 l 419 517 l 423 521 l 423 525 l S\n"  // center diamond hint

                        // ── "This certificate is proudly presented to" ───────────────────
                        + "0.40 0.40 0.38 rg\n"
                        + "BT /F3 13 Tf 268 494 Td (This certificate is proudly presented to) Tj ET\n"

                        // ── Student name ─────────────────────────────────────────────────
                        + "0.11 0.20 0.35 rg\n"
                        + "BT /F2 28 Tf " + nameX + " 448 Td (" + studentName + ") Tj ET\n"

                        // ── Underline below name ─────────────────────────────────────────
                        + "0.78 0.71 0.38 RG 1 w\n"
                        + "120 440 m 722 440 l S\n"

                        // ── "for successfully completing..." ────────────────────────────
                        + "0.40 0.40 0.38 rg\n"
                        + "BT /F3 12 Tf 255 418 Td (for successfully completing at least 85%% of the course) Tj ET\n"

                        // ── Course title ─────────────────────────────────────────────────
                        + "0.11 0.20 0.35 rg\n"
                        + "BT /F2 20 Tf " + courseX + " 382 Td (" + courseTitle + ") Tj ET\n"

                        // ── Divider above footer ─────────────────────────────────────────
                        + "0.78 0.71 0.38 RG 0.5 w\n"
                        + "80 345 m 762 345 l S\n"

                        // ── Footer info ──────────────────────────────────────────────────
                        + "0.40 0.40 0.38 rg\n"
                        + "BT /F1 11 Tf 90 322 Td (Instructor:) Tj ET\n"
                        + "0.11 0.20 0.35 rg\n"
                        + "BT /F2 11 Tf 168 322 Td (" + instructor + ") Tj ET\n"

                        + "0.40 0.40 0.38 rg\n"
                        + "BT /F1 11 Tf 90 306 Td (Issued at:) Tj ET\n"
                        + "0.11 0.20 0.35 rg\n"
                        + "BT /F1 11 Tf 162 306 Td (" + issuedAt + ") Tj ET\n"

                        + "0.40 0.40 0.38 rg\n"
                        + "BT /F1 11 Tf 90 290 Td (Certificate code:) Tj ET\n"
                        + "0.55 0.55 0.53 rg\n"
                        + "BT /F1 10 Tf 210 290 Td (" + certCode + ") Tj ET\n"

                        // ── Signature line (bottom-right) ────────────────────────────────
                        + "0.11 0.20 0.35 RG 0.5 w\n"
                        + "580 290 m 750 290 l S\n"
                        + "0.40 0.40 0.38 rg\n"
                        + "BT /F1 10 Tf 640 278 Td (LMS System) Tj ET\n"

                        // ── Bottom decorative double line ────────────────────────────────
                        + "0.78 0.71 0.38 RG\n"
                        + "1.5 w\n"
                        + "80 62 m 762 62 l S\n"
                        + "0.5 w\n"
                        + "80 57 m 762 57 l S\n";
    }

    // ── Unchanged helpers ────────────────────────────────────────────────────

    private static byte[] writePdf(List<String> objects) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        write(output, "%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(output.size());
            write(output, (i + 1) + " 0 obj\n");
            write(output, objects.get(i));
            write(output, "\nendobj\n");
        }
        int xrefOffset = output.size();
        write(output, "xref\n0 " + (objects.size() + 1) + "\n");
        write(output, "0000000000 65535 f \n");
        for (Integer offset : offsets) {
            write(output, String.format("%010d 00000 n \n", offset));
        }
        write(output, "trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\n");
        write(output, "startxref\n" + xrefOffset + "\n%%EOF");
        return output.toByteArray();
    }

    private static void write(ByteArrayOutputStream output, String value) {
        output.writeBytes(value.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static String pdfText(String value) {
        String ascii = toAscii(value == null || value.isBlank() ? "N/A" : value);
        return ascii.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private static String toAscii(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .replaceAll("[^\\x20-\\x7E]", "?");
    }

    private static int centerX(String value, int fontSize) {
        int len = toAscii(value == null ? "" : value).length();
        int estimatedWidth = len * fontSize / 2;
        return Math.max(60, 421 - (estimatedWidth / 2));
    }
}

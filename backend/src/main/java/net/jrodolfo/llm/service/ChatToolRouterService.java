package net.jrodolfo.llm.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatToolRouterService {

    private static final Pattern REGION_PATTERN = Pattern.compile("\\b(af|ap|ca|eu|il|me|sa|us)-[a-z]+-\\d\\b");
    private static final Pattern DAYS_PATTERN = Pattern.compile("\\b(\\d{1,3})\\s+days?\\b");
    private static final Pattern BUCKET_PATTERN = Pattern.compile("\\bbucket\\s+([a-z0-9.-]{3,255})\\b");
    private static final List<String> AUDIT_SERVICES = List.of(
            "sts", "aws-config", "s3", "ec2", "elbv2", "rds", "lambda",
            "ecs", "eks", "sagemaker", "opensearch", "secretsmanager", "logs", "tagging"
    );

    public ToolDecision route(String message) {
        String normalized = message.toLowerCase(Locale.ROOT).trim();

        if (mentionsReportRead(normalized)) {
            return new ToolDecision(
                    DecisionType.READ_LATEST_REPORT,
                    inferReportType(normalized),
                    null,
                    null,
                    null,
                    "latest report lookup"
            );
        }

        if (mentionsReportListing(normalized)) {
            return new ToolDecision(
                    DecisionType.LIST_REPORTS,
                    inferReportType(normalized),
                    null,
                    null,
                    null,
                    "recent report lookup"
            );
        }

        if (mentionsS3Cloudwatch(normalized)) {
            return new ToolDecision(
                    DecisionType.S3_CLOUDWATCH_REPORT,
                    null,
                    extractBucket(message),
                    extractRegion(normalized),
                    extractDays(normalized),
                    "s3 cloudwatch metrics request"
            );
        }

        if (mentionsAudit(normalized)) {
            return new ToolDecision(
                    DecisionType.AWS_REGION_AUDIT,
                    null,
                    null,
                    extractRegion(normalized),
                    null,
                    "aws audit request",
                    extractServices(normalized)
            );
        }

        return ToolDecision.none();
    }

    private boolean mentionsReportRead(String normalized) {
        return ((normalized.contains("latest report") || normalized.contains("read report") || normalized.contains("show report"))
                && normalized.contains("audit"))
                || normalized.contains("read the latest audit report")
                || normalized.contains("read the latest s3 report")
                || normalized.contains("show the latest audit report");
    }

    private boolean mentionsReportListing(String normalized) {
        return normalized.contains("recent reports")
                || normalized.contains("latest reports")
                || normalized.contains("list reports");
    }

    private boolean mentionsS3Cloudwatch(String normalized) {
        return normalized.contains("s3 cloudwatch")
                || normalized.contains("bucket metrics")
                || (normalized.contains("bucket") && normalized.contains("metrics"))
                || normalized.contains("bucket report")
                || normalized.contains("cloudwatch report for bucket");
    }

    private boolean mentionsAudit(String normalized) {
        return normalized.contains("aws audit")
                || normalized.contains("run audit")
                || normalized.contains("region audit")
                || (normalized.contains("audit") && !normalized.contains("report"));
    }

    private String inferReportType(String normalized) {
        if (normalized.contains("s3")) {
            return "s3_cloudwatch";
        }
        if (normalized.contains("audit")) {
            return "audit";
        }
        return "all";
    }

    private String extractBucket(String message) {
        Matcher bucketMatcher = BUCKET_PATTERN.matcher(message.toLowerCase(Locale.ROOT));
        if (bucketMatcher.find()) {
            return bucketMatcher.group(1);
        }
        return null;
    }

    private String extractRegion(String normalized) {
        Matcher regionMatcher = REGION_PATTERN.matcher(normalized);
        if (regionMatcher.find()) {
            return regionMatcher.group();
        }
        return null;
    }

    private Integer extractDays(String normalized) {
        Matcher daysMatcher = DAYS_PATTERN.matcher(normalized);
        if (!daysMatcher.find()) {
            return null;
        }

        int days = Integer.parseInt(daysMatcher.group(1));
        return Math.min(days, 365);
    }

    private List<String> extractServices(String normalized) {
        List<String> services = new ArrayList<>();
        for (String service : AUDIT_SERVICES) {
            if (normalized.contains(service)) {
                services.add(service);
            }
        }
        return services;
    }

    public enum DecisionType {
        NONE,
        LIST_REPORTS,
        READ_LATEST_REPORT,
        AWS_REGION_AUDIT,
        S3_CLOUDWATCH_REPORT
    }

    public record ToolDecision(
            DecisionType type,
            String reportType,
            String bucket,
            String region,
            Integer days,
            String reason,
            List<String> services
    ) {
        public ToolDecision(
                DecisionType type,
                String reportType,
                String bucket,
                String region,
                Integer days,
                String reason
        ) {
            this(type, reportType, bucket, region, days, reason, List.of());
        }

        public static ToolDecision none() {
            return new ToolDecision(DecisionType.NONE, null, null, null, null, "no tool matched", List.of());
        }

        public boolean shouldUseTool() {
            return type != DecisionType.NONE;
        }
    }
}

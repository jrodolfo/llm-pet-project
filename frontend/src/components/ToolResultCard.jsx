function ToolResultCard({ toolResult }) {
  if (!toolResult?.type) {
    return null;
  }

  if (toolResult.type === 'report_list') {
    const reports = Array.isArray(toolResult.reports) ? toolResult.reports : [];
    return (
      <div className="tool-result-card">
        <span className="tool-result-title">reports</span>
        <span>type: {toolResult.reportType || 'all'}</span>
        {reports.length === 0 ? <span>no reports found.</span> : null}
        {reports.map((report) => (
          <div key={report.run_dir || report.summary_json || report.report_txt} className="tool-result-item">
            <span>{report.report_type || 'report'}</span>
            {report.created_at ? <span>{report.created_at}</span> : null}
            {report.run_dir ? <span>{report.run_dir}</span> : null}
          </div>
        ))}
      </div>
    );
  }

  if (toolResult.type === 'report_summary') {
    const summary = toolResult.summary || {};
    return (
      <div className="tool-result-card">
        <span className="tool-result-title">report summary</span>
        {toolResult.reportType ? <span>type: {toolResult.reportType}</span> : null}
        {toolResult.runDir ? <span>run dir: {toolResult.runDir}</span> : null}
        {summary.success_count != null ? <span>success: {summary.success_count}</span> : null}
        {summary.failure_count != null ? <span>failures: {summary.failure_count}</span> : null}
        {toolResult.reportPreview ? <span>{toolResult.reportPreview}</span> : null}
      </div>
    );
  }

  if (toolResult.type === 'audit_summary') {
    const failedSteps = Array.isArray(toolResult.failedSteps) ? toolResult.failedSteps : [];
    return (
      <div className="tool-result-card">
        <span className="tool-result-title">aws audit</span>
        {toolResult.accountId ? <span>account: {toolResult.accountId}</span> : null}
        {toolResult.runDir ? <span>run dir: {toolResult.runDir}</span> : null}
        <span>success: {toolResult.successCount ?? 0}</span>
        <span>failures: {toolResult.failureCount ?? 0}</span>
        <span>skipped: {toolResult.skippedCount ?? 0}</span>
        {Array.isArray(toolResult.selectedRegions) && toolResult.selectedRegions.length > 0 ? (
          <span>regions: {toolResult.selectedRegions.join(', ')}</span>
        ) : null}
        {Array.isArray(toolResult.selectedServices) && toolResult.selectedServices.length > 0 ? (
          <span>services: {toolResult.selectedServices.join(', ')}</span>
        ) : null}
        {failedSteps.length > 0 ? (
          <div className="tool-result-item">
            <span>failed steps:</span>
            {failedSteps.map((step) => (
              <span key={step.step || step.stderr_path}>{step.step || 'unknown step'}</span>
            ))}
          </div>
        ) : null}
      </div>
    );
  }

  if (toolResult.type === 's3_report_summary') {
    return (
      <div className="tool-result-card">
        <span className="tool-result-title">s3 cloudwatch report</span>
        {toolResult.bucket ? <span>bucket: {toolResult.bucket}</span> : null}
        {toolResult.runDir ? <span>run dir: {toolResult.runDir}</span> : null}
        <span>success: {toolResult.successCount ?? 0}</span>
        <span>failures: {toolResult.failureCount ?? 0}</span>
        <span>skipped: {toolResult.skippedCount ?? 0}</span>
      </div>
    );
  }

  return null;
}

export default ToolResultCard;

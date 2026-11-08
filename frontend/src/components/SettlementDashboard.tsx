import React, { useState } from 'react';

interface ReportSummary {
  total_count: number;
  total_amount: number;
  success_count: number;
  failed_count: number;
}

interface ReportData {
  report_id: string;
  business_date: string;
  summary: ReportSummary;
}

export const SettlementDashboard: React.FC = () => {
  const [date, setDate] = useState<string>('2026-10-20');
  const [report, setReport] = useState<ReportData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);

  const fetchReport = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`/api/v1/settlement-reports?date=${date}`);
      if (!res.ok) {
        throw new Error('Failed to fetch settlement report');
      }
      const data: ReportData = await res.json();
      setReport(data);
    } catch (err: any) {
      setError(err.message || 'Error occurred');
      setReport(null);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: '20px' }}>
      <h2>Settlement Daily Report (S6 Dashboard)</h2>
      <div style={{ marginBottom: '15px' }}>
        <input
          type="date"
          data-testid="report-date-input"
          value={date}
          onChange={(e) => setDate(e.target.value)}
        />
        <button data-testid="fetch-report-btn" onClick={fetchReport} disabled={loading}>
          {loading ? 'Loading...' : 'Fetch Report'}
        </button>
      </div>

      {error && (
        <div data-testid="error-banner" style={{ color: 'red', marginBottom: '10px' }}>
          {error}
        </div>
      )}

      {report && (
        <div data-testid="report-metrics-container">
          <p><strong>Report ID:</strong> {report.report_id}</p>
          <div data-testid="metric-total-count">Total Count: {report.summary.total_count}</div>
          <div data-testid="metric-total-amount">Total Amount: {report.summary.total_amount} IRR</div>
          <div data-testid="metric-success-count">Success: {report.summary.success_count}</div>
          <div data-testid="metric-failed-count">Failed: {report.summary.failed_count}</div>
        </div>
      )}
    </div>
  );
};
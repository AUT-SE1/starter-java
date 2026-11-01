import { useState, useEffect } from 'react';
import { api } from './api/client';
import type { components } from './api/schema';

type SettlementReportData = components['schemas']['SettlementReportData'];

interface ProblemDetails {
  type?: string;
  title?: string;
  status?: number;
  code?: string;
  detail?: string;
  correlation_id?: string;
}

export default function App() {
  const [healthStatus, setHealthStatus] = useState<string>('در حال بررسی...');
  const [dbStatus, setDbStatus] = useState<string>('در حال بررسی...');
  const [errorDetails, setErrorDetails] = useState<ProblemDetails | null>(null);
  const [loadingHealth, setLoadingHealth] = useState<boolean>(false);

  const [reportDate, setReportDate] = useState<string>(() => {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    return yesterday.toISOString().split('T')[0];
  });
  const [reportData, setReportData] = useState<SettlementReportData | null>(null);
  const [loadingReport, setLoadingReport] = useState<boolean>(false);
  const [reportError, setReportError] = useState<string | null>(null);

  const checkStatus = async () => {
    setLoadingHealth(true);
    setErrorDetails(null);

    try {
      const { data } = await api.GET('/health');
      if (data && data.status) {
        setHealthStatus(data.status.toUpperCase());
      } else {
        setHealthStatus('DOWN');
      }
    } catch {
      setHealthStatus('UNREACHABLE');
    }

    try {
      const resReady = await fetch('/ready', {
        headers: {
          'X-Correlation-ID': crypto.randomUUID(),
        },
      });

      if (resReady.ok) {
        const data = await resReady.json();
        setDbStatus(data.database || 'UP');
      } else {
        setDbStatus('DOWN');
        const errJson: ProblemDetails = await resReady.json();
        setErrorDetails(errJson);
      }
    } catch {
      setDbStatus('DOWN');
      setErrorDetails({
        title: 'خطای شبکه',
        detail: 'عدم برقراری ارتباط با سرویس یا پایگاه‌داده.',
      });
    } finally {
      setLoadingHealth(false);
    }
  };

  const fetchSettlementReport = async () => {
    setLoadingReport(true);
    setReportError(null);

    try {
      const { data, error } = await api.GET('/settlement-reports', {
        params: {
          query: {
            date: reportDate,
          },
        },
      });

      if (error) {
        setReportError('خطا در دریافت گزارش از سامانه تسویه.');
        setReportData(null);
      } else if (data) {
        setReportData(data);
      }
    } catch (err: any) {
      setReportError(err?.message || 'خطای غیرمنتظره در ارتباط با سرور.');
      setReportData(null);
    } finally {
      setLoadingReport(false);
    }
  };

  useEffect(() => {
    checkStatus();
    fetchSettlementReport();
  }, []);

  return (
    <div style={containerStyle}>
      <header style={{ textAlign: 'center', marginBottom: '28px' }}>
        <h2 style={{ margin: '0 0 6px 0', color: '#0f172a' }}>داشبورد سامانه تسویه (S5)</h2>
        <span style={{ fontSize: '13px', color: '#64748b' }}>پایش وضعیت سرویس و گزارش تسویه روزانه</span>
      </header>

      {/* ۱. وضعیت سلامت سرویس */}
      <div style={cardStyle}>
        <h3 style={sectionTitleStyle}>وضعیت سلامت سرویس</h3>
        <div style={rowStyle}>
          <span>وضعیت پروسس (Liveness):</span>
          <span style={badgeStyle(healthStatus === 'UP' || healthStatus === 'OK')}>{healthStatus}</span>
        </div>

        <div style={rowStyle}>
          <span>اتصال به دیتابیس (Readiness):</span>
          <span style={badgeStyle(dbStatus === 'UP')}>{dbStatus}</span>
        </div>

        <button onClick={checkStatus} disabled={loadingHealth} style={buttonStyle}>
          {loadingHealth ? 'در حال بررسی...' : 'بررسی مجدد سلامت'}
        </button>
      </div>

      {errorDetails && (
        <div style={errorCardStyle}>
          <h4 style={{ margin: '0 0 8px 0', color: '#b91c1c' }}>{errorDetails.title || 'خطا در دسترسی به دیتابیس'}</h4>
          <p style={{ margin: '4px 0', fontSize: '14px' }}>
            <strong>پیام:</strong> {errorDetails.detail}
          </p>
          {errorDetails.correlation_id && (
            <p style={{ margin: '4px 0', fontSize: '13px', color: '#555' }}>
              <strong>Correlation ID:</strong> <code>{errorDetails.correlation_id}</code>
            </p>
          )}
        </div>
      )}

      {/* ۲. گزارش تسویه روزانه */}
      <div style={{ ...cardStyle, marginTop: '24px' }}>
        <h3 style={sectionTitleStyle}>گزارش تسویه روزانه (Settlement Daily Report)</h3>
        
        <div style={{ display: 'flex', gap: '10px', alignItems: 'center', marginBottom: '18px' }}>
          <label style={{ fontSize: '14px', fontWeight: 'bold' }}>تاریخ کاری:</label>
          <input
            type="date"
            value={reportDate}
            onChange={(e) => setReportDate(e.target.value)}
            style={inputStyle}
          />
          <button
            onClick={fetchSettlementReport}
            disabled={loadingReport || !reportDate}
            style={{ ...buttonStyle, marginTop: 0, width: 'auto', padding: '8px 18px' }}
          >
            {loadingReport ? 'در حال دریافت...' : 'دریافت گزارش'}
          </button>
        </div>

        {reportError && (
          <div style={{ ...errorCardStyle, margin: '12px 0' }}>
            <span style={{ fontSize: '13px', color: '#b91c1c' }}>{reportError}</span>
          </div>
        )}

        {reportData && (
          <div>
            <div style={{ fontSize: '12px', color: '#64748b', marginBottom: '14px' }}>
              شناسه یکتای گزارش: <code>{reportData.report_id}</code>
            </div>

            <div style={statsGridStyle}>
              <div style={statBoxStyle}>
                <div style={statLabelStyle}>تعداد کل</div>
                <div style={statValueStyle}>{reportData.summary.totalCount.toLocaleString('fa-IR')}</div>
              </div>
              <div style={statBoxStyle}>
                <div style={statLabelStyle}>مبلغ کل (ریال)</div>
                <div style={statValueStyle}>{reportData.summary.totalAmount.toLocaleString('fa-IR')}</div>
              </div>
              <div style={statBoxStyle}>
                <div style={statLabelStyle}>موفق</div>
                <div style={{ ...statValueStyle, color: '#16a34a' }}>
                  {reportData.summary.successCount.toLocaleString('fa-IR')}
                </div>
              </div>
              <div style={statBoxStyle}>
                <div style={statLabelStyle}>ناموفق</div>
                <div style={{ ...statValueStyle, color: '#dc2626' }}>
                  {reportData.summary.failedCount.toLocaleString('fa-IR')}
                </div>
              </div>
            </div>

            <h4 style={{ margin: '18px 0 10px 0', fontSize: '15px' }}>ریز تراکنش‌های تسویه</h4>
            {reportData.transactions.length === 0 ? (
              <div style={{ textAlign: 'center', color: '#94a3b8', padding: '16px' }}>
                هیچ تراکنشی برای این تاریخ ثبت نشده است.
              </div>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table style={tableStyle}>
                  <thead>
                    <tr style={{ backgroundColor: '#f8fafc', textAlign: 'right' }}>
                      <th style={thStyle}>شناسه</th>
                      <th style={thStyle}>شبا مقصد</th>
                      <th style={thStyle}>مبلغ (ریال)</th>
                      <th style={thStyle}>نوع</th>
                      <th style={thStyle}>وضعیت</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reportData.transactions.map((tx) => (
                      <tr key={tx.transactionId} style={{ borderBottom: '1px solid #f1f5f9' }}>
                        <td style={tdStyle}>
                          <span title={tx.transactionId}>{tx.transactionId.substring(0, 8)}...</span>
                        </td>
                        <td style={tdStyle}>
                          <span style={{ direction: 'ltr', display: 'inline-block', fontFamily: 'monospace' }}>
                            {tx.destinationIban}
                          </span>
                        </td>
                        <td style={tdStyle}>{tx.amount.toLocaleString('fa-IR')}</td>
                        <td style={tdStyle}>
                          <span style={typeBadgeStyle}>{tx.settlementType}</span>
                        </td>
                        <td style={tdStyle}>
                          <span style={statusBadgeStyle(tx.status)}>{tx.status}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

// تعریف کامل استایل‌ها
const containerStyle: React.CSSProperties = {
  fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  maxWidth: '780px',
  margin: '32px auto',
  padding: '0 16px',
  direction: 'rtl',
};

const cardStyle: React.CSSProperties = {
  backgroundColor: '#fff',
  border: '1px solid #e2e8f0',
  borderRadius: '10px',
  padding: '20px',
  boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
};

const sectionTitleStyle: React.CSSProperties = {
  margin: '0 0 16px 0',
  fontSize: '16px',
  fontWeight: '600',
  color: '#1e293b',
  borderBottom: '1px solid #f1f5f9',
  paddingBottom: '8px',
};

const rowStyle: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  marginBottom: '14px',
};

const badgeStyle = (isUp: boolean): React.CSSProperties => ({
  backgroundColor: isUp ? '#dcfce7' : '#fee2e2',
  color: isUp ? '#15803d' : '#b91c1c',
  padding: '4px 10px',
  borderRadius: '9999px',
  fontWeight: '600',
  fontSize: '12px',
});

const buttonStyle: React.CSSProperties = {
  width: '100%',
  padding: '9px 16px',
  marginTop: '10px',
  backgroundColor: '#2563eb',
  color: '#fff',
  border: 'none',
  borderRadius: '6px',
  cursor: 'pointer',
  fontSize: '14px',
  fontWeight: '500',
};

const inputStyle: React.CSSProperties = {
  padding: '8px 12px',
  border: '1px solid #cbd5e1',
  borderRadius: '6px',
  fontSize: '14px',
  outline: 'none',
  fontFamily: 'inherit',
};

const errorCardStyle: React.CSSProperties = {
  marginTop: '16px',
  backgroundColor: '#fef2f2',
  border: '1px solid #fecaca',
  borderRadius: '8px',
  padding: '14px',
};

const statsGridStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))',
  gap: '12px',
  marginBottom: '18px',
};

const statBoxStyle: React.CSSProperties = {
  backgroundColor: '#f8fafc',
  border: '1px solid #e2e8f0',
  borderRadius: '8px',
  padding: '12px',
  textAlign: 'center',
};

const statLabelStyle: React.CSSProperties = {
  fontSize: '12px',
  color: '#64748b',
  marginBottom: '4px',
};

const statValueStyle: React.CSSProperties = {
  fontSize: '18px',
  fontWeight: 'bold',
  color: '#0f172a',
};

const tableStyle: React.CSSProperties = {
  width: '100%',
  borderCollapse: 'collapse',
  fontSize: '13px',
};

const thStyle: React.CSSProperties = {
  padding: '10px 8px',
  borderBottom: '2px solid #e2e8f0',
  color: '#475569',
  fontSize: '12px',
};

const tdStyle: React.CSSProperties = {
  padding: '10px 8px',
  verticalAlign: 'middle',
};

const typeBadgeStyle: React.CSSProperties = {
  backgroundColor: '#f1f5f9',
  color: '#475569',
  padding: '2px 8px',
  borderRadius: '4px',
  fontSize: '11px',
  fontWeight: 'bold',
};

const statusBadgeStyle = (status: string): React.CSSProperties => {
  const isSuccess = status === 'SUCCESS' || status === 'SETTLED_CONFIRMED';
  const isPending = status === 'PENDING';
  return {
    backgroundColor: isSuccess ? '#dcfce7' : isPending ? '#fef9c3' : '#fee2e2',
    color: isSuccess ? '#15803d' : isPending ? '#a16207' : '#b91c1c',
    padding: '2px 8px',
    borderRadius: '4px',
    fontSize: '11px',
    fontWeight: 'bold',
  };
};
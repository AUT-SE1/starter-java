/// <reference types="jest" />
import React from 'react';
import { render, screen } from '@testing-library/react';
import { fireEvent, waitFor } from '@testing-library/dom';
import { SettlementDashboard } from './SettlementDashboard';

describe('SettlementDashboard Component Tests', () => {
  beforeEach(() => {
    jest.resetAllMocks();
  });

  test('Frontend 1: Renders settlement report metrics on successful 200 API response', async () => {
    const mockApiResponse = {
      report_id: 'baad7125-1753-3fcb-88bb-1bafc04ced48',
      business_date: '2026-10-20',
      summary: {
        total_count: 5,
        total_amount: 500000,
        success_count: 4,
        failed_count: 1,
      },
    };

    globalThis.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => mockApiResponse,
    });

    render(<SettlementDashboard />);

    const fetchBtn = screen.getByTestId('fetch-report-btn');
    fireEvent.click(fetchBtn);

    await waitFor(() => {
      expect(screen.getByTestId('report-metrics-container')).toBeTruthy();
      expect(screen.getByTestId('metric-total-count').textContent).toContain('Total Count: 5');
      expect(screen.getByTestId('metric-total-amount').textContent).toContain('Total Amount: 500000 IRR');
      expect(screen.getByTestId('metric-success-count').textContent).toContain('Success: 4');
      expect(screen.getByTestId('metric-failed-count').textContent).toContain('Failed: 1');
    });
  });

  test('Frontend 2: Displays error banner when API returns 400 Bad Request', async () => {
    globalThis.fetch = jest.fn().mockResolvedValue({
      ok: false,
      status: 400,
    });

    render(<SettlementDashboard />);

    const fetchBtn = screen.getByTestId('fetch-report-btn');
    fireEvent.click(fetchBtn);

    await waitFor(() => {
      const errorBanner = screen.getByTestId('error-banner');
      expect(errorBanner).toBeTruthy();
      expect(errorBanner.textContent).toContain('Failed to fetch settlement report');
    });
  });
});
import { apiClient } from './client'
import type {
  PaymentStats,
  RevenueByDate,
  PaymentsByStatus,
  FraudScoreDistribution,
  TopCustomer,
} from '@/types/analytics'
import type { Payment } from '@/types/payment'

export const analyticsApi = {
  /**
   * Get payment statistics
   */
  getStats: async (days: number = 30): Promise<PaymentStats> => {
    // For MVP, calculate from payments list
    // In production, this would be a dedicated analytics endpoint
    const payments = await apiClient.get<{ data: Payment[] }>('/payments', {
      params: { limit: 100 },
    })
    
    const paymentsData = payments.data.data || []
    
    // Filter by date range
    const cutoffDate = new Date()
    cutoffDate.setDate(cutoffDate.getDate() - days)
    
    const filteredPayments = paymentsData.filter(
      (p) => new Date(p.createdAt) >= cutoffDate
    )
    
    return {
      totalCount: filteredPayments.length,
      totalAmount: filteredPayments.reduce((sum, p) => sum + p.amount, 0),
      averageAmount:
        filteredPayments.length > 0
          ? filteredPayments.reduce((sum, p) => sum + p.amount, 0) / filteredPayments.length
          : 0,
      authorizedCount: filteredPayments.filter((p) => p.status === 'authorized').length,
      capturedCount: filteredPayments.filter((p) => p.status === 'captured').length,
      failedCount: filteredPayments.filter((p) => p.status === 'failed').length,
      declinedCount: filteredPayments.filter((p) => p.status === 'declined').length,
      refundedCount: filteredPayments.filter(
        (p) => p.status === 'refunded' || p.status === 'partially_refunded'
      ).length,
    }
  },

  /**
   * Get revenue by date
   */
  getRevenueByDate: async (days: number = 30): Promise<RevenueByDate[]> => {
    const payments = await apiClient.get<{ data: Payment[] }>('/payments', {
      params: { limit: 100 },
    })
    
    const paymentsData = payments.data.data || []
    
    // Group by date
    const revenueMap = new Map<string, { revenue: number; count: number }>()
    
    const cutoffDate = new Date()
    cutoffDate.setDate(cutoffDate.getDate() - days)
    
    paymentsData
      .filter((p) => p.status === 'captured' && new Date(p.createdAt) >= cutoffDate)
      .forEach((payment) => {
        const date = new Date(payment.createdAt).toISOString().split('T')[0]
        const existing = revenueMap.get(date) || { revenue: 0, count: 0 }
        revenueMap.set(date, {
          revenue: existing.revenue + payment.amount,
          count: existing.count + 1,
        })
      })
    
    // Convert to array and sort
    return Array.from(revenueMap.entries())
      .map(([date, data]) => ({
        date,
        revenue: data.revenue,
        paymentCount: data.count,
        averageAmount: data.count > 0 ? data.revenue / data.count : 0,
      }))
      .sort((a, b) => a.date.localeCompare(b.date))
  },

  /**
   * Get payments by status
   */
  getPaymentsByStatus: async (days: number = 30): Promise<PaymentsByStatus[]> => {
    const payments = await apiClient.get<{ data: Payment[] }>('/payments', {
      params: { limit: 100 },
    })
    
    const paymentsData = payments.data.data || []
    
    const cutoffDate = new Date()
    cutoffDate.setDate(cutoffDate.getDate() - days)
    
    const filteredPayments = paymentsData.filter(
      (p) => new Date(p.createdAt) >= cutoffDate
    )
    
    const statusCount = new Map<string, number>()
    
    filteredPayments.forEach((payment) => {
      const count = statusCount.get(payment.status) || 0
      statusCount.set(payment.status, count + 1)
    })
    
    const total = filteredPayments.length
    
    return Array.from(statusCount.entries()).map(([status, count]) => ({
      status,
      count,
      percentage: total > 0 ? (count / total) * 100 : 0,
    }))
  },

  /**
   * Get fraud score distribution
   */
  getFraudScoreDistribution: async (days: number = 30): Promise<FraudScoreDistribution[]> => {
    const payments = await apiClient.get<{ data: Payment[] }>('/payments', {
      params: { limit: 100 },
    })
    
    const paymentsData = payments.data.data || []
    
    const cutoffDate = new Date()
    cutoffDate.setDate(cutoffDate.getDate() - days)
    
    const filteredPayments = paymentsData.filter(
      (p) => new Date(p.createdAt) >= cutoffDate && p.fraudDetails?.score !== undefined
    )
    
    const ranges = [
      { range: '0-10 (Very Low)', min: 0, max: 10 },
      { range: '10-25 (Low)', min: 10, max: 25 },
      { range: '25-50 (Medium)', min: 25, max: 50 },
      { range: '50-75 (High)', min: 50, max: 75 },
      { range: '75-100 (Critical)', min: 75, max: 100 },
    ]
    
    return ranges.map((range) => ({
      range: range.range,
      count: filteredPayments.filter(
        (p) =>
          p.fraudDetails!.score >= range.min && p.fraudDetails!.score < range.max
      ).length,
    }))
  },

  /**
   * Get top customers
   */
  getTopCustomers: async (days: number = 30, limit: number = 10): Promise<TopCustomer[]> => {
    const payments = await apiClient.get<{ data: Payment[] }>('/payments', {
      params: { limit: 100 },
    })
    
    const paymentsData = payments.data.data || []
    
    const cutoffDate = new Date()
    cutoffDate.setDate(cutoffDate.getDate() - days)
    
    const filteredPayments = paymentsData.filter(
      (p) => p.status === 'captured' && new Date(p.createdAt) >= cutoffDate
    )
    
    // Group by customer
    const customerMap = new Map<string, { email: string; totalSpent: number; count: number }>()
    
    filteredPayments.forEach((payment) => {
      const customerId = payment.customer?.id || 'anonymous'
      const email = payment.customer?.email || 'anonymous@example.com'
      
      const existing = customerMap.get(customerId) || {
        email,
        totalSpent: 0,
        count: 0,
      }
      
      customerMap.set(customerId, {
        email,
        totalSpent: existing.totalSpent + payment.amount,
        count: existing.count + 1,
      })
    })
    
    // Convert to array and sort by total spent
    return Array.from(customerMap.entries())
      .map(([customerId, data]) => ({
        customerId,
        email: data.email,
        totalSpent: data.totalSpent,
        paymentCount: data.count,
      }))
      .sort((a, b) => b.totalSpent - a.totalSpent)
      .slice(0, limit)
  },
}

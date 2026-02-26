export interface PaymentStats {
    totalCount: number
    totalAmount: number
    averageAmount: number
    authorizedCount: number
    capturedCount: number
    failedCount: number
    declinedCount: number
    refundedCount: number
  }
  
  export interface RevenueByDate {
    date: string
    revenue: number
    paymentCount: number
    averageAmount: number
  }
  
  export interface PaymentsByStatus {
    status: string
    count: number
    percentage: number
  }
  
  export interface FraudScoreDistribution {
    range: string
    count: number
  }
  
  export interface TopCustomer {
    customerId: string
    email: string
    totalSpent: number
    paymentCount: number
  }
  
  export interface TimeRange {
    label: string
    days: number
  }
  
  export const TIME_RANGES: TimeRange[] = [
    { label: 'Last 7 days', days: 7 },
    { label: 'Last 30 days', days: 30 },
    { label: 'Last 90 days', days: 90 },
    { label: 'Last year', days: 365 },
  ]
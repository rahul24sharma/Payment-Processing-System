import { useQuery } from '@tanstack/react-query'
import { analyticsApi } from '@/api/analytics'

export function usePaymentStats(days: number = 30) {
  return useQuery({
    queryKey: ['payment-stats', days],
    queryFn: () => analyticsApi.getStats(days),
    staleTime: 60000, // 1 minute
  })
}

export function useRevenueByDate(days: number = 30) {
  return useQuery({
    queryKey: ['revenue-by-date', days],
    queryFn: () => analyticsApi.getRevenueByDate(days),
    staleTime: 60000,
  })
}

export function usePaymentsByStatus(days: number = 30) {
  return useQuery({
    queryKey: ['payments-by-status', days],
    queryFn: () => analyticsApi.getPaymentsByStatus(days),
    staleTime: 60000,
  })
}

export function useFraudScoreDistribution(days: number = 30) {
  return useQuery({
    queryKey: ['fraud-distribution', days],
    queryFn: () => analyticsApi.getFraudScoreDistribution(days),
    staleTime: 60000,
  })
}

export function useTopCustomers(days: number = 30, limit: number = 10) {
  return useQuery({
    queryKey: ['top-customers', days, limit],
    queryFn: () => analyticsApi.getTopCustomers(days, limit),
    staleTime: 60000,
  })
}
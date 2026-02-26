export interface RefundWithPayment {
    refund: {
      id: string
      amount: number
      currency: string
      status: string
      reason?: string
      createdAt: string
      completedAt?: string
      failureReason?: string
    }
    payment: {
      id: string
      amount: number
      currency: string
      customerEmail?: string
      createdAt: string
    }
  }
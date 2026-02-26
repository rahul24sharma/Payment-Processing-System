export enum PaymentStatus {
    PENDING = 'pending',
    AUTHORIZED = 'authorized',
    CAPTURED = 'captured',
    VOID = 'void',
    REFUNDED = 'refunded',
    PARTIALLY_REFUNDED = 'partially_refunded',
    FAILED = 'failed',
    DECLINED = 'declined',
    EXPIRED = 'expired',
  }
  
  export interface Payment {
    id: string
    object: string
    amount: number
    currency: string
    status: PaymentStatus
    captured: boolean
    paymentMethod?: {
      id: string
      type: string
      card?: {
        brand: string
        last4: string
        expMonth: number
        expYear: number
        expired: boolean
      }
    }
    customer?: {
      id: string
      email: string
      name?: string
    }
    fraudDetails?: {
      score: number
      riskLevel: string
    }
    refunds?: Refund[]
    metadata?: Record<string, any>
    description?: string
    failureReason?: string
    failureCode?: string
    createdAt: string
    authorizedAt?: string
    capturedAt?: string
    nextAction?: PaymentNextAction
  }

  export interface PaymentNextAction {
    type: string
    clientSecret?: string
    paymentIntentId?: string
    processor?: string
    status?: string
  }
  
  export interface Refund {
    id: string
    object: string
    paymentId: string
    amount: number
    currency: string
    status: string
    reason?: string
    failureReason?: string
    createdAt: string
    completedAt?: string
  }
  
  export interface CreatePaymentRequest {
    amount: number
    currency: string
    paymentMethod: {
      type: string
      cardToken?: string
      savedPaymentMethodId?: string
    }
    customer?: {
      email: string
      name?: string
      address?: {
        line1?: string
        line2?: string
        city?: string
        state?: string
        postalCode?: string
        country?: string
      }
    }
    capture?: boolean
    metadata?: Record<string, any>
    description?: string
  }
  
  export interface RefundRequest {
    amount?: number
    reason?: string
  }

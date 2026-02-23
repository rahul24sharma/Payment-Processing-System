export interface WebhookEndpoint {
    id: string
    merchantId: string
    url: string
    secret: string
    events: string[]
    isActive: boolean
    createdAt: string
    updatedAt?: string
  }
  
  export interface Webhook {
    id: string
    merchantId: string
    endpointId: string
    url: string
    eventType: string
    payload: string
    status: 'pending' | 'delivered' | 'failed'
    attempts: number
    lastResponseCode?: number
    lastError?: string
    createdAt: string
    deliveredAt?: string
    failedAt?: string
    nextRetryAt?: string
  }
  
  export interface CreateWebhookEndpointRequest {
    url: string
    events: string[]
  }
  
  export const AVAILABLE_EVENTS = [
    'PAYMENT_CREATED',
    'PAYMENT_AUTHORIZED',
    'PAYMENT_CAPTURED',
    'PAYMENT_FAILED',
    'PAYMENT_DECLINED',
    'PAYMENT_REFUNDED',
    'PAYMENT_VOIDED',
  ] as const
  
  export const EVENT_DESCRIPTIONS: Record<string, string> = {
    'PAYMENT_CREATED': 'Occurs when a payment is created',
    'PAYMENT_AUTHORIZED': 'Occurs when a payment is authorized',
    'PAYMENT_CAPTURED': 'Occurs when funds are captured',
    'PAYMENT_FAILED': 'Occurs when a payment fails',
    'PAYMENT_DECLINED': 'Occurs when a payment is declined',
    'PAYMENT_REFUNDED': 'Occurs when a refund is issued',
    'PAYMENT_VOIDED': 'Occurs when an authorization is voided',
  }
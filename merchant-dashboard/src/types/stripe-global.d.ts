interface StripeConfirmCardPaymentResult {
  error?: {
    message?: string
  }
  paymentIntent?: {
    id: string
    status: string
  }
}

interface StripeCardElement {
  mount(selectorOrElement: string | HTMLElement): void
  destroy(): void
}

interface StripeElements {
  create(type: 'card', options?: Record<string, unknown>): StripeCardElement
}

interface StripeCreatePaymentMethodResult {
  error?: {
    message?: string
  }
  paymentMethod?: {
    id: string
  }
}

interface StripeInstance {
  elements(): StripeElements
  createPaymentMethod(input: {
    type: 'card'
    card: StripeCardElement
    billing_details?: {
      name?: string
      email?: string
      address?: {
        line1?: string
        line2?: string
        city?: string
        state?: string
        postal_code?: string
        country?: string
      }
    }
  }): Promise<StripeCreatePaymentMethodResult>
  confirmCardPayment(clientSecret: string): Promise<StripeConfirmCardPaymentResult>
}

interface Window {
  Stripe?: (publishableKey: string) => StripeInstance
}

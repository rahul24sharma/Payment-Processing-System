export interface MerchantSettings {
    id?: string
    businessName: string
    email: string
    createdAt?: string
    phone?: string
    website?: string
    address?: {
      line1: string
      line2?: string
      city: string
      state: string
      postalCode: string
      country: string
    }
    bankAccount?: {
      accountHolderName: string
      accountNumber: string
      accountNumberLast4?: string
      routingNumber: string
      routingNumberLast4?: string
      accountType: 'checking' | 'savings'
    }
    notifications?: {
      emailOnPayment: boolean
      emailOnRefund: boolean
      emailOnPayout: boolean
      emailOnFraud: boolean
    }
    riskProfile?: string
    status?: string
  }
  
  export interface UpdateProfileRequest {
    businessName?: string
    phone?: string
    website?: string
  }
  
  export interface ChangePasswordRequest {
    currentPassword: string
    newPassword: string
    confirmPassword: string
  }
  
  export interface UpdateBankAccountRequest {
    accountHolderName: string
    accountNumber: string
    routingNumber: string
    accountType: 'checking' | 'savings'
  }
  
  export interface UpdateNotificationPreferencesRequest {
    emailOnPayment: boolean
    emailOnRefund: boolean
    emailOnPayout: boolean
    emailOnFraud: boolean
  }

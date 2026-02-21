export function formatCurrency(amount: number, currency: string = 'USD'): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency.toUpperCase(),
    }).format(amount / 100) // Convert cents to dollars
  }
  
  export function formatDate(date: string | Date): string {
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(date))
  }
  
  export function formatStatus(status: string): string {
    return status
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ')
  }
  
  export function getStatusColor(status: string): string {
    switch (status.toLowerCase()) {
      case 'captured':
      case 'succeeded':
        return 'green'
      case 'authorized':
      case 'pending':
        return 'blue'
      case 'failed':
      case 'declined':
        return 'red'
      case 'void':
      case 'refunded':
        return 'gray'
      default:
        return 'gray'
    }
  }
  
  export function getRiskLevelColor(riskLevel: string): string {
    switch (riskLevel.toLowerCase()) {
      case 'very_low':
      case 'low':
        return 'green'
      case 'medium':
        return 'yellow'
      case 'high':
        return 'orange'
      case 'critical':
        return 'red'
      default:
        return 'gray'
    }
  }
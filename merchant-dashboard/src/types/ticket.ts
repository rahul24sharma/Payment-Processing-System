export interface TicketComment {
  id: string
  authorType: string
  message: string
  createdAt: string
}

export interface Ticket {
  id: string
  merchantId: string
  title: string
  description: string
  category: string
  priority: string
  status: string
  createdAt: string
  updatedAt?: string | null
  closedAt?: string | null
  comments: TicketComment[]
}

export interface CreateTicketRequest {
  title: string
  description: string
  category?: string
  priority?: string
}

export interface AddTicketCommentRequest {
  message: string
}

export interface UpdateTicketStatusRequest {
  status: string
}

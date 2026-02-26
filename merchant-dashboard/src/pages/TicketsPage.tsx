import { useEffect, useId, useMemo, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { useAddTicketComment, useCreateTicket, useTicket, useTickets, useUpdateTicketStatus } from '@/hooks/useTickets'
import { useToast } from '@/contexts/ToastContext'
import type { Ticket } from '@/types/ticket'
import './TicketsPage.css'

const STATUS_OPTIONS = ['OPEN', 'IN_PROGRESS', 'WAITING_ON_MERCHANT', 'RESOLVED', 'CLOSED'] as const
const PRIORITY_OPTIONS = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'] as const
const CATEGORY_OPTIONS = ['GENERAL', 'PAYMENTS', 'REFUNDS', 'WEBHOOKS', 'API_KEYS', 'ACCOUNT'] as const

export default function TicketsPage() {
  const [statusFilter, setStatusFilter] = useState<string>('ALL')
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL')
  const [priorityFilter, setPriorityFilter] = useState<string>('ALL')
  const [sortBy, setSortBy] = useState<'newest' | 'oldest' | 'priority'>('newest')
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedTicketId, setSelectedTicketId] = useState<string | null>(null)
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [newTitle, setNewTitle] = useState('')
  const [newDescription, setNewDescription] = useState('')
  const [newCategory, setNewCategory] = useState<string>('GENERAL')
  const [newPriority, setNewPriority] = useState<string>('MEDIUM')
  const [commentDraft, setCommentDraft] = useState('')
  const ticketDialogTitleId = useId()
  const ticketDialogPanelRef = useRef<HTMLDivElement | null>(null)

  const toast = useToast()
  const { data: tickets = [], isLoading, error } = useTickets(statusFilter === 'ALL' ? undefined : statusFilter)
  const { data: selectedTicket } = useTicket(selectedTicketId ?? undefined)
  const createTicket = useCreateTicket()
  const updateStatus = useUpdateTicketStatus()
  const addComment = useAddTicketComment()

  const filteredTickets = useMemo(() => {
    const query = searchQuery.trim().toLowerCase()
    const priorityRank: Record<string, number> = { URGENT: 4, HIGH: 3, MEDIUM: 2, LOW: 1 }

    return [...tickets]
      .filter((ticket) => (categoryFilter === 'ALL' ? true : ticket.category === categoryFilter))
      .filter((ticket) => (priorityFilter === 'ALL' ? true : ticket.priority === priorityFilter))
      .filter((ticket) => {
        if (!query) return true
        return (
          ticket.title.toLowerCase().includes(query) ||
          ticket.description.toLowerCase().includes(query) ||
          ticket.id.toLowerCase().includes(query)
        )
      })
      .sort((a, b) => {
        if (sortBy === 'oldest') {
          return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
        }
        if (sortBy === 'priority') {
          const diff = (priorityRank[b.priority] || 0) - (priorityRank[a.priority] || 0)
          if (diff !== 0) return diff
        }
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      })
  }, [tickets, categoryFilter, priorityFilter, searchQuery, sortBy])

  const stats = useMemo(() => {
    const total = filteredTickets.length
    const open = filteredTickets.filter((ticket) => ['OPEN', 'IN_PROGRESS', 'WAITING_ON_MERCHANT'].includes(ticket.status)).length
    const closed = filteredTickets.filter((ticket) => ['RESOLVED', 'CLOSED'].includes(ticket.status)).length
    const urgent = filteredTickets.filter((ticket) => ticket.priority === 'URGENT').length
    return { total, open, closed, urgent }
  }, [filteredTickets])

  const activeTicket: Ticket | null = selectedTicket ?? filteredTickets.find((ticket) => ticket.id === selectedTicketId) ?? tickets.find((ticket) => ticket.id === selectedTicketId) ?? null

  const handleCreate = async (event: FormEvent) => {
    event.preventDefault()
    try {
      const created = await createTicket.mutateAsync({
        title: newTitle,
        description: newDescription,
        category: newCategory,
        priority: newPriority,
      })
      setSelectedTicketId(created.id)
      setShowCreateForm(false)
      setNewTitle('')
      setNewDescription('')
      setNewCategory('GENERAL')
      setNewPriority('MEDIUM')
      toast.success('Support ticket created')
    } catch (createError) {
      console.error(createError)
      toast.error('Failed to create support ticket')
    }
  }

  const handleStatusUpdate = async (status: string) => {
    if (!activeTicket) return
    try {
      await updateStatus.mutateAsync({ id: activeTicket.id, request: { status } })
      toast.success(`Ticket marked ${status.toLowerCase().replaceAll('_', ' ')}`)
    } catch (statusError) {
      console.error(statusError)
      toast.error('Failed to update ticket status')
    }
  }

  const handleAddComment = async (event: FormEvent) => {
    event.preventDefault()
    if (!activeTicket || !commentDraft.trim()) return
    try {
      await addComment.mutateAsync({ id: activeTicket.id, request: { message: commentDraft } })
      setCommentDraft('')
      toast.success('Comment added')
    } catch (commentError) {
      console.error(commentError)
      toast.error('Failed to add comment')
    }
  }

  useEffect(() => {
    if (!showCreateForm) return
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    ticketDialogPanelRef.current?.focus()

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !createTicket.isPending) {
        setShowCreateForm(false)
      }
    }
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.body.style.overflow = previousOverflow
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [showCreateForm, createTicket.isPending])

  return (
    <div className="tickets-page">
      <section className="tickets-page__hero">
        <div>
          <p className="tickets-page__eyebrow">Support Operations</p>
          <h1>Tickets</h1>
          <p className="tickets-page__subtitle">
            Track merchant support requests, collaborate on issue resolution, and keep payment incidents documented.
          </p>
        </div>
        <button type="button" className="tickets-page__primary-btn" onClick={() => setShowCreateForm(true)}>
          + New Ticket
        </button>
      </section>

      <section className="tickets-page__stats">
        <StatCard label="Total Tickets" value={stats.total} tone="neutral" />
        <StatCard label="Open / Active" value={stats.open} tone="warning" />
        <StatCard label="Resolved / Closed" value={stats.closed} tone="success" />
        <StatCard label="Urgent" value={stats.urgent} tone="danger" />
      </section>

      <div className="tickets-page__layout">
        <section className="tickets-panel">
          <div className="tickets-panel__toolbar">
            <div>
              <h2>All Tickets</h2>
              <p>{filteredTickets.length} shown{filteredTickets.length !== tickets.length ? ` • ${tickets.length} total` : ''}</p>
            </div>
            <div className="tickets-panel__toolbar-controls">
              <input
                className="tickets-panel__search"
                type="search"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search title, description, or ID"
                aria-label="Search tickets"
              />
              <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="tickets-panel__select">
                <option value="ALL">All statuses</option>
                {STATUS_OPTIONS.map((status) => (
                  <option key={status} value={status}>
                    {formatLabel(status)}
                  </option>
                ))}
              </select>
              <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)} className="tickets-panel__select">
                <option value="ALL">All categories</option>
                {CATEGORY_OPTIONS.map((category) => (
                  <option key={category} value={category}>
                    {formatLabel(category)}
                  </option>
                ))}
              </select>
              <select value={priorityFilter} onChange={(e) => setPriorityFilter(e.target.value)} className="tickets-panel__select">
                <option value="ALL">All priorities</option>
                {PRIORITY_OPTIONS.map((priority) => (
                  <option key={priority} value={priority}>
                    {formatLabel(priority)}
                  </option>
                ))}
              </select>
              <select value={sortBy} onChange={(e) => setSortBy(e.target.value as 'newest' | 'oldest' | 'priority')} className="tickets-panel__select">
                <option value="newest">Newest first</option>
                <option value="oldest">Oldest first</option>
                <option value="priority">Priority first</option>
              </select>
            </div>
          </div>

          {isLoading && <div className="tickets-panel__state">Loading tickets…</div>}
          {error && <div className="tickets-panel__state tickets-panel__state--error">Failed to load tickets</div>}

          {!isLoading && !error && tickets.length === 0 && (
            <div className="tickets-panel__state">No tickets yet. Create your first support ticket.</div>
          )}
          {!isLoading && !error && tickets.length > 0 && filteredTickets.length === 0 && (
            <div className="tickets-panel__state">No tickets match your current search and filters.</div>
          )}

          <div className="tickets-list">
            {filteredTickets.map((ticket) => (
              <button
                type="button"
                key={ticket.id}
                className={`ticket-row ${selectedTicketId === ticket.id ? 'ticket-row--active' : ''}`}
                onClick={() => setSelectedTicketId(ticket.id)}
              >
                <div className="ticket-row__top">
                  <span className="ticket-row__title">{ticket.title}</span>
                  <span className={`ticket-badge ticket-badge--${ticket.status.toLowerCase()}`}>{formatLabel(ticket.status)}</span>
                </div>
                <div className="ticket-row__meta">
                  <span>{ticket.id.slice(0, 8)}</span>
                  <span>{ticket.category}</span>
                  <span className={`ticket-priority ticket-priority--${ticket.priority.toLowerCase()}`}>{ticket.priority}</span>
                  <span>{new Date(ticket.createdAt).toLocaleString()}</span>
                </div>
              </button>
            ))}
          </div>
        </section>

        <section className="ticket-detail">
          {!activeTicket ? (
            <div className="ticket-detail__empty">Select a ticket to view details and conversation history.</div>
          ) : (
            <>
              <div className="ticket-detail__header">
                <div>
                  <div className="ticket-detail__eyebrow">Ticket {activeTicket.id.slice(0, 8)}</div>
                  <h2>{activeTicket.title}</h2>
                  <p>{activeTicket.description}</p>
                </div>
                <div className="ticket-detail__badges">
                  <span className={`ticket-badge ticket-badge--${activeTicket.status.toLowerCase()}`}>
                    {formatLabel(activeTicket.status)}
                  </span>
                  <span className={`ticket-priority ticket-priority--${activeTicket.priority.toLowerCase()}`}>
                    {activeTicket.priority}
                  </span>
                </div>
              </div>

              <div className="ticket-detail__actions">
                {STATUS_OPTIONS.map((status) => (
                  <button
                    key={status}
                    type="button"
                    className={`ticket-action ${activeTicket.status === status ? 'ticket-action--active' : ''}`}
                    onClick={() => handleStatusUpdate(status)}
                    disabled={updateStatus.isPending || activeTicket.status === status}
                  >
                    {formatLabel(status)}
                  </button>
                ))}
              </div>

              <div className="ticket-detail__meta-grid">
                <MetaItem label="Category" value={activeTicket.category} />
                <MetaItem label="Created" value={new Date(activeTicket.createdAt).toLocaleString()} />
                <MetaItem label="Updated" value={activeTicket.updatedAt ? new Date(activeTicket.updatedAt).toLocaleString() : '—'} />
                <MetaItem label="Closed" value={activeTicket.closedAt ? new Date(activeTicket.closedAt).toLocaleString() : '—'} />
              </div>

              <div className="ticket-thread">
                <div className="ticket-thread__header">
                  <div>
                    <h3>Conversation Timeline</h3>
                    <p>Merchant updates and support responses for this ticket.</p>
                  </div>
                  <span className="ticket-thread__count">{(activeTicket.comments ?? []).length} entries</span>
                </div>
                <div className="ticket-thread__messages">
                  {(activeTicket.comments ?? []).map((comment, index) => (
                    <div key={comment.id} className={`ticket-comment ticket-comment--${comment.authorType.toLowerCase()}`}>
                      <div className="ticket-comment__timeline" aria-hidden="true">
                        <span className="ticket-comment__dot" />
                        {index < (activeTicket.comments?.length ?? 0) - 1 ? <span className="ticket-comment__line" /> : null}
                      </div>
                      <div className="ticket-comment__card">
                        <div className="ticket-comment__header">
                          <span className="ticket-comment__author">{formatLabel(comment.authorType)}</span>
                          <span>{new Date(comment.createdAt).toLocaleString()}</span>
                        </div>
                        <p>{comment.message}</p>
                      </div>
                    </div>
                  ))}
                  {(activeTicket.comments ?? []).length === 0 && (
                    <div className="ticket-thread__empty">No comments yet.</div>
                  )}
                </div>

                <form className="ticket-thread__composer" onSubmit={handleAddComment}>
                  <label className="ticket-thread__composer-label" htmlFor="ticket-comment-composer">
                    Add update
                  </label>
                  <textarea
                    id="ticket-comment-composer"
                    value={commentDraft}
                    onChange={(e) => setCommentDraft(e.target.value)}
                    placeholder="Summarize the issue, next steps, or customer communication"
                    rows={4}
                  />
                  <div className="ticket-thread__composer-footer">
                    <small>Tip: include what changed, why, and any next action for support or the customer.</small>
                  <button type="submit" disabled={addComment.isPending || !commentDraft.trim()}>
                    {addComment.isPending ? 'Posting…' : 'Add Comment'}
                  </button>
                  </div>
                </form>
              </div>
            </>
          )}
        </section>
      </div>

      {showCreateForm && (
        <div className="tickets-modal" role="dialog" aria-modal="true" aria-labelledby={ticketDialogTitleId}>
          <button
            type="button"
            className="tickets-modal__backdrop"
            onClick={() => !createTicket.isPending && setShowCreateForm(false)}
            aria-label="Close create ticket dialog"
          />
          <div className="tickets-modal__panel" ref={ticketDialogPanelRef} tabIndex={-1}>
            <div className="tickets-modal__header">
              <div>
                <p className="tickets-modal__eyebrow">Support Request</p>
                <h2 id={ticketDialogTitleId}>Create Ticket</h2>
              </div>
              <button
                type="button"
                className="tickets-modal__close"
                onClick={() => setShowCreateForm(false)}
                aria-label="Close create ticket dialog"
                disabled={createTicket.isPending}
              >
                ×
              </button>
            </div>
            <form className="tickets-form" onSubmit={handleCreate}>
              <label>
                <span>Title</span>
                <input value={newTitle} onChange={(e) => setNewTitle(e.target.value)} required />
              </label>
              <div className="tickets-form__grid">
                <label>
                  <span>Category</span>
                  <select value={newCategory} onChange={(e) => setNewCategory(e.target.value)}>
                    {CATEGORY_OPTIONS.map((category) => (
                      <option key={category} value={category}>
                        {formatLabel(category)}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  <span>Priority</span>
                  <select value={newPriority} onChange={(e) => setNewPriority(e.target.value)}>
                    {PRIORITY_OPTIONS.map((priority) => (
                      <option key={priority} value={priority}>
                        {formatLabel(priority)}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
              <label>
                <span>Description</span>
                <textarea
                  rows={6}
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                  required
                />
              </label>
              <div className="tickets-form__actions">
                <button type="button" className="tickets-form__secondary" onClick={() => setShowCreateForm(false)}>
                  Cancel
                </button>
                <button type="submit" disabled={createTicket.isPending}>
                  {createTicket.isPending ? 'Creating…' : 'Create Ticket'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

function StatCard({ label, value, tone }: { label: string; value: number; tone: string }) {
  return (
    <div className={`tickets-stat tickets-stat--${tone}`}>
      <div className="tickets-stat__label">{label}</div>
      <div className="tickets-stat__value">{value}</div>
    </div>
  )
}

function MetaItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="ticket-meta-item">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function formatLabel(value: string) {
  return value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase())
}

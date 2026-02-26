import type { ReactNode } from 'react'
import './AsyncState.css'

type AsyncStateKind = 'loading' | 'error' | 'empty'

interface AsyncStateProps {
  kind: AsyncStateKind
  title?: string
  message?: ReactNode
  action?: ReactNode
  compact?: boolean
}

export default function AsyncState({
  kind,
  title,
  message,
  action,
  compact = false,
}: AsyncStateProps) {
  const role = kind === 'error' ? 'alert' : 'status'
  const ariaLive = kind === 'error' ? 'assertive' : 'polite'

  const defaultTitle =
    kind === 'loading' ? 'Loading' : kind === 'error' ? 'Something went wrong' : 'Nothing to show yet'

  const defaultMessage =
    kind === 'loading'
      ? 'Please wait while we load your data.'
      : kind === 'error'
        ? 'Please refresh the page or try again in a moment.'
        : 'Try adjusting your filters or create a new record.'

  return (
    <div
      className={`async-state async-state--${kind}${compact ? ' async-state--compact' : ''}`}
      role={role}
      aria-live={ariaLive}
    >
      <div className="async-state__icon" aria-hidden="true">
        {kind === 'loading' ? <span className="async-state__spinner" /> : kind === 'error' ? '!' : '·'}
      </div>
      <div className="async-state__body">
        <h3 className="async-state__title">{title ?? defaultTitle}</h3>
        <div className="async-state__message">{message ?? defaultMessage}</div>
        {action ? <div className="async-state__action">{action}</div> : null}
      </div>
    </div>
  )
}

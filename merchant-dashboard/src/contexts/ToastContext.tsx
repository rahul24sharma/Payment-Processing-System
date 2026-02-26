import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react'
import './Toast.css'

type ToastVariant = 'success' | 'error' | 'info'

interface ToastItem {
  id: number
  title?: string
  message: string
  variant: ToastVariant
  durationMs: number
}

interface ToastOptions {
  title?: string
  durationMs?: number
}

interface ToastApi {
  success: (message: string, options?: ToastOptions) => void
  error: (message: string, options?: ToastOptions) => void
  info: (message: string, options?: ToastOptions) => void
}

const ToastContext = createContext<ToastApi | undefined>(undefined)

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])
  const idRef = useRef(1)

  const removeToast = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  const pushToast = useCallback(
    (variant: ToastVariant, message: string, options?: ToastOptions) => {
      const id = idRef.current++
      const durationMs = options?.durationMs ?? (variant === 'error' ? 5500 : 3800)

      setToasts((current) => [
        ...current,
        {
          id,
          title: options?.title,
          message,
          variant,
          durationMs,
        },
      ])

      window.setTimeout(() => {
        removeToast(id)
      }, durationMs)
    },
    [removeToast],
  )

  const api = useMemo<ToastApi>(
    () => ({
      success: (message, options) => pushToast('success', message, options),
      error: (message, options) => pushToast('error', message, options),
      info: (message, options) => pushToast('info', message, options),
    }),
    [pushToast],
  )

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toast-stack" aria-live="polite" aria-atomic="false">
        {toasts.map((toast) => (
          <div key={toast.id} className={`toast toast--${toast.variant}`} role="status">
            <div className="toast__content">
              {toast.title ? <div className="toast__title">{toast.title}</div> : null}
              <div className="toast__message">{toast.message}</div>
            </div>
            <button
              type="button"
              className="toast__close"
              aria-label="Dismiss notification"
              onClick={() => removeToast(toast.id)}
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const context = useContext(ToastContext)
  if (!context) {
    throw new Error('useToast must be used within ToastProvider')
  }
  return context
}

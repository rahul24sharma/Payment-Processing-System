import { useParams } from 'react-router-dom'
import PaymentDetail from '@/components/PaymentDetail'

export default function PaymentDetailPage() {
  const { id } = useParams<{ id: string }>()
  
  if (!id) {
    return <div>Payment ID not provided</div>
  }
  
  return <PaymentDetail paymentId={id} />
}
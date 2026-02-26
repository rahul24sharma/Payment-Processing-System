import { useState, useEffect } from 'react'
import { apiClient } from '@/api/client'

export default function MLMonitoringPage() {
  const [modelInfo, setModelInfo] = useState<any>(null)
  const [testAmount, setTestAmount] = useState('10000')
  const [prediction, setPrediction] = useState<any>(null)
  const [loading, setLoading] = useState(false)
  
  useEffect(() => {
    // Load model info
    apiClient.get('/fraud/ml/model-info')
      .then(response => setModelInfo(response.data))
      .catch(error => console.error('Failed to load model info:', error))
  }, [])
  
  const handleTest = async () => {
    setLoading(true)
    try {
      const response = await apiClient.post('/fraud/ml/predict', {
        amount: parseInt(testAmount),
        currency: 'USD',
      })
      setPrediction(response.data)
    } catch (error) {
      console.error('Prediction failed:', error)
    } finally {
      setLoading(false)
    }
  }
  
  return (
    <div style={{ margin: '20px', maxWidth: '1000px' }}>
      <h1>ML Fraud Model Monitoring</h1>
      
      {/* Model Info */}
      {modelInfo && (
        <div style={{
          border: '1px solid #ddd',
          borderRadius: '8px',
          padding: '20px',
          marginTop: '20px',
          background: 'white',
        }}>
          <h3>Model Information</h3>
          <table style={{ width: '100%', marginTop: '15px' }}>
            <tbody>
              <tr>
                <td style={{ padding: '8px', fontWeight: 'bold' }}>Version:</td>
                <td style={{ padding: '8px' }}>{modelInfo.model_version}</td>
              </tr>
              <tr>
                <td style={{ padding: '8px', fontWeight: 'bold' }}>Features:</td>
                <td style={{ padding: '8px' }}>{modelInfo.feature_count}</td>
              </tr>
            </tbody>
          </table>
          
          <h4 style={{ marginTop: '20px' }}>Feature Importance:</h4>
          <div style={{ marginTop: '10px' }}>
            {Object.entries(modelInfo.feature_importance || {})
              .sort((a: any, b: any) => b[1] - a[1])
              .map(([feature, importance]: [string, any]) => (
                <div key={feature} style={{ marginBottom: '10px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', marginBottom: '5px' }}>
                    <span>{feature}</span>
                    <span>{importance.toFixed(2)}</span>
                  </div>
                  <div style={{ width: '100%', height: '8px', background: '#f0f0f0', borderRadius: '4px' }}>
                    <div
                      style={{
                        width: `${(importance / 3) * 100}%`,
                        height: '100%',
                        background: '#007bff',
                        borderRadius: '4px',
                      }}
                    ></div>
                  </div>
                </div>
              ))}
          </div>
        </div>
      )}
      
      {/* Test Prediction */}
      <div style={{
        border: '1px solid #ddd',
        borderRadius: '8px',
        padding: '20px',
        marginTop: '20px',
        background: 'white',
      }}>
        <h3>Test Model Prediction</h3>
        
        <div style={{ marginTop: '15px' }}>
          <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>
            Test Amount (cents):
          </label>
          <input
            type="number"
            value={testAmount}
            onChange={(e) => setTestAmount(e.target.value)}
            style={{
              width: '200px',
              padding: '10px',
              border: '1px solid #ddd',
              borderRadius: '4px',
              marginRight: '10px',
            }}
          />
          <button
            onClick={handleTest}
            disabled={loading}
            style={{
              padding: '10px 20px',
              background: loading ? '#ccc' : '#007bff',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: loading ? 'not-allowed' : 'pointer',
            }}
          >
            {loading ? 'Predicting...' : 'Test Prediction'}
          </button>
        </div>
        
        {prediction && (
          <div style={{ marginTop: '20px', padding: '15px', background: '#f8f9fa', borderRadius: '4px' }}>
            <h4>Prediction Result:</h4>
            <div style={{ fontSize: '48px', fontWeight: 'bold', color: getRiskColor(prediction.risk_level), marginTop: '10px' }}>
              {prediction.fraud_score} / 100
            </div>
            <div style={{ fontSize: '18px', marginTop: '5px' }}>
              Risk Level: <strong>{prediction.risk_level.toUpperCase()}</strong>
            </div>
            
            <h4 style={{ marginTop: '20px' }}>Feature Values:</h4>
            <pre style={{ background: 'white', padding: '10px', borderRadius: '4px', fontSize: '12px', overflow: 'auto' }}>
              {JSON.stringify(
                prediction.feature_names.reduce((obj: any, name: string, i: number) => {
                  obj[name] = prediction.features[i].toFixed(4)
                  return obj
                }, {}),
                null,
                2
              )}
            </pre>
          </div>
        )}
      </div>
    </div>
  )
}

function getRiskColor(riskLevel: string): string {
  switch (riskLevel.toLowerCase()) {
    case 'very_low':
    case 'low':
      return '#28a745'
    case 'medium':
      return '#ffc107'
    case 'high':
      return '#ff5722'
    case 'critical':
      return '#dc3545'
    default:
      return '#6c757d'
  }
}
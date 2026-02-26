"""
Train fraud detection model

Requirements:
pip install pandas scikit-learn onnx skl2onnx

Usage:
python train_fraud_model.py
"""

import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.metrics import classification_report, roc_auc_score, confusion_matrix
from sklearn.preprocessing import StandardScaler
import joblib
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType

def load_data(filepath='training_data.csv'):
    """Load training data exported from Java service"""
    df = pd.read_csv(filepath)
    
    print(f"Loaded {len(df)} records")
    print(f"Fraud cases: {df['is_fraud'].sum()}")
    print(f"Legitimate: {len(df) - df['is_fraud'].sum()}")
    
    return df

def train_model(df):
    """Train fraud detection model"""
    
    # Features
    feature_cols = ['velocity_score', 'rule_score', 'ml_score']
    X = df[feature_cols]
    y = df['is_fraud']
    
    # Split data
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )
    
    # Scale features
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)
    
    # Train multiple models
    models = {
        'Logistic Regression': LogisticRegression(random_state=42),
        'Random Forest': RandomForestClassifier(n_estimators=100, random_state=42),
        'Gradient Boosting': GradientBoostingClassifier(n_estimators=100, random_state=42)
    }
    
    results = {}
    
    for name, model in models.items():
        print(f"\n{'='*50}")
        print(f"Training {name}...")
        print(f"{'='*50}")
        
        model.fit(X_train_scaled, y_train)
        
        # Predict
        y_pred = model.predict(X_test_scaled)
        y_prob = model.predict_proba(X_test_scaled)[:, 1]
        
        # Evaluate
        print("\nClassification Report:")
        print(classification_report(y_test, y_pred))
        
        print(f"\nROC-AUC Score: {roc_auc_score(y_test, y_prob):.4f}")
        
        print("\nConfusion Matrix:")
        print(confusion_matrix(y_test, y_pred))
        
        results[name] = {
            'model': model,
            'scaler': scaler,
            'auc': roc_auc_score(y_test, y_prob)
        }
    
    # Select best model by AUC
    best_name = max(results, key=lambda k: results[k]['auc'])
    best_model = results[best_name]['model']
    best_scaler = results[best_name]['scaler']
    
    print(f"\n{'='*50}")
    print(f"Best Model: {best_name} (AUC: {results[best_name]['auc']:.4f})")
    print(f"{'='*50}")
    
    return best_model, best_scaler, feature_cols

def export_to_onnx(model, scaler, feature_cols, output_path='fraud_model.onnx'):
    """Export model to ONNX format for Java"""
    
    # Create pipeline
    from sklearn.pipeline import Pipeline
    pipeline = Pipeline([
        ('scaler', scaler),
        ('classifier', model)
    ])
    
    # Define input type
    initial_type = [('float_input', FloatTensorType([None, len(feature_cols)]))]
    
    # Convert to ONNX
    onnx_model = convert_sklearn(pipeline, initial_types=initial_type)
    
    # Save
    with open(output_path, 'wb') as f:
        f.write(onnx_model.SerializeToString())
    
    print(f"\nModel exported to: {output_path}")
    print("Copy this file to: fraud-service/src/main/resources/models/")

def save_model(model, scaler, feature_cols):
    """Save model using joblib (alternative to ONNX)"""
    joblib.dump({
        'model': model,
        'scaler': scaler,
        'features': feature_cols
    }, 'fraud_model.pkl')
    
    print("Model saved to: fraud_model.pkl")

if __name__ == '__main__':
    # Load data
    df = load_data()
    
    # Train model
    model, scaler, features = train_model(df)
    
    # Export
    export_to_onnx(model, scaler, features)
    save_model(model, scaler, features)
    
    print("\n✅ Training complete!")
    print("\nNext steps:")
    print("1. Copy fraud_model.onnx to fraud-service/src/main/resources/models/")
    print("2. Uncomment TensorFlowFraudModel.java")
    print("3. Restart fraud-service")
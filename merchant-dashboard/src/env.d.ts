/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_ENABLE_ML_MONITORING?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

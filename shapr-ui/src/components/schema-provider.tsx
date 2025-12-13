'use client'

import type { SchemaResponse } from '@/types/schema'
import * as React from 'react'

const SchemaContext = React.createContext<SchemaResponse | null>(null)

export function SchemaProvider({
  children,
  schema,
}: {
  children: React.ReactNode
  schema: SchemaResponse
}) {
  return (
    <SchemaContext.Provider value={schema}>{children}</SchemaContext.Provider>
  )
}

export function useSchema() {
  const context = React.useContext(SchemaContext)
  if (!context) {
    throw new Error('useSchema must be used within a SchemaProvider')
  }
  return context
}

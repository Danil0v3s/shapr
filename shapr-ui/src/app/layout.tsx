import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import './globals.css'

import { SchemaProvider } from '@/components/schema-provider'
import { Sidebar } from '@/components/sidebar'
import { fetchSchema } from '@/lib/api'
import type { SchemaResponse } from '@/types/schema'

const inter = Inter({ subsets: ['latin'] })

export const metadata: Metadata = {
  title: 'Shapr Admin',
  description: 'Schema-driven admin dashboard',
}

export default async function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  let schema: SchemaResponse = { collections: [] }
  
  try {
    schema = await fetchSchema()
  } catch (error) {
    console.error('Failed to fetch schema:', error)
  }

  return (
    <html lang="en">
      <body className={inter.className}>
        <SchemaProvider schema={schema}>
          <div className="flex h-screen">
            <Sidebar collections={schema.collections} />
            <main className="flex-1 overflow-auto">
              {children}
            </main>
          </div>
        </SchemaProvider>
      </body>
    </html>
  )
}

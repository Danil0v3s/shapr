import type { ClientCollectionSchema, SchemaResponse } from '@/types/schema'

const API_BASE = process.env.NEXT_PUBLIC_API_URL || ''

export async function fetchSchema(): Promise<SchemaResponse> {
  const res = await fetch(`${API_BASE}/api/_schema`, {
    cache: 'no-store',
  })
  if (!res.ok) throw new Error('Failed to fetch schema')
  return res.json()
}

export async function fetchCollectionSchema(slug: string): Promise<ClientCollectionSchema> {
  const res = await fetch(`${API_BASE}/api/_schema/${slug}`, {
    cache: 'no-store',
  })
  if (!res.ok) throw new Error(`Failed to fetch schema for ${slug}`)
  return res.json()
}

export async function fetchCollectionData(slug: string): Promise<unknown[]> {
  const res = await fetch(`${API_BASE}/api/${slug}`, {
    cache: 'no-store',
  })
  if (!res.ok) throw new Error(`Failed to fetch data for ${slug}`)
  return res.json()
}

export async function createDocument(slug: string, data: Record<string, unknown>): Promise<unknown> {
  const res = await fetch(`${API_BASE}/api/${slug}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error(`Failed to create document in ${slug}`)
  return res.json()
}

export async function updateDocument(slug: string, id: string | number, data: Record<string, unknown>): Promise<unknown> {
  const res = await fetch(`${API_BASE}/api/${slug}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error(`Failed to update document in ${slug}`)
  return res.json()
}

export async function deleteDocument(slug: string, id: string | number): Promise<void> {
  const res = await fetch(`${API_BASE}/api/${slug}/${id}`, {
    method: 'DELETE',
  })
  if (!res.ok) throw new Error(`Failed to delete document in ${slug}`)
}

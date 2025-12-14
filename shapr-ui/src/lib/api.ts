import type { ClientCollectionSchema, DataResponse, PaginatedDocs, SchemaResponse } from '@/types/schema'

const API_BASE = process.env.NEXT_PUBLIC_API_URL || ''

export interface QueryOptions {
  where?: Record<string, unknown>
  limit?: number
  page?: number
  sort?: string
  pagination?: boolean
}

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

export async function fetchCollectionData(
  slug: string,
  options?: QueryOptions
): Promise<PaginatedDocs<Record<string, unknown>>> {
  const params = new URLSearchParams()
  
  if (options?.where) {
    params.append('where', JSON.stringify(options.where))
  }
  if (options?.limit) {
    params.append('limit', options.limit.toString())
  }
  if (options?.page) {
    params.append('page', options.page.toString())
  }
  if (options?.sort) {
    params.append('sort', options.sort)
  }
  if (options?.pagination !== undefined) {
    params.append('pagination', options.pagination.toString())
  }

  const url = `${API_BASE}/api/${slug}${params.toString() ? `?${params.toString()}` : ''}`
  const res = await fetch(url, {
    cache: 'no-store',
  })
  if (!res.ok) throw new Error(`Failed to fetch data for ${slug}`)
  return res.json()
}

export async function createDocument(slug: string, data: Record<string, unknown>): Promise<Record<string, unknown>> {
  const res = await fetch(`${API_BASE}/api/${slug}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error(`Failed to create document in ${slug}`)
  const response: DataResponse<Record<string, unknown>> = await res.json()
  return response.data
}

export async function updateDocument(slug: string, id: string | number, data: Record<string, unknown>): Promise<Record<string, unknown>> {
  const res = await fetch(`${API_BASE}/api/${slug}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error(`Failed to update document in ${slug}`)
  const response: DataResponse<Record<string, unknown>> = await res.json()
  return response.data
}

export async function deleteDocument(slug: string, id: string | number): Promise<void> {
  const res = await fetch(`${API_BASE}/api/${slug}/${id}`, {
    method: 'DELETE',
  })
  if (!res.ok) throw new Error(`Failed to delete document in ${slug}`)
}

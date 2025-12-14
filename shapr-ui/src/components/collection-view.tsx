'use client'

import { ChevronLeft, ChevronRight, Plus, RefreshCw } from 'lucide-react'
import * as React from 'react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog'
import { createDocument, deleteDocument, fetchCollectionData, updateDocument } from '@/lib/api'
import type { ClientCollectionSchema, PaginatedDocs } from '@/types/schema'
import { DynamicForm } from './dynamic-form'
import { DynamicTable } from './dynamic-table'

interface CollectionViewProps {
  schema: ClientCollectionSchema
}

export function CollectionView({ schema }: CollectionViewProps) {
  const [data, setData] = React.useState<Record<string, unknown>[]>([])
  const [pagination, setPagination] = React.useState<PaginatedDocs<Record<string, unknown>> | null>(null)
  const [isLoading, setIsLoading] = React.useState(true)
  const [isSaving, setIsSaving] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)
  const [isDialogOpen, setIsDialogOpen] = React.useState(false)
  const [editingItem, setEditingItem] = React.useState<Record<string, unknown> | null>(null)
  const [currentPage, setCurrentPage] = React.useState(1)
  const [pageSize] = React.useState(10)

  const loadData = React.useCallback(async (page: number = 1) => {
    setIsLoading(true)
    setError(null)
    try {
      const result = await fetchCollectionData(schema.slug, {
        page,
        limit: pageSize,
        pagination: true,
      })
      setPagination(result)
      setData(result.docs)
      setCurrentPage(result.page || 1)
    } catch (err) {
      setError('Failed to load data')
      console.error(err)
    } finally {
      setIsLoading(false)
    }
  }, [schema.slug, pageSize])

  React.useEffect(() => {
    loadData(currentPage)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentPage])

  const handleCreate = () => {
    setEditingItem(null)
    setIsDialogOpen(true)
  }

  const handleEdit = (item: Record<string, unknown>) => {
    setEditingItem(item)
    setIsDialogOpen(true)
  }

  const handleDelete = async (item: Record<string, unknown>) => {
    if (!confirm('Are you sure you want to delete this item?')) return

    try {
      await deleteDocument(schema.slug, item.id as string | number)
      loadData(currentPage)
    } catch (err) {
      console.error('Failed to delete:', err)
      alert('Failed to delete item')
    }
  }

  const handleSubmit = async (formData: Record<string, unknown>) => {
    setIsSaving(true)
    try {
      if (editingItem) {
        await updateDocument(schema.slug, editingItem.id as string | number, formData)
      } else {
        await createDocument(schema.slug, formData)
      }
      setIsDialogOpen(false)
      loadData(currentPage)
    } catch (err) {
      console.error('Failed to save:', err)
      alert('Failed to save item')
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{schema.labels.plural}</h1>
          <p className="text-muted-foreground">
            Manage your {schema.labels.plural.toLowerCase()} collection
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => loadData(currentPage)} disabled={isLoading}>
            <RefreshCw className={`mr-2 h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
            Refresh
          </Button>
          <Button onClick={handleCreate}>
            <Plus className="mr-2 h-4 w-4" />
            Create {schema.labels.singular}
          </Button>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>All {schema.labels.plural}</CardTitle>
          <CardDescription>
            {pagination ? (
              <>
                Showing {pagination.docs.length} of {pagination.totalDocs} {pagination.totalDocs === 1 ? 'item' : 'items'}
                {pagination.totalPages > 1 && ` (Page ${pagination.page} of ${pagination.totalPages})`}
              </>
            ) : (
              `${data.length} ${data.length === 1 ? 'item' : 'items'} found`
            )}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {error ? (
            <div className="py-8 text-center">
              <p className="text-destructive">{error}</p>
              <Button variant="outline" onClick={() => loadData(currentPage)} className="mt-4">
                Try Again
              </Button>
            </div>
          ) : (
            <>
              <DynamicTable
                schema={schema}
                data={data}
                onEdit={handleEdit}
                onDelete={handleDelete}
              />
              {pagination && pagination.totalPages > 1 && (
                <div className="mt-4 flex items-center justify-between">
                  <div className="text-sm text-muted-foreground">
                    Page {pagination.page} of {pagination.totalPages}
                  </div>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        if (pagination.prevPage) {
                          setCurrentPage(pagination.prevPage)
                        }
                      }}
                      disabled={!pagination.hasPrevPage || isLoading}
                    >
                      <ChevronLeft className="h-4 w-4" />
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        if (pagination.nextPage) {
                          setCurrentPage(pagination.nextPage)
                        }
                      }}
                      disabled={!pagination.hasNextPage || isLoading}
                    >
                      Next
                      <ChevronRight className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>

      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>
              {editingItem ? `Edit ${schema.labels.singular}` : `Create ${schema.labels.singular}`}
            </DialogTitle>
            <DialogDescription>
              {editingItem
                ? `Update the ${schema.labels.singular.toLowerCase()} details below`
                : `Fill in the details for the new ${schema.labels.singular.toLowerCase()}`}
            </DialogDescription>
          </DialogHeader>
          <DynamicForm
            fields={schema.fields}
            initialData={editingItem || {}}
            onSubmit={handleSubmit}
            onCancel={() => setIsDialogOpen(false)}
            isLoading={isSaving}
          />
        </DialogContent>
      </Dialog>
    </div>
  )
}

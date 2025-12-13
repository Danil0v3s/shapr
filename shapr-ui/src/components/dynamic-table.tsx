'use client'

import { Edit, Trash2 } from 'lucide-react'
import * as React from 'react'

import { Button } from '@/components/ui/button'
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@/components/ui/table'
import type { ClientCollectionSchema } from '@/types/schema'

interface DynamicTableProps {
  schema: ClientCollectionSchema
  data: Record<string, unknown>[]
  onEdit: (item: Record<string, unknown>) => void
  onDelete: (item: Record<string, unknown>) => void
}

export function DynamicTable({ schema, data, onEdit, onDelete }: DynamicTableProps) {
  const columns = getColumns(schema)

  return (
    <Table>
      <TableHeader>
        <TableRow>
          {columns.map((column) => (
            <TableHead key={column}>{formatColumnName(column)}</TableHead>
          ))}
          <TableHead className="w-[100px]">Actions</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {data.length === 0 ? (
          <TableRow>
            <TableCell colSpan={columns.length + 1} className="h-24 text-center">
              No results.
            </TableCell>
          </TableRow>
        ) : (
          data.map((item, index) => (
            <TableRow key={(item.id as string | number) ?? index}>
              {columns.map((column) => (
                <TableCell key={column}>{formatCellValue(item[column])}</TableCell>
              ))}
              <TableCell>
                <div className="flex items-center gap-2">
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => onEdit(item)}
                  >
                    <Edit className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => onDelete(item)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          ))
        )}
      </TableBody>
    </Table>
  )
}

function getColumns(schema: ClientCollectionSchema): string[] {
  if (schema.admin.defaultColumns && schema.admin.defaultColumns.length > 0) {
    return schema.admin.defaultColumns
  }
  return ['id', ...schema.fields.slice(0, 4).map((f) => f.name)]
}

function formatColumnName(name: string): string {
  return name
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, (str) => str.toUpperCase())
    .trim()
}

function formatCellValue(value: unknown): React.ReactNode {
  if (value === null || value === undefined) {
    return <span className="text-muted-foreground">—</span>
  }

  if (typeof value === 'boolean') {
    return value ? 'Yes' : 'No'
  }

  if (value instanceof Date || (typeof value === 'string' && isISODateString(value))) {
    return formatDate(value)
  }

  if (typeof value === 'object') {
    return JSON.stringify(value)
  }

  const stringValue = String(value)
  if (stringValue.length > 50) {
    return stringValue.substring(0, 50) + '...'
  }

  return stringValue
}

function isISODateString(value: string): boolean {
  return /^\d{4}-\d{2}-\d{2}/.test(value)
}

function formatDate(value: unknown): string {
  try {
    const date = new Date(value as string | Date)
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return String(value)
  }
}

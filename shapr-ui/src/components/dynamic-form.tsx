'use client'

import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import type { ClientFieldSchema } from '@/types/schema'
import * as React from 'react'

interface DynamicFormProps {
  fields: ClientFieldSchema[]
  initialData?: Record<string, unknown>
  onSubmit: (data: Record<string, unknown>) => void
  onCancel: () => void
  isLoading?: boolean
}

export function DynamicForm({
  fields,
  initialData = {},
  onSubmit,
  onCancel,
  isLoading = false,
}: DynamicFormProps) {
  const [formData, setFormData] = React.useState<Record<string, unknown>>(
    () => {
      const data: Record<string, unknown> = {}
      fields.forEach((field) => {
        if (initialData[field.name] !== undefined) {
          data[field.name] = initialData[field.name]
        } else if (field.config.defaultValue !== undefined) {
          data[field.name] = field.config.defaultValue
        } else {
          switch (field.type) {
            case 'checkbox':
              data[field.name] = false
              break
            case 'number':
              data[field.name] = ''
              break
            default:
              data[field.name] = ''
          }
        }
      })
      return data
    }
  )

  const handleChange = (name: string, value: unknown) => {
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const processedData: Record<string, unknown> = {}
    fields.forEach((field) => {
      const value = formData[field.name]
      if (field.type === 'number' && typeof value === 'string') {
        processedData[field.name] = value ? Number(value) : null
      } else {
        processedData[field.name] = value
      }
    })
    onSubmit(processedData)
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {fields.map((field) => (
        <div key={field.name} className="space-y-2">
          <Label htmlFor={field.name}>
            {field.label}
            {field.required && <span className="ml-1 text-destructive">*</span>}
          </Label>
          {renderField(field, formData[field.name], handleChange)}
          {field.config.maxLength && (
            <p className="text-xs text-muted-foreground">
              Max {field.config.maxLength as number} characters
            </p>
          )}
        </div>
      ))}
      <div className="flex gap-2 pt-4">
        <Button type="submit" disabled={isLoading}>
          {isLoading ? 'Saving...' : 'Save'}
        </Button>
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </form>
  )
}

function renderField(
  field: ClientFieldSchema,
  value: unknown,
  onChange: (name: string, value: unknown) => void
) {
  switch (field.type) {
    case 'text':
      return (
        <Input
          id={field.name}
          type="text"
          value={(value as string) || ''}
          onChange={(e) => onChange(field.name, e.target.value)}
          required={field.required}
          maxLength={field.config.maxLength as number | undefined}
          minLength={field.config.minLength as number | undefined}
        />
      )

    case 'textarea':
      return (
        <Textarea
          id={field.name}
          value={(value as string) || ''}
          onChange={(e) => onChange(field.name, e.target.value)}
          required={field.required}
        />
      )

    case 'number':
      return (
        <Input
          id={field.name}
          type="number"
          value={(value as string | number) ?? ''}
          onChange={(e) => onChange(field.name, e.target.value)}
          required={field.required}
          min={field.config.min as number | undefined}
          max={field.config.max as number | undefined}
          step={field.config.integerOnly ? 1 : 'any'}
        />
      )

    case 'email':
      return (
        <Input
          id={field.name}
          type="email"
          value={(value as string) || ''}
          onChange={(e) => onChange(field.name, e.target.value)}
          required={field.required}
        />
      )

    case 'date':
      return (
        <Input
          id={field.name}
          type={field.config.dateOnly ? 'date' : 'datetime-local'}
          value={(value as string) || ''}
          onChange={(e) => onChange(field.name, e.target.value)}
          required={field.required}
        />
      )

    case 'checkbox':
      return (
        <div className="flex items-center space-x-2">
          <Checkbox
            id={field.name}
            checked={value as boolean}
            onCheckedChange={(checked) => onChange(field.name, checked)}
          />
          <label
            htmlFor={field.name}
            className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
          >
            {field.label}
          </label>
        </div>
      )

    case 'relationship':
      return (
        <Input
          id={field.name}
          type="text"
          value={(value as string) || ''}
          onChange={(e) => onChange(field.name, e.target.value)}
          required={field.required}
          placeholder={`Enter ${field.config.relationTo} ID`}
        />
      )

    default:
      return (
        <Input
          id={field.name}
          type="text"
          value={(value as string) || ''}
          onChange={(e) => onChange(field.name, e.target.value)}
          required={field.required}
        />
      )
  }
}

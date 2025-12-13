export interface Labels {
  singular: string
  plural: string
}

export interface ClientFieldSchema {
  name: string
  type: 'text' | 'textarea' | 'number' | 'checkbox' | 'email' | 'date' | 'relationship'
  label: string
  required: boolean
  unique: boolean
  config: Record<string, unknown>
}

export interface ClientAccessControl {
  create: string
  read: string
  update: string
  delete: string
}

export interface CollectionAdminConfig {
  useAsTitle?: string
  defaultColumns?: string[]
}

export interface ClientCollectionSchema {
  name: string
  slug: string
  labels: Labels
  fields: ClientFieldSchema[]
  access: ClientAccessControl
  admin: CollectionAdminConfig
  timestamps: boolean
}

export interface SchemaResponse {
  collections: ClientCollectionSchema[]
}

import { Database } from 'lucide-react'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { fetchSchema } from '@/lib/api'
import Link from 'next/link'

export default async function DashboardPage() {
  let schema = { collections: [] }
  
  try {
    schema = await fetchSchema()
  } catch (error) {
    console.error('Failed to fetch schema:', error)
  }

  return (
    <div className="p-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground">
          Welcome to the Shapr admin dashboard. Manage your collections below.
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {schema.collections.map((collection) => (
          <Link key={collection.slug} href={`/collections/${collection.slug}`}>
            <Card className="cursor-pointer transition-colors hover:bg-muted/50">
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">
                  {collection.labels.plural}
                </CardTitle>
                <Database className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{collection.fields.length}</div>
                <p className="text-xs text-muted-foreground">
                  {collection.fields.length} fields configured
                </p>
                <div className="mt-4 flex flex-wrap gap-1">
                  {collection.fields.slice(0, 3).map((field) => (
                    <span
                      key={field.name}
                      className="inline-flex items-center rounded-md bg-muted px-2 py-1 text-xs"
                    >
                      {field.name}
                    </span>
                  ))}
                  {collection.fields.length > 3 && (
                    <span className="inline-flex items-center rounded-md bg-muted px-2 py-1 text-xs">
                      +{collection.fields.length - 3} more
                    </span>
                  )}
                </div>
              </CardContent>
            </Card>
          </Link>
        ))}
      </div>

      {schema.collections.length === 0 && (
        <Card>
          <CardHeader>
            <CardTitle>No collections found</CardTitle>
            <CardDescription>
              Unable to fetch schema from the API. Make sure your backend is running.
            </CardDescription>
          </CardHeader>
        </Card>
      )}
    </div>
  )
}

import { notFound } from 'next/navigation'

import { CollectionView } from '@/components/collection-view'
import { fetchCollectionSchema } from '@/lib/api'

interface CollectionPageProps {
  params: {
    slug: string
  }
}

export default async function CollectionPage({ params }: CollectionPageProps) {
  let schema

  try {
    schema = await fetchCollectionSchema(params.slug)
  } catch (error) {
    console.error('Failed to fetch collection schema:', error)
    notFound()
  }

  return (
    <div className="p-8">
      <CollectionView schema={schema} />
    </div>
  )
}

export async function generateMetadata({ params }: CollectionPageProps) {
  try {
    const schema = await fetchCollectionSchema(params.slug)
    return {
      title: `${schema.labels.plural} | Shapr Admin`,
      description: `Manage ${schema.labels.plural}`,
    }
  } catch {
    return {
      title: 'Collection | Shapr Admin',
    }
  }
}

'use client'

import { Database, LayoutDashboard, Settings } from 'lucide-react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'

import { Button } from '@/components/ui/button'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Separator } from '@/components/ui/separator'
import type { ClientCollectionSchema } from '@/types/schema'

interface SidebarProps {
  collections: ClientCollectionSchema[]
}

export function Sidebar({ collections }: SidebarProps) {
  const pathname = usePathname()

  return (
    <div className="flex h-full w-64 flex-col border-r bg-muted/10">
      <div className="flex h-14 items-center border-b px-4">
        <Link href="/" className="flex items-center gap-2 font-semibold">
          <Database className="h-6 w-6" />
          <span>Shapr Admin</span>
        </Link>
      </div>
      <ScrollArea className="flex-1 px-3 py-4">
        <div className="space-y-4">
          <div>
            <Link href="/">
              <Button
                variant={pathname === '/' ? 'secondary' : 'ghost'}
                className="w-full justify-start"
              >
                <LayoutDashboard className="mr-2 h-4 w-4" />
                Dashboard
              </Button>
            </Link>
          </div>
          <Separator />
          <div className="space-y-1">
            <h4 className="mb-2 px-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Collections
            </h4>
            {collections.map((collection) => (
              <Link key={collection.slug} href={`/collections/${collection.slug}`}>
                <Button
                  variant={
                    pathname === `/collections/${collection.slug}`
                      ? 'secondary'
                      : 'ghost'
                  }
                  className="w-full justify-start"
                >
                  <Database className="mr-2 h-4 w-4" />
                  {collection.labels.plural}
                </Button>
              </Link>
            ))}
          </div>
        </div>
      </ScrollArea>
      <div className="border-t p-3">
        <Button variant="ghost" className="w-full justify-start">
          <Settings className="mr-2 h-4 w-4" />
          Settings
        </Button>
      </div>
    </div>
  )
}

import { QueryClient } from '@tanstack/react-query'
import { ApiError } from './api/client'

export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        // Client errors (401, 404, ...) won't fix themselves, so only retry network and server errors.
        retry: (failureCount, error) =>
          failureCount < 2 && !(error instanceof ApiError && error.status >= 400 && error.status < 500),
      },
    },
  })
}

export const queryKeys = {
  me: ['me'] as const,
  applications: ['applications'] as const,
  applicationList: (params: object) => ['applications', 'list', params] as const,
  application: (id: string) => ['applications', 'detail', id] as const,
}

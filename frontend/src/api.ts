import type { Bin, CollectionRequest, Location, Road, Route, Truck, WasteType } from './types'

const request = async <T>(path: string, options?: RequestInit): Promise<T> => {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json', ...(options?.headers ?? {}) },
    ...options,
  })
  if (!response.ok) {
    const error = await response.json().catch(() => null) as { message?: string } | null
    throw new Error(error?.message ?? `Request failed with status ${response.status}`)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const api = {
  getBins: () => request<Bin[]>('/api/bins'),
  createBin: (payload: { binCode: string; locationId: number; capacityKg: number; currentFillKg: number; wasteType: WasteType }) => request<Bin>('/api/bins', { method: 'POST', body: JSON.stringify(payload) }),
  getLocations: () => request<Location[]>('/api/locations'),
  getRoads: () => request<Road[]>('/api/roads'),
  getTrucks: () => request<Truck[]>('/api/trucks'),
  getRequests: (status = 'PENDING') => request<CollectionRequest[]>(`/api/collection-requests?status=${status}`),
  getActiveRoute: async () => (await request<Route | undefined>('/api/routes/active')) ?? null,
  tick: () => request<{ changedBinCount: number; createdRequestCount: number }>('/api/simulation/tick', { method: 'POST' }),
  optimize: (truckId: number, requestIds: number[]) => request<Route>('/api/routes/optimize', { method: 'POST', body: JSON.stringify({ truckId, requestIds, algorithm: 'DIJKSTRA' }) }),
  startRoute: (routeId: number) => request<Route>(`/api/routes/${routeId}/start`, { method: 'POST' }),
  completeStop: (routeId: number, stopId: number) => request<Route>(`/api/routes/${routeId}/stops/${stopId}/complete`, { method: 'POST' }),
  completeRoute: (routeId: number) => request<Route>(`/api/routes/${routeId}/complete`, { method: 'POST' }),
}

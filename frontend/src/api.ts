import type { Bin, CoaSimulation, CollectionRequest, Location, Road, Route, RouteAlgorithm, Truck, WasteType } from './types'

const request = async <T>(path: string, options?: RequestInit): Promise<T> => {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json', ...(options?.headers ?? {}) },
    cache: 'no-store',
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
  getBins: (search?: string) => request<Bin[]>(search ? `/api/bins?search=${encodeURIComponent(search)}` : '/api/bins'),
  createBin: (payload: { binCode: string; locationId: number; capacityKg: number; currentFillKg: number; wasteType: WasteType }) => request<Bin>('/api/bins', { method: 'POST', body: JSON.stringify(payload) }),
  getLocations: () => request<Location[]>('/api/locations'),
  getRoads: () => request<Road[]>('/api/roads'),
  getTrucks: () => request<Truck[]>('/api/trucks'),
  getRequests: (status = 'PENDING') => request<CollectionRequest[]>(`/api/collection-requests?status=${status}`),
  getActiveRoute: async () => (await request<Route | undefined>('/api/routes/active')) ?? null,
  tick: () => request<{ changedBinCount: number; createdRequestCount: number }>('/api/simulation/tick', { method: 'POST' }),
  optimize: (truckId: number, requestIds: number[], algorithm: RouteAlgorithm = 'DIJKSTRA') => request<Route>('/api/routes/optimize', { method: 'POST', body: JSON.stringify({ truckId, requestIds, algorithm }) }),
  undoLatestRoute: () => request<Route>('/api/routes/undo-latest', { method: 'POST' }),
  startRoute: (routeId: number) => request<Route>(`/api/routes/${routeId}/start`, { method: 'POST' }),
  completeStop: (routeId: number, stopId: number) => request<Route>(`/api/routes/${routeId}/stops/${stopId}/complete`, { method: 'POST' }),
  completeRoute: (routeId: number) => request<Route>(`/api/routes/${routeId}/complete`, { method: 'POST' }),
  simulateBinStatus: (currentFillKg: number, capacityKg: number) => request<CoaSimulation>('/api/coa/bin-status', { method: 'POST', body: JSON.stringify({ currentFillKg, capacityKg }) }),
  simulateRouteTotals: (distanceKm: number, travelTimeMinutes: number) => request<CoaSimulation>('/api/coa/route-totals', { method: 'POST', body: JSON.stringify({ legs: [{ distanceKm, travelTimeMinutes }] }) }),
  simulateRouteWorkload: (distanceKm: number, repetitionCount: number) => request<CoaSimulation>('/api/coa/route-workload', { method: 'POST', body: JSON.stringify({ distanceKm, repetitionCount }) }),
  simulateRouteSummary: (binCodes: string[]) => request<CoaSimulation>('/api/coa/route-summary', { method: 'POST', body: JSON.stringify({ binCodes }) }),
}

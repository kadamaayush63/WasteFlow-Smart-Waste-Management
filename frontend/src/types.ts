export type BinStatus = 'NORMAL' | 'MEDIUM' | 'HIGH' | 'CRITICAL' | 'COLLECTED'
export type WasteType = 'GENERAL' | 'ORGANIC' | 'RECYCLABLE' | 'HAZARDOUS'
export type RequestStatus = 'PENDING' | 'ASSIGNED' | 'COMPLETED' | 'CANCELLED'
export type TruckStatus = 'AVAILABLE' | 'ASSIGNED' | 'COLLECTING' | 'UNAVAILABLE'

export interface Bin {
  id: number
  binCode: string
  locationId: number
  locationName: string
  capacityKg: number
  currentFillKg: number
  wasteType: WasteType
  status: BinStatus
}

export interface Location {
  id: number
  name: string
  xCoordinate: number
  yCoordinate: number
}

export interface Road {
  id: number
  sourceLocationId: number
  destinationLocationId: number
  distance: number
  travelTimeMinutes: number
  bidirectional: boolean
}

export interface Truck {
  id: number
  truckCode: string
  capacityKg: number
  currentLoadKg: number
  status: TruckStatus
  locationId: number
}

export interface CollectionRequest {
  id: number
  binId: number
  binCode: string
  priorityScore: number
  urgency: string
  status: RequestStatus
}

export interface RouteStop {
  id: number
  sequenceNumber: number
  binId: number
  locationId: number
  status: 'PENDING' | 'COMPLETED' | 'SKIPPED'
  collectedKg: number | null
}

export interface Route {
  id: number
  truckId: number
  algorithm: string
  status: string
  totalDistance: number
  totalTime: number
  stops: RouteStop[]
}

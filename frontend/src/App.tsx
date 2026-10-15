import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Activity, AlertTriangle, ArrowUpRight, Box, Check, ChevronRight, CircleHelp, Cpu, Factory, Gauge, Map, PackagePlus, RefreshCw, Route as RouteIcon, Truck as TruckIcon, Wifi } from 'lucide-react'
import { api } from './api'
import { CoaLab } from './CoaLab'
import { RouteCanvas } from './RouteCanvas'
import type { Bin, CollectionRequest, Location, Road, Route, RouteAlgorithm, Truck, WasteType } from './types'
import './styles.css'
import './route-lifecycle.css'
import './animation.css'

const statusLabel: Record<string, string> = { NORMAL: 'Normal', MEDIUM: 'Medium', HIGH: 'High', CRITICAL: 'Critical', COLLECTED: 'Collected' }
const DATA_REFRESH_INTERVAL_MS = 20_000
const formatNumber = (value: number) => new Intl.NumberFormat('en-IN', { maximumFractionDigits: 1 }).format(value)
const formatDate = (date: Date) => {
  const parts = new Intl.DateTimeFormat('en-GB', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' }).formatToParts(date)
  const part = (type: Intl.DateTimeFormatPartTypes) => parts.find((item) => item.type === type)?.value ?? ''
  return `${part('weekday')} · ${part('day')} ${part('month')} ${part('year')}`.toUpperCase()
}

function App() {
  const [today, setToday] = useState(() => new Date())
  const [bins, setBins] = useState<Bin[]>([])
  const [requests, setRequests] = useState<CollectionRequest[]>([])
  const [locations, setLocations] = useState<Location[]>([])
  const [roads, setRoads] = useState<Road[]>([])
  const [trucks, setTrucks] = useState<Truck[]>([])
  const [selectedRequests, setSelectedRequests] = useState<number[]>([])
  const [selectedTruck, setSelectedTruck] = useState<number | ''>('')
  const [routeAlgorithm, setRouteAlgorithm] = useState<RouteAlgorithm>('DIJKSTRA')
  const [route, setRoute] = useState<Route | null>(null)
  const [activeView, setActiveView] = useState<'overview' | 'bins' | 'routes' | 'coa'>('overview')
  const [isLoading, setIsLoading] = useState(true)
  const [isTicking, setIsTicking] = useState(false)
  const [isOptimizing, setIsOptimizing] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const isLoadingData = useRef(false)

  const loadData = useCallback(async () => {
    if (isLoadingData.current) return
    isLoadingData.current = true
    setError('')
    try {
      const [nextBins, nextRequests, nextLocations, nextRoads, nextTrucks, nextRoute] = await Promise.all([
        api.getBins(), api.getRequests(), api.getLocations(), api.getRoads(), api.getTrucks(), api.getActiveRoute(),
      ])
      setBins(nextBins); setRequests(nextRequests); setLocations(nextLocations); setRoads(nextRoads); setTrucks(nextTrucks); setRoute(nextRoute)
      if (nextRoute) setActiveView('routes')
      setSelectedTruck((currentTruck) => {
        if (currentTruck !== '' && nextTrucks.some((truck) => truck.id === currentTruck && truck.status === 'AVAILABLE')) {
          return currentTruck
        }
        return nextTrucks.find((truck) => truck.status === 'AVAILABLE')?.id ?? ''
      })
    } catch (loadError) { setError(loadError instanceof Error ? loadError.message : 'Could not reach the WasteFlow API') }
    finally {
      isLoadingData.current = false
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadData()
    const interval = window.setInterval(() => void loadData(), DATA_REFRESH_INTERVAL_MS)
    return () => window.clearInterval(interval)
  }, [loadData])
  useEffect(() => {
    const interval = window.setInterval(() => setToday(new Date()), 60_000)
    return () => window.clearInterval(interval)
  }, [])

  const stats = useMemo(() => ({
    critical: bins.filter((bin) => bin.status === 'CRITICAL').length,
    attention: bins.filter((bin) => bin.status === 'HIGH' || bin.status === 'CRITICAL').length,
    availableTrucks: trucks.filter((truck) => truck.status === 'AVAILABLE').length,
    totalWaste: bins.reduce((total, bin) => total + bin.currentFillKg, 0),
  }), [bins, trucks])

  const routeEligibleRequests = requests.filter((request) => {
    const bin = bins.find((item) => item.id === request.binId)
    return bin !== undefined && bin.capacityKg > 0 && (bin.currentFillKg / bin.capacityKg) * 100 >= 10
  })
  const eligibleRequestIds = new Set(routeEligibleRequests.map((request) => request.id))
  const selectedRouteRequestIds = selectedRequests.filter((requestId) => eligibleRequestIds.has(requestId))

  const selectedWeight = routeEligibleRequests.filter((request) => selectedRouteRequestIds.includes(request.id)).reduce((total, request) => {
    const bin = bins.find((item) => item.id === request.binId)
    return total + (bin?.currentFillKg ?? 0)
  }, 0)

  const runTick = async () => {
    setIsTicking(true); setMessage('')
    try { const result = await api.tick(); setMessage(`${result.createdRequestCount} new collection request${result.createdRequestCount === 1 ? '' : 's'} created`); await loadData() }
    catch (tickError) { setError(tickError instanceof Error ? tickError.message : 'Simulation tick failed') }
    finally { setIsTicking(false) }
  }

  const optimizeRoute = async () => {
    if (selectedTruck === '' || selectedRouteRequestIds.length === 0) return
    setIsOptimizing(true); setError(''); setMessage('')
    const truck = trucks.find((item) => item.id === selectedTruck)
    if (!truck || truck.status !== 'AVAILABLE') { setError('Choose an available truck before building a route'); return }
    try { const nextRoute = await api.optimize(Number(selectedTruck), selectedRouteRequestIds, routeAlgorithm); setRoute(nextRoute); setSelectedRequests([]); setMessage(`Route ${nextRoute.id} planned with ${nextRoute.stops.length} stops`); setActiveView('routes'); await loadData() }
    catch (routeError) { setError(routeError instanceof Error ? routeError.message : 'Route optimization failed') }
    finally { setIsOptimizing(false) }
  }

  const startRoute = async () => {
    if (!route) return
    try { setRoute(await api.startRoute(route.id)); setMessage('Route started. Complete stops as the truck reaches each bin.') }
    catch (routeError) { setError(routeError instanceof Error ? routeError.message : 'Could not start route') }
  }

  const completeStop = async (stopId: number) => {
    if (!route) return
    try { setRoute(await api.completeStop(route.id, stopId)); await loadData(); setMessage('Collection stop completed.') }
    catch (routeError) { setError(routeError instanceof Error ? routeError.message : 'Could not complete stop') }
  }

  const completeRoute = async () => {
    if (!route) return
    try { setRoute(await api.completeRoute(route.id)); await loadData(); setMessage('Route completed. Truck is available again.') }
    catch (routeError) { setError(routeError instanceof Error ? routeError.message : 'Could not complete route') }
  }

  const toggleRequest = (id: number) => setSelectedRequests((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id])

  if (isLoading) return <div className="loading-screen"><div className="brand-mark">WF</div><p>Connecting to collection control...</p></div>

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><div className="brand-mark">WF</div><div><strong>WasteFlow</strong><span>route intelligence</span></div></div>
        <div className="system-state"><span className="pulse" /> SYSTEM LIVE <span className="mono">/ 01</span></div>
        <nav className="nav-list" aria-label="Main navigation">
          <button className={activeView === 'overview' ? 'nav-item active' : 'nav-item'} onClick={() => setActiveView('overview')}><Gauge size={17} /> Overview <ChevronRight size={15} /></button>
          <button className={activeView === 'bins' ? 'nav-item active' : 'nav-item'} onClick={() => setActiveView('bins')}><Box size={17} /> Bin network <span className="nav-count">{bins.length}</span></button>
          <button className={activeView === 'routes' ? 'nav-item active' : 'nav-item'} onClick={() => setActiveView('routes')}><RouteIcon size={17} /> Route planner <span className="nav-count">{requests.length}</span></button>
          <button className={activeView === 'coa' ? 'nav-item active' : 'nav-item'} onClick={() => setActiveView('coa')}><Cpu size={17} /> COA lab <ChevronRight size={15} /></button>
        </nav>
        <div className="sidebar-bottom"><div className="side-note"><CircleHelp size={15} /><span>Demo dataset<br /><b>Campus North</b></span></div><button className="profile"><span className="avatar">OP</span><span><b>Operator</b><small>Field control</small></span><ArrowUpRight size={14} /></button></div>
      </aside>

      <main className="main-content">
        <header className="topbar"><div className="crumb"><span>WasteFlow</span><ChevronRight size={14} /><b>{activeView === 'overview' ? 'Overview' : activeView === 'bins' ? 'Bin network' : activeView === 'routes' ? 'Route planner' : 'COA lab'}</b></div><div className="top-actions"><span className="last-sync"><Wifi size={14} /> API connected</span><button className="icon-button" title="Refresh data" onClick={() => void loadData()}><RefreshCw size={17} /></button></div></header>
        <section className="page-intro"><div><p className="eyebrow">{activeView === 'coa' ? 'PROCESSOR MODEL · TEACHING SIMULATOR' : formatDate(today)}</p><h1>{activeView === 'overview' ? <>Good evening, <em>operator.</em></> : activeView === 'bins' ? <>Bin network <em>control.</em></> : activeView === 'routes' ? <>Plan the next <em>collection.</em></> : <>Explore the <em>CPU.</em></>}</h1><p className="intro-copy">{activeView === 'coa' ? 'Trace application calculations through simulated 64-bit registers and ALU instructions.' : 'Monitor fill levels, surface urgent work, and send the right truck down the shortest path.'}</p></div>{activeView !== 'coa' && <button className="primary-button" onClick={runTick} disabled={isTicking}><Activity size={17} /> {isTicking ? 'Simulating...' : 'Run simulation tick'}</button>}</section>
        {error && <div className="alert error"><AlertTriangle size={17} /> {error}<button onClick={() => setError('')}>Dismiss</button></div>}
        {message && <div className="alert success"><Check size={17} /> {message}</div>}

        {activeView !== 'coa' && <section className="stat-grid">
          <article className="stat-card accent"><div className="stat-heading"><span>Urgent bins</span><AlertTriangle size={17} /></div><strong>{stats.attention.toString().padStart(2, '0')}</strong><p><b>{stats.critical}</b> critical · needs attention</p></article>
          <article className="stat-card"><div className="stat-heading"><span>Open requests</span><PackagePlus size={17} /></div><strong>{requests.length.toString().padStart(2, '0')}</strong><p>Priority queue · <b>live</b></p></article>
          <article className="stat-card"><div className="stat-heading"><span>Fleet ready</span><TruckIcon size={17} /></div><strong>{stats.availableTrucks.toString().padStart(2, '0')}</strong><p>of {trucks.length.toString().padStart(2, '0')} trucks available</p></article>
          <article className="stat-card"><div className="stat-heading"><span>Waste on route</span><Factory size={17} /></div><strong>{formatNumber(stats.totalWaste)}<small> kg</small></strong><p>across monitored bins</p></article>
        </section>}

        {(activeView === 'overview' || activeView === 'routes') && <div className="route-strategy-control"><label htmlFor="route-algorithm">Stop order</label><select id="route-algorithm" value={routeAlgorithm} onChange={(event) => setRouteAlgorithm(event.target.value as RouteAlgorithm)}><option value="DIJKSTRA">Priority first</option><option value="NEAREST_STOP">Nearest stop</option></select></div>}

        {activeView === 'coa' ? <CoaLab bins={bins} route={route} /> : activeView === 'bins' ? <BinsPanel bins={bins} locations={locations} onCreated={loadData} /> : <>
          <section className="workspace-grid">
            <div className="panel request-panel"><div className="panel-header"><div><p className="eyebrow">PRIORITY QUEUE</p><h2>Requests needing a truck</h2></div><span className="queue-badge">{routeEligibleRequests.length} eligible</span></div><p className="panel-subtitle">Only bins at 10% fill or above can be added to a route. Highest fill level is served first.</p><div className="request-list">{routeEligibleRequests.length === 0 ? <div className="empty-state"><Check size={20} /><p>No route-eligible bins.<br />Bins need at least 10% fill.</p></div> : routeEligibleRequests.map((request, index) => { const bin = bins.find((item) => item.id === request.binId); const selected = selectedRouteRequestIds.includes(request.id); const fillPercentage = bin ? bin.currentFillKg / bin.capacityKg * 100 : request.priorityScore; const currentStatus = bin?.status ?? request.urgency; return <button key={request.id} className={`request-row ${selected ? 'selected' : ''}`} onClick={() => toggleRequest(request.id)}><span className="rank">0{index + 1}</span><span className={`priority-dot ${currentStatus.toLowerCase()}`} /><span className="request-main"><b>{request.binCode}</b><small>{bin?.locationName ?? 'Unknown location'} · {formatNumber(bin?.currentFillKg ?? 0)} / {formatNumber(bin?.capacityKg ?? 0)} kg</small></span><span className="request-score"><b>{formatNumber(fillPercentage)}%</b><small>full</small></span><span className={`status-pill ${currentStatus.toLowerCase()}`}>{statusLabel[currentStatus] ?? currentStatus}</span><span className={`check-box ${selected ? 'checked' : ''}`}>{selected && <Check size={13} />}</span></button> })}</div></div>
            <div className="panel fleet-panel"><div className="panel-header"><div><p className="eyebrow">FLEET STATUS</p><h2>Available trucks</h2></div><TruckIcon size={19} /></div><div className="truck-list">{trucks.map((truck) => <button key={truck.id} disabled={truck.status !== 'AVAILABLE'} className={`truck-row ${selectedTruck === truck.id ? 'selected' : ''}`} onClick={() => setSelectedTruck(truck.id)}><span className="truck-icon"><TruckIcon size={18} /></span><span><b>{truck.truckCode}</b><small>{formatNumber(truck.capacityKg)} kg capacity · {truck.status.toLowerCase()}</small></span><span className={`fleet-dot ${truck.status.toLowerCase()}`} /></button>)}</div><div className="capacity-meter"><div><span>Selected load</span><b>{formatNumber(selectedWeight)} / {selectedTruck ? formatNumber(trucks.find((truck) => truck.id === selectedTruck)?.capacityKg ?? 0) : '—'} kg</b></div><div className="meter-track"><span style={{ width: `${Math.min(100, selectedWeight / (Number(selectedTruck ? trucks.find((truck) => truck.id === selectedTruck)?.capacityKg : 1) || 1) * 100)}%` }} /></div></div><button className="route-button" onClick={optimizeRoute} disabled={isOptimizing || selectedRouteRequestIds.length === 0 || selectedTruck === '' || trucks.find((truck) => truck.id === selectedTruck)?.status !== 'AVAILABLE'}><Map size={17} /> {isOptimizing ? 'Calculating shortest path...' : 'Build optimized route'} <ArrowUpRight size={16} /></button></div>
          </section>
          <section className="map-panel panel"><div className="panel-header map-header"><div><p className="eyebrow">LIVE ROAD GRAPH · DIJKSTRA</p><h2>Collection area</h2></div><div className="map-legend"><span><i className="legend-dot depot" /> Depot</span><span><i className="legend-dot urgent" /> Urgent bin</span><span><i className="legend-dot route" /> Planned route</span></div></div><div className="map-wrap"><RouteCanvas locations={locations} roads={roads} bins={bins} route={route} truckLocationId={route ? trucks.find((truck) => truck.id === route.truckId)?.locationId ?? null : null} /><div className="map-overlay"><span className="mono">{route ? `ROUTE ${String(route.id).padStart(3, '0')}` : 'NO ACTIVE ROUTE'}</span><b>{route ? `${formatNumber(route.totalDistance)} km planned` : 'Select requests to begin'}</b></div></div></section>
        </>}

        {activeView === 'routes' && route && <section className="panel route-summary"><div><p className="eyebrow">LATEST PLAN</p><h2>Route #{route.id} ready for dispatch</h2><p>{route.stops.length} stops · {formatNumber(route.totalDistance)} km · {formatNumber(route.totalTime)} minutes</p><div className="route-stops">{route.stops.map((stop) => <div className="route-stop" key={stop.id}><span className={stop.status === 'COMPLETED' ? 'stop-number done' : 'stop-number'}>{stop.status === 'COMPLETED' ? <Check size={12} /> : String(stop.sequenceNumber).padStart(2, '0')}</span><span><b>Stop {String(stop.sequenceNumber).padStart(2, '0')}</b><small>{bins.find((bin) => bin.id === stop.binId)?.binCode ?? `Bin #${stop.binId}`}</small></span>{stop.status === 'PENDING' && route.status === 'COLLECTING' && <button className="stop-action" onClick={() => void completeStop(stop.id)}>Complete</button>}</div>)}</div></div><div className="route-actions"><span className="route-state"><span className="pulse" /> {route.status}</span>{route.status === 'ASSIGNED' && <button className="secondary-button" onClick={() => void startRoute()}>Start route</button>}{route.status === 'COLLECTING' && <button className="primary-button compact" disabled={route.stops.some((stop) => stop.status === 'PENDING')} onClick={() => void completeRoute()}>Complete route</button>}</div></section>}
        {activeView === 'routes' && route && <p className="route-report">{route.summary}</p>}
      </main>
    </div>
  )
}

function BinsPanel({ bins, locations, onCreated }: { bins: Bin[]; locations: Location[]; onCreated: () => Promise<void> }) {
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ binCode: '', locationId: locations[0]?.id ?? 0, capacityKg: 100, currentFillKg: 0, wasteType: 'GENERAL' as WasteType })
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')
  const submit = async (event: React.FormEvent) => { event.preventDefault(); setSaving(true); setFormError(''); try { await api.createBin(form); setShowForm(false); setForm({ ...form, binCode: '', currentFillKg: 0 }); await onCreated() } catch (error) { setFormError(error instanceof Error ? error.message : 'Could not create bin') } finally { setSaving(false) } }
  return <section className="panel bins-panel"><div className="panel-header"><div><p className="eyebrow">ASSET REGISTER · {bins.length} UNITS</p><h2>Bin network</h2></div><button className="secondary-button" onClick={() => setShowForm((current) => !current)}><PackagePlus size={16} /> {showForm ? 'Close form' : 'Add bin'}</button></div>{showForm && <form className="bin-form" onSubmit={submit}><label>Bin code<input required value={form.binCode} onChange={(event) => setForm({ ...form, binCode: event.target.value })} placeholder="BIN-004" /></label><label>Location<select value={form.locationId} onChange={(event) => setForm({ ...form, locationId: Number(event.target.value) })}>{locations.map((location) => <option key={location.id} value={location.id}>{location.name}</option>)}</select></label><label>Capacity (kg)<input type="number" min="1" value={form.capacityKg} onChange={(event) => setForm({ ...form, capacityKg: Number(event.target.value) })} /></label><label>Current fill (kg)<input type="number" min="0" value={form.currentFillKg} onChange={(event) => setForm({ ...form, currentFillKg: Number(event.target.value) })} /></label><label>Waste type<select value={form.wasteType} onChange={(event) => setForm({ ...form, wasteType: event.target.value as WasteType })}>{['GENERAL', 'ORGANIC', 'RECYCLABLE', 'HAZARDOUS'].map((type) => <option key={type}>{type}</option>)}</select></label><button className="primary-button compact" disabled={saving}>{saving ? 'Saving...' : 'Save bin'}</button>{formError && <span className="form-error">{formError}</span>}</form>}<div className="bin-table"><div className="table-head"><span>Bin</span><span>Location</span><span>Fill level</span><span>Type</span><span>Status</span></div>{bins.map((bin) => <div className="table-row" key={bin.id}><span><b>{bin.binCode}</b><small>#{String(bin.id).padStart(3, '0')}</small></span><span>{bin.locationName}</span><span><div className="mini-meter"><span style={{ width: `${(bin.currentFillKg / bin.capacityKg) * 100}%` }} /></div><small>{formatNumber(bin.currentFillKg)} / {formatNumber(bin.capacityKg)} kg</small></span><span className="type-label">{bin.wasteType}</span><span className={`status-pill ${bin.status.toLowerCase()}`}>{statusLabel[bin.status]}</span></div>)}</div></section>
}

export default App

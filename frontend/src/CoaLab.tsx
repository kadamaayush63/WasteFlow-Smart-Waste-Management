import { useState } from 'react'
import { Cpu, Play } from 'lucide-react'
import { api } from './api'
import type { Bin, CoaSimulation, Route } from './types'

interface CoaLabProps {
  bins: Bin[]
  route: Route | null
}

export function CoaLab({ bins, route }: CoaLabProps) {
  const [selectedBinId, setSelectedBinId] = useState<number | ''>(bins[0]?.id ?? '')
  const [binSimulation, setBinSimulation] = useState<CoaSimulation | null>(null)
  const [routeSimulation, setRouteSimulation] = useState<CoaSimulation | null>(null)
  const [busyOperation, setBusyOperation] = useState<'bin' | 'route' | null>(null)
  const [error, setError] = useState('')

  const selectedBin = bins.find((bin) => bin.id === selectedBinId)

  const runBinStatus = async () => {
    if (!selectedBin) return
    setBusyOperation('bin')
    setError('')
    try {
      setBinSimulation(await api.simulateBinStatus(selectedBin.currentFillKg, selectedBin.capacityKg))
    } catch (simulationError) {
      setError(simulationError instanceof Error ? simulationError.message : 'Bin CPU simulation failed')
    } finally {
      setBusyOperation(null)
    }
  }

  const runRouteTotals = async () => {
    if (!route) return
    setBusyOperation('route')
    setError('')
    try {
      setRouteSimulation(await api.simulateRouteTotals(route.totalDistance, route.totalTime))
    } catch (simulationError) {
      setError(simulationError instanceof Error ? simulationError.message : 'Route CPU simulation failed')
    } finally {
      setBusyOperation(null)
    }
  }

  return (
    <section className="coa-lab">
      <div className="panel coa-intro">
        <div className="panel-header">
          <div><p className="eyebrow">COMPUTER ORGANIZATION &amp; ARCHITECTURE</p><h2>Processor-level application model</h2></div>
          <Cpu size={20} />
        </div>
        <p className="panel-subtitle">Run WasteFlow calculations through a teaching simulator with 64-bit registers, ALU operations, flags, and fetch/decode/execute steps.</p>
        <p className="coa-notice">Educational x86-64 instruction-subset simulation; it does not execute native machine code on the CPU.</p>
      </div>

      {error && <div className="alert error">{error}</div>}

      <div className="coa-operation-grid">
        <article className="panel coa-operation">
          <div className="panel-header"><div><p className="eyebrow">OPERATION 01 · BIN DATA</p><h2>Calculate fill status</h2></div></div>
          <label className="coa-field">Waste bin
            <select value={selectedBinId} onChange={(event) => setSelectedBinId(Number(event.target.value))} disabled={bins.length === 0}>
              {bins.map((bin) => <option key={bin.id} value={bin.id}>{bin.binCode} · {bin.currentFillKg}/{bin.capacityKg} kg</option>)}
            </select>
          </label>
          <p className="panel-subtitle">Runs integer multiply/divide and comparisons to determine NORMAL, MEDIUM, HIGH, or CRITICAL status.</p>
          <button className="secondary-button coa-run" onClick={runBinStatus} disabled={!selectedBin || busyOperation !== null}><Play size={14} /> {busyOperation === 'bin' ? 'Executing...' : 'Run CPU simulation'}</button>
          {binSimulation && <SimulationOutput simulation={binSimulation} />}
        </article>

        <article className="panel coa-operation">
          <div className="panel-header"><div><p className="eyebrow">OPERATION 02 · ROUTE DATA</p><h2>Accumulate route totals</h2></div></div>
          {route
            ? <p className="coa-route-input">Route #{route.id}: {route.totalDistance.toFixed(3)} km · {route.totalTime.toFixed(3)} minutes (existing route totals supplied as one aggregate leg)</p>
            : <p className="panel-subtitle">Build or load a route first. Its application-computed distance and time will become CPU simulator inputs.</p>}
          <p className="panel-subtitle">Uses 64-bit ADD instructions and RAX/RBX registers to accumulate distance and travel time in milli-units.</p>
          <button className="secondary-button coa-run" onClick={runRouteTotals} disabled={!route || busyOperation !== null}><Play size={14} /> {busyOperation === 'route' ? 'Executing...' : 'Run CPU simulation'}</button>
          {routeSimulation && <SimulationOutput simulation={routeSimulation} />}
        </article>
      </div>
    </section>
  )
}

function SimulationOutput({ simulation }: { simulation: CoaSimulation }) {
  return (
    <div className="coa-result" aria-live="polite">
      <p className="coa-result-value">{simulation.result}</p>
      <div className="coa-registers"><b>Final registers</b>{Object.entries(simulation.registers).map(([register, value]) => <span key={register}>{register}<code>{value}</code></span>)}</div>
      <div className="coa-flags"><b>Flags</b> ZF {Number(simulation.flags.zero)} · SF {Number(simulation.flags.sign)} · CF {Number(simulation.flags.carry)} · OF {Number(simulation.flags.overflow)}</div>
      <details className="coa-trace"><summary>Assembly listing and execution trace ({simulation.trace.length} steps)</summary>
        <pre>{simulation.assemblyProgram.join('\n')}</pre>
        <ol>{simulation.trace.map((step) => <li key={step.step}><code>{step.instruction}</code><small>RAX {step.registers.RAX} · RBX {step.registers.RBX} · RCX {step.registers.RCX} · RDX {step.registers.RDX} · ZF {Number(step.flags.zero)}</small></li>)}</ol>
      </details>
    </div>
  )
}

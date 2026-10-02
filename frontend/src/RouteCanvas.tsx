import { Pause, Play, RotateCcw } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { drawClippedRasterLine, drawMidpointCircleOutline, drawMidpointFilledCircle } from './graphicsAlgorithms'
import type { Bin, Location, Road, Route } from './types'

interface RouteCanvasProps {
  locations: Location[]
  roads: Road[]
  bins: Bin[]
  route: Route | null
  truckLocationId: number | null
}

const palette = {
  ink: '#17221f',
  grid: '#d7d6ca',
  lime: '#c8f45b',
  coral: '#f2765f',
  paper: '#f7f5ed',
  blue: '#5c91a8',
}

export function RouteCanvas({ locations, roads, bins, route, truckLocationId }: RouteCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const animationFrame = useRef<number | null>(null)
  const startedAt = useRef(0)
  const pausedAt = useRef(0)
  const [progress, setProgress] = useState(0)
  const [isPlaying, setIsPlaying] = useState(false)

  const routeLocations = useMemo(() => {
    const start = locations.find((location) => location.id === truckLocationId)
    const stops = route?.stops.map((stop) => locations.find((location) => location.id === stop.locationId)).filter((location): location is Location => Boolean(location)) ?? []
    return start ? [start, ...stops] : stops
  }, [locations, route, truckLocationId])

  const stopCount = Math.max(0, routeLocations.length - 1)

  useEffect(() => {
    setProgress(0)
    setIsPlaying(route?.status === 'COLLECTING')
    pausedAt.current = 0
  }, [route?.id, route?.status])

  useEffect(() => {
    if (!isPlaying || stopCount === 0) return
    const duration = Math.max(2600, stopCount * 2200)
    startedAt.current = performance.now() - pausedAt.current * duration
    const animate = (now: number) => {
      const nextProgress = Math.min(1, (now - startedAt.current) / duration)
      setProgress(nextProgress)
      if (nextProgress < 1) animationFrame.current = requestAnimationFrame(animate)
      else { setIsPlaying(false); pausedAt.current = 1 }
    }
    animationFrame.current = requestAnimationFrame(animate)
    return () => { if (animationFrame.current !== null) cancelAnimationFrame(animationFrame.current) }
  }, [isPlaying, stopCount])

  const togglePlayback = () => {
    if (progress >= 1) { pausedAt.current = 0; setProgress(0) }
    else pausedAt.current = progress
    setIsPlaying((playing) => !playing)
  }

  const replay = () => { pausedAt.current = 0; setProgress(0); setIsPlaying(true) }

  useEffect(() => {
    const render = () => {
      const canvas = canvasRef.current
      if (!canvas) return
      const context = canvas.getContext('2d')
      if (!context) return
      const viewWidth = canvas.clientWidth
      const viewHeight = canvas.clientHeight
      if (viewWidth <= 0 || viewHeight <= 0) return

      const pixelRatio = window.devicePixelRatio || 1
      canvas.width = Math.round(viewWidth * pixelRatio)
      canvas.height = Math.round(viewHeight * pixelRatio)
      context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0)
      context.fillStyle = palette.paper
      context.fillRect(0, 0, viewWidth, viewHeight)

      const minX = Math.min(...locations.map((location) => location.xCoordinate), 0)
      const maxX = Math.max(...locations.map((location) => location.xCoordinate), 100)
      const minY = Math.min(...locations.map((location) => location.yCoordinate), 0)
      const maxY = Math.max(...locations.map((location) => location.yCoordinate), 100)
      const scale = Math.min((viewWidth - 64) / (maxX - minX), (viewHeight - 64) / (maxY - minY))
      const point = (locationId: number) => {
        const location = locations.find((item) => item.id === locationId)
        if (!location) return { x: viewWidth / 2, y: viewHeight / 2 }
        return {
          x: 32 + (location.xCoordinate - minX) * scale,
          y: 32 + (location.yCoordinate - minY) * scale,
        }
      }

      for (let x = 32; x < viewWidth; x += 32) {
        drawClippedRasterLine(context, { x, y: 0 }, { x, y: viewHeight }, viewWidth, viewHeight, palette.grid)
      }
      for (let y = 32; y < viewHeight; y += 32) {
        drawClippedRasterLine(context, { x: 0, y }, { x: viewWidth, y }, viewWidth, viewHeight, palette.grid)
      }

      roads.forEach((road) => {
        drawClippedRasterLine(
          context,
          point(road.sourceLocationId),
          point(road.destinationLocationId),
          viewWidth,
          viewHeight,
          '#bbbeb2',
          3,
        )
      })

      const stopLocations = routeLocations.map((location) => location.id)
      for (let index = 0; index < stopLocations.length - 1; index += 1) {
        drawClippedRasterLine(
          context,
          point(stopLocations[index]),
          point(stopLocations[index + 1]),
          viewWidth,
          viewHeight,
          palette.ink,
          5,
          [9, 7],
        )
      }

      if (routeLocations.length > 1) {
        const segmentProgress = progress * (routeLocations.length - 1)
        const segmentIndex = Math.max(0, Math.min(routeLocations.length - 2, Math.floor(segmentProgress)))
        const localProgress = Math.min(1, segmentProgress - segmentIndex)
        const startLocation = routeLocations[segmentIndex]
        const endLocation = routeLocations[segmentIndex + 1]
        if (!startLocation || !endLocation) return
        const start = point(startLocation.id)
        const end = point(endLocation.id)
        const truckX = start.x + (end.x - start.x) * localProgress
        const truckY = start.y + (end.y - start.y) * localProgress
        const heading = Math.atan2(end.y - start.y, end.x - start.x)
        context.save()
        context.translate(truckX, truckY)
        context.rotate(heading)
        context.fillStyle = palette.coral
        context.strokeStyle = palette.paper
        context.lineWidth = 3
        context.beginPath()
        context.roundRect(-14, -9, 24, 18, 4)
        context.fill(); context.stroke()
        context.fillStyle = palette.ink
        context.beginPath(); context.roundRect(5, -7, 9, 14, 2); context.fill()
        context.fillStyle = palette.ink
        context.beginPath(); context.arc(-7, 10, 3, 0, Math.PI * 2); context.fill()
        context.beginPath(); context.arc(8, 10, 3, 0, Math.PI * 2); context.fill()
        context.restore()
      }

      locations.forEach((location) => {
        const current = point(location.id)
        const isDepot = location.name.toLowerCase().includes('depot')
        drawMidpointFilledCircle(context, current, isDepot ? 8 : 5, isDepot ? palette.lime : palette.ink)
        context.fillStyle = palette.ink
        context.font = '11px Space Mono, monospace'
        context.fillText(location.name, current.x + 9, current.y - 9)
      })

      bins.forEach((bin) => {
        const current = point(bin.locationId)
        const color = bin.status === 'CRITICAL' ? palette.coral : bin.status === 'HIGH' ? '#f3bd57' : palette.blue
        drawMidpointFilledCircle(context, current, 9, color)
        drawMidpointCircleOutline(context, current, 10, palette.paper)
      })
    }

    render()
    const canvas = canvasRef.current
    if (!canvas) return
    const resizeObserver = new ResizeObserver(render)
    resizeObserver.observe(canvas)
    return () => resizeObserver.disconnect()
  }, [bins, locations, roads, progress, route, routeLocations])

  const currentStop = stopCount === 0 ? 0 : Math.min(stopCount, Math.floor(progress * stopCount) + 1)
  return <div className="route-canvas-stage"><canvas ref={canvasRef} className="route-canvas" aria-label="Waste collection road graph and animated truck route" /><div className="animation-controls"><button className="animation-button" onClick={togglePlayback} title={isPlaying ? 'Pause truck animation' : 'Play truck animation'}>{isPlaying ? <Pause size={14} /> : <Play size={14} />}</button><button className="animation-button" onClick={replay} title="Replay truck animation"><RotateCcw size={14} /></button><span><b>{route ? (progress >= 1 ? 'Route complete' : `Truck moving · stop ${currentStop} of ${stopCount}`) : 'No route selected'}</b><small>{route ? `${Math.round(progress * 100)}% of optimal sequence` : 'Build a route to animate'}</small></span><div className="animation-progress"><span style={{ width: `${progress * 100}%` }} /></div></div></div>
}

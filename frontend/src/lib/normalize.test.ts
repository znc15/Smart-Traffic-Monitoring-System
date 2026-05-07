import { describe, expect, it } from 'vitest'
import {
  normalizeAdminNodeHealth,
  normalizeMapOverviewPoint,
  normalizeNodeRuntimeConfig,
  normalizeSiteSettings,
  normalizeTrafficEvent,
  normalizeTrafficInfo,
} from './normalize'

describe('lib/normalize', () => {
  it('should prefer snake_case fields and keep numeric defaults stable', () => {
    const result = normalizeTrafficInfo({
      count_car: 5,
      countMotor: 99,
      count_person: 2,
      speed_car: 31.5,
      densityStatus: 'clear',
      density_status: 'busy',
      speed_status: 'slow',
      online: true,
    })

    expect(result).toEqual({
      count_car: 5,
      count_motor: 99,
      count_person: 2,
      speed_car: 31.5,
      speed_motor: 0,
      density_status: 'busy',
      speed_status: 'slow',
      online: true,
    })
  })

  it('should derive density status from counts and force offline status when needed', () => {
    expect(
      normalizeTrafficInfo({
        count_car: 10,
        count_motor: 0,
        count_person: 0,
        online: true,
      }).density_status,
    ).toBe('busy')

    expect(
      normalizeTrafficInfo({
        count_car: 20,
        density_status: 'busy',
        online: false,
      }).density_status,
    ).toBe('offline')
  })

  it('should normalize map overview points from mixed snake_case and camelCase payloads', () => {
    const point = normalizeMapOverviewPoint({
      cameraId: 7,
      name: 'cam-1',
      road_name: '人民路',
      edgeNodeId: 'edge-01',
      latitude: '31.2304',
      longitude: '121.4737',
      congestionIndex: 0.72,
      countCar: 12,
      count_motor: 3,
      countPerson: 1,
      speedCar: 26.5,
      speed_motor: 18.2,
      snapshotUrl: '/api/v1/frames_no_auth/人民路',
      updatedAt: '2026-03-11T10:00:00',
      online: true,
      densityStatus: 'congested',
    })

    expect(point.camera_id).toBe(7)
    expect(point.road_name).toBe('人民路')
    expect(point.edge_node_id).toBe('edge-01')
    expect(point.latitude).toBeCloseTo(31.2304)
    expect(point.longitude).toBeCloseTo(121.4737)
    expect(point.count_car).toBe(12)
    expect(point.count_motor).toBe(3)
    expect(point.snapshot_url).toBe('/api/v1/frames_no_auth/人民路')
    expect(point.density_status).toBe('congested')
  })

  it('should show offline map overview points as offline even without density payload', () => {
    const point = normalizeMapOverviewPoint({
      road_name: '离线路段',
      online: false,
      count_car: 12,
    })

    expect(point.density_status).toBe('offline')
  })

  it('should normalize node runtime config arrays and numeric fields', () => {
    const config = normalizeNodeRuntimeConfig({
      roadName: '中山路',
      mode: 'camera',
      camera_source: 'rtsp://demo',
      analysisRoi: [0.1, 0.2, 0.9, 0.95],
      lane_split_ratios: ['0.3', 0.7],
      speedMetersPerPixel: '0.12',
      parking_stationary_seconds: '6',
      wrongWayMinTrackPoints: '5',
      telemetry_interval_sec: '4.5',
    })

    expect(config).toEqual({
      road_name: '中山路',
      mode: 'camera',
      camera_source: 'rtsp://demo',
      analysis_roi: [0.1, 0.2, 0.9, 0.95],
      lane_split_ratios: [0.3, 0.7],
      speed_meters_per_pixel: 0.12,
      parking_stationary_seconds: 6,
      wrong_way_min_track_points: 5,
      telemetry_interval_sec: 4.5,
    })
  })

  it('should normalize site settings with amap_key from mixed payload styles', () => {
    const settings = normalizeSiteSettings({
      siteName: '智慧交通',
      announcement: '欢迎使用',
      logo_url: 'https://example.com/logo.png',
      footerText: 'demo footer',
      amapKey: 'demo-amap-key',
    })

    expect(settings).toEqual({
      site_name: '智慧交通',
      announcement: '欢迎使用',
      logo_url: 'https://example.com/logo.png',
      footer_text: 'demo footer',
      amap_key: 'demo-amap-key',
    })
  })

  it('should normalize admin node health and preserve diagnostic fields', () => {
    const node = normalizeAdminNodeHealth({
      cameraId: 11,
      name: 'edge-cam-01',
      road_name: '人民路',
      edgeNodeId: 'edge-01',
      nodeUrl: 'http://edge-node',
      online: true,
      health_status: 'degraded',
      statusReasonCode: 'frame_fetch_failed',
      status_reason_message: '视频帧拉取失败',
      lastErrorStage: 'frame',
      last_error: 'Read timed out',
      latency_ms: 215,
      errorCount: 3,
      consecutive_failures: 2,
      edge_metrics: { fps: 24.5 },
    })

    expect(node).toEqual({
      camera_id: 11,
      name: 'edge-cam-01',
      road_name: '人民路',
      edge_node_id: 'edge-01',
      node_url: 'http://edge-node',
      online: true,
      health_status: 'degraded',
      status_reason_code: 'frame_fetch_failed',
      status_reason_message: '视频帧拉取失败',
      last_error_stage: 'frame',
      last_success_time: null,
      last_poll_time: null,
      latency_ms: 215,
      error_count: 3,
      consecutive_failures: 2,
      last_error: 'Read timed out',
      edge_metrics: { fps: 24.5 },
      cpu_usage: null,
      memory_usage: null,
      disk_usage: null,
      uptime_seconds: null,
    })
  })

  it('should derive monitoring metrics from edge_metrics payloads', () => {
    const node = normalizeAdminNodeHealth({
      road_name: '中山路',
      edge_metrics: {
        cpu_percent: '62.4',
        memory_percent: 48.5,
        disk_percent: 71.2,
        uptime_s: '7200',
      },
    })

    expect(node.cpu_usage).toBe(62.4)
    expect(node.memory_usage).toBe(48.5)
    expect(node.disk_usage).toBe(71.2)
    expect(node.uptime_seconds).toBe(7200)
  })

  it('should parse traffic event payload_json for node health transitions', () => {
    const event = normalizeTrafficEvent({
      road_name: '人民路',
      event_type: 'node_health_status_changed',
      level: 'warning',
      start_at: '2026-03-11T14:00:00',
      payload_json: JSON.stringify({
        from_health_status: 'offline',
        to_health_status: 'degraded',
        reason_code: 'frame_fetch_failed',
      }),
    })

    expect(event.event_type).toBe('node_health_status_changed')
    expect(event.payload).toEqual({
      from_health_status: 'offline',
      to_health_status: 'degraded',
      reason_code: 'frame_fetch_failed',
    })
  })
})

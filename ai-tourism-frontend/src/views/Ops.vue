<template>
  <div class="ops-page">
    <header class="ops-header">
      <div><RouterLink to="/explore" title="返回游客端"><i class="fas fa-arrow-left"></i></RouterLink><h1>公益运营看板</h1><span>匿名聚合 · 演示环境</span></div>
      <div class="demo-actions">
        <button :disabled="busy" @click="injectClosure"><i class="fas fa-road-barrier"></i>注入当前点位封路</button>
        <button :disabled="busy" @click="resetAll"><i class="fas fa-rotate-left"></i>一键重置</button>
      </div>
    </header>

    <main class="ops-main">
      <section class="metric-strip">
        <article><span>匿名路线</span><strong>{{ dashboard.routes_generated || 0 }}</strong></article>
        <article><span>按时完成率</span><strong>{{ percent(dashboard.on_time_rate) }}</strong></article>
        <article><span>改线成功率</span><strong>{{ percent(dashboard.reroute_success_rate) }}</strong></article>
        <article><span>生效事件</span><strong>{{ dashboard.active_events || 0 }}</strong></article>
        <article><span>待处理工单</span><strong>{{ dashboard.open_feedback || 0 }}</strong></article>
      </section>

      <section class="ops-band overview-grid">
        <div>
          <div class="section-heading"><div><span>ANONYMOUS FLOW</span><h2>澳门区域热力</h2></div><em>模拟 / 匿名聚合数据</em></div>
          <div id="ops-heat-map" class="heat-map"></div>
        </div>
        <div class="region-table">
          <h3>区域路线分布</h3>
          <div v-for="region in regions" :key="region.code"><span>{{ region.label }}</span><progress :value="region.value" :max="maxRegion"></progress><strong>{{ region.value }}</strong></div>
        </div>
      </section>

      <section class="ops-band event-grid">
        <form class="event-form" @submit.prevent="createEvent">
          <div class="section-heading"><div><span>LEFT · TEST PARAMETERS</span><h2>测试事件参数</h2></div><em>区域可直接输入</em></div>
          <div class="form-grid">
            <label>事件类型<select v-model="eventForm.type"><option value="ROAD_CLOSURE">封路</option><option value="HEAVY_RAIN">暴雨</option><option value="VENUE_CLOSED">场馆关闭</option></select></label>
            <label>影响等级<select v-model="eventForm.severity"><option value="INFO">普通预警</option><option value="MODERATE">中度影响</option><option value="HIGH">高危安全</option></select></label>
            <label>影响区域<input v-model.trim="eventForm.region" list="event-region-options" maxlength="80" placeholder="选择或输入街区/区域" required /><datalist id="event-region-options"><option v-for="(label, code) in regionNames" :key="code" :value="code">{{ label }}</option></datalist></label>
            <div ref="poiSelectionPanel" class="poi-multi-field" :class="{ invalid: selectionError }">
              <div class="multi-heading"><strong>{{ affectedPoiLabel }} <em>必选</em></strong><span>已选 {{ eventForm.poi_codes.length }} 个</span></div>
              <div class="multi-tools">
                <button type="button" @click="selectAllAvailable"><i class="fas fa-check-double"></i>选择当前区域全部</button>
                <button type="button" @click="clearPoiSelection"><i class="fas fa-xmark"></i>清空</button>
              </div>
              <label class="address-search"><i class="fas fa-magnifying-glass"></i><input v-model.trim="poiQuery" type="search" placeholder="搜索地点名称或高德地址" /></label>
              <div class="poi-check-list">
                <div v-if="catalogLoading" class="catalog-option-state"><span class="loading-dot"></span>正在加载澳门地点目录...</div>
                <div v-else-if="catalogError" class="catalog-option-state error"><span>{{ catalogError }}</span><button type="button" @click="loadCatalog"><i class="fas fa-rotate"></i>重试加载</button></div>
                <template v-else>
                  <label v-for="poi in filteredAvailablePois" :key="poi.poi_code">
                    <input v-model="eventForm.poi_codes" type="checkbox" :value="poi.poi_code" />
                    <span><strong>{{ poi.name }}</strong><small class="poi-address"><i class="fas fa-location-dot"></i>{{ addressFor(poi.poi_code) || '正在读取高德地址...' }}</small><small>{{ regionNames[poi.region] || poi.region }} · {{ poi.poi_code }}</small></span>
                  </label>
                </template>
                <p v-if="!catalogLoading && !catalogError && !filteredAvailablePois.length" class="empty-addresses">当前区域没有匹配的地点或地址</p>
              </div>
              <p v-if="selectionError" class="selection-error"><i class="fas fa-circle-exclamation"></i>请在上方清单中至少勾选一个受影响地点</p>
            </div>
          </div>
          <div v-if="selectedEventPois.length" class="event-location-previews">
            <div v-for="poi in selectedEventPois" :key="poi.poi_code" class="event-location-preview">
              <span class="location-icon"><i :class="eventIcon(eventForm.type)"></i></span>
              <div><strong>{{ poi.name }}</strong><span>{{ addressFor(poi.poi_code) || `${regionNames[poi.region] || poi.region} · ${poi.poi_code}` }}</span><small v-if="previewFor(poi.poi_code)?.location">高德坐标 {{ formatCoordinate(previewFor(poi.poi_code).location) }}</small><small v-else>{{ previewFor(poi.poi_code)?.error || '正在通过高德定位...' }}</small></div>
              <button type="button" title="在地图定位" @click="focusSelectedPois(eventForm.poi_codes, eventForm.type, poi.poi_code)"><i class="fas fa-crosshairs"></i></button>
            </div>
          </div>
          <label>标题<input v-model.trim="eventForm.title" maxlength="120" required /></label>
          <label>说明<textarea v-model.trim="eventForm.description" maxlength="500" required></textarea></label>
          <label class="check-line"><input v-model="eventForm.simulated" type="checkbox" />标记为模拟事件</label>
          <button class="primary"><i class="fas fa-plus"></i>发布事件</button>
        </form>

        <div class="event-list">
          <div class="section-heading"><div><span>RIGHT · TEST RESULTS</span><h2>事件结果与生命周期</h2></div><button class="icon-button" title="刷新" @click="load"><i class="fas fa-rotate"></i></button></div>
          <article v-for="event in events" :key="event.event_id">
            <div><span :class="`severity ${event.severity.toLowerCase()}`">{{ event.severity }}</span><em v-if="event.simulated">SIM</em><strong>{{ event.title }}</strong></div>
            <p>{{ event.description }}</p><small>事件版本 v{{ event.version }}</small>
            <small class="event-place"><i class="fas fa-location-dot"></i>{{ displayRegion(event.region) }} · {{ affectedPoiNames(event) }}</small>
            <div class="event-card-actions">
              <button title="在地图定位" @click="focusEvent(event)"><i class="fas fa-crosshairs"></i></button>
              <button v-if="event.status === 'ACTIVE'" title="撤销事件" @click="cancelEvent(event.event_id)"><i class="fas fa-ban"></i>撤销</button>
              <span v-else class="event-status">{{ event.status }}</span>
            </div>
          </article>
          <p v-if="!events.length" class="empty-state">暂无事件</p>
        </div>
      </section>

      <section class="ops-band feedback-band">
        <div class="section-heading"><div><span>FEEDBACK DESK</span><h2>游客反馈工单</h2></div><em>不包含非必要身份信息</em></div>
        <div class="feedback-table">
          <div class="table-head"><span>编号</span><span>分类</span><span>内容</span><span>关联行程</span><span>状态</span><span>操作</span></div>
          <div v-for="item in feedback" :key="item.feedback_id" class="table-row">
            <span>{{ item.feedback_id }}</span><span>{{ item.category }}</span><span>{{ item.content }}</span><span>{{ item.trip_id || '—' }} <small v-if="item.trip_version">v{{ item.trip_version }}</small></span><span>{{ item.status }}</span>
            <span><button v-if="item.status !== 'CLOSED'" @click="closeFeedback(item.feedback_id)"><i class="fas fa-check"></i>关闭</button><em v-else>已处置</em></span>
          </div>
          <p v-if="!feedback.length" class="empty-state">暂无游客反馈</p>
        </div>
      </section>
      <p v-if="message" class="ops-message">{{ message }}</p>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { changeOpsEventStatus, createOpsEvent, fetchCatalogPois, fetchOpsDashboard, fetchOpsEvents, fetchOpsFeedback, resetDemo, updateOpsFeedback } from '../utils/api.js'

const dashboard = ref({})
const events = ref([])
const feedback = ref([])
const pois = ref([])
const busy = ref(false)
const message = ref('')
const locationPreviews = ref({})
const poiDetails = ref({})
const poiQuery = ref('')
const poiSelectionPanel = ref(null)
const catalogLoading = ref(true)
const catalogError = ref('')
const selectionError = ref(false)
const eventForm = reactive({ type: 'ROAD_CLOSURE', severity: 'MODERATE', region: 'PENINSULA', poi_codes: [], title: '道路临时封闭', description: '受交通管制影响，所选地点暂时无法通行。', simulated: true })
let heatMap = null
let map = null
let eventLocationOverlays = []
let eventLocationRenderVersion = 0
const amapEventLocations = new Map()
const regionNames = { PENINSULA: '澳门半岛', TAIPA: '氹仔', COTAI: '路氹', COLOANE: '路环' }
const regions = computed(() => Object.entries(regionNames).map(([code, label]) => ({ code, label, value: dashboard.value.routes_by_region?.[code] || 0 })))
const maxRegion = computed(() => Math.max(1, ...regions.value.map(item => item.value)))
const venueCategories = new Set(['ATTRACTION', 'CULTURE', 'PUBLIC_SERVICE', 'WELLNESS'])
const availablePois = computed(() => {
  let candidates = eventForm.type === 'VENUE_CLOSED' ? pois.value.filter(poi => venueCategories.has(poi.category)) : pois.value
  if (regionNames[eventForm.region]) candidates = candidates.filter(poi => poi.region === eventForm.region)
  return candidates.length ? candidates : pois.value
})
const filteredAvailablePois = computed(() => {
  const query = poiQuery.value.toLowerCase()
  if (!query) return availablePois.value
  return availablePois.value.filter(poi => `${poi.name} ${addressFor(poi.poi_code)} ${poi.poi_code}`.toLowerCase().includes(query))
})
const affectedPoiLabel = computed(() => eventForm.type === 'VENUE_CLOSED' ? '选择关闭场馆' : '受影响地点')
const selectedEventPois = computed(() => eventForm.poi_codes.map(code => pois.value.find(poi => poi.poi_code === code)).filter(Boolean))
function percent(value) { return `${Math.round((value ?? 0) * 100)}%` }
function displayRegion(region) { return regionNames[region] || region }
function poiName(code) { return pois.value.find(poi => poi.poi_code === code)?.name || code }
function affectedPoiNames(event) { return (event.affected_poi_codes || []).map(poiName).join('、') || '未指定地点' }
function eventIcon(type) { return type === 'VENUE_CLOSED' ? 'fas fa-building-circle-xmark' : type === 'HEAVY_RAIN' ? 'fas fa-cloud-showers-heavy' : 'fas fa-road-barrier' }
function formatCoordinate(location) { return `${location[1].toFixed(5)}, ${location[0].toFixed(5)}` }
function previewFor(code) { return locationPreviews.value[code] }
function addressFor(code) { return poiDetails.value[code]?.address || '' }
function selectAllAvailable() { eventForm.poi_codes = availablePois.value.map(poi => poi.poi_code); selectionError.value = false }
function clearPoiSelection() { eventForm.poi_codes = [] }
function requirePoiSelection() {
  if (eventForm.poi_codes.length) { selectionError.value = false; return true }
  selectionError.value = true
  message.value = '请至少选择一个受影响地点'
  nextTick(() => poiSelectionPanel.value?.scrollIntoView({ behavior: 'smooth', block: 'center' }))
  return false
}
async function loadCatalog() {
  catalogLoading.value = true; catalogError.value = ''
  try {
    const catalog = await fetchCatalogPois({ lang: 'zh-Hans' })
    pois.value = catalog.items || []
    if (!pois.value.length) catalogError.value = '地点目录为空，请重试加载'
    else queueMicrotask(loadAvailablePoiAddresses)
  } catch (error) {
    pois.value = []
    catalogError.value = error.message || '地点目录加载失败'
  } finally { catalogLoading.value = false }
}

watch([() => eventForm.type, () => eventForm.region, availablePois], () => {
  const visibleCodes = new Set(availablePois.value.map(poi => poi.poi_code))
  eventForm.poi_codes = eventForm.poi_codes.filter(code => visibleCodes.has(code))
  if (eventForm.type === 'VENUE_CLOSED') {
    eventForm.title = '场馆临时关闭'; eventForm.description = '受现场管控影响，所选场馆暂时关闭，请调整行程。'
  }
  queueMicrotask(loadAvailablePoiAddresses)
  queueMicrotask(() => focusSelectedPois(eventForm.poi_codes, eventForm.type))
}, { flush: 'post' })
watch(() => [...eventForm.poi_codes], codes => focusSelectedPois(codes, eventForm.type), { flush: 'post' })
watch(() => eventForm.poi_codes.length, count => { if (count) selectionError.value = false })

async function load() {
  try {
    const [metrics, eventData, feedbackData] = await Promise.all([fetchOpsDashboard(), fetchOpsEvents(), fetchOpsFeedback()])
    dashboard.value = metrics || {}; events.value = eventData || []; feedback.value = feedbackData || []
    renderHeat()
  } catch (error) { message.value = error.message }
}
async function createEvent() {
  if (!requirePoiSelection()) return
  busy.value = true
  try {
    const now = new Date(); const end = new Date(now.getTime() + 4 * 60 * 60 * 1000)
    await createOpsEvent({ type: eventForm.type, severity: eventForm.severity, title: eventForm.title, description: eventForm.description, region: eventForm.region, affected_poi_codes: [...eventForm.poi_codes], starts_at: now.toISOString(), ends_at: end.toISOString(), simulated: eventForm.simulated })
    message.value = '事件已发布并通过 SSE 推送'; await load()
  } catch (error) { message.value = error.message } finally { busy.value = false }
}
async function cancelEvent(id) { await changeOpsEventStatus(id, 'CANCELLED'); await load() }
async function closeFeedback(id) { await updateOpsFeedback(id, { status: 'CLOSED', resolution: '运营端已核查并完成处置。' }); await load() }
async function injectClosure() {
  if (!requirePoiSelection()) return
  busy.value = true
  try {
    const now = new Date(); const end = new Date(now.getTime() + 2 * 60 * 60 * 1000)
    await createOpsEvent({ type: 'ROAD_CLOSURE', severity: 'MODERATE', title: '多地点模拟封路', description: '所选地点已注入模拟交通冲突。', region: eventForm.region, affected_poi_codes: [...eventForm.poi_codes], starts_at: now.toISOString(), ends_at: end.toISOString(), simulated: true })
    message.value = `已注入 ${eventForm.poi_codes.length} 个地点的模拟封路`; await load()
  } finally { busy.value = false }
}
async function resetAll() { busy.value = true; try { await resetDemo(); message.value = '演示数据已重置'; await load() } finally { busy.value = false } }
async function initHeatMap() {
  for (let count = 0; count < 30 && !window.AMap; count += 1) await new Promise(resolve => setTimeout(resolve, 200))
  if (!window.AMap) return
  map = new window.AMap.Map('ops-heat-map', { center: [113.5554, 22.1654], zoom: 12, viewMode: '2D', mapStyle: 'amap://styles/normal' })
  window.AMap.plugin(['AMap.HeatMap', 'AMap.PlaceSearch'], () => {
    heatMap = new window.AMap.HeatMap(map, { radius: 32, opacity: [0, .75] }); renderHeat(); loadAvailablePoiAddresses(); focusSelectedPois(eventForm.poi_codes, eventForm.type)
  })
}
function resolvePoiLocation(poi) {
  if (amapEventLocations.has(poi.poi_code)) return Promise.resolve(amapEventLocations.get(poi.poi_code))
  return new Promise(resolve => {
    if (!window.AMap?.PlaceSearch) { resolve(null); return }
    const search = new window.AMap.PlaceSearch({ city: '澳门特别行政区', citylimit: true, pageSize: 6, extensions: 'base' })
    search.search(poi.name, (status, result) => {
      const candidates = status === 'complete' ? (result?.poiList?.pois || []) : []
      const match = candidates.find(item => item.name === poi.name) || candidates[0]
      if (!match?.location) {
        const unavailable = { location: null, address: '高德未返回该地点地址' }
        amapEventLocations.set(poi.poi_code, unavailable)
        poiDetails.value = { ...poiDetails.value, [poi.poi_code]: unavailable }
        resolve(unavailable)
        return
      }
      const returnedAddress = Array.isArray(match.address) ? match.address.join('') : match.address
      const detail = {
        location: [Number(match.location.lng), Number(match.location.lat)],
        address: returnedAddress || [match.pname, match.cityname, match.adname].filter(Boolean).join('') || poi.name
      }
      amapEventLocations.set(poi.poi_code, detail)
      poiDetails.value = { ...poiDetails.value, [poi.poi_code]: detail }
      resolve(detail)
    })
  })
}
async function loadAvailablePoiAddresses() {
  if (!window.AMap?.PlaceSearch) return
  const pending = availablePois.value.filter(poi => !amapEventLocations.has(poi.poi_code))
  for (let index = 0; index < pending.length; index += 4) {
    await Promise.all(pending.slice(index, index + 4).map(resolvePoiLocation))
  }
}
function clearEventLocation() {
  if (eventLocationOverlays.length) map?.remove(eventLocationOverlays)
  eventLocationOverlays = []
}
async function focusSelectedPois(codes, type = 'ROAD_CLOSURE', focusCode = null) {
  if (!map) return
  const renderVersion = ++eventLocationRenderVersion
  clearEventLocation()
  const nextPreviews = {}
  for (const code of codes) {
    const poi = pois.value.find(item => item.poi_code === code)
    if (!poi) continue
    const detail = await resolvePoiLocation(poi)
    if (renderVersion !== eventLocationRenderVersion) return
    const location = detail?.location
    nextPreviews[code] = location ? { poi_code: code, location, address: detail.address, error: '' } : { poi_code: code, location: null, error: '高德未返回该地点位置' }
    if (!location) continue
    const node = document.createElement('span')
    node.className = `ops-event-marker ${type.toLowerCase()}`
    node.innerHTML = `<i class="${eventIcon(type)}"></i>`
    const marker = new window.AMap.Marker({ position: location, content: node, offset: new window.AMap.Pixel(-17, -17), zIndex: 300 })
    const radius = type === 'HEAVY_RAIN' ? 500 : type === 'VENUE_CLOSED' ? 90 : 180
    const circle = new window.AMap.Circle({ center: location, radius, strokeColor: '#b2473b', strokeWeight: 2, strokeOpacity: .85, fillColor: '#ef8b77', fillOpacity: .18, zIndex: 120 })
    eventLocationOverlays.push(circle, marker)
  }
  locationPreviews.value = nextPreviews
  if (!eventLocationOverlays.length) return
  map.add(eventLocationOverlays)
  const focused = nextPreviews[focusCode]?.location
  if (focused) map.setZoomAndCenter(type === 'HEAVY_RAIN' ? 14 : 16, focused, false, 350)
  else map.setFitView(eventLocationOverlays, false, [60, 60, 60, 60], 15)
}
function focusEvent(event) { return focusSelectedPois(event.affected_poi_codes || [], event.type) }
function renderHeat() {
  if (!heatMap) return
  const data = (dashboard.value.heat_points || []).map(item => ({ lng: item.longitude, lat: item.latitude, count: Math.max(1, item.value) }))
  heatMap.setDataSet({ data, max: Math.max(1, ...data.map(item => item.count)) })
}
onMounted(async () => {
  await loadCatalog()
  await nextTick(); await Promise.all([load(), initHeatMap()])
})
onBeforeUnmount(() => map?.destroy())
</script>

<style scoped>
.ops-page { min-height: 100vh; background: #f3f6f5; color: #17242d; font-family: Inter,"Microsoft YaHei",sans-serif; }.ops-header { min-height: 64px; display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 10px 24px; border-bottom: 1px solid #dce3e4; background: #fff; }.ops-header > div { display: flex; align-items: center; gap: 10px; }.ops-header a { width: 34px; height: 34px; display: grid; place-items: center; border: 1px solid #dce3e4; border-radius: 5px; color: #176d5d; }.ops-header h1 { margin: 0; font-size: 19px; }.ops-header span { color: #6c797f; font-size: 11px; }.demo-actions button { min-height: 34px; border: 1px solid #d5dfe0; border-radius: 5px; background: #fff; color: #486068; padding: 0 11px; cursor: pointer; }.demo-actions button:first-child { border-color: #a95748; color: #914337; }.demo-actions i { margin-right: 5px; }
.ops-main { width: 100%; max-width: 1680px; display: flex; flex-direction: column; box-sizing: border-box; margin: auto; padding: 18px 24px 36px; }.metric-strip { order: 0; display: grid; grid-template-columns: repeat(5,1fr); border: 1px solid #dce3e4; background: #fff; }.metric-strip article { min-width: 0; padding: 15px 17px; border-right: 1px solid #e3e8e9; }.metric-strip article:last-child { border: 0; }.metric-strip span { display: block; color: #6d7b81; font-size: 11px; }.metric-strip strong { display: block; margin-top: 6px; font-size: 24px; }
.ops-band { margin-top: 18px; padding: 18px; border: 1px solid #dce3e4; background: #fff; }.overview-grid { order: 2; display: grid; grid-template-columns: minmax(0,2fr) minmax(240px,1fr); gap: 20px; }.feedback-band { order: 3; }.section-heading { min-height: 42px; display: flex; align-items: start; justify-content: space-between; gap: 12px; }.section-heading span { color: #176d5d; font-size: 9px; font-weight: 900; }.section-heading h2 { margin: 3px 0 0; font-size: 16px; }.section-heading em { color: #7b878c; font-size: 10px; font-style: normal; }.heat-map { height: 330px; border: 1px solid #dce3e4; }.region-table h3 { margin: 5px 0 16px; font-size: 13px; }.region-table > div { display: grid; grid-template-columns: 74px 1fr 28px; gap: 8px; align-items: center; margin: 14px 0; font-size: 11px; }.region-table progress { width: 100%; height: 8px; accent-color: #176d5d; }
.event-grid { order: 1; display: grid; grid-template-columns: minmax(440px,1fr) minmax(0,1.2fr); gap: 32px; align-items: start; }.event-form,.event-list { min-width: 0; }.event-form { padding-right: 28px; border-right: 1px solid #e1e7e8; }.event-form > label { display: grid; gap: 5px; margin: 10px 0; color: #596a71; font-size: 10px; font-weight: 700; }.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 9px; }.form-grid label { display: grid; gap: 5px; color: #596a71; font-size: 10px; font-weight: 700; }.event-form input,.event-form select,.event-form textarea { width: 100%; box-sizing: border-box; min-width: 0; min-height: 36px; border: 1px solid #d6dfe0; border-radius: 5px; padding: 7px 8px; background: #fff; font: inherit; }.event-form textarea { min-height: 70px; resize: vertical; }.event-form .check-line { display: flex; align-items: center; }.event-form .check-line input { width: auto; min-height: 0; }.primary { min-height: 36px; border: 1px solid #176d5d; border-radius: 5px; background: #176d5d; color: #fff; padding: 0 13px; font-weight: 800; cursor: pointer; }.primary i { margin-right: 5px; }
.poi-multi-field { grid-column: 1 / -1; min-width: 0; padding: 8px; border: 1px solid transparent; border-radius: 6px; transition: border-color .18s ease, background .18s ease; }.poi-multi-field.invalid { border-color: #c54e40; background: #fff5f2; box-shadow: 0 0 0 2px rgba(197,78,64,.1); }.multi-heading { display: flex; align-items: center; justify-content: space-between; color: #596a71; font-size: 10px; }.multi-heading em { margin-left: 4px; color: #b54336; font-size: 8px; font-style: normal; }.multi-heading span { color: #176d5d; font-weight: 800; }.multi-tools { display: flex; gap: 6px; margin: 6px 0; }.multi-tools button { min-height: 28px; border: 1px solid #d6dfe0; border-radius: 4px; background: #fff; color: #52666d; padding: 0 8px; font-size: 9px; cursor: pointer; }.multi-tools i { margin-right: 4px; }.address-search { height: 34px; display: grid !important; grid-template-columns: 16px 1fr !important; align-items: center; gap: 5px !important; margin-bottom: 6px; padding: 0 8px; border: 1px solid #d6dfe0; border-radius: 5px; background: #fff; color: #829096 !important; }.address-search input { width: 100%; min-height: 30px; padding: 0; border: 0; outline: 0; }.poi-check-list { max-height: 230px; display: grid; grid-template-columns: 1fr 1fr; gap: 4px; overflow-y: auto; padding: 5px; border: 1px solid #d6dfe0; border-radius: 5px; background: #f8faf9; }.poi-check-list label { min-width: 0; min-height: 52px; display: grid; grid-template-columns: 16px minmax(0,1fr); align-items: center; gap: 6px; padding: 5px 6px; border-radius: 4px; background: #fff; color: #42575f; cursor: pointer; }.poi-check-list input { width: 14px; min-height: 14px; padding: 0; accent-color: #176d5d; }.poi-check-list span { min-width: 0; display: grid; gap: 2px; }.poi-check-list strong,.poi-check-list small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.poi-check-list strong { font-size: 10px; }.poi-check-list small { color: #7a888e; font-size: 8px; }.poi-check-list .poi-address { color: #486a62; }.poi-address i { margin-right: 4px; color: #b14c3f; }.empty-addresses { grid-column: 1 / -1; margin: 0; padding: 18px; color: #7a878c; text-align: center; font-size: 10px; }.catalog-option-state { grid-column: 1 / -1; min-height: 80px; display: flex; align-items: center; justify-content: center; gap: 8px; color: #65767d; font-size: 10px; }.catalog-option-state.error { flex-direction: column; color: #a04438; }.catalog-option-state button { min-height: 28px; border: 1px solid #b85a4b; border-radius: 4px; background: #fff; color: #93483b; padding: 0 9px; cursor: pointer; }.loading-dot { width: 10px; height: 10px; border: 2px solid #bdd3ce; border-top-color: #176d5d; border-radius: 50%; animation: eventSpin .7s linear infinite; }.selection-error { margin: 6px 0 0; color: #ac4033; font-size: 9px; font-weight: 800; }.selection-error i { margin-right: 4px; }@keyframes eventSpin { to { transform: rotate(360deg); } }
.event-location-previews { max-height: 176px; overflow-y: auto; margin: 10px 0; padding-right: 3px; }
.event-location-preview { display: grid; grid-template-columns: 38px 1fr 32px; gap: 9px; align-items: center; margin: 11px 0; padding: 9px; border: 1px solid #d8e3e4; border-left: 3px solid #b95445; border-radius: 5px; background: #f8fbfa; }.location-icon { width: 34px; height: 34px; display: grid; place-items: center; border-radius: 50%; background: #f6ded8; color: #9a4034; }.event-location-preview > div { min-width: 0; display: grid; gap: 2px; }.event-location-preview strong { font-size: 11px; }.event-location-preview span,.event-location-preview small { color: #708087; font-size: 9px; }.event-location-preview button { width: 30px; height: 30px; border: 1px solid #cfdadb; border-radius: 5px; background: #fff; color: #176d5d; cursor: pointer; }
.event-list { min-width: 0; }.event-list article { position: relative; padding: 10px 112px 10px 0; border-top: 1px solid #e4e8e9; }.event-list article > div:first-child { display: flex; align-items: center; gap: 6px; }.event-list article strong { font-size: 12px; }.event-list article p { margin: 5px 0; color: #56686f; font-size: 10px; }.event-list article small { display: block; color: #7c888d; font-size: 9px; }.event-place { margin-top: 4px; color: #486970 !important; font-weight: 700; }.event-place i { margin-right: 5px; color: #b24d3f; }.event-card-actions { position: absolute; right: 0; top: 10px; display: flex; gap: 5px; }.event-card-actions button,.event-status { min-height: 28px; border: 1px solid #d8dfe0; border-radius: 4px; background: #fff; color: #7d4c43; padding: 0 7px; font-size: 9px; cursor: pointer; }.event-card-actions button:first-child { width: 30px; padding: 0; color: #176d5d; }.event-status { display: inline-flex; align-items: center; cursor: default; }.severity,.event-list em { padding: 2px 4px; border-radius: 3px; background: #e9f1ef; color: #176d5d; font-size: 8px; font-style: normal; }.severity.high { background: #f6dfda; color: #9a3e32; }.severity.moderate { background: #f8ecd9; color: #8a5c19; }
:deep(.ops-event-marker) { width: 34px; height: 34px; display: grid; place-items: center; border: 3px solid #fff; border-radius: 50%; background: #b74e40; color: #fff; box-shadow: 0 0 0 3px rgba(183,78,64,.28), 0 5px 14px rgba(46,32,29,.3); font-size: 14px; animation: eventPulse 1.8s ease-in-out infinite; }:deep(.ops-event-marker.venue_closed) { background: #754b91; }:deep(.ops-event-marker.heavy_rain) { background: #347498; }@keyframes eventPulse { 50% { transform: scale(1.1); } }
.feedback-table { overflow-x: auto; }.table-head,.table-row { min-width: 860px; display: grid; grid-template-columns: 110px 110px minmax(220px,1.5fr) 150px 90px 90px; gap: 8px; align-items: center; }.table-head { min-height: 34px; border-block: 1px solid #dce3e4; color: #66767d; font-size: 9px; font-weight: 800; }.table-row { min-height: 48px; border-bottom: 1px solid #edf0f0; font-size: 10px; }.table-row button { min-height: 27px; border: 1px solid #176d5d; border-radius: 4px; background: #fff; color: #176d5d; font-size: 9px; cursor: pointer; }.table-row em { color: #63747a; font-style: normal; }.empty-state { padding: 24px; color: #7a878c; text-align: center; font-size: 11px; }.icon-button { width: 32px; height: 32px; border: 1px solid #dce3e4; border-radius: 5px; background: #fff; cursor: pointer; }.ops-message { position: fixed; right: 20px; bottom: 20px; z-index: 10; padding: 10px 13px; border-radius: 5px; background: #173a34; color: #fff; font-size: 11px; }
@media (max-width: 980px) { .event-grid { grid-template-columns: 1fr; }.event-form { padding-right: 0; border-right: 0; border-bottom: 1px solid #e1e7e8; padding-bottom: 18px; } }
@media (max-width: 800px) { .ops-header { align-items: flex-start; padding: 10px 12px; }.ops-header span { display: none; }.demo-actions { flex-wrap: wrap; justify-content: flex-end; }.ops-main { padding: 12px; }.metric-strip { grid-template-columns: repeat(2,1fr); }.metric-strip article { border-bottom: 1px solid #e3e8e9; }.overview-grid { grid-template-columns: 1fr; }.heat-map { height: 280px; }.form-grid { grid-template-columns: 1fr; }.poi-check-list { grid-template-columns: 1fr; } }
</style>

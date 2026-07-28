<template>
  <div class="explore-page">
    <header class="explore-header">
      <div class="brand-block">
        <div class="brand-mark"><i class="fas fa-compass"></i></div>
        <div>
          <h1>{{ t('brand') }}</h1>
          <p>{{ t('subtitle') }}</p>
        </div>
      </div>
      <div class="header-actions">
        <div class="header-language" :aria-label="t('languageSelector')">
          <button v-for="option in languages" :key="option.value"
                  :class="{ active: filters.lang === option.value }"
                  @click="setLanguage(option.value)">{{ option.label }}</button>
        </div>
        <a class="header-link" :href="sessionActionHref" :title="sessionActionTitle">
          <i :class="isGuest ? 'fas fa-user-clock' : 'fas fa-comments'"></i>
          <span class="desktop-session-label">{{ isGuest ? t('guestAction') : t('assistant') }}</span>
          <span class="mobile-session-label">{{ isGuest ? t('guestActionMobile') : t('assistantMobile') }}</span>
        </a>
      </div>
    </header>

    <main class="explore-main">
      <aside class="catalog-panel">
        <div class="panel-heading">
          <div>
            <span class="eyebrow">{{ t('discovery') }}</span>
            <h2>{{ t('explore') }}</h2>
          </div>
          <button class="icon-button" :title="t('reload')" @click="loadCatalog" :disabled="loading">
            <i class="fas fa-rotate-right"></i>
          </button>
        </div>

        <div class="workspace-tabs three-tabs" role="tablist" :aria-label="t('workspace')">
          <button role="tab" :aria-selected="activePanel === 'preferences'"
                  :class="{ active: activePanel === 'preferences' }" @click="activePanel = 'preferences'">
            <i class="fas fa-sliders"></i> {{ t('preferences') }}
          </button>
          <button role="tab" :aria-selected="activePanel === 'catalog'"
                  :class="{ active: activePanel === 'catalog' }" @click="activePanel = 'catalog'">
            <i class="fas fa-list"></i> {{ t('catalog') }}
          </button>
          <button role="tab" :aria-selected="activePanel === 'trip'"
                  :class="{ active: activePanel === 'trip' }" @click="activePanel = 'trip'">
            <i class="fas fa-route"></i> {{ t('itinerary') }}
          </button>
        </div>

        <PreferencesPanel v-if="activePanel === 'preferences'" :pois="poiOptions" :language="filters.lang"
                          @language-change="applyPreferenceLanguage" />

        <TripPanel v-else-if="activePanel === 'trip'" :language="filters.lang" :active-events="activeEvents" :transit-plans="transitPlans"
                   @trip-change="handleTripChange" />

        <template v-else>

        <label class="search-box">
          <i class="fas fa-magnifying-glass"></i>
          <input v-model.trim="filters.q" @keyup.enter="loadCatalog" :placeholder="t('searchPlaceholder')" />
          <button v-if="filters.q" class="clear-search" :title="t('clearSearch')" @click="clearSearch">
            <i class="fas fa-xmark"></i>
          </button>
        </label>

        <div class="filter-group">
          <span class="filter-label">{{ t('region') }}</span>
          <div class="chip-row">
            <button :class="{ active: !filters.region }" @click="setFilter('region', '')">{{ t('all') }}</button>
            <button v-for="region in regions" :key="region"
                    :class="{ active: filters.region === region }"
                    @click="setFilter('region', region)">{{ regionLabel(region) }}</button>
          </div>
        </div>

        <div class="filter-group">
          <span class="filter-label">{{ t('category') }}</span>
          <select v-model="filters.category" @change="loadCatalog">
            <option value="">{{ t('allCategories') }}</option>
            <option v-for="category in categories" :key="category" :value="category">
              {{ categoryLabel(category) }}
            </option>
          </select>
        </div>

        <div class="catalog-status" v-if="loading"><span class="spinner"></span> {{ t('loadingCatalog') }}</div>
        <div class="catalog-status error" v-else-if="error">
          <i class="fas fa-circle-exclamation"></i> {{ error }}
        </div>
        <div class="poi-list" v-else>
          <button v-for="poi in items" :key="poi.poi_code" class="poi-row"
                  :class="{ selected: selectedPoi?.poi_code === poi.poi_code }"
                  @click="selectPoi(poi)">
            <span class="poi-icon" :style="{ backgroundColor: categoryColors[poi.category] || '#177c68' }">
              <i :class="categoryIcons[poi.category] || 'fas fa-location-dot'"></i>
            </span>
            <span class="poi-copy">
              <strong>{{ poi.name }}</strong>
              <small>{{ regionLabel(poi.region) }} · {{ categoryLabel(poi.category) }}</small>
            </span>
            <i class="fas fa-chevron-right row-arrow"></i>
          </button>
          <div class="empty-list" v-if="!items.length">
            <i class="fas fa-map-location-dot"></i>
            <p>{{ t('emptyCatalog') }}</p>
          </div>
        </div>
        </template>
      </aside>

      <section class="map-stage">
        <div id="catalog-map" class="catalog-map"></div>
        <nav class="map-tools" :aria-label="t('accessibilityTools')">
          <button :class="{ active: accessibility.largeText }" :title="t('largeText')" @click="toggleAccessibility('largeText')"><i class="fas fa-text-height"></i></button>
          <button :class="{ active: accessibility.highContrast }" :title="t('highContrast')" @click="toggleAccessibility('highContrast')"><i class="fas fa-circle-half-stroke"></i></button>
          <button :class="{ active: accessibility.simplified }" :title="t('simplifiedView')" @click="toggleAccessibility('simplified')"><i class="fas fa-minimize"></i></button>
          <RouterLink to="/ops" title="运营看板"><i class="fas fa-chart-line"></i></RouterLink>
        </nav>
        <section v-if="latestEvent" class="live-event" :class="latestEvent.severity.toLowerCase()">
          <div><i class="fas fa-triangle-exclamation"></i><strong>{{ latestEvent.title }}</strong><span v-if="latestEvent.simulated">SIM</span></div>
          <p>{{ latestEvent.description }}</p>
          <p class="event-location"><i class="fas fa-location-dot"></i>{{ t('affectedLocation') }}：{{ latestEventLocation }}</p>
          <button class="locate-event" @click="focusLatestEvent"><i class="fas fa-crosshairs"></i>{{ t('locateEvent') }}</button>
          <button class="icon-button" :title="t('close')" @click="latestEvent = null"><i class="fas fa-xmark"></i></button>
        </section>
        <div class="map-loading" v-if="mapLoading">
          <span class="spinner dark"></span> {{ t('loadingMap') }}
        </div>
        <div class="map-error" v-if="mapError">
          <i class="fas fa-triangle-exclamation"></i>
          <strong>{{ t('mapUnavailable') }}</strong>
          <span>{{ mapError }}</span>
        </div>

        <div class="map-legend">
          <span v-for="category in visibleLegend" :key="category">
            <i :style="{ backgroundColor: categoryColors[category] }"></i>{{ categoryLabel(category) }}
          </span>
        </div>

        <article class="detail-panel" v-if="selectedPoi">
          <button class="detail-close icon-button" :title="t('closeDetails')" @click="selectedPoi = null">
            <i class="fas fa-xmark"></i>
          </button>
          <div class="detail-meta">
            <span>{{ regionLabel(selectedPoi.region) }}</span>
            <span>{{ categoryLabel(selectedPoi.category) }}</span>
            <span v-if="selectedPoi.natural_merchant">{{ t('naturalMerchant') }}</span>
          </div>
          <h3>{{ selectedPoi.name }}</h3>
          <p>{{ selectedPoi.description }}</p>
          <dl>
            <div><dt><i class="far fa-clock"></i> {{ t('openingHours') }}</dt><dd>{{ openingHoursLabel(selectedPoi.opening_hours) }}</dd></div>
            <div><dt><i class="fas fa-universal-access"></i> {{ t('accessibility') }}</dt><dd>{{ accessibilityLabel(selectedPoi.accessibility_level) }}</dd></div>
            <div><dt><i class="fas fa-building-columns"></i> {{ t('dataSource') }}</dt><dd>{{ sourceOrganizationLabel(selectedPoi.source_organization) }}</dd></div>
            <div><dt><i class="far fa-calendar-check"></i> {{ t('validUntil') }}</dt><dd>{{ t('until') }} {{ selectedPoi.valid_until }}</dd></div>
          </dl>
          <a :href="selectedPoi.source_url" target="_blank" rel="noopener noreferrer" class="source-link">
            {{ t('viewSource') }} <i class="fas fa-arrow-up-right-from-square"></i>
          </a>
        </article>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { eventStreamUrl, fetchActiveEvents, fetchCatalogPois } from '../utils/api.js'
import { localizeOpeningHours, translate } from '../utils/i18n.js'
import PreferencesPanel from '../components/PreferencesPanel.vue'
import TripPanel from '../components/TripPanel.vue'

const MACAU_CENTER = [113.5554, 22.1654]
const MACAU_BOUNDS = { west: 113.52, east: 113.61, south: 22.10, north: 22.23 }
const items = ref([])
const poiOptions = ref([])
const regions = ref([])
const categories = ref([])
const loading = ref(false)
const error = ref('')
const mapLoading = ref(true)
const mapError = ref('')
const selectedPoi = ref(null)
const isGuest = ref(!localStorage.getItem('token'))
const activePanel = ref('preferences')
const activeEvents = ref([])
const latestEvent = ref(null)
const latestEventLocation = computed(() => {
  if (!latestEvent.value) return ''
  const regionKey = `region_${latestEvent.value.region}`
  const translatedRegion = t(regionKey)
  const region = translatedRegion === regionKey ? latestEvent.value.region : translatedRegion
  const names = (latestEvent.value.affected_poi_codes || []).map(code => poiOptions.value.find(poi => poi.poi_code === code)?.name || code)
  return [region, names.join('、')].filter(Boolean).join(' · ')
})
const transitPlans = ref([])
const accessibility = reactive({ largeText: false, highContrast: false, simplified: false })
const filters = reactive({ region: '', category: '', q: '', lang: 'zh-Hans' })
let map = null
let markers = []
let tripOverlays = []
let eventSource = null
const amapPositions = new Map()
let resolvingPositions = false

const languages = [
  { value: 'zh-Hans', label: '简' },
  { value: 'zh-Hant', label: '繁' },
  { value: 'en', label: 'EN' },
  { value: 'pt', label: 'PT' }
]
const t = (key, params) => translate(filters.lang, key, params)
const sessionActionHref = computed(() => isGuest.value ? '/login?redirect=/explore' : '/home')
const sessionActionTitle = computed(() => isGuest.value
  ? t('guestTitle')
  : t('assistantTitle'))
function syncSessionState() { isGuest.value = !localStorage.getItem('token') }
const regionLabel = region => t(`region_${region}`)
const categoryLabel = category => t(`category_${category}`)
const accessibilityLabel = level => t(`accessibility_${level}`)
const openingHoursLabel = value => localizeOpeningHours(value, filters.lang)
const sourceOrganizationLabel = value => value === '澳门特别行政区政府旅游局' ? t('sourceOrganization') : value
const categoryColors = {
  ATTRACTION: '#c84f3b', CULTURE: '#8a5a9e', NATURE: '#267b5d', TRANSPORT: '#356aa0',
  PUBLIC_SERVICE: '#3f6f76', FOOD: '#b66a2c', RETAIL: '#a04f65', WELLNESS: '#55805f'
}
const categoryMarkerStyles = {
  ATTRACTION: { fill: '#ffd9d0', ink: '#a83228', accent: '#f06b59' },
  CULTURE: { fill: '#eadcf5', ink: '#67357f', accent: '#a875c0' },
  NATURE: { fill: '#d5efdf', ink: '#176b4e', accent: '#54a77e' },
  TRANSPORT: { fill: '#d8e9fa', ink: '#245b92', accent: '#67a0d5' },
  PUBLIC_SERVICE: { fill: '#d5eeee', ink: '#27676c', accent: '#64a7aa' },
  FOOD: { fill: '#ffe4c6', ink: '#9a5018', accent: '#ed9a4b' },
  RETAIL: { fill: '#f8dce7', ink: '#913c60', accent: '#d7799d' },
  WELLNESS: { fill: '#dff0d2', ink: '#456d2d', accent: '#83b661' }
}
const categoryIcons = {
  ATTRACTION: 'fas fa-landmark', CULTURE: 'fas fa-masks-theater', NATURE: 'fas fa-leaf',
  TRANSPORT: 'fas fa-bus', PUBLIC_SERVICE: 'fas fa-circle-info', FOOD: 'fas fa-utensils',
  RETAIL: 'fas fa-store', WELLNESS: 'fas fa-heart-pulse'
}
const visibleLegend = computed(() => categories.value.slice(0, 8))
async function loadCatalog() {
  loading.value = true
  error.value = ''
  try {
    const data = await fetchCatalogPois(filters)
    items.value = data.items
    if (!filters.region && !filters.category && !filters.q) poiOptions.value = data.items
    regions.value = data.regions
    categories.value = data.categories
    if (selectedPoi.value) {
      selectedPoi.value = items.value.find(item => item.poi_code === selectedPoi.value.poi_code) || null
    }
    renderMarkers()
  } catch (err) {
    error.value = t('catalogLoadFailed')
  } finally {
    loading.value = false
  }
}

function setFilter(key, value) {
  filters[key] = value
  loadCatalog()
}

function setLanguage(language) {
  filters.lang = language
  document.documentElement.lang = language
  updateMapLanguage()
  loadCatalog()
}

function applyPreferenceLanguage(language) {
  if (!language || filters.lang === language) return
  filters.lang = language
  document.documentElement.lang = language
  updateMapLanguage()
  loadCatalog()
}

function clearSearch() {
  filters.q = ''
  loadCatalog()
}

function selectPoi(poi) {
  selectedPoi.value = poi
  if (map) {
    const position = amapPositions.get(poi.poi_code)
    if (position) map.setZoomAndCenter(16, position, false, 350)
  }
}

async function focusLatestEvent() {
  const code = latestEvent.value?.affected_poi_codes?.[0]
  const poi = poiOptions.value.find(item => item.poi_code === code)
  if (!poi) return
  await findWithAMap(poi)
  selectPoi(poi)
}

async function waitForAMap() {
  for (let attempt = 0; attempt < 30; attempt += 1) {
    if (window.AMap) return window.AMap
    await new Promise(resolve => setTimeout(resolve, 200))
  }
  throw new Error(t('mapConnectionError'))
}

function loadAMapPoiPlugins(AMap) {
  return new Promise((resolve, reject) => {
    AMap.plugin(['AMap.PlaceSearch', 'AMap.Geocoder', 'AMap.Walking', 'AMap.Transfer'], () => {
      if (AMap.PlaceSearch && AMap.Geocoder && AMap.Walking && AMap.Transfer) resolve()
      else reject(new Error(t('mapServiceError')))
    })
  })
}

function toggleAccessibility(key) {
  accessibility[key] = !accessibility[key]
  const root = document.documentElement
  root.classList.toggle('a11y-large', accessibility.largeText)
  root.classList.toggle('a11y-contrast', accessibility.highContrast)
  root.classList.toggle('a11y-simple', accessibility.simplified)
}

function connectEvents() {
  eventSource?.close()
  eventSource = new EventSource(eventStreamUrl())
  eventSource.addEventListener('tourism-event', raw => {
    try {
      const event = JSON.parse(raw.data)
      const index = activeEvents.value.findIndex(item => item.event_id === event.event_id)
      if (event.status === 'ACTIVE') {
        if (index >= 0) activeEvents.value.splice(index, 1, event)
        else activeEvents.value.unshift(event)
        latestEvent.value = event
      } else if (index >= 0) activeEvents.value.splice(index, 1)
    } catch (_) {}
  })
}

async function handleTripChange(trip) {
  await renderTripRoute(trip)
}

async function renderTripRoute(trip) {
  if (!map || !window.AMap) return
  tripOverlays.forEach(overlay => {
    try { overlay.clear?.(); map.remove?.(overlay) } catch (_) {}
  })
  tripOverlays = []
  transitPlans.value = []
  if (!trip?.stops?.length) return
  const positions = trip.stops.map(stop => [Number(stop.longitude), Number(stop.latitude)])
  trip.stops.forEach((stop, index) => {
    const node = document.createElement('span')
    node.className = 'route-number-marker'
    node.textContent = String(index + 1)
    const marker = new window.AMap.Marker({ position: positions[index], content: node, offset: new window.AMap.Pixel(-11, -11), zIndex: 220 })
    marker.on('click', () => {
      const poi = items.value.find(item => item.poi_code === stop.poi_code)
      if (poi) selectPoi(poi)
    })
    map.add(marker); tripOverlays.push(marker)
  })
  for (let index = 1; index < positions.length; index += 1) {
    const leg = trip.legs[index] || trip.legs[index - 1]
    const transit = await drawAmapLeg(positions[index - 1], positions[index], leg?.mode || 'WALK', leg, index)
    if (transit) transitPlans.value = [...transitPlans.value, transit]
  }
  map.setFitView(tripOverlays.filter(item => item.CLASS_NAME === 'AMap.Marker' || item.getPosition), false, [70, 70, 70, 70], 16)
}

function locationArray(location) {
  if (!location) return null
  if (Array.isArray(location)) return [Number(location[0]), Number(location[1])]
  if (typeof location.getLng === 'function') return [Number(location.getLng()), Number(location.getLat())]
  if (location.lng != null && location.lat != null) return [Number(location.lng), Number(location.lat)]
  return locationArray(location.location)
}

function transitStop(stop) {
  if (!stop) return { name: '', location: null }
  return { name: stop.name || stop.stop_name || '', location: locationArray(stop.location || stop) }
}

function markTransitStop(stop, kind) {
  if (!stop.location) return
  const node = document.createElement('span')
  node.className = `transit-stop-marker ${kind}`
  node.title = stop.name
  node.innerHTML = '<i class="fas fa-bus-simple"></i>'
  const marker = new window.AMap.Marker({ position: stop.location, content: node, offset: new window.AMap.Pixel(-10, -10), zIndex: 230 })
  map.add(marker); tripOverlays.push(marker)
}

function parseTransitPlan(result, leg, legIndex) {
  const plan = result?.plans?.[0]
  if (!plan) return null
  const rides = []
  for (const segment of plan.segments || []) {
    const rawBuslines = segment?.bus?.buslines
    const buslines = Array.isArray(rawBuslines) ? rawBuslines : (rawBuslines ? [rawBuslines] : [])
    for (const line of buslines) {
      const board = transitStop(line.departure_stop || line.departureStop)
      const alight = transitStop(line.arrival_stop || line.arrivalStop)
      const rawViaStops = line.via_stops || line.viaStops || []
      const viaStops = Array.isArray(rawViaStops) ? rawViaStops : [rawViaStops]
      const via = viaStops.map(stop => transitStop(stop).name).filter(Boolean)
      if (!board.name && !alight.name) continue
      markTransitStop(board, 'board'); markTransitStop(alight, 'alight')
      rides.push({
        line_name: line.name || line.bus_name || t('transport_PUBLIC_TRANSIT'),
        boarding_stop: board.name || leg?.from_name,
        boarding_location: board.location,
        alighting_stop: alight.name || leg?.to_name,
        alighting_location: alight.location,
        via_stops: via
      })
    }
  }
  if (!rides.length) return null
  return {
    leg_index: legIndex,
    from_name: leg?.from_name || '',
    to_name: leg?.to_name || '',
    duration_minutes: Math.max(1, Math.ceil(Number(plan.time || 0) / 60)),
    walking_meters: Number(plan.walking_distance || plan.walkingDistance || 0),
    rides
  }
}

function drawAmapLeg(start, end, mode, leg, legIndex) {
  return new Promise(resolve => {
    let done = false
    const finish = value => { if (!done) { done = true; resolve(value || null) } }
    const fallback = () => {
      const line = new window.AMap.Polyline({ path: [start, end], strokeColor: mode === 'WALK' ? '#176d5d' : '#356aa0', strokeWeight: 5, strokeOpacity: .8, strokeStyle: 'dashed' })
      map.add(line); tripOverlays.push(line); finish(null)
    }
    const service = mode === 'PUBLIC_TRANSIT'
      ? new window.AMap.Transfer({ map, city: '澳门', cityd: '澳门', hideMarkers: true, autoFitView: false, extensions: 'all' })
      : new window.AMap.Walking({ map, hideMarkers: true, autoFitView: false })
    tripOverlays.push(service)
    const timeout = setTimeout(() => { service.clear?.(); fallback() }, 5000)
    service.search(start, end, (status, result) => {
      if (done) return
      clearTimeout(timeout)
      if (status !== 'complete') { service.clear?.(); fallback() }
      else finish(mode === 'PUBLIC_TRANSIT' ? parseTransitPlan(result, leg, legIndex) : null)
    })
  })
}

async function initMap() {
  try {
    const AMap = await waitForAMap()
    await loadAMapPoiPlugins(AMap)
    map = new AMap.Map('catalog-map', {
      center: MACAU_CENTER,
      zoom: 12,
      viewMode: '2D',
      mapStyle: 'amap://styles/normal',
      lang: filters.lang === 'zh-Hans' ? 'zh_cn' : 'en',
      resizeEnable: true
    })
    mapLoading.value = false
    renderMarkers()
  } catch (err) {
    mapLoading.value = false
    mapError.value = err.message
  }
}

function updateMapLanguage() {
  if (!map?.setLang) return
  map.setLang(filters.lang === 'zh-Hans' ? 'zh_cn' : 'en')
}

function isInsideMacau(location) {
  if (!location) return false
  const lng = Number(location.lng)
  const lat = Number(location.lat)
  return lng >= MACAU_BOUNDS.west && lng <= MACAU_BOUNDS.east &&
    lat >= MACAU_BOUNDS.south && lat <= MACAU_BOUNDS.north
}

function geocodeWithAMap(poi, resolve) {
  const geocoder = new window.AMap.Geocoder({ city: '澳门特别行政区' })
  geocoder.getLocation(`澳门 ${poi.name}`, (status, result) => {
    const match = status === 'complete' ? result?.geocodes?.find(item => isInsideMacau(item.location)) : null
    if (match?.location) {
      amapPositions.set(poi.poi_code, [Number(match.location.lng), Number(match.location.lat)])
    }
    resolve()
  })
}

function findWithAMap(poi) {
  if (amapPositions.has(poi.poi_code)) return Promise.resolve()
  return new Promise(resolve => {
    let completed = false
    const finish = () => {
      if (completed) return
      completed = true
      clearTimeout(timeout)
      resolve()
    }
    const timeout = setTimeout(finish, 4000)
    const search = new window.AMap.PlaceSearch({
      city: '澳门特别行政区',
      citylimit: true,
      pageSize: 5,
      extensions: 'base'
    })
    search.search(poi.name, (status, result) => {
      const candidates = status === 'complete' ? (result?.poiList?.pois || []) : []
      const exact = candidates.find(item => item.name === poi.name)
      const match = exact || candidates[0]
      if (match?.location && isInsideMacau(match.location)) {
        amapPositions.set(poi.poi_code, [Number(match.location.lng), Number(match.location.lat)])
        finish()
        return
      }
      geocodeWithAMap(poi, finish)
    })
  })
}

async function resolveWithAMap(pois) {
  if (resolvingPositions || !window.AMap?.PlaceSearch || !window.AMap?.Geocoder) return
  const unresolved = pois.filter(poi => !amapPositions.has(poi.poi_code))
  if (!unresolved.length) return
  resolvingPositions = true
  let cursor = 0
  const workers = Array.from({ length: Math.min(8, unresolved.length) }, async () => {
    while (cursor < unresolved.length) {
      const poi = unresolved[cursor]
      cursor += 1
      await findWithAMap(poi)
    }
  })
  await Promise.all(workers)
  resolvingPositions = false
}

async function renderMarkers() {
  if (!map || !window.AMap) return
  for (let attempt = 0; attempt < 3; attempt += 1) {
    await resolveWithAMap(items.value)
    if (items.value.every(poi => amapPositions.has(poi.poi_code))) break
    await new Promise(resolve => setTimeout(resolve, 350))
  }
  if (markers.length) map.remove(markers)
  markers = items.value.flatMap(poi => {
    const position = amapPositions.get(poi.poi_code)
    if (!position) return []
    const node = document.createElement('button')
    node.type = 'button'
    node.className = 'catalog-marker'
    const markerStyle = categoryMarkerStyles[poi.category] || categoryMarkerStyles.ATTRACTION
    node.style.setProperty('--marker-fill', markerStyle.fill)
    node.style.setProperty('--marker-ink', markerStyle.ink)
    node.style.setProperty('--marker-accent', markerStyle.accent)
    node.style.backgroundColor = markerStyle.fill
    node.style.color = markerStyle.ink
    node.style.borderColor = markerStyle.accent
    node.title = poi.name
    const icon = document.createElement('i')
    icon.className = categoryIcons[poi.category] || 'fas fa-location-dot'
    icon.style.color = markerStyle.ink
    node.appendChild(icon)
    const marker = new window.AMap.Marker({
      position,
      content: node,
      title: poi.name,
      offset: new window.AMap.Pixel(-12, -24),
      zIndex: 120
    })
    marker.on('click', () => selectPoi(poi))
    return [marker]
  })
  if (markers.length) {
    map.add(markers)
    if (!filters.region && !filters.category && !filters.q) {
      map.setCenter(MACAU_CENTER)
      map.setZoom(12)
    } else {
      map.setFitView(markers, false, [70, 70, 70, 390], 15)
    }
  }
}

watch(() => selectedPoi.value, value => {
  if (!value && map && !filters.region && !filters.category && !filters.q) {
    map.setZoomAndCenter(12, MACAU_CENTER)
  }
})

onMounted(async () => {
  window.addEventListener('tourism-session-change', syncSessionState)
  document.documentElement.lang = filters.lang
  await nextTick()
  const eventsPromise = fetchActiveEvents().then(data => { activeEvents.value = data || [] }).catch(() => {})
  await Promise.all([loadCatalog(), initMap(), eventsPromise])
  connectEvents()
})

onBeforeUnmount(() => {
  window.removeEventListener('tourism-session-change', syncSessionState)
  eventSource?.close()
  document.documentElement.classList.remove('a11y-large', 'a11y-contrast', 'a11y-simple')
  if (map) map.destroy()
})
</script>

<style>
:root { --ink: #17242d; --muted: #66757f; --line: #dce3e4; --paper: #f4f7f6; --green: #176d5d; }
* { box-sizing: border-box; }
body { margin: 0; }
.explore-page { min-height: 100vh; height: 100vh; display: flex; flex-direction: column; background: var(--paper); color: var(--ink); font-family: Inter, "Noto Sans SC", "Microsoft YaHei", sans-serif; }
.explore-header { height: 68px; flex: 0 0 68px; display: flex; align-items: center; justify-content: space-between; padding: 0 22px; background: #fff; border-bottom: 1px solid var(--line); z-index: 20; }
.brand-block { display: flex; align-items: center; gap: 11px; min-width: 0; }
.brand-mark { width: 38px; height: 38px; display: grid; place-items: center; background: #176d5d; color: #fff; border-radius: 6px; }
.brand-block h1 { margin: 0; font-size: 18px; line-height: 1.25; letter-spacing: 0; }
.brand-block p { margin: 3px 0 0; color: var(--muted); font-size: 12px; letter-spacing: 0; }
.header-actions { display: flex; align-items: center; gap: 10px; }
.header-link { min-height: 36px; display: inline-flex; align-items: center; gap: 7px; padding: 0 11px; border: 1px solid var(--line); border-radius: 6px; font-size: 13px; background: #fff; }
.header-link { color: var(--green); text-decoration: none; font-weight: 700; }
.mobile-session-label { display: none; }
.header-language { display: grid; grid-template-columns: repeat(4, 38px); padding: 3px; border: 1px solid var(--line); border-radius: 6px; background: #fff; }
.header-language button { height: 28px; border: 0; border-radius: 4px; background: transparent; color: var(--muted); font-size: 12px; font-weight: 700; cursor: pointer; }
.header-language button.active { background: #e4f0ed; color: var(--green); }
.explore-main { flex: 1; min-height: 0; display: grid; grid-template-columns: minmax(310px, 360px) 1fr; }
.catalog-panel { min-width: 0; display: flex; flex-direction: column; padding: 18px; background: #fff; border-right: 1px solid var(--line); overflow: hidden; z-index: 10; }
.panel-heading { display: flex; justify-content: space-between; align-items: start; margin-bottom: 14px; }
.eyebrow { color: var(--green); font-size: 10px; font-weight: 800; }
.panel-heading h2 { margin: 3px 0 0; font-size: 24px; letter-spacing: 0; }
.icon-button { width: 34px; height: 34px; border: 1px solid var(--line); background: #fff; color: #53646d; border-radius: 6px; cursor: pointer; }
.workspace-tabs { flex: 0 0 auto; display: grid; grid-template-columns: 1fr 1fr; gap: 3px; padding: 3px; margin-bottom: 12px; border: 1px solid var(--line); border-radius: 6px; background: #f5f7f7; }
.workspace-tabs.three-tabs { grid-template-columns: repeat(3, 1fr); }
.workspace-tabs button { height: 32px; border: 0; border-radius: 4px; background: transparent; color: #68777e; cursor: pointer; font-weight: 800; }
.workspace-tabs button.active { background: #fff; color: var(--green); box-shadow: 0 1px 4px rgba(24,49,57,.09); }
.workspace-tabs i { margin-right: 5px; }
.search-box { height: 40px; display: flex; align-items: center; gap: 9px; border: 1px solid var(--line); border-radius: 6px; padding: 0 10px; margin-bottom: 13px; }
.search-box i { color: #87959c; }
.search-box input { min-width: 0; flex: 1; height: 36px; border: 0; outline: 0; font-size: 14px; }
.clear-search { border: 0; background: transparent; cursor: pointer; }
.filter-group { margin-bottom: 12px; }
.filter-label { display: block; margin-bottom: 7px; color: var(--muted); font-size: 11px; font-weight: 800; }
.chip-row { display: flex; gap: 5px; flex-wrap: wrap; }
.chip-row button { height: 30px; border: 1px solid var(--line); border-radius: 5px; background: #fff; color: #52636b; padding: 0 9px; cursor: pointer; }
.chip-row button.active { color: #fff; background: var(--green); border-color: var(--green); }
.filter-group select { width: 100%; height: 36px; border: 1px solid var(--line); border-radius: 6px; padding: 0 9px; color: var(--ink); background: #fff; }
.catalog-status { padding: 24px 8px; text-align: center; color: var(--muted); font-size: 13px; }
.catalog-status.error { color: #a13c2f; }
.spinner { display: inline-block; width: 14px; height: 14px; margin-right: 6px; border: 2px solid #b8cfca; border-top-color: var(--green); border-radius: 50%; animation: spin .7s linear infinite; vertical-align: -2px; }
.spinner.dark { width: 20px; height: 20px; }
@keyframes spin { to { transform: rotate(360deg); } }
.poi-list { flex: 1; min-height: 0; overflow-y: auto; border-top: 1px solid var(--line); }
.poi-row { width: 100%; min-height: 68px; display: grid; grid-template-columns: 36px 1fr 16px; align-items: center; gap: 10px; padding: 10px 5px; background: #fff; border: 0; border-bottom: 1px solid #edf0f0; text-align: left; cursor: pointer; }
.poi-row:hover, .poi-row.selected { background: #f0f7f5; }
.poi-icon { width: 34px; height: 34px; display: grid; place-items: center; border-radius: 5px; color: #fff; }
.poi-copy { min-width: 0; display: block; }
.poi-copy strong { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; }
.poi-copy small { display: block; margin-top: 5px; color: var(--muted); font-size: 11px; }
.row-arrow { color: #a6b0b5; font-size: 11px; }
.empty-list { padding: 40px 10px; text-align: center; color: var(--muted); }
.empty-list i { font-size: 28px; }
.map-stage { min-width: 0; min-height: 0; position: relative; overflow: hidden; }
.catalog-map { width: 100%; height: 100%; }
.map-tools { position: absolute; top: 12px; right: 12px; z-index: 310; display: flex; gap: 4px; padding: 4px; border: 1px solid var(--line); border-radius: 6px; background: #fff; box-shadow: 0 2px 8px rgba(18,38,44,.12); }
.map-tools button,.map-tools a { width: 32px; height: 32px; display: grid; place-items: center; border: 0; border-radius: 4px; background: transparent; color: #506169; cursor: pointer; text-decoration: none; }.map-tools button.active { background: #176d5d; color: #fff; }
.live-event { position: absolute; top: 62px; left: 50%; z-index: 320; width: min(580px, calc(100% - 32px)); transform: translateX(-50%); padding: 11px 48px 12px 13px; border: 1px solid #db9f8e; border-radius: 6px; background: #fff7f4; color: #753d32; box-shadow: 0 8px 24px rgba(40,30,26,.14); }.live-event.high { border-color: #a63e34; background: #fae8e5; }.live-event > div { display: flex; align-items: center; gap: 7px; }.live-event div span { padding: 2px 4px; background: #a64b3d; color: #fff; font-size: 8px; }.live-event p { margin: 5px 0 0; font-size: 11px; }.live-event .event-location { display: flex; align-items: center; gap: 6px; padding: 6px 8px; border-radius: 4px; background: rgba(166,75,61,.08); color: #62382f; font-weight: 800; }.live-event .event-location i { color: #b1483b; }.live-event > .icon-button { position: absolute; top: 8px; right: 8px; }.live-event .locate-event { position: static; min-height: 30px; margin-top: 8px; border: 1px solid #b9695c; border-radius: 5px; background: #fff; color: #8f4035; padding: 0 9px; font-size: 10px; font-weight: 800; cursor: pointer; }.live-event .locate-event i { margin-right: 5px; }
:deep(.route-number-marker) { width: 22px; height: 22px; display: grid; place-items: center; border: 2px solid #fff; border-radius: 50%; background: #176d5d; color: #fff; box-shadow: 0 2px 6px rgba(0,0,0,.28); font-size: 10px; font-weight: 900; }
:deep(.transit-stop-marker) { width: 20px; height: 20px; display: grid; place-items: center; border: 2px solid #fff; border-radius: 4px; background: #23769a; color: #fff; box-shadow: 0 2px 6px rgba(0,0,0,.25); font-size: 9px; }:deep(.transit-stop-marker.alight) { background: #b75b43; }
:global(.a11y-large) { font-size: 118%; }:global(.a11y-large) button,:global(.a11y-large) input,:global(.a11y-large) select { font-size: 1em; }
:global(.a11y-contrast) .explore-page { --ink: #000; --muted: #2b2b2b; --line: #555; --paper: #fff; --green: #005f42; }
:global(.a11y-simple) .map-legend,:global(.a11y-simple) .detail-panel,:global(.a11y-simple) .brand-block p { display: none; }
.map-loading, .map-error { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; gap: 9px; background: rgba(244,247,246,.94); z-index: 7; }
.map-error { flex-direction: column; color: #8f3a2e; }
.map-legend { position: absolute; top: 14px; left: 14px; max-width: calc(100% - 28px); display: flex; flex-wrap: wrap; gap: 5px 10px; padding: 8px 10px; background: rgba(255,255,255,.95); border: 1px solid var(--line); border-radius: 6px; box-shadow: 0 5px 16px rgba(26,42,49,.09); font-size: 11px; z-index: 5; }
.map-legend span { display: inline-flex; align-items: center; gap: 4px; }
.map-legend i { width: 8px; height: 8px; border-radius: 50%; }
.detail-panel { position: absolute; right: 18px; bottom: 18px; width: min(390px, calc(100% - 36px)); max-height: calc(100% - 90px); overflow-y: auto; padding: 18px; background: #fff; border: 1px solid var(--line); border-radius: 8px; box-shadow: 0 14px 36px rgba(20,42,51,.18); z-index: 6; }
.detail-close { position: absolute; top: 12px; right: 12px; }
.detail-meta { display: flex; flex-wrap: wrap; gap: 5px; padding-right: 40px; }
.detail-meta span { padding: 4px 7px; border-radius: 4px; background: #eaf2f0; color: var(--green); font-size: 10px; font-weight: 800; }
.detail-panel h3 { margin: 13px 40px 6px 0; font-size: 21px; letter-spacing: 0; }
.detail-panel > p { margin: 0 0 13px; color: #51626b; line-height: 1.6; font-size: 13px; }
.detail-panel dl { margin: 0; border-top: 1px solid var(--line); }
.detail-panel dl div { display: grid; grid-template-columns: 92px 1fr; gap: 9px; padding: 8px 0; border-bottom: 1px solid #edf0f0; font-size: 12px; }
.detail-panel dt { color: var(--muted); }
.detail-panel dt i { width: 16px; }
.detail-panel dd { margin: 0; color: var(--ink); }
.source-link { display: inline-flex; align-items: center; gap: 7px; margin-top: 12px; color: var(--green); text-decoration: none; font-weight: 800; font-size: 12px; }
.catalog-marker { position: relative; width: 24px; height: 24px; display: grid; place-items: center; box-sizing: border-box; border: 2px solid var(--marker-accent); border-radius: 48% 52% 46% 54%; background: var(--marker-fill); color: var(--marker-ink); box-shadow: 0 2px 0 rgba(39,62,72,.18), 0 4px 8px rgba(31,53,62,.2); cursor: pointer; transform: rotate(-3deg); transition: transform .16s ease, box-shadow .16s ease; }
.catalog-marker::before { content: ''; position: absolute; top: 3px; left: 4px; width: 4px; height: 3px; border-radius: 50%; background: rgba(255,255,255,.82); transform: rotate(-25deg); }
.catalog-marker::after { content: ''; position: absolute; right: 1px; bottom: -4px; width: 7px; height: 7px; border-right: 2px solid var(--marker-accent); border-bottom: 2px solid var(--marker-accent); background: var(--marker-fill); transform: rotate(38deg); border-radius: 1px 1px 3px 1px; z-index: -1; }
.catalog-marker:hover { transform: translateY(-2px) rotate(2deg) scale(1.08); box-shadow: 0 3px 0 rgba(39,62,72,.17), 0 7px 12px rgba(31,53,62,.23); }
.catalog-marker i { position: relative; z-index: 1; color: var(--marker-ink); font-size: 10px; filter: drop-shadow(0 1px 0 rgba(255,255,255,.8)); }
@media (max-width: 760px) {
  .explore-header { height: 60px; flex-basis: 60px; padding: 0 12px; }
  .brand-block h1 { font-size: 15px; }
  .brand-block p { display: none; }
  .brand-mark { display: none; }
  .header-actions { gap: 6px; }
  .header-language { grid-template-columns: repeat(4, 31px); }
  .header-link { padding: 0 9px; }
  .desktop-session-label { display: none; }
  .mobile-session-label { display: inline; }
  .explore-main { grid-template-columns: 1fr; grid-template-rows: 44% 56%; }
  .catalog-panel { border-right: 0; border-bottom: 1px solid var(--line); padding: 12px; }
  .panel-heading { margin-bottom: 8px; }
  .panel-heading h2 { font-size: 18px; }
  .eyebrow, .filter-label { display: none; }
  .workspace-tabs { margin-bottom: 8px; }
  .search-box { width: 100%; margin-bottom: 8px; }
  .filter-group { margin-bottom: 7px; }
  .filter-group select { height: 32px; }
  .chip-row { flex-wrap: nowrap; overflow-x: auto; }
  .chip-row button { flex: 0 0 auto; }
  .poi-row { min-height: 58px; }
  .map-legend { display: none; }
  .detail-panel { right: 10px; bottom: 10px; width: calc(100% - 20px); max-height: calc(100% - 20px); }
}
</style>
